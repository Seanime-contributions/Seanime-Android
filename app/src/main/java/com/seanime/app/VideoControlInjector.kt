package com.seanime.app

import android.webkit.WebView

object VideoControlInjector {

    fun inject(view: WebView) {
        injectPlayerBehavior(view)
    }

    // ── Behavior (show/hide, double-tap seek) ─────────────────────────────────

    private fun injectPlayerBehavior(view: WebView) {
        val js = """
        (function() {
            if (window.__seanimeControlPatchActive) return;
            window.__seanimeControlPatchActive = true;

            var HIDE_DELAY_MS = 3000;
            var DOUBLE_TAP_DELAY = 280;
            var hideTimer = null;
            var isScrubbing = false;
            var firstPatch = true;

            var patchedContainer = null;
            var patchedTopBar = null;
            var patchedBottomBar = null;

            var lastClickTime = 0;
            var lastClickSide = null;
            var singleTapTimer = null;

            function isEntryRoute() {
                var url = window.location.pathname + window.location.search;
                return url.indexOf('/entry') !== -1 && url.indexOf('id=') !== -1;
            }

            function getTopBar()    { return document.querySelector('[data-vc-element="mobile-control-bar-top-section"]'); }
            function getBottomBar() { return document.querySelector('[data-vc-element="mobile-control-bar-bottom-section"]'); }
            function getContainer() { return document.querySelector('[data-vc-element="container"]'); }
            function getVideo()     { return document.querySelector('video'); }
            function getOverlayParent() {
                return document.fullscreenElement || getContainer() || document.body;
            }

            function isHideTransform(transform) {
                if (!transform || transform === 'none' || transform === '') return false;
                var match = transform.match(/translateY\(([^)]+)\)/);
                if (!match) return false;
                return parseFloat(match[1]) !== 0;
            }

            function showBars() {
                var top = getTopBar(); var bot = getBottomBar();
                if (!top || !bot) return;
                top.__seanimeHidden = false; bot.__seanimeHidden = false;
                top.__seanimeOurControl = true; bot.__seanimeOurControl = true;
                top.style.transform = 'translateY(0px)';
                bot.style.transform = 'translateY(0px)';
            }

            function hideBars() {
                if (isScrubbing) return;
                var top = getTopBar(); var bot = getBottomBar();
                if (!top || !bot) return;
                top.__seanimeHidden = true; bot.__seanimeHidden = true;
                top.__seanimeOurControl = true; bot.__seanimeOurControl = true;
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
                    var role   = cur.getAttribute ? (cur.getAttribute('role') || '') : '';
                    var dataVc = cur.getAttribute ? (cur.getAttribute('data-vc-element') || '') : '';
                    if (dataVc === 'mobile-control-bar-bottom-section' ||
                        dataVc === 'mobile-control-bar-top-section' ||
                        dataVc === 'container') break;
                    if (role === 'slider' || role === 'progressbar') return true;
                    if (dataVc.indexOf('progress') !== -1 ||
                        dataVc.indexOf('seek') !== -1 ||
                        dataVc.indexOf('slider') !== -1 ||
                        dataVc === 'time-range') return true;
                    var cls = (cur.className && typeof cur.className === 'string') ? cur.className : '';
                    if (cls.indexOf('scrub') !== -1 || cls.indexOf('seek') !== -1) return true;
                    if ((cur.tagName || '').toLowerCase() === 'input' &&
                        cur.getAttribute('type') === 'range') return true;
                    cur = cur.parentElement;
                }
                return false;
            }

            // ── Seek animation ────────────────────────────────────────────────

            function injectSeekStyles() {
                if (document.getElementById('__seanime-seek-styles')) return;
                var style = document.createElement('style');
                style.id = '__seanime-seek-styles';
                style.textContent = [
                    '.__seanime-seek-overlay {',
                    '  position: absolute; top: 50%;',
                    '  transform: translateY(-50%) scale(0.85);',
                    '  width: 110px; height: 190px;',
                    '  display: flex; flex-direction: column;',
                    '  align-items: center; justify-content: center;',
                    '  pointer-events: none; z-index: 99999;',
                    '  background: rgba(255,255,255,0.18);',
                    '  opacity: 0;',
                    '  animation: __seanime-seek-pop 0.85s cubic-bezier(0.4,0,0.2,1) forwards;',
                    '}',
                    '.__seanime-seek-left  { left: 0;  border-radius: 0 999px 999px 0; }',
                    '.__seanime-seek-right { right: 0; border-radius: 999px 0 0 999px; }',
                    '@keyframes __seanime-seek-pop {',
                    '  0%   { opacity: 0;   transform: translateY(-50%) scale(0.8);  }',
                    '  18%  { opacity: 1;   transform: translateY(-50%) scale(1.05); }',
                    '  30%  { opacity: 1;   transform: translateY(-50%) scale(1);    }',
                    '  72%  { opacity: 1;   transform: translateY(-50%) scale(1);    }',
                    '  100% { opacity: 0;   transform: translateY(-50%) scale(0.92); }',
                    '}',
                    '.__seanime-seek-arrows { display: flex; gap: 1px; margin-bottom: 10px; }',
                    '.__seanime-seek-arrow {',
                    '  color: white; font-size: 22px; opacity: 0;',
                    '  animation: __seanime-arrow-wave 0.55s ease forwards;',
                    '}',
                    '.__seanime-seek-arrow:nth-child(1) { animation-delay: 0.05s; }',
                    '.__seanime-seek-arrow:nth-child(2) { animation-delay: 0.15s; }',
                    '.__seanime-seek-arrow:nth-child(3) { animation-delay: 0.25s; }',
                    '@keyframes __seanime-arrow-wave {',
                    '  0%   { opacity: 0;   transform: scale(0.7); }',
                    '  45%  { opacity: 1;   transform: scale(1.1); }',
                    '  100% { opacity: 0.5; transform: scale(1);   }',
                    '}',
                    '.__seanime-seek-label {',
                    '  color: white; font-size: 13px; font-weight: 700;',
                    '  font-family: sans-serif; text-shadow: 0 1px 4px rgba(0,0,0,0.5);',
                    '  letter-spacing: 0.3px;',
                    '}'
                ].join('\n');
                (document.head || document.documentElement).appendChild(style);
            }

            function showSeekAnimation(side) {
                injectSeekStyles();
                var existing = document.getElementById('__seanime-seek-anim');
                if (existing && existing.parentNode) existing.parentNode.removeChild(existing);

                var parent = getOverlayParent();
                var ps = window.getComputedStyle(parent).position;
                if (ps === 'static') parent.style.position = 'relative';

                var el = document.createElement('div');
                el.id = '__seanime-seek-anim';
                el.className = '__seanime-seek-overlay __seanime-seek-' + side;
                var arrow = side === 'right' ? '\u25B6' : '\u25C0';
                el.innerHTML =
                    '<div class="__seanime-seek-arrows">' +
                        '<span class="__seanime-seek-arrow">' + arrow + '</span>' +
                        '<span class="__seanime-seek-arrow">' + arrow + '</span>' +
                        '<span class="__seanime-seek-arrow">' + arrow + '</span>' +
                    '</div>' +
                    '<div class="__seanime-seek-label">' + (side === 'right' ? '+10s' : '-10s') + '</div>';
                parent.appendChild(el);
                setTimeout(function() { if (el.parentNode) el.parentNode.removeChild(el); }, 900);
            }

            function seekVideo(seconds) {
                var video = getVideo();
                if (!video) return;
                video.currentTime = Math.max(0, Math.min(video.duration || 0, video.currentTime + seconds));
            }

            // ── Scrubbing listeners ───────────────────────────────────────────

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

            // ── Tap / double-tap listener ─────────────────────────────────────

            var clickListenerAttached = false;
            function attachClickListener() {
                if (clickListenerAttached) return;
                clickListenerAttached = true;

                document.addEventListener('dblclick', function(e) {
                    if (!isEntryRoute()) return;
                    var container = getContainer();
                    if (!container || !container.contains(e.target)) return;
                    e.preventDefault();
                    e.stopPropagation();
                }, true);

                document.addEventListener('click', function(e) {
                    if (!isEntryRoute()) return;
                    var container = getContainer();
                    if (!container || !container.contains(e.target)) return;
                    if (isProgressBarTarget(e.target)) return;

                    var now = Date.now();
                    var rect = container.getBoundingClientRect();
                    var side = e.clientX < rect.left + rect.width / 2 ? 'left' : 'right';
                    var isDouble = (now - lastClickTime) < DOUBLE_TAP_DELAY && side === lastClickSide;

                    lastClickTime = now;
                    lastClickSide = side;

                    if (isDouble) {
                        e.stopPropagation();
                        clearTimeout(singleTapTimer);
                        lastClickTime = 0;
                        seekVideo(side === 'right' ? 10 : -10);
                        showSeekAnimation(side);
                        showBars();
                        scheduleHide();
                        return;
                    }

                    clearTimeout(singleTapTimer);
                    singleTapTimer = setTimeout(function() {
                        var top = getTopBar();
                        if (!top) return;
                        var currentlyVisible = !isHideTransform(top.style.transform);
                        if (currentlyVisible) { scheduleHide(); }
                        else { showBars(); scheduleHide(); }
                    }, DOUBLE_TAP_DELAY);
                }, true);
            }

            function watchBar(el, hideValue) {
                new MutationObserver(function(mutations) {
                    mutations.forEach(function(m) {
                        if (m.attributeName !== 'style') return;
                        if (!isEntryRoute()) return;
                        if (el.__seanimeOurControl) { el.__seanimeOurControl = false; return; }
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

                var nodesReplaced = (topBar    !== patchedTopBar) ||
                                    (bottomBar !== patchedBottomBar) ||
                                    (container !== patchedContainer);
                if (!nodesReplaced) return;

                patchedTopBar    = topBar;
                patchedBottomBar = bottomBar;
                patchedContainer = container;

                watchBar(topBar,    'translateY(-100%)');
                watchBar(bottomBar, 'translateY(100%)');

                if (!('__seanimeHidden' in topBar))
                    Object.defineProperty(topBar,    '__seanimeHidden', { value: false, writable: true, configurable: true });
                if (!('__seanimeHidden' in bottomBar))
                    Object.defineProperty(bottomBar, '__seanimeHidden', { value: false, writable: true, configurable: true });

                if (firstPatch) { firstPatch = false; scheduleHide(); }
            }

            attachScrubbingListeners();
            attachClickListener();

            function onRouteChange() {
                patchedContainer = null;
                patchedTopBar    = null;
                patchedBottomBar = null;
                firstPatch = true;
                setTimeout(patchPlayer, 500);
            }

            var _pushState = history.pushState.bind(history);
            history.pushState = function() { _pushState.apply(history, arguments); onRouteChange(); };
            var _replaceState = history.replaceState.bind(history);
            history.replaceState = function() { _replaceState.apply(history, arguments); onRouteChange(); };
            window.addEventListener('popstate', onRouteChange);

            new MutationObserver(patchPlayer).observe(document.body, { childList: true, subtree: true });
            patchPlayer();
        })();
        """.trimIndent()

        view.evaluateJavascript(js, null)
    }
}