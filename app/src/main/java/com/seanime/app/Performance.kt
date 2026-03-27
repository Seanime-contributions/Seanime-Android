package com.seanime.app

import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView

object Performance {

    fun init(context: Context, webView: WebView) {
        applyWebViewSettings(context, webView)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. WebView settings — applied before page load
    // ─────────────────────────────────────────────────────────────────────────

    private fun applyWebViewSettings(context: Context, webView: WebView) {
        val settings: WebSettings = webView.settings

        // ── Rendering ────────────────────────────────────────────────────────
        // Hardware acceleration is set on the View layer; enabling it here ensures
        // the compositor path is fully active.
        webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

        // Use the widest viewport so the page layout matches a desktop/tablet
        // layout engine — avoids double-render from a narrow initial viewport.
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        // ── JavaScript engine ────────────────────────────────────────────────
        @Suppress("SetJavaScriptEnabled")
        settings.javaScriptEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = false // no pop-ups

        // ── Caching ──────────────────────────────────────────────────────────
        // LOAD_DEFAULT honours HTTP cache-control headers; falls back to cache
        // when offline. This avoids unnecessary network round-trips for static assets.
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // ── Network ──────────────────────────────────────────────────────────
        // Allow the page to store data (IndexedDB, localStorage, etc.)
        settings.domStorageEnabled = true
        settings.databaseEnabled   = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        // ── Text / media ─────────────────────────────────────────────────────
        settings.mediaPlaybackRequiresUserGesture = false  // inline video/audio
        settings.loadsImagesAutomatically = true

        // Disable features that add overhead or expand attack surface
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.setSupportMultipleWindows(false)
        settings.allowFileAccess    = false
        settings.allowContentAccess = false

        // ── User-agent tweak ─────────────────────────────────────────────────
        val ua = settings.userAgentString
        if (!ua.contains("SeanimeAndroid")) {
            val versionName = try {
                context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .versionName ?: "0"
            } catch (e: Exception) {
                "0"
            }
            settings.userAgentString = "$ua SeanimeAndroid/$versionName"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. JavaScript runtime optimisations — call from WebViewClient.onPageFinished
    // ─────────────────────────────────────────────────────────────────────────

    fun injectRuntimeOptimizations(webView: WebView) {
        injectScrollOptimizations(webView)
        injectImageLazyLoading(webView)
        injectMemoryPressureHandler(webView)
        injectAnimationThrottling(webView)
        injectNetworkHints(webView)
    }

    private fun injectScrollOptimizations(webView: WebView) {
        val js = """
        (function() {
            if (window.__seanime_scroll_observer) {
                window.__seanime_scroll_observer.disconnect();
            }

            var CSS_SCROLL = [
                '[data-main-layout-content="true"]',
                '.overflow-y-auto',
                '.overflow-x-auto',
                'body'
            ];
            CSS_SCROLL.forEach(function(sel) {
                document.querySelectorAll(sel).forEach(function(el) {
                    el.style.willChange = 'scroll-position';
                });
            });

            var CSS_PROMOTE = [
                '[data-media-entry-card-body="true"]',
                '#__seanime_bottom_nav',
                '[data-media-page-header-entry-details-cover-image-container="true"]'
            ];

            function promoteElements(root) {
                CSS_PROMOTE.forEach(function(sel) {
                    (root.querySelectorAll ? root.querySelectorAll(sel) : []).forEach(function(el) {
                        el.style.willChange = 'transform';
                        el.style.transform  = 'translateZ(0)';
                    });
                    if (root.matches && root.matches(sel)) {
                        root.style.willChange = 'transform';
                        root.style.transform  = 'translateZ(0)';
                    }
                });
            }

            promoteElements(document);

            var promoObserver = new MutationObserver(function(mutations) {
                mutations.forEach(function(m) {
                    m.addedNodes.forEach(function(node) {
                        if (node.nodeType !== 1) return;
                        promoteElements(node);
                    });
                });
            });
            promoObserver.observe(document.body, { childList: true, subtree: true });
            window.__seanime_scroll_observer = promoObserver;
        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun injectImageLazyLoading(webView: WebView) {
        val js = """
        (function() {
            if (window.__seanime_img_observer) {
                window.__seanime_img_observer.disconnect();
            }

            function optimiseImages(root) {
                (root || document).querySelectorAll('img').forEach(function(img) {
                    if (img.loading === 'eager') return;
                    if (!img.hasAttribute('loading'))  img.setAttribute('loading',  'lazy');
                    if (!img.hasAttribute('decoding')) img.setAttribute('decoding', 'async');
                });
            }

            optimiseImages(document);

            var imgObserver = new MutationObserver(function(mutations) {
                mutations.forEach(function(m) {
                    m.addedNodes.forEach(function(node) {
                        if (node.nodeType !== 1) return;
                        if (node.tagName === 'IMG') optimiseImages(node.parentElement);
                        else if (node.querySelector) optimiseImages(node);
                    });
                });
            });
            imgObserver.observe(document.body, { childList: true, subtree: true });
            window.__seanime_img_observer = imgObserver;
        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun injectMemoryPressureHandler(webView: WebView) {
        val js = """
        (function() {
            if (window.__seanime_scroll_mem_handler) {
                window.removeEventListener('scroll', window.__seanime_scroll_mem_handler);
            }

            var scrollTimer = null;
            var scrollHandler = function() {
                document.body.dataset.scrolling = 'true';
                clearTimeout(scrollTimer);
                scrollTimer = setTimeout(function() {
                    delete document.body.dataset.scrolling;
                }, 150);
            };
            window.__seanime_scroll_mem_handler = scrollHandler;
            window.addEventListener('scroll', scrollHandler, { passive: true });

            window.__seanime_onMemoryPressure = function() {
                // 1. Release any object-URL blobs still held in memory.
                document.querySelectorAll('img[src^="blob:"]').forEach(function(img) {
                    try { URL.revokeObjectURL(img.src); } catch(e) {}
                });
                
                // 2. Fire a custom DOM event so the app's own listeners can react.
                // The application should listen for 'seanime:memorypressure' to clear its own caches.
                window.dispatchEvent(new Event('seanime:memorypressure'));
                console.log('[Seanime] Memory pressure signal received — event dispatched.');
            };
        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun injectAnimationThrottling(webView: WebView) {
        val js = """
        (function() {
            var PAUSE_STYLE_ID    = '__seanime_anim_throttle';
            var REDUCED_STYLE_ID  = '__seanime_reduced_motion';
            var SCROLL_STYLE_ID   = '__seanime_scroll_throttle';

            if (window.__seanime_vis_handler) {
                document.removeEventListener('visibilitychange', window.__seanime_vis_handler);
            }

            function setAnimationState(paused) {
                var existing = document.getElementById(PAUSE_STYLE_ID);
                if (paused) {
                    if (existing) return;
                    var s = document.createElement('style');
                    s.id = PAUSE_STYLE_ID;
                    s.textContent = '*, *::before, *::after { animation-play-state: paused !important; transition: none !important; }';
                    document.head.appendChild(s);
                } else {
                    if (existing) existing.remove();
                }
            }

            var visHandler = function() { setAnimationState(document.hidden); };
            window.__seanime_vis_handler = visHandler;
            document.addEventListener('visibilitychange', visHandler);
            setAnimationState(document.hidden);

            var existingReduced = document.getElementById(REDUCED_STYLE_ID);
            if (existingReduced) existingReduced.remove();

            var mq = window.matchMedia('(prefers-reduced-motion: reduce)');
            if (mq.matches) {
                var sr = document.createElement('style');
                sr.id = REDUCED_STYLE_ID;
                sr.textContent = [
                    '*, *::before, *::after {',
                    '  animation-duration: 0.01ms !important;',
                    '  animation-iteration-count: 1 !important;',
                    '  transition-duration: 0.01ms !important;',
                    '  scroll-behavior: auto !important;',
                    '}'
                ].join('\n');
                document.head.appendChild(sr);
            }

            if (window.__seanime_scroll_anim_handler) {
                window.removeEventListener('scroll', window.__seanime_scroll_anim_handler);
            }

            var scrollThrottle = null;
            var scrollAnimHandler = function() {
                if (!document.getElementById(SCROLL_STYLE_ID)) {
                    var s2 = document.createElement('style');
                    s2.id = SCROLL_STYLE_ID;
                    s2.textContent = '[data-media-entry-card-body="true"] img { transition: none !important; }';
                    document.head.appendChild(s2);
                }
                clearTimeout(scrollThrottle);
                scrollThrottle = setTimeout(function() {
                    var el = document.getElementById(SCROLL_STYLE_ID);
                    if (el) el.remove();
                    scrollThrottle = null;
                }, 200);
            };
            window.__seanime_scroll_anim_handler = scrollAnimHandler;
            window.addEventListener('scroll', scrollAnimHandler, { passive: true });
        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun injectNetworkHints(webView: WebView) {
        val js = """
        (function() {
            var PRECONNECT = [
                'https://s4.anilist.co',
                'https://img.anili.st',
                'https://anilist.co',
                'https://graphql.anilist.co'
            ];

            var DNS_PREFETCH = [
                'https://s4.anilist.co',
                'https://cdn.myanimelist.net'
            ];

            PRECONNECT.forEach(function(origin) {
                if (document.querySelector('link[rel="preconnect"][href="' + origin + '"]')) return;
                var link = document.createElement('link');
                link.rel = 'preconnect';
                link.href = origin;
                link.crossOrigin = 'anonymous';
                document.head.appendChild(link);
            });

            DNS_PREFETCH.forEach(function(origin) {
                if (document.querySelector('link[rel="dns-prefetch"][href="' + origin + '"]')) return;
                var link = document.createElement('link');
                link.rel = 'dns-prefetch';
                link.href = origin;
                document.head.appendChild(link);
            });

            document.querySelectorAll(
                '[data-media-page-header-entry-details-cover-image="true"], img[alt="banner image"]'
            ).forEach(function(img) {
                img.setAttribute('fetchpriority', 'high');
                img.setAttribute('loading', 'eager');
            });
        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    fun onTrimMemory(webView: WebView, level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            webView.evaluateJavascript(
                "if(typeof window.__seanime_onMemoryPressure === 'function') window.__seanime_onMemoryPressure();",
                null
            )
            if (level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
                webView.clearCache(false)
            }
        }
    }

    fun onLowMemory(webView: WebView) {
        webView.evaluateJavascript(
            "if(typeof window.__seanime_onMemoryPressure === 'function') window.__seanime_onMemoryPressure();",
            null
        )
        webView.clearCache(false)
    }
}