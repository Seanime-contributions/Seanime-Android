package com.seanime.app

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.animation.DecelerateInterpolator
import android.webkit.*
import android.widget.FrameLayout
import android.widget.TextView

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

    inner class DonateBridge {
        @JavascriptInterface
        fun openDonate() {
            runOnUiThread {
                showDonateDialog()
            }
        }
    }

    private fun showDonateDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.parseColor("#99000000"))

        val card = FrameLayout(this)
        card.setBackgroundColor(Color.TRANSPARENT)

        val cardLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.82).toInt()
        )
        cardLp.gravity = Gravity.BOTTOM
        card.layoutParams = cardLp

        val cardBg = GradientDrawable()
        cardBg.setColor(Color.parseColor("#0f0f14"))
        cardBg.cornerRadii = floatArrayOf(32f, 32f, 32f, 32f, 0f, 0f, 0f, 0f)
        cardBg.setStroke(1, Color.parseColor("#1a1a2e"))
        card.background = cardBg

        val handle = View(this)
        val handleLp = FrameLayout.LayoutParams(120, 10)
        handleLp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        handleLp.topMargin = 20
        handle.layoutParams = handleLp
        val handleBg = GradientDrawable()
        handleBg.setColor(Color.parseColor("#44ffffff"))
        handleBg.cornerRadius = 99f
        handle.background = handleBg

        val topBar = FrameLayout(this)
        val topBarLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 160)
        topBarLp.topMargin = 48
        topBar.layoutParams = topBarLp

        val title = TextView(this)
        title.text = "Support Seanime"
        title.setTextColor(Color.WHITE)
        title.textSize = 17f
        title.typeface = Typeface.DEFAULT_BOLD
        val titleLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        titleLp.gravity = Gravity.CENTER
        title.layoutParams = titleLp

        val closeBtn = TextView(this)
        closeBtn.text = "✕"
        closeBtn.setTextColor(Color.parseColor("#88ffffff"))
        closeBtn.textSize = 18f
        val closeLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        closeLp.gravity = Gravity.CENTER_VERTICAL or Gravity.END
        closeLp.rightMargin = 56
        closeBtn.layoutParams = closeLp
        closeBtn.setOnClickListener { dialog.dismiss() }

        topBar.addView(title)
        topBar.addView(closeBtn)

        val divider = View(this)
        val dividerLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
        dividerLp.topMargin = 208
        divider.layoutParams = dividerLp
        divider.setBackgroundColor(Color.parseColor("#1affffff"))

        val donateWebView = WebView(this)
        val wvLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        wvLp.topMargin = 209
        donateWebView.layoutParams = wvLp
        donateWebView.setBackgroundColor(Color.parseColor("#0f0f14"))
        donateWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        donateWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val css = "body, .application-main, .logged-out { background: #0f0f14 !important; } " +
                        "header, .Header, .js-header-wrapper, footer, .footer { display: none !important; } " +
                        ".container-xl, .container-lg { padding-top: 8px !important; }"
                view?.evaluateJavascript(
                    """(function(){
                        var s = document.createElement('style');
                        s.textContent = '${css.replace("'", "\\'")}';
                        document.head.appendChild(s);
                    })();""", null
                )
            }
        }
        donateWebView.loadUrl("https://github.com/sponsors/5rahim")

        card.addView(handle)
        card.addView(topBar)
        card.addView(divider)
        card.addView(donateWebView)
        root.addView(card)

        root.setOnClickListener { dialog.dismiss() }
        card.setOnClickListener { /* consume tap so root doesn't dismiss */ }

        dialog.setContentView(root)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            statusBarColor = Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setDecorFitsSystemWindows(false)
            }
        }

        card.translationY = resources.displayMetrics.heightPixels.toFloat()
        dialog.show()
        card.animate()
            .translationY(0f)
            .setDuration(320)
            .setInterpolator(DecelerateInterpolator(2f))
            .start()
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

        webView.addJavascriptInterface(OrientationBridge(), "OrientationBridge")
        webView.addJavascriptInterface(DonateBridge(), "DonateBridge")

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
                val url = request?.url ?: return false
                val host = url.host ?: return false

                // Keep local Seanime traffic inside the main WebView
                if (host == LOCAL_HOST) return false

                // Everything else → custom popup bottom sheet
                PopupWebViewSheet.show(this@MainActivity, url.toString())
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
                // FloatingPill removed
                pipManager.injectHijacker()
                DualModeManager.inject(webView)
                VideoControlInjector.inject(webView)
                UIPatches.inject(webView)
                UIHomePatch.inject(webView)
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
