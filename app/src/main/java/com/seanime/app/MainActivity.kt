package com.seanime.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import java.net.URISyntaxException

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var pipManager: PiPManager
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private val retryCountMap = mutableMapOf<WebView, Int>()
    private val MAX_RETRIES = 5
    private val REQUEST_CODE_NOTIFICATIONS = 101

    /** The host that the main WebView serves — navigations to this host stay in-app. */
    private val LOCAL_HOST = "127.0.0.1"

    inner class OrientationBridge {
        @JavascriptInterface
        fun setLandscape(landscape: Boolean) {
            runOnUiThread {
                requestedOrientation = if (landscape) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), REQUEST_CODE_NOTIFICATIONS)
        }

        setupWebView()
        startSeanimeService()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Forward to AppUpdater (handles WRITE_EXTERNAL_STORAGE permission for older Androids)
        AppUpdater.onRequestPermissionsResult(requestCode, grantResults)

        // Handle notification permission (existing)
        if (requestCode == REQUEST_CODE_NOTIFICATIONS &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            sendBroadcast(Intent(SeanimeService.ACTION_NOTIFICATION_PERMISSION_GRANTED))
        }
    }

    private fun startSeanimeService() {
        val intent = Intent(this, SeanimeService::class.java)
        startForegroundService(intent)
    }

    private fun setupWebView() {
        webView = WebView(this)
        setContentView(webView)

        pipManager = PiPManager(this, webView)
        pipManager.registerBridge()

        AppUpdater.init(this, webView)

        webView.addJavascriptInterface(OrientationBridge(), "OrientationBridge")

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            userAgentString = userAgentString.replace("; wv", "")
        }

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false

                // ========== HANDLE ANY INTENT:// SCHEME (for any media player) ==========
                if (uri.scheme == "intent") {
                    return try {
                        val intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                        // Try to start the intent – Android will resolve to the appropriate player
                        startActivity(intent)
                        true // handled
                    } catch (e: URISyntaxException) {
                        e.printStackTrace()
                        true // prevent WebView from loading invalid URI
                    } catch (e: ActivityNotFoundException) {
                        // The app that can handle this intent is not installed
                        val packageName = intent.`package`
                        if (packageName != null) {
                            // Redirect to the Play Store page for that specific app
                            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                            startActivity(marketIntent)
                        } else {
                            // No package specified – show a user-friendly message
                            Toast.makeText(this@MainActivity, "No app found to handle this link", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                }

                // ========== EXISTING HANDLING FOR OTHER URLS ==========
                val host = uri.host ?: return false

                // Keep local Seanime traffic inside the main WebView
                if (host == LOCAL_HOST) return false

                // Everything else → custom popup bottom sheet
                PopupWebViewSheet.show(this@MainActivity, uri.toString())
                return true
            }

            override fun onReceivedError(view: WebView?, errorCode: Int, desc: String?, url: String?) {
                if (view != null && url != null && url == view.url) {
                    retry(view)
                }
            }

            override fun onReceivedError(view: WebView?, req: WebResourceRequest?, err: WebResourceError?) {
                if (req?.isForMainFrame == true) retry(view)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (view != null) retryCountMap[view] = 0
                pipManager.injectHijacker()
                DualModeManager.inject(webView)
                VideoControlInjector.inject(webView)
                UIPatches.inject(webView)
                AppUpdater.inject(webView)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    onHideCustomView()
                    return
                }
                customView = view
                customViewCallback = callback
                val decor = window.decorView as FrameLayout
                decor.addView(customView, FrameLayout.LayoutParams(-1, -1))
                webView.visibility = View.GONE
                toggleSystemBars(true)
            }

            override fun onHideCustomView() {
                val decor = window.decorView as FrameLayout
                decor.removeView(customView)
                customView = null
                customViewCallback?.onCustomViewHidden()
                webView.visibility = View.VISIBLE
                toggleSystemBars(false)
            }
        }

        webView.postDelayed({
            webView.loadUrl("http://127.0.0.1:43211")
        }, 1000)
    }

    private fun retry(view: WebView?) {
        view ?: return
        val count = retryCountMap.getOrDefault(view, 0)
        if (count >= MAX_RETRIES) return
        retryCountMap[view] = count + 1
        val delayMs = (count + 1) * 1000L
        view.postDelayed({ view.reload() }, delayMs)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipManager.onPiPModeChanged(isInPictureInPictureMode)
    }

    private fun toggleSystemBars(hide: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                if (hide) {
                    it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    it.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                }
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (hide) {
                (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            } else View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    override fun onBackPressed() {
        if (customView != null) {
            webView.webChromeClient?.onHideCustomView()
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}