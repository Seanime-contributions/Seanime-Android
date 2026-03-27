package com.seanime.app

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import java.net.URI

object PopupWebViewSheet {

    fun show(activity: Activity, url: String) {
        val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        val dp = activity.resources.displayMetrics.density
        val screenH = activity.resources.displayMetrics.heightPixels

        // ── Root scrim ──────────────────────────────────────────────────────
        val root = FrameLayout(activity)
        root.setBackgroundColor(Color.parseColor("#99000000"))

        // ── Card ────────────────────────────────────────────────────────────
        val card = FrameLayout(activity)
        val cardLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (screenH * 0.88).toInt()
        )
        cardLp.gravity = Gravity.BOTTOM
        card.layoutParams = cardLp

        val cardBg = GradientDrawable()
        cardBg.setColor(Color.parseColor("#0f0f14"))
        val r = (24 * dp)
        cardBg.cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        cardBg.setStroke(1, Color.parseColor("#1a1a2e"))
        card.background = cardBg

        // ── Drag handle ─────────────────────────────────────────────────────
        val handle = View(activity)
        val handleLp = FrameLayout.LayoutParams((48 * dp).toInt(), (4 * dp).toInt())
        handleLp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        handleLp.topMargin = (10 * dp).toInt()
        handle.layoutParams = handleLp
        val handleBg = GradientDrawable()
        handleBg.setColor(Color.parseColor("#44ffffff"))
        handleBg.cornerRadius = 99f
        handle.background = handleBg

        // ── Top bar ─────────────────────────────────────────────────────────
        val topBarH = (52 * dp).toInt()
        val topBarTopMargin = (24 * dp).toInt()
        val topBar = FrameLayout(activity)
        val topBarLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, topBarH)
        topBarLp.topMargin = topBarTopMargin
        topBar.layoutParams = topBarLp

        // Host label (centred)
        val urlLabel = TextView(activity)
        urlLabel.text = hostFrom(url)
        urlLabel.setTextColor(Color.parseColor("#88ffffff"))
        urlLabel.textSize = 13f
        urlLabel.typeface = Typeface.DEFAULT
        urlLabel.maxLines = 1
        urlLabel.ellipsize = TextUtils.TruncateAt.MIDDLE
        val urlLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        urlLp.gravity = Gravity.CENTER
        urlLabel.layoutParams = urlLp

        // Close button
        val closeBtn = TextView(activity)
        closeBtn.text = "✕"
        closeBtn.setTextColor(Color.parseColor("#88ffffff"))
        closeBtn.textSize = 17f
        val closeLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        closeLp.gravity = Gravity.CENTER_VERTICAL or Gravity.END
        closeLp.rightMargin = (20 * dp).toInt()
        closeBtn.layoutParams = closeLp
        closeBtn.setOnClickListener { dismissWithAnim(dialog, card, screenH) }

        topBar.addView(urlLabel)
        topBar.addView(closeBtn)

        // ── Divider ─────────────────────────────────────────────────────────
        val dividerTopMargin = topBarTopMargin + topBarH
        val divider = View(activity)
        val dividerLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
        dividerLp.topMargin = dividerTopMargin
        divider.layoutParams = dividerLp
        divider.setBackgroundColor(Color.parseColor("#1affffff"))

        // ── Progress bar ────────────────────────────────────────────────────
        val progressH = (3 * dp).toInt()
        val progress = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal)
        val progressLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, progressH)
        progressLp.topMargin = dividerTopMargin + 1
        progress.layoutParams = progressLp
        progress.max = 100

        // Fix deprecation warning: use PorterDuffColorFilter with colorFilter setter
        val progressColor = Color.parseColor("#818cf8")
        progress.progressDrawable?.colorFilter = PorterDuffColorFilter(progressColor, PorterDuff.Mode.SRC_IN)

        // ── Popup WebView ───────────────────────────────────────────────────
        val contentTopMargin = dividerTopMargin + 1 + progressH
        val popupWebView = WebView(activity)
        val wvLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        wvLp.topMargin = contentTopMargin
        popupWebView.layoutParams = wvLp
        popupWebView.setBackgroundColor(Color.parseColor("#0f0f14"))
        popupWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        popupWebView.webViewClient = object : WebViewClient() {
            // Stay inside the popup for all navigations
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                activity.runOnUiThread {
                    urlLabel.text = hostFrom(url ?: "")
                    progress.visibility = View.VISIBLE
                    progress.progress = 0
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                activity.runOnUiThread { progress.visibility = View.GONE }
            }
        }

        popupWebView.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                activity.runOnUiThread {
                    progress.progress = newProgress
                    if (newProgress >= 100) progress.visibility = View.GONE
                }
            }
        }

        popupWebView.loadUrl(url)

        // ── Assemble ─────────────────────────────────────────────────────────
        card.addView(handle)
        card.addView(topBar)
        card.addView(divider)
        card.addView(progress)
        card.addView(popupWebView)
        root.addView(card)

        root.setOnClickListener { dismissWithAnim(dialog, card, screenH) }
        card.setOnClickListener { /* consume – don't let taps reach scrim */ }

        dialog.setContentView(root)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            statusBarColor = Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setDecorFitsSystemWindows(false)
            }
        }

        // ── Slide-up entrance ────────────────────────────────────────────────
        card.translationY = screenH.toFloat()
        dialog.show()
        card.animate()
            .translationY(0f)
            .setDuration(320)
            .setInterpolator(DecelerateInterpolator(2f))
            .start()
    }

    private fun dismissWithAnim(dialog: Dialog, card: FrameLayout, screenH: Int) {
        card.animate()
            .translationY(screenH.toFloat())
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .withEndAction { dialog.dismiss() }
            .start()
    }

    private fun hostFrom(url: String): String = try {
        URI(url).host ?: url
    } catch (e: Exception) {
        url
    }
}