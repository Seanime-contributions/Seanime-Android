package com.seanime.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.seanime.app.R
import com.seanime.app.SeanimeService
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections

class AnimeListService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AnimeListFactory(applicationContext)
    }
}

class AnimeListFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private val animeItems = Collections.synchronizedList(mutableListOf<AnimeEntry>())
    private val cache = WidgetCache(context)
    
    data class AnimeEntry(
        val title: String,
        val episode: Int,
        val timeUntil: Long,
        val mediaId: Int,
        val coverUrl: String,
        var thumbnail: Bitmap?
    )

    override fun onCreate() {
        // Initialize with cache data if available
        loadFromCache()
    }

    override fun onDestroy() {
        synchronized(this) {
            animeItems.clear()
        }
    }

    override fun getCount(): Int = animeItems.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= animeItems.size) return null

        val views = RemoteViews(context.packageName, R.layout.widget_item_anime)
        val item = animeItems[position]

        views.setTextViewText(R.id.anime_title, item.title)
        
        val s = item.timeUntil
        val timeStr = if (s / 86400 > 0) {
            "${s / 86400}d ${(s % 86400) / 3600}h"
        } else {
            "${(s % 86400) / 3600}h ${(s % 3600) / 60}m"
        }
        views.setTextViewText(R.id.anime_timer, "Ep ${item.episode}: $timeStr")

        // Load thumbnail if not cached
        if (item.thumbnail == null) {
            item.thumbnail = loadThumbnail(item)
        }
        
        if (item.thumbnail != null) {
            views.setImageViewBitmap(R.id.anime_cover, item.thumbnail)
        }

        // Deep link intent to the app
        val fillInIntent = Intent()
        fillInIntent.data = Uri.parse("seanime://entry?id=${item.mediaId}")
        views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    override fun onDataSetChanged() {
        // Check if we should use cache or fetch fresh data
        if (cache.hasValidCache() && !cache.isLoading()) {
            loadFromCache()
            return
        }

        cache.setLoading(true)
        try {
            Log.d("SeanimeWidget", "Widget fetch starting (no valid cache)")
            val freshData = fetchFreshData()

            synchronized(this) {
                animeItems.clear()
                animeItems.addAll(freshData)
            }

            cache.saveCacheData(freshData)

            Log.d("SeanimeWidget", "Widget fetch completed: ${freshData.size} items")

            context.sendBroadcast(Intent(context, UpcomingAnimeWidget::class.java).apply {
                action = UpcomingAnimeWidget.ACTION_DATA_UPDATED
            })

            // Close server process (service owns the native process)
            context.stopService(Intent(context, SeanimeService::class.java))
        } catch (e: Exception) {
            Log.e("SeanimeWidget", "Sync Error: ${e.message}")
            cache.setError(e.message ?: "Failed to load data")
            loadFromCache()
        } finally {
            cache.setLoading(false)
        }
    }

    private fun loadFromCache() {
        try {
            val cachedData = cache.getCacheData()
            synchronized(this) {
                animeItems.clear()
                animeItems.addAll(cachedData)
            }
        } catch (e: Exception) {
            Log.e("SeanimeWidget", "Cache load error: ${e.message}")
        }
    }

    private fun fetchFreshData(): List<AnimeEntry> {
        val tempItems = mutableListOf<AnimeEntry>()
        
        try {
            // 1. Get Username
            try {
                Thread.sleep(1000L)
            } catch (_: InterruptedException) {
            }
            Log.d("SeanimeWidget", "Fetching status from localhost...")
            val statusJson = makeRequestWithRetry("http://localhost:43211/api/v1/status", "GET", null)
            val username = JSONObject(statusJson)
                .getJSONObject("data")
                .getJSONObject("user")
                .getJSONObject("viewer")
                .getString("name")
            Log.d("SeanimeWidget", "Local status OK; viewer=$username")

            // 2. Get User ID
            val idQuery = "query { User(name: \"$username\") { id } }"
            val idRes = makeRequest("https://graphql.anilist.co", "POST", 
                JSONObject().put("query", idQuery).toString())
            val userId = JSONObject(idRes)
                .getJSONObject("data")
                .getJSONObject("User")
                .getInt("id")
            Log.d("SeanimeWidget", "AniList userId=$userId")

            // 3. Get Media List
            val listQuery = """
                query ($$userId: Int) { 
                    MediaListCollection(userId: $$userId, type: ANIME) { 
                        lists { 
                            entries { 
                                media { 
                                    id 
                                    title { userPreferred } 
                                    coverImage { medium } 
                                    nextAiringEpisode { timeUntilAiring episode } 
                                } 
                            } 
                        } 
                    } 
                }
            """.trimIndent()
            
            val vars = JSONObject().put("userId", userId)
            val listRes = makeRequest("https://graphql.anilist.co", "POST", 
                JSONObject().put("query", listQuery).put("variables", vars).toString())

            val lists = JSONObject(listRes)
                .getJSONObject("data")
                .getJSONObject("MediaListCollection")
                .getJSONArray("lists")
                
            for (i in 0 until lists.length()) {
                val entries = lists.getJSONObject(i).getJSONArray("entries")
                for (j in 0 until entries.length()) {
                    val media = entries.getJSONObject(j).getJSONObject("media")
                    if (!media.isNull("nextAiringEpisode")) {
                        val next = media.getJSONObject("nextAiringEpisode")
                        
                        val coverUrl = media.getJSONObject("coverImage").getString("medium")
                        val thumb = downloadAndScale(coverUrl)

                        tempItems.add(AnimeEntry(
                            media.getJSONObject("title").getString("userPreferred"),
                            next.getInt("episode"),
                            next.getLong("timeUntilAiring"),
                            media.getInt("id"),
                            coverUrl,
                            thumb
                        ))
                    }
                }
            }
            
            tempItems.sortBy { it.timeUntil }
            
        } catch (e: Exception) {
            Log.e("SeanimeWidget", "Fresh data fetch error: ${e.message}")
            throw e
        }
        
        return tempItems
    }

    private fun loadThumbnail(entry: AnimeEntry): Bitmap? {
        return downloadAndScale(entry.coverUrl)
    }

    private fun makeRequestWithRetry(urlStr: String, method: String, body: String?): String {
        var last: Exception? = null
        // Seanime server can take a few seconds to boot. Give it a wider window.
        for (i in 0 until 40) {
            try {
                return makeRequest(urlStr, method, body)
            } catch (e: Exception) {
                last = e
                try {
                    Thread.sleep(250L)
                } catch (_: InterruptedException) {
                }
            }
        }
        throw last ?: RuntimeException("Request failed")
    }

    private fun makeRequest(urlStr: String, method: String, body: String?): String {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 10000
        
        if (body != null) {
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.outputStream.use { os ->
                os.write(body.toByteArray(Charsets.UTF_8))
            }
        }
        
        val response = conn.inputStream.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                StringBuilder().apply {
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        append(line)
                    }
                }.toString()
            }
        }
        
        conn.disconnect()
        return response
    }

    private fun downloadAndScale(urlStr: String): Bitmap? {
        return try {
            val inputStream = URL(urlStr).openStream()
            val raw = BitmapFactory.decodeStream(inputStream)
            if (raw == null) return null
            
            // Scale to widget dimensions
            val scaled = Bitmap.createScaledBitmap(raw, 150, 220, true)
            
            // Apply 16px rounding to the pixels
            getRoundedCornerBitmap(scaled, 24)
        } catch (e: Exception) {
            null
        }
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, pixels: Int): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val color = 0xff424242.toInt()
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawRoundRect(rectF, pixels.toFloat(), pixels.toFloat(), paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }
}
