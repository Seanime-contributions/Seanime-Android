package com.seanime.app

import android.webkit.WebView

object UISettingsPatch {

    fun inject(webView: WebView) {
        injectSettingsFix(webView)
        injectSettingsMenuFix(webView)
        injectSettingsActionBar(webView)
    }

    private fun injectSettingsFix(webView: WebView) {
        val js = """
            (function() {
                var style = document.getElementById('__seanime_settings_fix');
                if (style) return;
                style = document.createElement('style');
                style.id = '__seanime_settings_fix';
                style.textContent = `
                    [data-open-issue-recorder-button="true"] {
                        transform: none !important;
                        transition: background-color 0.2s ease, box-shadow 0.2s ease !important;
                    }
                    [data-open-issue-recorder-button="true"]:hover {
                        transform: none !important;
                    }
                    .UI-Card__content .pb-3.flex.gap-2 {
                        flex-wrap: wrap !important;
                        overflow: hidden !important;
                    }
                    .UI-Card__content .pb-3.flex.gap-2 button {
                        flex-shrink: 1 !important;
                        min-width: 0 !important;
                        max-width: 100% !important;
                    }
                `;
                document.head.appendChild(style);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun injectSettingsMenuFix(webView: WebView) {
        val js = """
            (function() {
                function ensureDonateHideStyle() {
                    if (document.getElementById('__seanime_donate_hide')) return;
                    var style = document.createElement('style');
                    style.id = '__seanime_donate_hide';
                    style.textContent = '[data-seanime-donate="true"] { display: none !important; }';
                    document.head.appendChild(style);
                }
                ensureDonateHideStyle();

                function tagDonateButtons() {
                    var ourDrawer = document.getElementById('__seanime_drawer');
                    document.querySelectorAll('button.UI-Button_root, button').forEach(function(btn) {
                        if (ourDrawer && ourDrawer.contains(btn)) return;
                        if (btn.dataset.seanimeOwned === 'true') return;
                        var isDonate = btn.textContent.trim() === 'Donate' ||
                            Array.from(btn.querySelectorAll('span')).some(function(s) {
                                return s.textContent.trim() === 'Donate';
                            });
                        if (isDonate) btn.setAttribute('data-seanime-donate', 'true');
                    });
                }

                function isSettingsPage() {
                    return /\/settings/.test(window.location.href);
                }

                function cleanupDrawer() {
                    ['__seanime_drawer', '__seanime_backdrop', '__seanime_drawer_tab'].forEach(function(id) {
                        var el = document.getElementById(id);
                        if (el) el.remove();
                    });
                    var origNav = document.querySelector('.overflow-x-none.overflow-y-hidden');
                    if (origNav) {
                        origNav.style.removeProperty('position');
                        origNav.style.removeProperty('top');
                        origNav.style.removeProperty('left');
                        origNav.style.removeProperty('visibility');
                        origNav.style.removeProperty('pointer-events');
                    }
                }

                function buildDrawer() {
                    if (!isSettingsPage()) return;
                    if (document.getElementById('__seanime_drawer_tab')) return;

                    var triggers = document.querySelectorAll('.UI-Tabs__trigger');
                    if (triggers.length === 0) return;

                    var origNav = document.querySelector('.overflow-x-none.overflow-y-hidden');
                    if (!origNav) return;

                    origNav.style.setProperty('position', 'fixed', 'important');
                    origNav.style.setProperty('top', '-9999px', 'important');
                    origNav.style.setProperty('left', '-9999px', 'important');
                    origNav.style.setProperty('pointer-events', 'auto', 'important');
                    origNav.style.setProperty('visibility', 'hidden', 'important');

                    var backdrop = document.createElement('div');
                    backdrop.id = '__seanime_backdrop';
                    backdrop.style.cssText = [
                        'position:fixed','inset:0','z-index:998',
                        'background:rgba(0,0,0,0.6)','opacity:0',
                        'pointer-events:none','transition:opacity 0.25s ease'
                    ].join(';');
                    document.body.appendChild(backdrop);

                    var drawer = document.createElement('div');
                    drawer.id = '__seanime_drawer';
                    drawer.style.cssText = [
                        'position:fixed','top:0','left:0','height:100%',
                        'width:75vw','max-width:300px','z-index:999',
                        'transform:translateX(-100%)',
                        'transition:transform 0.28s cubic-bezier(0.4,0,0.2,1)',
                        'background:rgba(10,10,15,0.55)',
                        'backdrop-filter:blur(24px)','-webkit-backdrop-filter:blur(24px)',
                        'border-right:1px solid rgba(255,255,255,0.07)',
                        'display:flex','flex-direction:column','overflow-y:auto','overflow-x:hidden'
                    ].join(';');

                    var header = document.createElement('div');
                    header.style.cssText = 'padding:1.5rem 1.25rem 1.1rem;border-bottom:1px solid rgba(255,255,255,0.07);flex-shrink:0;';
                    header.innerHTML = '<p style="margin:0;font-size:1.1rem;font-weight:700;color:white;">Settings</p>';
                    drawer.appendChild(header);

                    var groups = origNav.querySelectorAll('.UI-Card__root');
                    groups.forEach(function(group, gi) {
                        var btns = group.querySelectorAll('.UI-Tabs__trigger');
                        if (btns.length === 0) return;
                        if (gi > 0) {
                            var sep = document.createElement('div');
                            sep.style.cssText = 'height:1px;background:rgba(255,255,255,0.06);margin:0.15rem 0;flex-shrink:0;';
                            drawer.appendChild(sep);
                        }
                        btns.forEach(function(btn) {
                            var svgEl = btn.querySelector('svg');
                            var label = btn.textContent.trim();
                            var isActive = btn.getAttribute('data-state') === 'active';

                            var item = document.createElement('button');
                            item.setAttribute('data-drawer-for', btn.id);
                            item.dataset.seanimeOwned = 'true';
                            item.style.cssText = [
                                'display:flex','align-items:center','gap:0.85rem','width:100%',
                                'padding:0.8rem 1.25rem',
                                'background:' + (isActive ? 'rgba(255,255,255,0.06)' : 'transparent'),
                                'border:none',
                                'border-left:3px solid ' + (isActive ? 'var(--brand,#818cf8)' : 'transparent'),
                                'color:' + (isActive ? 'white' : 'rgba(255,255,255,0.45)'),
                                'font-size:0.92rem','font-weight:' + (isActive ? '600' : '400'),
                                'text-align:left','cursor:pointer','flex-shrink:0','box-sizing:border-box'
                            ].join(';');

                            var iconSpan = document.createElement('span');
                            iconSpan.style.cssText = 'display:flex;align-items:center;justify-content:center;flex-shrink:0;width:20px;height:20px;opacity:' + (isActive ? '1' : '0.5') + ';';
                            if (svgEl) {
                                var clonedSvg = svgEl.cloneNode(true);
                                clonedSvg.setAttribute('width', '20');
                                clonedSvg.setAttribute('height', '20');
                                clonedSvg.style.cssText = 'width:20px;height:20px;flex-shrink:0;';
                                iconSpan.appendChild(clonedSvg);
                            }

                            var labelSpan = document.createElement('span');
                            labelSpan.textContent = label;
                            labelSpan.style.cssText = 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;';

                            item.appendChild(iconSpan);
                            item.appendChild(labelSpan);

                            item.addEventListener('click', function() {
                                origNav.style.setProperty('visibility', 'visible', 'important');
                                var realBtn = document.getElementById(item.getAttribute('data-drawer-for'));
                                if (realBtn) {
                                    realBtn.dispatchEvent(new PointerEvent('pointerdown', {bubbles:true, cancelable:true}));
                                    realBtn.dispatchEvent(new MouseEvent('mousedown', {bubbles:true, cancelable:true}));
                                    realBtn.dispatchEvent(new PointerEvent('pointerup', {bubbles:true, cancelable:true}));
                                    realBtn.dispatchEvent(new MouseEvent('mouseup', {bubbles:true, cancelable:true}));
                                    realBtn.dispatchEvent(new MouseEvent('click', {bubbles:true, cancelable:true}));
                                }
                                setTimeout(function() {
                                    origNav.style.setProperty('visibility', 'hidden', 'important');
                                }, 50);
                                drawer.querySelectorAll('[data-drawer-for]').forEach(function(i) {
                                    i.style.background = 'transparent';
                                    i.style.borderLeft = '3px solid transparent';
                                    i.style.color = 'rgba(255,255,255,0.45)';
                                    i.style.fontWeight = '400';
                                    var ico = i.querySelector('span:first-child');
                                    if (ico) ico.style.opacity = '0.5';
                                });
                                item.style.background = 'rgba(255,255,255,0.06)';
                                item.style.borderLeft = '3px solid var(--brand,#818cf8)';
                                item.style.color = 'white';
                                item.style.fontWeight = '600';
                                var ico = item.querySelector('span:first-child');
                                if (ico) ico.style.opacity = '1';
                            });

                            drawer.appendChild(item);
                        });
                    });

                    var donateSep = document.createElement('div');
                    donateSep.style.cssText = 'height:1px;background:rgba(255,255,255,0.06);margin:0.15rem 0;flex-shrink:0;margin-top:auto;';
                    drawer.appendChild(donateSep);

                    var donateWrapper = document.createElement('div');
                    donateWrapper.style.cssText = 'padding:1.25rem;flex-shrink:0;';

                    var donateBtn = document.createElement('button');
                    donateBtn.id = '__seanime_drawer_donate_btn';
                    donateBtn.dataset.seanimeOwned = 'true';
                    donateBtn.style.cssText = [
                        'display:flex','align-items:center','gap:0.85rem','width:100%',
                        'padding:0.8rem 1.25rem','background:transparent','border:none',
                        'border-left:3px solid transparent','color:rgba(255,255,255,0.45)',
                        'font-size:0.92rem','font-weight:400','text-align:left',
                        'cursor:pointer','flex-shrink:0','box-sizing:border-box'
                    ].join(';');

                    var donateIconSpan = document.createElement('span');
                    donateIconSpan.style.cssText = 'display:flex;align-items:center;justify-content:center;flex-shrink:0;width:20px;height:20px;opacity:0.5;';
                    donateIconSpan.innerHTML = '<svg width="20" height="20" stroke="currentColor" fill="currentColor" stroke-width="0" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M4 21h9.62a3.995 3.995 0 0 0 3.037-1.397l5.102-5.952a1 1 0 0 0-.442-1.6l-1.968-.656a3.043 3.043 0 0 0-2.823.503l-3.185 2.547-.617-1.235A3.98 3.98 0 0 0 9.146 11H4c-1.103 0-2 .897-2 2v6c0 1.103.897 2 2 2zm0-8h5.146c.763 0 1.448.423 1.789 1.105l.447.895H7v2h6.014a.996.996 0 0 0 .442-.11l.003-.001.004-.002h.003l.002-.001h.004l.001-.001c.009.003.003-.001.003-.001.01 0 .002-.001.002-.001h.001l.002-.001.003-.001.002-.001.002-.001.003-.001.002-.001c.003 0 .001-.001.002-.001l.003-.002.002-.001.002-.001.003-.001.002-.001h.001l.002-.001h.001l.002-.001.002-.001c.009-.001.003-.001.003-.001l.002-.001a.915.915 0 0 0 .11-.078l4.146-3.317c.262-.208.623-.273.94-.167l.557.186-4.133 4.823a2.029 2.029 0 0 1-1.52.688H4v-6zM16 2h-.017c-.163.002-1.006.039-1.983.705-.951-.648-1.774-.7-1.968-.704L12.002 2h-.004c-.801 0-1.555.313-2.119.878C9.313 3.445 9 4.198 9 5s.313 1.555.861 2.104l3.414 3.586a1.006 1.006 0 0 0 1.45-.001l3.396-3.568C18.688 6.555 19 5.802 19 5s-.313-1.555-.878-2.121A2.978 2.978 0 0 0 16.002 2H16zm1 3c0 .267-.104.518-.311.725L14 8.55l-2.707-2.843C11.104 5.518 11 5.267 11 5s.104-.518.294-.708A.977.977 0 0 1 11.979 4c.025.001.502.032 1.067.485.081.065.163.139.247.222l.707.707.707-.707c.084-.083.166-.157.247-.222.529-.425.976-.478 1.052-.484a.987.987 0 0 1 .701.292c.189.189.293.44.293.707z"></path></svg>';

                    var donateLabelSpan = document.createElement('span');
                    donateLabelSpan.textContent = 'Donate';
                    donateLabelSpan.style.cssText = 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;';

                    donateBtn.appendChild(donateIconSpan);
                    donateBtn.appendChild(donateLabelSpan);
                    donateBtn.addEventListener('click', function() {
                        if (window.DonateBridge) window.DonateBridge.openDonate();
                    });

                    donateWrapper.appendChild(donateBtn);
                    drawer.appendChild(donateWrapper);
                    document.body.appendChild(drawer);
                    tagDonateButtons();

                    var drawerW = Math.min(window.innerWidth * 0.75, 300);
                    var isOpen = false;

                    var tab = document.createElement('button');
                    tab.id = '__seanime_drawer_tab';
                    tab.dataset.seanimeOwned = 'true';
                    tab.style.cssText = [
                        'position:fixed','left:0','top:50%','transform:translateY(-50%)',
                        'z-index:1000','width:20px','height:56px',
                        'background:rgba(255,255,255,0.07)',
                        'backdrop-filter:blur(12px)','-webkit-backdrop-filter:blur(12px)',
                        'border:1px solid rgba(255,255,255,0.10)','border-left:none',
                        'border-radius:0 8px 8px 0','color:rgba(255,255,255,0.6)',
                        'display:flex','align-items:center','justify-content:center',
                        'cursor:pointer','padding:0',
                        'transition:left 0.28s cubic-bezier(0.4,0,0.2,1)','box-sizing:border-box'
                    ].join(';');

                    function setArrow(pointRight) {
                        tab.innerHTML = pointRight
                            ? '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"></polyline></svg>'
                            : '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"></polyline></svg>';
                    }
                    setArrow(true);

                    function openDrawer() {
                        isOpen = true;
                        drawer.style.transform = 'translateX(0)';
                        backdrop.style.opacity = '1';
                        backdrop.style.pointerEvents = 'auto';
                        tab.style.left = drawerW + 'px';
                        setArrow(false);
                    }
                    function closeDrawer() {
                        isOpen = false;
                        drawer.style.transform = 'translateX(-100%)';
                        backdrop.style.opacity = '0';
                        backdrop.style.pointerEvents = 'none';
                        tab.style.left = '0';
                        setArrow(true);
                    }

                    tab.addEventListener('click', function() { if (isOpen) closeDrawer(); else openDrawer(); });
                    backdrop.addEventListener('click', closeDrawer);
                    document.body.appendChild(tab);
                }

                buildDrawer();

                var observer = new MutationObserver(function(mutations) {
                    var added = mutations.some(function(m) { return m.addedNodes.length > 0; });
                    if (added) { tagDonateButtons(); buildDrawer(); }
                });
                observer.observe(document.body, { childList: true, subtree: true });

                var lastHref = window.location.href;
                setInterval(function() {
                    if (window.location.href !== lastHref) {
                        lastHref = window.location.href;
                        cleanupDrawer();
                        setTimeout(buildDrawer, 400);
                        setTimeout(buildDrawer, 900);
                    }
                }, 300);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    /**
     * Floats the "Reset all" (pill) and "Save" (button) actions above the bottom nav bar.
     *
     * Visibility is driven purely by whether the original action bar exists in the DOM:
     *   - buttons present  → build/show the floating bar, hide the original
     *   - buttons absent   → remove the floating bar (modal open, tab switch, navigated away, etc.)
     *
     * No overlay/dialog detection needed — React unmounts the form when a modal covers it,
     * so the originals simply disappear from the DOM at the right time.
     */
    private fun injectSettingsActionBar(webView: WebView) {
        val js = """
            (function() {
                var TABLET_BP  = 768;
                var BAR_ID     = '__seanime_settings_float_bar';
                var HIDE_STYLE = '__seanime_action_bar_hide';
                var TAG_ATTR   = 'data-seanime-action-bar';

                function isPhone()        { return window.innerWidth < TABLET_BP; }
                function isSettingsPage() { return /\/settings/.test(window.location.href); }

                /* ── Find the original Reset-all / Save container ── */
                function findActionBar() {
                    var divs = document.querySelectorAll('div.flex.justify-between');
                    for (var i = 0; i < divs.length; i++) {
                        var div = divs[i];
                        var resetBtn = null, saveBtn = null;
                        div.querySelectorAll('button').forEach(function(btn) {
                            var t = btn.textContent.trim();
                            if (t === 'Reset all') resetBtn = btn;
                            if (t === 'Save')      saveBtn  = btn;
                        });
                        if (resetBtn && saveBtn) {
                            return { container: div, resetBtn: resetBtn, saveBtn: saveBtn };
                        }
                    }
                    return null;
                }

                /* ── CSS that hides every tagged original action bar ── */
                function ensureHideStyle() {
                    if (document.getElementById(HIDE_STYLE)) return;
                    var s = document.createElement('style');
                    s.id = HIDE_STYLE;
                    s.textContent = '[' + TAG_ATTR + '="true"] { visibility: hidden !important; pointer-events: none !important; }';
                    document.head.appendChild(s);
                }

                /* ── Remove floating bar and untag the original ── */
                function removeBar() {
                    document.querySelectorAll('[' + TAG_ATTR + '="true"]').forEach(function(el) {
                        el.removeAttribute(TAG_ATTR);
                    });
                    var bar = document.getElementById(BAR_ID);
                    if (!bar) return;
                    bar.style.opacity = '0';
                    setTimeout(function() {
                        var b = document.getElementById(BAR_ID);
                        if (b) b.remove();
                    }, 200);
                }

                /*
                 * ── sync: single source of truth ──
                 * Original buttons in DOM  →  show floating bar (build if needed).
                 * Original buttons gone    →  remove floating bar.
                 * This naturally handles modals, tab switches, and navigation without
                 * any explicit overlay detection.
                 */
                function sync() {
                    if (!isPhone() || !isSettingsPage()) { removeBar(); return; }

                    var found = findActionBar();
                    if (!found) { removeBar(); return; }

                    /* Tag the original so it stays invisible */
                    ensureHideStyle();
                    if (!found.container.hasAttribute(TAG_ATTR)) {
                        found.container.setAttribute(TAG_ATTR, 'true');
                    }

                    /* Bar already present — nothing more to do */
                    if (document.getElementById(BAR_ID)) return;

                    /* ── Build the floating bar ── */
                    var bar = document.createElement('div');
                    bar.id = BAR_ID;
                    bar.style.cssText = [
                        'position:fixed',
                        'bottom:4.5rem',
                        'left:0', 'right:0',
                        'z-index:8999',
                        'display:flex',
                        'align-items:center',
                        'justify-content:space-between',
                        'padding:0.6rem 1rem',
                        'pointer-events:none',
                        'opacity:0',
                        'transition:opacity 0.2s ease'
                    ].join(';');

                    /* ─ Reset All: frosted-glass pill ─ */
                    var resetPill = document.createElement('button');
                    resetPill.textContent = 'Reset all';
                    resetPill.style.cssText = [
                        'pointer-events:auto',
                        'height:2.5rem',
                        'padding:0 1.35rem',
                        'border-radius:9999px',
                        'border:1px solid rgba(239,68,68,0.35)',
                        'background:rgba(12,12,18,0.88)',
                        'backdrop-filter:blur(20px)',
                        '-webkit-backdrop-filter:blur(20px)',
                        'color:var(--red,#ef4444)',
                        'font-size:0.875rem',
                        'font-weight:600',
                        'cursor:pointer',
                        'box-shadow:0 4px 18px rgba(0,0,0,0.45)',
                        'transition:background 0.15s, opacity 0.15s',
                        '-webkit-tap-highlight-color:transparent'
                    ].join(';');
                    resetPill.addEventListener('click', function() {
                        var f = findActionBar();
                        if (f && f.resetBtn) {
                            f.resetBtn.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
                        }
                    });

                    /* ─ Save: dark green button, no glow ─ */
                    var saveBtn = document.createElement('button');
                    saveBtn.textContent = 'Save';
                    saveBtn.style.cssText = [
                        'pointer-events:auto',
                        'height:2.5rem',
                        'padding:0 1.5rem',
                        'border-radius:0.5rem',
                        'border:1px solid rgba(20,83,45,0.6)',
                        'background:#166534',
                        'color:white',
                        'font-size:0.875rem',
                        'font-weight:600',
                        'cursor:pointer',
                        'transition:opacity 0.15s',
                        '-webkit-tap-highlight-color:transparent'
                    ].join(';');
                    saveBtn.addEventListener('click', function() {
                        var f = findActionBar();
                        if (!f) return;
                        var form = f.saveBtn.closest('form');
                        if (form) {
                            try { form.requestSubmit(f.saveBtn); }
                            catch (e) {
                                f.saveBtn.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
                            }
                        } else {
                            f.saveBtn.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
                        }
                    });

                    bar.appendChild(resetPill);
                    bar.appendChild(saveBtn);
                    document.body.appendChild(bar);
                    requestAnimationFrame(function() { bar.style.opacity = '1'; });
                }

                /* ── Initial sync ── */
                sync();

                /* ── MutationObserver: re-sync on every DOM change ── */
                var syncTimer = null;
                new MutationObserver(function() {
                    if (syncTimer) return;
                    syncTimer = setTimeout(function() {
                        syncTimer = null;
                        sync();
                    }, 80);
                }).observe(document.body, { childList: true, subtree: true });

                /* ── SPA navigation ── */
                var lastHref = window.location.href;
                setInterval(function() {
                    if (window.location.href !== lastHref) {
                        lastHref = window.location.href;
                        sync();
                        setTimeout(sync, 400);
                        setTimeout(sync, 900);
                    }
                }, 300);

                /* ── Resize ── */
                window.addEventListener('resize', sync);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }
}
