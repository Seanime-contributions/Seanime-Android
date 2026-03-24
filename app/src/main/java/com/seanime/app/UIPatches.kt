package com.seanime.app

import android.webkit.WebView

object UIPatches {

    fun inject(webView: WebView) {
        UIBottomNav.inject(webView)
        UIHomePatch.inject(webView)
        UIMangaHomePatch.inject(webView)
        UIEntryPatch.inject(webView)
        UISettingsPatch.inject(webView)
        UITorrentPatch.inject(webView)
        UIDiscoverPatch.inject(webView)
    }
}
