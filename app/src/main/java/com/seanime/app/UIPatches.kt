package com.seanime.app

import android.webkit.WebView

object UIPatches {

    fun inject(webView: WebView) {
        injectSettingsFix(webView)
        injectEntryPagePatches(webView)
        injectSettingsMenuFix(webView)
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

                    // Backdrop
                    var backdrop = document.createElement('div');
                    backdrop.id = '__seanime_backdrop';
                    backdrop.style.cssText = [
                        'position:fixed',
                        'inset:0',
                        'z-index:998',
                        'background:rgba(0,0,0,0.6)',
                        'opacity:0',
                        'pointer-events:none',
                        'transition:opacity 0.25s ease'
                    ].join(';');
                    document.body.appendChild(backdrop);

                    // Drawer
                    var drawer = document.createElement('div');
                    drawer.id = '__seanime_drawer';
                    drawer.style.cssText = [
                        'position:fixed',
                        'top:0',
                        'left:0',
                        'height:100%',
                        'width:75vw',
                        'max-width:300px',
                        'z-index:999',
                        'transform:translateX(-100%)',
                        'transition:transform 0.28s cubic-bezier(0.4,0,0.2,1)',
                        'background:rgba(10,10,15,0.55)',
                        'backdrop-filter:blur(24px)',
                        '-webkit-backdrop-filter:blur(24px)',
                        'border-right:1px solid rgba(255,255,255,0.07)',
                        'display:flex',
                        'flex-direction:column',
                        'overflow-y:auto',
                        'overflow-x:hidden'
                    ].join(';');

                    // Header
                    var header = document.createElement('div');
                    header.style.cssText = 'padding:1.5rem 1.25rem 1.1rem;border-bottom:1px solid rgba(255,255,255,0.07);flex-shrink:0;';
                    header.innerHTML = '<p style="margin:0;font-size:1.1rem;font-weight:700;color:white;">Settings</p>';
                    drawer.appendChild(header);

                    // Menu items
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
                            item.style.cssText = [
                                'display:flex',
                                'align-items:center',
                                'gap:0.85rem',
                                'width:100%',
                                'padding:0.8rem 1.25rem',
                                'background:' + (isActive ? 'rgba(255,255,255,0.06)' : 'transparent'),
                                'border:none',
                                'border-left:3px solid ' + (isActive ? 'var(--brand,#818cf8)' : 'transparent'),
                                'color:' + (isActive ? 'white' : 'rgba(255,255,255,0.45)'),
                                'font-size:0.92rem',
                                'font-weight:' + (isActive ? '600' : '400'),
                                'text-align:left',
                                'cursor:pointer',
                                'flex-shrink:0',
                                'box-sizing:border-box'
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

                    // Donate button
                    var donateWrapper = document.createElement('div');
                    donateWrapper.style.cssText = 'margin-top:auto;padding:1.25rem;border-top:1px solid rgba(255,255,255,0.07);flex-shrink:0;';
                    var donateBtn = document.createElement('button');
                    donateBtn.style.cssText = [
                        'display:flex',
                        'align-items:center',
                        'gap:0.75rem',
                        'color:rgba(255,255,255,0.45)',
                        'font-size:0.92rem',
                        'background:transparent',
                        'border:none',
                        'cursor:pointer',
                        'padding:0.5rem 0',
                        'width:100%'
                    ].join(';');
                    donateBtn.innerHTML = '<svg style="width:20px;height:20px;flex-shrink:0;" stroke="currentColor" fill="currentColor" stroke-width="0" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M4 21h9.62a3.995 3.995 0 0 0 3.037-1.397l5.102-5.952a1 1 0 0 0-.442-1.6l-1.968-.656a3.043 3.043 0 0 0-2.823.503l-3.185 2.547-.617-1.235A3.98 3.98 0 0 0 9.146 11H4c-1.103 0-2 .897-2 2v6c0 1.103.897 2 2 2zm0-8h5.146c.763 0 1.448.423 1.789 1.105l.447.895H7v2h6.014a.996.996 0 0 0 .442-.11l.003-.001.004-.002h.003l.002-.001h.004l.001-.001c.009.003.003-.001.003-.001.01 0 .002-.001.002-.001h.001l.002-.001.003-.001.002-.001.002-.001.003-.001.002-.001c.003 0 .001-.001.002-.001l.003-.002.002-.001.002-.001.003-.001.002-.001h.001l.002-.001h.001l.002-.001.002-.001c.009-.001.003-.001.003-.001l.002-.001a.915.915 0 0 0 .11-.078l4.146-3.317c.262-.208.623-.273.94-.167l.557.186-4.133 4.823a2.029 2.029 0 0 1-1.52.688H4v-6zM16 2h-.017c-.163.002-1.006.039-1.983.705-.951-.648-1.774-.7-1.968-.704L12.002 2h-.004c-.801 0-1.555.313-2.119.878C9.313 3.445 9 4.198 9 5s.313 1.555.861 2.104l3.414 3.586a1.006 1.006 0 0 0 1.45-.001l3.396-3.568C18.688 6.555 19 5.802 19 5s-.313-1.555-.878-2.121A2.978 2.978 0 0 0 16.002 2H16zm1 3c0 .267-.104.518-.311.725L14 8.55l-2.707-2.843C11.104 5.518 11 5.267 11 5s.104-.518.294-.708A.977.977 0 0 1 11.979 4c.025.001.502.032 1.067.485.081.065.163.139.247.222l.707.707.707-.707c.084-.083.166-.157.247-.222.529-.425.976-.478 1.052-.484a.987.987 0 0 1 .701.292c.189.189.293.44.293.707z"></path></svg><span>Donate</span>';
                    donateBtn.addEventListener('click', function() {
                        if (window.DonateBridge) {
                            window.DonateBridge.openDonate();
                        }
                    });
                    donateWrapper.appendChild(donateBtn);
                    drawer.appendChild(donateWrapper);

                    document.body.appendChild(drawer);

                    // Toggle tab
                    var drawerW = Math.min(window.innerWidth * 0.75, 300);
                    var isOpen = false;

                    var tab = document.createElement('button');
                    tab.id = '__seanime_drawer_tab';
                    tab.style.cssText = [
                        'position:fixed',
                        'left:0',
                        'top:50%',
                        'transform:translateY(-50%)',
                        'z-index:1000',
                        'width:20px',
                        'height:56px',
                        'background:rgba(255,255,255,0.07)',
                        'backdrop-filter:blur(12px)',
                        '-webkit-backdrop-filter:blur(12px)',
                        'border:1px solid rgba(255,255,255,0.10)',
                        'border-left:none',
                        'border-radius:0 8px 8px 0',
                        'color:rgba(255,255,255,0.6)',
                        'display:flex',
                        'align-items:center',
                        'justify-content:center',
                        'cursor:pointer',
                        'padding:0',
                        'transition:left 0.28s cubic-bezier(0.4,0,0.2,1)',
                        'box-sizing:border-box'
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

                    tab.addEventListener('click', function() {
                        if (isOpen) closeDrawer(); else openDrawer();
                    });
                    backdrop.addEventListener('click', closeDrawer);

                    document.body.appendChild(tab);
                }

                buildDrawer();

                var observer = new MutationObserver(function(mutations) {
                    var added = mutations.some(function(m) { return m.addedNodes.length > 0; });
                    if (added) buildDrawer();
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

    private fun injectEntryPagePatches(webView: WebView) {
        val js = """
            (function() {
                if (window.__seanime_entry_observer) return;
                window.__seanime_entry_observer = true;

                function isEntryPage() {
                    return /\/entry\?id=/.test(window.location.href);
                }

                function injectCharacterStyles() {
                    if (document.getElementById('__seanime_characters_fix')) return;
                    var s = document.createElement('style');
                    s.id = '__seanime_characters_fix';
                    s.textContent = `
                        [data-media-entry-characters-section-grid="true"] {
                            grid-template-columns: repeat(2, minmax(0, 1fr)) !important;
                        }
                        [data-media-entry-characters-section-grid-item-container="true"] {
                            gap: 0.5rem !important;
                            padding-top: 0.5rem !important;
                            padding-bottom: 0.5rem !important;
                            padding-right: 0.5rem !important;
                        }
                        [data-media-entry-characters-section-grid-item-image-container="true"] {
                            width: 3rem !important;
                            height: 3rem !important;
                            min-width: 3rem !important;
                            min-height: 3rem !important;
                        }
                        [data-media-entry-characters-section-grid-item-content="true"] p.text-lg {
                            font-size: 0.8rem !important;
                            line-height: 1.1rem !important;
                        }
                        [data-media-entry-characters-section-grid-item-content="true"] p.text-sm {
                            font-size: 0.7rem !important;
                        }
                        [data-media-entry-characters-section-grid-item-content="true"] p.text-xs {
                            font-size: 0.65rem !important;
                        }
                        [data-media-card-grid="true"][data-carouselized="true"] {
                            display: flex !important;
                            flex-direction: row !important;
                            flex-wrap: nowrap !important;
                            overflow-x: auto !important;
                            overflow-y: visible !important;
                            gap: 0.75rem !important;
                            padding: 0.5rem 0.25rem 0.75rem 0.25rem !important;
                            scroll-behavior: smooth !important;
                            -webkit-overflow-scrolling: touch !important;
                            scrollbar-width: none !important;
                        }
                        [data-media-card-grid="true"][data-carouselized="true"]::-webkit-scrollbar {
                            display: none !important;
                        }
                        [data-media-card-grid="true"][data-carouselized="true"] .col-span-1 {
                            flex: 0 0 auto !important;
                            width: 180px !important;
                        }
                    `;
                    document.head.appendChild(s);
                }

                function carouselizeGrids() {
                    if (!isEntryPage()) return;
                    var grids = document.querySelectorAll('[data-media-card-grid="true"]:not([data-carouselized])');
                    grids.forEach(function(grid) {
                        grid.setAttribute('data-carouselized', 'true');
                    });
                }

                function applyAll() {
                    injectCharacterStyles();
                    if (isEntryPage()) carouselizeGrids();
                }

                applyAll();

                var observer = new MutationObserver(function(mutations) {
                    var relevant = mutations.some(function(m) { return m.addedNodes.length > 0; });
                    if (relevant) applyAll();
                });
                observer.observe(document.body, { childList: true, subtree: true });

                var lastHref = window.location.href;
                setInterval(function() {
                    if (window.location.href !== lastHref) {
                        lastHref = window.location.href;
                        setTimeout(applyAll, 300);
                        setTimeout(applyAll, 800);
                        setTimeout(applyAll, 1500);
                    }
                }, 300);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }
}