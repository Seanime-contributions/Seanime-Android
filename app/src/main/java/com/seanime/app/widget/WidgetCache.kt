package com.seanime.app.widget

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class WidgetCache(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
    private val editor = prefs.edit()
    
    companion object {
        private const val KEY_CACHE_DATA = "anime_cache_data"
        private const val KEY_CACHE_TIMESTAMP = "cache_timestamp"
        private const val KEY_IS_LOADING = "is_loading"
        private const val KEY_ERROR = "error_message"
        private const val CACHE_DURATION = 30 * 60 * 1000L // 30 minutes in milliseconds
    }
    
    fun saveCacheData(animeEntries: List<AnimeListFactory.AnimeEntry>) {
        try {
            val jsonArray = JSONArray()
            for (entry in animeEntries) {
                val jsonEntry = JSONObject().apply {
                    put("title", entry.title)
                    put("episode", entry.episode)
                    put("timeUntil", entry.timeUntil)
                    put("mediaId", entry.mediaId)
                    put("coverUrl", entry.coverUrl)
                }
                jsonArray.put(jsonEntry)
            }
            
            editor.putString(KEY_CACHE_DATA, jsonArray.toString())
            editor.putLong(KEY_CACHE_TIMESTAMP, System.currentTimeMillis())
            editor.putBoolean(KEY_IS_LOADING, false)
            editor.remove(KEY_ERROR)
            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getCacheData(): List<AnimeListFactory.AnimeEntry> {
        try {
            val cacheData = prefs.getString(KEY_CACHE_DATA, null)
            val timestamp = prefs.getLong(KEY_CACHE_TIMESTAMP, 0)
            
            if (cacheData != null && (System.currentTimeMillis() - timestamp) < CACHE_DURATION) {
                val jsonArray = JSONArray(cacheData)
                val entries = mutableListOf<AnimeListFactory.AnimeEntry>()
                
                for (i in 0 until jsonArray.length()) {
                    val jsonEntry = jsonArray.getJSONObject(i)
                    val entry = AnimeListFactory.AnimeEntry(
                        jsonEntry.getString("title"),
                        jsonEntry.getInt("episode"),
                        jsonEntry.getLong("timeUntil"),
                        jsonEntry.getInt("mediaId"),
                        jsonEntry.optString("coverUrl", ""),
                        null // Thumbnail will be loaded on demand
                    )
                    entries.add(entry)
                }
                
                return entries
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return emptyList()
    }
    
    fun setLoading(isLoading: Boolean) {
        editor.putBoolean(KEY_IS_LOADING, isLoading)
        editor.apply()
    }
    
    fun isLoading(): Boolean {
        return prefs.getBoolean(KEY_IS_LOADING, false)
    }
    
    fun clearCache() {
        editor.remove(KEY_CACHE_DATA)
        editor.remove(KEY_CACHE_TIMESTAMP)
        editor.remove(KEY_IS_LOADING)
        editor.remove(KEY_ERROR)
        editor.apply()
    }

    fun setError(message: String) {
        editor.putString(KEY_ERROR, message)
        editor.putBoolean(KEY_IS_LOADING, false)
        editor.apply()
    }

    fun getError(): String? {
        return prefs.getString(KEY_ERROR, null)
    }

    fun clearError() {
        editor.remove(KEY_ERROR)
        editor.apply()
    }

    fun hasValidCache(): Boolean {
        val timestamp = prefs.getLong(KEY_CACHE_TIMESTAMP, 0)
        return prefs.contains(KEY_CACHE_DATA) &&
               (System.currentTimeMillis() - timestamp) < CACHE_DURATION
    }
}
