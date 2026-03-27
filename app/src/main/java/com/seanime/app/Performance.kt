package com.seanime.app

import android.content.Context
import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView

object Performance {

    fun init(context: Context, webView: WebView) {
        applyWebViewSettings(context, webView)
        injectRuntimeOptimizations(webView)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. WebView settings — applied before page load
    // ─────────────────────────────────────────────────────────────────────────

    private fun applyWebViewSettings(context: Context, webView: WebView) {
        val settings: WebSettings = webView.settings

        // ── Rendering ────────────────────────────────────────────────────────
        // Hardware acceleration is set on the View layer (see below); enabling
        // it in settings as well ensures the compositor path is fully active.
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
        // LOAD_DEFAULT honours HTTP cache-control headers; fall back to cache
        // when offline.  This avoids unnecessary network round-trips for
        // static assets (JS bundles, images, fonts).
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // Point the app cache at the app's private cache directory so the OS
        // can reclaim it under memory pressure without user data loss.
        settings.setAppCachePath(context.cacheDir.absolutePath)
        @Suppress("DEPRECATION")
        settings.setAppCacheEnabled(true)

        // ── Network ──────────────────────────────────────────────────────────
        // Allow the page to store data (IndexedDB, localStorage, etc.) which
        // the Next.js / React app uses for client-side caching.
        settings.domStorageEnabled = true
        settings.databaseEnabled  = true

        // Fetch sub-resources (images, fonts, XHR) over HTTP when the page
        // itself is HTTPS — needed if the local dev server is plain HTTP.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        // ── Text / media ─────────────────────────────────────────────────────
        settings.mediaPlaybackRequiresUserGesture = false  // inline video/audio
        settings.loadsImagesAutomatically = true

        // Disable unnecessary features that add overhead
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.setSupportMultipleWindows(false)
        settings.allowFileAccess = false          // not needed; reduces attack surface
        settings.allowContentAccess = false

        // ── User-agent tweak ─────────────────────────────────────────────────
        // Append a short token so the server can serve optimised responses
        // (e.g. skip SSR hydration hints not needed in the WebView shell).
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
    // 2. JavaScript runtime optimisations — injected after every page load
    //    Call injectRuntimeOptimizations(webView) from WebViewClient.onPageFinished
    //    as well as here for the initial load.
    // ─────────────────────────────────────────────────────────────────────────

    fun injectRuntimeOptimizations(webView: WebView) {
        injectScrollOptimizations(webView)
        injectImageLazyLoading(webView)
        injectMemoryPressureHandler(webView)
        injectAnimationThrottling(webView)
        injectNetworkHints(webView)
    }

    // ── 2a. Scroll optimisations ─────────────────────────────────────────────
    // passive listeners + will-change hints keep scroll jank-free on older
    // Chromium builds embedded in WebView.
    private fun injectScrollOptimizations(webView: WebView) {
        val js = """
        (function() {
            if (window.__seanime_scroll_opt) return;
            window.__seanime_scroll_opt = true;

            // Force GPU-composited scrolling on the main scroll containers.
            var CSS_SCROLL = [
                '[data-main-layout-content="true"]',
                '.overflow-y-auto',
                '.overflow-x-auto',
                'body'
            ];
            CSS_SCROLL.forEach(function(sel) {
                document.querySelectorAll(sel).forEach(function(el) {
                    el.style.webkitOverflowScrolling = 'touch';
                    el.style.willChange = 'scroll-position';
                });
            });

            // Promote heavy animated elements to their own compositor layer.
            var CSS_PROMOTE = [
                '[data-media-entry-card-body="true"]',
                '#__seanime_bottom_nav',
                '[data-media-page-header-entry-details-cover-image-container="true"]'
            ];
            CSS_PROMOTE.forEach(function(sel) {
                document.querySelectorAll(sel).forEach(function(el) {
                    el.style.willChange = 'transform';
                    el.style.transform  = 'translateZ(0)';
                });
            });

            // Re-apply to nodes added after initial render (lazy-loaded cards, etc.)
            var promoObserver = new MutationObserver(function(mutations) {
                mutations.forEach(function(m) {
                    m.addedNodes.forEach(function(node) {
                        if (node.nodeType !== 1) return;
                        CSS_PROMOTE.forEach(function(sel) {
                            if (node.matches && node.matches(sel)) {
                                node.style.willChange = 'transform';
                                node.style.transform  = 'translateZ(0)';
                            }
                            node.querySelectorAll && node.querySelectorAll(sel).forEach(function(el) {
                                el.style.willChange = 'transform';
                                el.style.transform  = 'translateZ(0)';
                            });
                        });
                    });
                });
            });
            promoObserver.observe(document.body, { childList: true, subtree: true });
        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    // ── 2b. Image lazy-loading ───────────────────────────────────────────────
    // Retrofit native lazy-loading onto any image that doesn't already have it,
    // and decode images asynchronously so the main thread stays unblocked.
    private fun injectImageLazyLoading(webView: WebView) {
        val js = """
        (function() {
            if (window.__seanime_img_opt) return;
            window.__seanime_img_opt = true;

            function optimiseImages(root) {
                (root || document).querySelectorAll('img').forEach(function(img) {
                    // Skip cover / banner images that are above-the-fold / eager.
                    if (img.loading === 'eager') return;
                    if (!img.hasAttribute('loading')) img.setAttribute('loading', 'lazy');
                    if (!img.hasAttribute('decoding')) img.setAttribute('decoding', 'async');
                    // Prevent layout shift — only set if no explicit size is given.
                    if (!img.width && !img.style.width) {
                        img.style.contentVisibility = 'auto';
                    }
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
        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    // ── 2c. Memory pressure handler ─────────────────────────────────────────
    // When the Android system fires a low-memory event we forward it to the
    // page so React / the app can flush caches.
    private fun injectMemoryPressureHandler(webView: WebView) {
        val js = """
        (function() {
            if (window.__seanime_mem_opt) return;
            window.__seanime_mem_opt = true;

            // Throttle expensive re-renders while a scroll is in flight.
            var scrollTimer = null;
            window.addEventListener('scroll', function() {
                document.body.dataset.scrolling = 'true';
                clearTimeout(scrollTimer);
                scrollTimer = setTimeout(function() {
                    delete document.body.dataset.scrolling;
                }, 150);
            }, { passive: true });

            // Expose a hook the native layer can call to signal memory pressure:
            //   webView.evaluateJavascript("window.__seanime_onMemoryPressure()", null)
            window.__seanime_onMemoryPressure = function() {
                // 1. Release any object-URL blobs still held in memory.
                document.querySelectorAll('img[src^="blob:"]').forEach(function(img) {
                    try { URL.revokeObjectURL(img.src); } catch(e) {}
                });
                // 2. Ask React Query / SWR to clear their in-memory caches if available.
                try {
                    if (window.__REACT_QUERY_DEVTOOLS_GLOBAL_HOOK__) {
                        window.__REACT_QUERY_DEVTOOLS_GLOBAL_HOOK__.queryClient.clear();
                    }
                } catch(e) {}
                // 3. Fire a custom DOM event so the app's own listeners can react.
                window.dispatchEvent(new Event('seanime:memorypressure'));
                console.log('[Seanime] Memory pressure signal received — caches flushed.');
            };
        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    // ── 2d. Animation throttling ─────────────────────────────────────────────
    // Cap CSS animations / transitions on low-end devices and pause them
    // entirely when the app is backgrounded.
    private fun injectAnimationThrottling(webView: WebView) {
        val js = """
        (function() {
            if (window.__seanime_anim_opt) return;
            window.__seanime_anim_opt = true;

            var styleId = '__seanime_anim_throttle';

            function setAnimationState(paused) {
                var existing = document.getElementById(styleId);
                if (paused) {
                    if (existing) return;
                    var s = document.createElement('style');
                    s.id = styleId;
                    // Pause all CSS animations and transitions while hidden.
                    s.textContent = '*, *::before, *::after { animation-play-state: paused !important; transition: none !important; }';
                    document.head.appendChild(s);
                } else {
                    if (existing) existing.remove();
                }
            }

            // Pause animations when the tab/app goes into the background.
            document.addEventListener('visibilitychange', function() {
                setAnimationState(document.hidden);
            });

            // Reduce motion if the OS "reduce motion" preference is set.
            var mq = window.matchMedia('(prefers-reduced-motion: reduce)');
            if (mq.matches) {
                var s = document.createElement('style');
                s.id = '__seanime_reduced_motion';
                s.textContent = [
                    '*, *::before, *::after {',
                    '  animation-duration: 0.01ms !important;',
                    '  animation-iteration-count: 1 !important;',
                    '  transition-duration: 0.01ms !important;',
                    '  scroll-behavior: auto !important;',
                    '}'
                ].join('\n');
                document.head.appendChild(s);
            }

            // Throttle hover-triggered scale transitions on media cards while
            // scrolling — avoids triggering hundreds of compositor updates.
            var scrollThrottle = null;
            window.addEventListener('scroll', function() {
                if (!scrollThrottle) {
                    var s2 = document.createElement('style');
                    s2.id = '__seanime_scroll_throttle';
                    s2.textContent = '.media-entry-card__body img { transition: none !important; }';
                    document.head.appendChild(s2);
                }
                clearTimeout(scrollThrottle);
                scrollThrottle = setTimeout(function() {
                    var el = document.getElementById('__seanime_scroll_throttle');
                    if (el) el.remove();
                    scrollThrottle = null;
                }, 200);
            }, { passive: true });
        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    // ── 2e. Network / resource hints ────────────────────────────────────────
    // Inject <link rel="preconnect"> for known third-party origins so the
    // TLS handshake is done before the browser actually needs the resource.
    private fun injectNetworkHints(webView: WebView) {
        val js = """
        (function() {
            if (window.__seanime_net_opt) return;
            window.__seanime_net_opt = true;

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

            // Fetch-priority: mark banner/cover images as high-priority so the
            // browser's preload scanner picks them up before lazy images.
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

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Native memory-pressure forwarding
    //    Call this from Activity.onTrimMemory / onLowMemory.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Forward Android memory-pressure events into the page so the JS layer
     * can release caches.  Wire up in MainActivity:
     *
     *   override fun onTrimMemory(level: Int) {
     *       super.onTrimMemory(level)
     *       Performance.onTrimMemory(webView, level)
     *   }
     *   override fun onLowMemory() {
     *       super.onLowMemory()
     *       Performance.onLowMemory(webView)
     *   }
     */
    fun onTrimMemory(webView: WebView, level: Int) {
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            webView.evaluateJavascript(
                "if(window.__seanime_onMemoryPressure) window.__seanime_onMemoryPressure();",
                null
            )
            if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
                // On severe pressure also ask WebView to free its internal caches.
                webView.clearCache(false)   // false = keep disk cache
            }
        }
    }

    fun onLowMemory(webView: WebView) {
        webView.evaluateJavascript(
            "if(window.__seanime_onMemoryPressure) window.__seanime_onMemoryPressure();",
            null
        )
        webView.clearCache(false)
    }
}
