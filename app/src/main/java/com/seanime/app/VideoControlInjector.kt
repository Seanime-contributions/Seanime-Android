package com.seanime.app

import android.webkit.WebView

object VideoControlInjector {

    fun inject(view: WebView) {
        // language=JavaScript
        val js = """
        (function() {
            if (window.__seanimeControlPatchActive) return;
            window.__seanimeControlPatchActive = true;

            var HIDE_DELAY_MS = 3000;
            var hideTimer = null;
            var isScrubbing = false;

            // Track the actual DOM nodes we've patched so we can detect replacements
            var patchedContainer = null;
            var patchedTopBar = null;
            var patchedBottomBar = null;

            function isEntryRoute() {
                var url = window.location.pathname + window.location.search;
                return url.indexOf('/entry') !== -1 && url.indexOf('id=') !== -1;
            }

            function getTopBar() {
                return document.querySelector('[data-vc-element="mobile-control-bar-top-section"]');
            }
            function getBottomBar() {
                return document.querySelector('[data-vc-element="mobile-control-bar-bottom-section"]');
            }
            function getContainer() {
                return document.querySelector('[data-vc-element="container"]');
            }

            function isHideTransform(transform) {
                if (!transform || transform === 'none' || transform === '') return false;
                var match = transform.match(/translateY\(([^)]+)\)/);
                if (!match) return false;
                return parseFloat(match[1]) !== 0;
            }

            function showBars() {
                var top = getTopBar();
                var bot = getBottomBar();
                if (!top || !bot) return;
                top.__seanimeHidden = false;
                bot.__seanimeHidden = false;
                top.__seanimeOurControl = true;
                bot.__seanimeOurControl = true;
                top.style.transform = 'translateY(0px)';
                bot.style.transform = 'translateY(0px)';
            }

            function hideBars() {
                if (isScrubbing) return;
                var top = getTopBar();
                var bot = getBottomBar();
                if (!top || !bot) return;
                top.__seanimeHidden = true;
                bot.__seanimeHidden = true;
                top.__seanimeOurControl = true;
                bot.__seanimeOurControl = true;
                top.style.transform = 'translateY(-100%)';
                bot.style.transform = 'translateY(100%)';
            }

            function scheduleHide() {
                clearTimeout(hideTimer);
                if (isScrubbing) return;
                hideTimer = setTimeout(hideBars, HIDE_DELAY_MS);
            }

            function isProgressBarTarget(el) {
                if (!el) return false;
                var cur = el;
                for (var i = 0; i < 6; i++) {
                    if (!cur) break;
                    var role = cur.getAttribute ? (cur.getAttribute('role') || '') : '';
                    var dataVc = cur.getAttribute ? (cur.getAttribute('data-vc-element') || '') : '';
                    if (dataVc === 'mobile-control-bar-bottom-section' ||
                        dataVc === 'mobile-control-bar-top-section' ||
                        dataVc === 'container') break;
                    if (role === 'slider' || role === 'progressbar') return true;
                    if (dataVc.indexOf('progress') !== -1 ||
                        dataVc.indexOf('seek') !== -1 ||
                        dataVc.indexOf('slider') !== -1) return true;
                    var cls = (cur.className && typeof cur.className === 'string') ? cur.className : '';
                    if (cls.indexOf('scrub') !== -1 || cls.indexOf('seek') !== -1) return true;
                    if ((cur.tagName || '').toLowerCase() === 'input' &&
                        cur.getAttribute('type') === 'range') return true;
                    cur = cur.parentElement;
                }
                return false;
            }

            // Scrubbing listeners — attached once at document level, survive everything
            var scrubbingListenersAttached = false;
            function attachScrubbingListeners() {
                if (scrubbingListenersAttached) return;
                scrubbingListenersAttached = true;

                function onScrubStart(e) {
                    if (!isEntryRoute()) return;
                    if (isProgressBarTarget(e.target)) {
                        isScrubbing = true;
                        clearTimeout(hideTimer);
                        showBars();
                    }
                }
                function onScrubEnd() {
                    if (!isScrubbing) return;
                    isScrubbing = false;
                    scheduleHide();
                }

                document.addEventListener('pointerdown',   onScrubStart, { capture: true, passive: true });
                document.addEventListener('touchstart',    onScrubStart, { capture: true, passive: true });
                document.addEventListener('pointerup',     onScrubEnd,   { capture: true, passive: true });
                document.addEventListener('pointercancel', onScrubEnd,   { capture: true, passive: true });
                document.addEventListener('touchend',      onScrubEnd,   { capture: true, passive: true });
                document.addEventListener('touchcancel',   onScrubEnd,   { capture: true, passive: true });
            }

            function watchBar(el, hideValue) {
                new MutationObserver(function(mutations) {
                    mutations.forEach(function(m) {
                        if (m.attributeName !== 'style') return;
                        if (!isEntryRoute()) return;
                        if (el.__seanimeOurControl) {
                            el.__seanimeOurControl = false;
                            return;
                        }
                        // Player tried to mutate — restore our desired state
                        el.__seanimeOurControl = true;
                        el.style.transform = el.__seanimeHidden ? hideValue : 'translateY(0px)';
                    });
                }).observe(el, { attributes: true, attributeFilter: ['style'] });
            }

            function patchPlayer() {
                if (!isEntryRoute()) return;

                var topBar    = getTopBar();
                var bottomBar = getBottomBar();
                var container = getContainer();
                if (!topBar || !bottomBar || !container) return;

                // Re-patch whenever DOM nodes are replaced (e.g. entering/leaving fullscreen)
                var nodesReplaced = (topBar    !== patchedTopBar) ||
                                    (bottomBar !== patchedBottomBar) ||
                                    (container !== patchedContainer);
                if (!nodesReplaced) return;

                patchedTopBar    = topBar;
                patchedBottomBar = bottomBar;
                patchedContainer = container;

                // (Re-)attach style observers on the current bar nodes
                watchBar(topBar,    'translateY(-100%)');
                watchBar(bottomBar, 'translateY(100%)');

                // Init hidden flags on fresh nodes
                if (!('__seanimeHidden' in topBar)) {
                    Object.defineProperty(topBar, '__seanimeHidden',    { value: false, writable: true, configurable: true });
                }
                if (!('__seanimeHidden' in bottomBar)) {
                    Object.defineProperty(bottomBar, '__seanimeHidden', { value: false, writable: true, configurable: true });
                }

                // Attach click listener on the current container node
                container.addEventListener('click', function(e) {
                    if (!isEntryRoute()) return;
                    if (isProgressBarTarget(e.target)) return;

                    var top = getTopBar();
                    if (!top) return;
                    var currentlyVisible = !isHideTransform(top.style.transform);

                    if (currentlyVisible) {
                        scheduleHide();
                    } else {
                        setTimeout(function() {
                            showBars();
                            scheduleHide();
                        }, 100);
                    }
                }, false);

                scheduleHide();
            }

            attachScrubbingListeners();

            // Reset cached nodes on route change so patchPlayer re-runs fully
            function onRouteChange() {
                patchedContainer = null;
                patchedTopBar    = null;
                patchedBottomBar = null;
                setTimeout(patchPlayer, 500);
            }

            var _pushState = history.pushState.bind(history);
            history.pushState = function() {
                _pushState.apply(history, arguments);
                onRouteChange();
            };
            var _replaceState = history.replaceState.bind(history);
            history.replaceState = function() {
                _replaceState.apply(history, arguments);
                onRouteChange();
            };
            window.addEventListener('popstate', onRouteChange);

            // MutationObserver catches fullscreen DOM swaps mid-route
            new MutationObserver(function() {
                patchPlayer();
            }).observe(document.body, { childList: true, subtree: true });

            patchPlayer();
        })();
        """.trimIndent()

        view.evaluateJavascript(js, null)
    }
}
