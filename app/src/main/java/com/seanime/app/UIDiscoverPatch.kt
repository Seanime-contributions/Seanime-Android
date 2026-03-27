package com.seanime.app

import android.webkit.WebView

object UIDiscoverPatch {

    fun inject(webView: WebView) {
        injectDiscoverCardStyles(webView)
        injectDiscoverCardScript(webView)
    }

    // CSS handles the visual sizing
    private fun injectDiscoverCardStyles(webView: WebView) {
        val js = """
        (function() {
            if (!window.location.pathname.startsWith('/discover')) return;
            if (document.getElementById('__seanime_discover_patch_styles')) return;

            var s = document.createElement('style');
            s.id = '__seanime_discover_patch_styles';
            s.textContent = `
                @media (max-width: 767px) {

                    /* ── Card container ── */
                    [data-media-entry-card-container="true"][class*="basis-[200px]"],
                    [data-media-entry-card-container="true"][class*="basis-[250px]"] {
                        flex-basis: 160px !important;
                        min-width:  160px !important;
                        max-width:  160px !important;
                        margin-left:  0.4rem !important;
                        margin-right: 0.4rem !important;
                    }

                    /* ── Title text ── */
                    [data-media-entry-card-container="true"][class*="basis-[200px]"] [data-media-entry-card-title-section-title="true"],
                    [data-media-entry-card-container="true"][class*="basis-[250px]"] [data-media-entry-card-title-section-title="true"] {
                        font-size: 0.78rem !important;
                        line-height: 1.3   !important;
                    }

                    /* ── Year/season label ── */
                    [data-media-entry-card-container="true"][class*="basis-[200px]"] [data-media-entry-card-title-section-year-season="true"],
                    [data-media-entry-card-container="true"][class*="basis-[250px]"] [data-media-entry-card-title-section-year-season="true"] {
                        font-size: 0.7rem !important;
                    }
                }
            `;
            document.head.appendChild(s);
        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    // JS forcibly sets inline width/minWidth/maxWidth on every card AND skeleton
    // so Tailwind's inline basis classes can't override us, and skeletons match.
    private fun injectDiscoverCardScript(webView: WebView) {
        val js = """
        (function() {
            if (!window.location.pathname.startsWith('/discover')) return;
            if (window.__seanime_discover_patch_init) return;
            window.__seanime_discover_patch_init = true;

            var MOBILE_MAX = 767;
            var CARD_WIDTH = '160px';

            function isMobile() {
                return window.innerWidth <= MOBILE_MAX;
            }

            function patchElement(el) {
                if (!isMobile()) return;
                el.style.setProperty('flex-basis', CARD_WIDTH, 'important');
                el.style.setProperty('min-width',  CARD_WIDTH, 'important');
                el.style.setProperty('max-width',  CARD_WIDTH, 'important');
                el.style.setProperty('width',      CARD_WIDTH, 'important');
            }

            function patchAll() {
                if (!isMobile()) return;
                // Real cards
                document.querySelectorAll(
                    '[data-media-entry-card-container="true"][class*="basis-[200px]"],' +
                    '[data-media-entry-card-container="true"][class*="basis-[250px]"]'
                ).forEach(patchElement);

                // Skeleton / loading placeholders — they sit alongside the real
                // cards inside the same flex row and share the same basis classes.
                document.querySelectorAll(
                    '[class*="basis-[200px]"],[class*="basis-[250px]"]'
                ).forEach(function(el) {
                    // Only patch elements inside a discover scroll row, not
                    // unrelated elements that happen to share the class name.
                    if (el.closest('[data-discover-section]') ||
                        el.closest('[class*="overflow-x-auto"]') ||
                        el.hasAttribute('data-media-entry-card-container')) {
                        patchElement(el);
                    }
                });
            }

            // Initial pass
            patchAll();

            // Watch for skeletons / cards being added dynamically (lazy sections)
            new MutationObserver(function(mutations) {
                if (!isMobile()) return;
                mutations.forEach(function(m) {
                    m.addedNodes.forEach(function(node) {
                        if (node.nodeType !== 1) return;
                        // Patch the node itself if it matches
                        var cls = node.className || '';
                        if (typeof cls === 'string' &&
                            (cls.indexOf('basis-[200px]') !== -1 ||
                             cls.indexOf('basis-[250px]') !== -1)) {
                            patchElement(node);
                        }
                        // Patch any matching descendants
                        node.querySelectorAll &&
                        node.querySelectorAll(
                            '[class*="basis-[200px]"],[class*="basis-[250px]"]'
                        ).forEach(patchElement);
                    });
                });
            }).observe(document.body, { childList: true, subtree: true });

            // Re-evaluate on resize (e.g. orientation change)
            window.addEventListener('resize', function() {
                if (isMobile()) {
                    patchAll();
                } else {
                    // Remove inline overrides when going to tablet/desktop
                    document.querySelectorAll(
                        '[class*="basis-[200px]"],[class*="basis-[250px]"]'
                    ).forEach(function(el) {
                        el.style.removeProperty('flex-basis');
                        el.style.removeProperty('min-width');
                        el.style.removeProperty('max-width');
                        el.style.removeProperty('width');
                    });
                }
            });
        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }
}
