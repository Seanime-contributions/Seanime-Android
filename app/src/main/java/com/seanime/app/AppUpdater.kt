package com.seanime.app

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern
import kotlin.concurrent.thread

object AppUpdater {

    private const val RELEASES_URL = "https://api.github.com/repos/Pal-droid/Seanime-Android/releases"
    private const val PERMISSION_REQUEST_CODE = 1001

    private lateinit var activity: Activity
    private lateinit var webView: WebView
    private val mainHandler = Handler(Looper.getMainLooper())

    // Flags for permission handling
    private var pendingDownloadAfterPermission = false
    private var pendingInstallAfterPermission = false
    private var pendingCurrentVersion: String? = null // store version while waiting for permission

    fun init(activity: Activity, webView: WebView) {
        this.activity = activity
        this.webView = webView
        webView.addJavascriptInterface(Bridge, "AppUpdater")
    }

    fun inject(webView: WebView) {
        val js = """
            (function() {
                if (window.__seanime_appupdater_init) return;
                window.__seanime_appupdater_init = true;

                if (!document.getElementById('__seanime_appupdater_styles')) {
                    var style = document.createElement('style');
                    style.id = '__seanime_appupdater_styles';
                    style.textContent = '@keyframes __sau_spin { to { transform: rotate(360deg); } }';
                    document.head.appendChild(style);
                }

                // Helper to get current version from the modal's H4
                function getCurrentVersion() {
                    var h4 = document.querySelector('h4.font-bold');
                    if (h4) {
                        var spans = h4.querySelectorAll('span');
                        if (spans.length >= 1) {
                            return spans[0].textContent.trim(); // first span = current version
                        }
                    }
                    return null;
                }

                function findUpdateNowBtn() {
                    return Array.from(document.querySelectorAll('button')).find(function(btn) {
                        var span = btn.querySelector('span.md\\:inline-block');
                        return span && span.textContent.trim() === 'Update now';
                    });
                }

                function findDownloadBtn() {
                    return Array.from(document.querySelectorAll('button')).find(function(btn) {
                        var span = btn.querySelector('span.md\\:inline-block');
                        return span && span.textContent.trim() === 'Download';
                    });
                }

                function patchUpdateNowBtn(btn) {
                    if (btn.dataset.sauPatched) return;
                    var clone = btn.cloneNode(true);
                    clone.dataset.sauPatched = 'true';
                    clone.addEventListener('click', function(e) {
                        e.stopPropagation(); e.preventDefault();
                        var currentVer = getCurrentVersion();
                        if (window.AppUpdater) window.AppUpdater.startUpdate(currentVer);
                    });
                    btn.parentNode.replaceChild(clone, btn);
                }

                function patchDownloadBtn(btn) {
                    if (btn.dataset.sauPatched) return;
                    var clone = btn.cloneNode(true);
                    clone.dataset.sauPatched = 'true';
                    clone.dataset.sauDownloadBtn = 'true';
                    clone.addEventListener('click', function(e) {
                        e.stopPropagation(); e.preventDefault();
                        clone.disabled = true;
                        clone.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="animation:__sau_spin 0.8s linear infinite;flex-shrink:0;"><path d="M21 12a9 9 0 1 1-6.219-8.56"/></svg>';
                        var currentVer = getCurrentVersion(); // optional, not used for download
                        if (window.AppUpdater) window.AppUpdater.startDownload(currentVer);
                    });
                    btn.parentNode.replaceChild(clone, btn);
                }

                function scanButtons() {
                    var updateBtn = findUpdateNowBtn(); if (updateBtn) patchUpdateNowBtn(updateBtn);
                    var dlBtn = findDownloadBtn(); if (dlBtn) patchDownloadBtn(dlBtn);
                }

                scanButtons();
                new MutationObserver(scanButtons).observe(document.body, { childList: true, subtree: true });
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, retry pending download/update
                if (pendingDownloadAfterPermission) {
                    pendingDownloadAfterPermission = false
                    Bridge.startDownload(pendingCurrentVersion)
                } else if (pendingInstallAfterPermission) {
                    pendingInstallAfterPermission = false
                    Bridge.startUpdate(pendingCurrentVersion)
                }
                pendingCurrentVersion = null
            } else {
                Toast.makeText(activity, "Storage permission denied. Cannot download to public folder.", Toast.LENGTH_LONG).show()
                pendingDownloadAfterPermission = false
                pendingInstallAfterPermission = false
                pendingCurrentVersion = null
            }
        }
    }

    object Bridge {
        @JavascriptInterface
        fun startUpdate(currentVersion: String?) {
            mainHandler.post {
                if (!checkStoragePermission(installAfter = true)) {
                    pendingInstallAfterPermission = true
                    pendingCurrentVersion = currentVersion
                    return@post
                }
                val dialog = showUpdatingDialog()
                fetchAndDownload(
                    installAfter = true,
                    currentVersion = currentVersion,
                    onComplete = {
                        mainHandler.post {
                            dialog.dismiss()
                            dismissSeanimeModal()
                        }
                    },
                    onError = { msg ->
                        mainHandler.post {
                            dialog.dismiss()
                            Toast.makeText(activity, "Update failed: $msg", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }

        @JavascriptInterface
        fun startDownload(currentVersion: String?) {
            mainHandler.post {
                if (!checkStoragePermission(installAfter = false)) {
                    pendingDownloadAfterPermission = true
                    pendingCurrentVersion = currentVersion
                    return@post
                }
                fetchAndDownload(
                    installAfter = false,
                    currentVersion = currentVersion, // not used for download but passed for consistency
                    onComplete = {
                        mainHandler.post {
                            Toast.makeText(activity, "Download complete", Toast.LENGTH_SHORT).show()
                            webView.evaluateJavascript("""
                                (function() {
                                    var btn = document.querySelector('[data-sau-download-btn="true"]');
                                    if (!btn) return;
                                    btn.disabled = false;
                                    btn.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink:0;"><polyline points="20 6 9 17 4 12"/></svg><span class="md:inline-block" style="margin-inline-start:0.5rem;">Downloaded</span>';
                                })();
                            """.trimIndent(), null)
                        }
                    },
                    onError = { msg ->
                        mainHandler.post {
                            Toast.makeText(activity, "Download failed: $msg", Toast.LENGTH_LONG).show()
                            webView.evaluateJavascript("""
                                (function() {
                                    var btn = document.querySelector('[data-sau-download-btn="true"]');
                                    if (!btn) return;
                                    btn.disabled = false;
                                    btn.innerHTML = '<span class="md:inline-block">Download</span>';
                                })();
                            """.trimIndent(), null)
                        }
                    }
                )
            }
        }
    }

    private fun checkStoragePermission(installAfter: Boolean): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    true
                } else {
                    activity.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
                    false
                }
            } else {
                true
            }
        } else {
            true
        }
    }

    private fun fetchAndDownload(installAfter: Boolean, currentVersion: String?, onComplete: () -> Unit, onError: (String) -> Unit) {
        thread {
            try {
                // 1. Fetch releases list
                val releaseConn = URL(RELEASES_URL).openConnection() as HttpURLConnection
                releaseConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                releaseConn.setRequestProperty("User-Agent", "Seanime-App-Updater")
                releaseConn.connectTimeout = 15000

                val releaseJson = releaseConn.inputStream.bufferedReader().readText()
                releaseConn.disconnect()

                val releasesArray = JSONArray(releaseJson)
                if (releasesArray.length() == 0) {
                    onError("No releases found")
                    return@thread
                }

                val latestRelease = releasesArray.getJSONObject(0)

                // 2. Find APK asset and extract its version
                val assets = latestRelease.getJSONArray("assets")
                var apkUrl: String? = null
                var apkVersion: String? = null

                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name")
                    if (name.endsWith(".apk")) {
                        apkUrl = asset.getString("browser_download_url")
                        apkVersion = extractVersionFromApkName(name)
                        break
                    }
                }

                if (apkUrl == null) {
                    onError("No APK found in release")
                    return@thread
                }

                // 3. Version check (only for updates)
                if (installAfter) {
                    val baseVersion = currentVersion ?: getCurrentVersionName()
                    if (apkVersion == null || !isNewerVersion(baseVersion, apkVersion)) {
                        onError("No updates available for Android yet, try again later")
                        return@thread
                    }
                }

                // 4. Download the APK
                val dlConn = URL(apkUrl).openConnection() as HttpURLConnection
                dlConn.setRequestProperty("User-Agent", "Seanime-App-Updater")
                dlConn.instanceFollowRedirects = true

                val fileName = apkUrl.substringAfterLast('/').ifEmpty { "update.apk" }
                val uri = dlConn.inputStream.use { inputStream ->
                    saveApkToDownloads(inputStream, fileName)
                }
                dlConn.disconnect()

                if (uri == null) {
                    onError("Could not save APK to Downloads")
                    return@thread
                }

                onComplete()

                if (installAfter) {
                    mainHandler.post { installApk(uri) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Network error")
            }
        }
    }

    /** Extract version from APK filename (e.g., "seanime_v0.1.0a-s3.5.2.apk" → "3.5.2") */
    private fun extractVersionFromApkName(name: String): String? {
        // Look for pattern: s followed by digits and dots, ending before .apk
        val pattern = Pattern.compile("s(\\d+\\.\\d+\\.\\d+)")
        val matcher = pattern.matcher(name)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun getCurrentVersionName(): String {
        return try {
            val pInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            pInfo.versionName ?: "0.0.0"
        } catch (e: Exception) {
            "0.0.0"
        }
    }

    private fun isNewerVersion(currentVersion: String, latestVersion: String): Boolean {
        val cleanCurrent = currentVersion.replace(Regex("^[^0-9]*"), "")
        val cleanLatest = latestVersion.replace(Regex("^[^0-9]*"), "")

        val currentParts = cleanCurrent.split('.').mapNotNull { it.toIntOrNull() }
        val latestParts = cleanLatest.split('.').mapNotNull { it.toIntOrNull() }

        for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
            val cur = if (i < currentParts.size) currentParts[i] else 0
            val lat = if (i < latestParts.size) latestParts[i] else 0
            if (lat > cur) return true
            if (lat < cur) return false
        }
        return false
    }

    private fun saveApkToDownloads(inputStream: java.io.InputStream, fileName: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = activity.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            uri
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            Uri.fromFile(file)
        }
    }

    private fun installApk(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(activity, "Please enable 'Install unknown apps' for Seanime", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
            return
        }

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            activity.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(activity, "Unable to launch installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun dismissSeanimeModal() {
        webView.evaluateJavascript("""
            (function() {
                document.dispatchEvent(new KeyboardEvent('keydown', {
                    key: 'Escape',
                    code: 'Escape',
                    keyCode: 27,
                    which: 27,
                    bubbles: true,
                    cancelable: true
                }));
            })();
        """.trimIndent(), null)
    }

    private fun showUpdatingDialog(): Dialog {
        // (unchanged, same as before)
        val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setCancelable(false)
        val root = FrameLayout(activity).apply { setBackgroundColor(Color.parseColor("#99000000")) }

        val cardW = (activity.resources.displayMetrics.widthPixels * 0.80).toInt()
        val card = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(cardW, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.gravity = Gravity.CENTER }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0f0f14"))
                cornerRadius = 28f
                setStroke(1, Color.parseColor("#22ffffff"))
            }
            setPadding(72, 72, 72, 80)
        }

        val title = TextView(activity).apply {
            text = "Updating Seanime"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL }
        }

        val subtitle = TextView(activity).apply {
            text = "Downloading latest release…"
            setTextColor(Color.parseColor("#88ffffff"))
            textSize = 11f
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                it.topMargin = 72
            }
        }

        val track = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 10).also { it.topMargin = 124 }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1affffff"))
                cornerRadius = 99f
            }
        }

        val fill = View(activity).apply {
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.parseColor("#6366f1"), Color.parseColor("#818cf8"))).apply { cornerRadius = 99f }
        }

        track.addView(fill)
        val barMaxW = cardW - 144
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1100
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val p = anim.animatedValue as Float
                val barW = (barMaxW * 0.42f).toInt()
                val offset = ((barMaxW - barW) * p).toInt()
                fill.layoutParams = FrameLayout.LayoutParams(barW, 10).also { it.leftMargin = offset }
                fill.requestLayout()
            }
        }

        animator.start()
        dialog.setOnDismissListener { animator.cancel() }
        card.addView(title); card.addView(subtitle); card.addView(track)
        root.addView(card); dialog.setContentView(root)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
        return dialog
    }
}