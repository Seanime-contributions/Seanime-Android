package com.seanime.app

import android.webkit.WebView

object UIBottomNav {

    fun inject(webView: WebView) {
        injectBottomNavStyles(webView)
        injectBottomNav(webView)
    }

    private fun injectBottomNavStyles(webView: WebView) {
        val js = """
        (function() {
            if (document.getElementById('__seanime_bottomnav_styles')) return;
            var s = document.createElement('style');
            s.id = '__seanime_bottomnav_styles';
            s.textContent = `
                /* ── Bottom nav safe-area padding so content isn't clipped ── */
                [data-main-layout-content="true"] {
                    padding-bottom: 4.5rem !important;
                }

                /* ── Bottom nav bar ── */
                #__seanime_bottom_nav {
                    position: fixed;
                    bottom: 0;
                    left: 0;
                    right: 0;
                    z-index: 9000;
                    height: 4rem;
                    display: flex;
                    align-items: stretch;
                    background: rgba(10, 10, 15, 0.82);
                    backdrop-filter: blur(20px);
                    -webkit-backdrop-filter: blur(20px);
                    border-top: 1px solid rgba(255,255,255,0.07);
                    padding-bottom: env(safe-area-inset-bottom, 0px);
                    opacity: 0;
                    pointer-events: none;
                    transition: opacity 0.25s ease;
                }
                #__seanime_bottom_nav.visible {
                    opacity: 1;
                    pointer-events: auto;
                }
                #__seanime_bottom_nav a,
                #__seanime_bottom_nav button {
                    flex: 1;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    gap: 0.22rem;
                    background: transparent;
                    border: none;
                    color: rgba(255,255,255,0.38);
                    font-size: 0.6rem;
                    font-weight: 600;
                    letter-spacing: 0.03em;
                    text-decoration: none;
                    cursor: pointer;
                    transition: color 0.15s;
                    padding: 0;
                    -webkit-tap-highlight-color: transparent;
                }
                #__seanime_bottom_nav a svg,
                #__seanime_bottom_nav button svg {
                    width: 22px;
                    height: 22px;
                    flex-shrink: 0;
                }
                #__seanime_bottom_nav a[data-active="true"],
                #__seanime_bottom_nav button[data-active="true"] {
                    color: white;
                }
                #__seanime_bottom_nav a:active,
                #__seanime_bottom_nav button:active {
                    color: rgba(255,255,255,0.7);
                }
                #__seanime_bottom_nav button[data-menu-open="true"] {
                    color: white;
                }

                /* ── Floating menu anchored above bottom nav ── */
                #android-floating-menu {
                    bottom: 4.5rem !important;
                    right: 1.25rem !important;
                }
            `;
            document.head.appendChild(s);
        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun injectBottomNav(webView: WebView) {
        val js = """
        (function() {
            if (window.__seanime_bottomnav_init) return;
            window.__seanime_bottomnav_init = true;

            var NAV_ITEMS = [
                {
                    id: 'home',
                    href: '/',
                    label: 'Home',
                    matchExact: true,
                    svg: '<svg stroke="currentColor" fill="currentColor" stroke-width="0" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg"><path fill="none" stroke-linecap="round" stroke-linejoin="round" stroke-width="32" d="M80 212v236a16 16 0 0 0 16 16h96V328a24 24 0 0 1 24-24h80a24 24 0 0 1 24 24v136h96a16 16 0 0 0 16-16V212"/><path fill="none" stroke-linecap="round" stroke-linejoin="round" stroke-width="32" d="M480 256 266.89 52c-5-5.28-16.69-5.34-21.78 0L32 256m368-77V64h-48v69"/></svg>'
                },
                {
                    id: 'lists',
                    href: '/lists',
                    label: 'My Lists',
                    matchExact: false,
                    svg: '<svg stroke="currentColor" fill="currentColor" stroke-width="0" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M8 6V9H5V6H8ZM3 4V11H10V4H3ZM13 4H21V6H13V4ZM13 11H21V13H13V11ZM13 18H21V20H13V18ZM10.707 16.207L9.293 14.793 6 18.086 4.207 16.293 2.793 17.707 6 20.914Z"/></svg>'
                },
                {
                    id: 'discover',
                    href: '/discover',
                    label: 'Discover',
                    matchExact: false,
                    svg: '<svg stroke="currentColor" fill="none" stroke-width="2" viewBox="0 0 24 24" stroke-linecap="round" stroke-linejoin="round" xmlns="http://www.w3.org/2000/svg"><path d="m16.24 7.76-1.804 5.411a2 2 0 0 1-1.265 1.265L7.76 16.24l1.804-5.411a2 2 0 0 1 1.265-1.265z"/><circle cx="12" cy="12" r="10"/></svg>'
                },
                {
                    id: 'search',
                    href: '/search',
                    label: 'Search',
                    matchExact: false,
                    svg: '<svg stroke="currentColor" fill="none" stroke-width="2" viewBox="0 0 24 24" stroke-linecap="round" stroke-linejoin="round" xmlns="http://www.w3.org/2000/svg"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>'
                },
                {
                    id: 'menu',
                    label: 'Menu',
                    isMenu: true,
                    svg: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="6" x2="20" y2="6"/><line x1="4" y1="18" x2="20" y2="18"/></svg>',
                    svgClose: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>'
                }
            ];

            var menuOpen = false;
            var menuBtn = null;

            function getMenuItem() {
                var found = null;
                NAV_ITEMS.forEach(function(n) { if (n.id === 'menu') found = n; });
                return found;
            }

            function snapshotNavItems() {
                var items = [];
                document.querySelectorAll('.UI-AppSidebar__sidebar a[data-vertical-menu-item-link]').forEach(function(a) {
                    var hiddenByApp = a.style.display === 'none' && !a.dataset.hiddenByUs;
                    if (hiddenByApp) return;
                    var label = a.getAttribute('data-vertical-menu-item-link');
                    var href = a.getAttribute('href');
                    var isCurrent = a.getAttribute('data-current') === 'true';
                    var svgEl = a.querySelector('svg');
                    var svg = svgEl ? svgEl.outerHTML : '';
                    if (label && href) items.push({ type: 'link', label: label, href: href, svg: svg, isCurrent: isCurrent });
                });
                document.querySelectorAll('.UI-AppSidebar__sidebar button[data-vertical-menu-item-button]').forEach(function(btn) {
                    var hiddenByApp = btn.style.display === 'none' && !btn.dataset.hiddenByUs;
                    if (hiddenByApp) return;
                    var label = btn.getAttribute('data-vertical-menu-item-button');
                    var svgEl = btn.querySelector('svg');
                    var svg = svgEl ? svgEl.outerHTML : '';
                    if (label) items.push({ type: 'button', label: label, svg: svg });
                });
                return items;
            }

            function buildFloatingMenu(items) {
                var menu = document.getElementById('android-floating-menu');
                if (!menu) return;
                menu.innerHTML = '';

                var reversedItems = items.slice().reverse();
                reversedItems.forEach(function(item) {
                    var row = document.createElement('div');
                    row.className = 'float-nav-item';

                    var tooltip = document.createElement('span');
                    tooltip.className = 'float-nav-tooltip';
                    tooltip.textContent = item.label;

                    var iconWrap = document.createElement('div');
                    iconWrap.className = 'float-nav-icon' + (item.isCurrent ? ' float-nav-icon--active' : '');
                    iconWrap.innerHTML = item.svg || '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/></svg>';

                    var svgInner = iconWrap.querySelector('svg');
                    if (svgInner) {
                        svgInner.setAttribute('width', '22');
                        svgInner.setAttribute('height', '22');
                        svgInner.style.cssText = 'width:22px!important;height:22px!important;display:block!important;margin:0!important;';
                    }

                    row.appendChild(tooltip);
                    row.appendChild(iconWrap);
                    menu.appendChild(row);

                    row.addEventListener('click', function(e) {
                        e.stopPropagation();
                        // Already on this page — just close the menu
                        if (item.type === 'link' && (
                            window.location.pathname === item.href ||
                            window.location.pathname.indexOf(item.href + '/') === 0
                        )) {
                            closeMenu();
                            return;
                        }
                        closeMenu(function() {
                            if (item.type === 'link') {
                                window.location.href = item.href;
                            } else if (item.type === 'button') {
                                var target = document.querySelector(
                                    '.UI-AppSidebar__sidebar button[data-vertical-menu-item-button="' + item.label.replace(/"/g, '\\"') + '"]'
                                );
                                if (target) target.click();
                            }
                        });
                    });
                });
            }

            function openMenu() {
                var menu = document.getElementById('android-floating-menu');
                if (!menu) return;
                var snapshot = snapshotNavItems();
                buildFloatingMenu(snapshot);
                menu.style.display = 'flex';
                menu.style.pointerEvents = 'auto';
                menuOpen = true;

                var menuItem = getMenuItem();
                if (menuBtn && menuItem) {
                    menuBtn.innerHTML = menuItem.svgClose + '<span>Menu</span>';
                    menuBtn.setAttribute('data-menu-open', 'true');
                }

                var items = menu.querySelectorAll('.float-nav-item');
                items.forEach(function(el, i) {
                    el.style.opacity = '0';
                    el.style.transform = 'translateY(10px)';
                    setTimeout(function() {
                        el.style.transition = 'opacity 0.2s ease, transform 0.25s cubic-bezier(0.17,0.67,0.83,0.67)';
                        el.style.opacity = '1';
                        el.style.transform = 'translateY(0)';
                    }, i * 40);
                });
            }

            function closeMenu(onComplete) {
                var menu = document.getElementById('android-floating-menu');
                menuOpen = false;

                var menuItem = getMenuItem();
                if (menuBtn && menuItem) {
                    menuBtn.innerHTML = menuItem.svg + '<span>Menu</span>';
                    menuBtn.setAttribute('data-menu-open', 'false');
                }

                if (!menu) {
                    if (onComplete) onComplete();
                    return;
                }
                menu.style.pointerEvents = 'none';
                var items = menu.querySelectorAll('.float-nav-item');
                if (items.length === 0) {
                    menu.style.display = 'none';
                    if (onComplete) onComplete();
                    return;
                }
                var reversed = Array.prototype.slice.call(items).reverse();
                reversed.forEach(function(el, i) {
                    setTimeout(function() {
                        el.style.transition = 'opacity 0.15s ease, transform 0.18s ease';
                        el.style.opacity = '0';
                        el.style.transform = 'translateY(8px)';
                    }, i * 30);
                });
                var totalDuration = (reversed.length - 1) * 30 + 180;
                setTimeout(function() {
                    menu.style.display = 'none';
                    if (onComplete) onComplete();
                }, totalDuration);
            }

            function hideSidebarTriggers() {
                document.querySelectorAll('.UI-AppSidebarTrigger__trigger').forEach(function(btn) {
                    btn.dataset.hiddenByUs = 'true';
                    btn.style.setProperty('display', 'none', 'important');
                });
            }

            function isReaderOpen() {
                return /\/manga\/entry/.test(window.location.pathname) &&
                    !!document.querySelector('[data-chapter-page-container="true"]');
            }

            function updateNavVisibility() {
                var nav = document.getElementById('__seanime_bottom_nav');
                if (!nav) return;
                if (isReaderOpen()) {
                    nav.classList.remove('visible');
                } else if (document.querySelector('.UI-AppSidebar__sidebar')) {
                    nav.classList.add('visible');
                }
            }

            function showNav() {
                var nav = document.getElementById('__seanime_bottom_nav');
                if (nav && !isReaderOpen()) nav.classList.add('visible');
                hideSidebarTriggers();
            }

            function waitForReady() {
                if (document.querySelector('.UI-AppSidebar__sidebar')) {
                    showNav();
                    return;
                }
                var observer = new MutationObserver(function() {
                    if (document.querySelector('.UI-AppSidebar__sidebar')) {
                        observer.disconnect();
                        showNav();
                    }
                });
                observer.observe(document.body, { childList: true, subtree: true });
            }

            function isActive(item) {
                if (item.isMenu) return false;
                var path = window.location.pathname;
                if (item.matchExact) return path === item.href;
                return path === item.href || path.indexOf(item.href + '/') === 0;
            }

            function updateActiveState() {
                var nav = document.getElementById('__seanime_bottom_nav');
                if (!nav) return;
                var els = nav.querySelectorAll('a[data-nav-id]');
                els.forEach(function(el) {
                    var id = el.getAttribute('data-nav-id');
                    var item = null;
                    NAV_ITEMS.forEach(function(n) { if (n.id === id) item = n; });
                    if (item) el.setAttribute('data-active', isActive(item) ? 'true' : 'false');
                });
                if (menuOpen) closeMenu();
            }

            function buildNav() {
                if (document.getElementById('__seanime_bottom_nav')) return;

                if (!document.getElementById('android-floating-menu')) {
                    var floatMenu = document.createElement('div');
                    floatMenu.id = 'android-floating-menu';
                    floatMenu.style.cssText = 'position:fixed;z-index:999998;display:none;flex-direction:column;align-items:center;gap:12px;width:52px;';
                    document.body.appendChild(floatMenu);
                }

                if (!document.getElementById('__seanime_float_item_styles')) {
                    var fs = document.createElement('style');
                    fs.id = '__seanime_float_item_styles';
                    fs.textContent = [
                        '.float-nav-item{display:flex;align-items:center;justify-content:center;position:relative;width:100%;}',
                        '.float-nav-tooltip{position:absolute;right:64px;background:rgba(15,15,15,0.95);color:white;font-size:13px;font-weight:500;padding:6px 12px;border-radius:8px;white-space:nowrap;border:1px solid rgba(255,255,255,0.1);backdrop-filter:blur(10px);pointer-events:none;}',
                        '.float-nav-icon{width:44px!important;height:44px!important;border-radius:12px;background:rgba(25,25,25,0.9);border:1px solid rgba(255,255,255,0.1);display:grid!important;place-items:center!important;color:#a0a0a0;box-shadow:0 4px 10px rgba(0,0,0,0.4);}',
                        '.float-nav-icon--active{background:rgba(255,255,255,0.15);color:white;border-color:rgba(255,255,255,0.3);}'
                    ].join('');
                    document.head.appendChild(fs);
                }

                var nav = document.createElement('nav');
                nav.id = '__seanime_bottom_nav';

                NAV_ITEMS.forEach(function(item) {
                    if (item.isMenu) {
                        var btn = document.createElement('button');
                        btn.setAttribute('data-menu-open', 'false');
                        btn.innerHTML = item.svg + '<span>' + item.label + '</span>';
                        menuBtn = btn;
                        btn.addEventListener('click', function(e) {
                            e.stopPropagation();
                            if (menuOpen) closeMenu(); else openMenu();
                        });
                        nav.appendChild(btn);
                    } else {
                        var a = document.createElement('a');
                        a.href = item.href;
                        a.setAttribute('data-nav-id', item.id);
                        a.setAttribute('data-active', isActive(item) ? 'true' : 'false');
                        a.innerHTML = item.svg + '<span>' + item.label + '</span>';
                        a.addEventListener('click', function(e) {
                            // Already on this page — don't navigate
                            if (window.location.pathname === item.href ||
                                (!item.matchExact && window.location.pathname.indexOf(item.href + '/') === 0)) {
                                e.preventDefault();
                                if (menuOpen) closeMenu();
                                return;
                            }
                            nav.querySelectorAll('a[data-nav-id]').forEach(function(n) {
                                n.setAttribute('data-active', 'false');
                            });
                            a.setAttribute('data-active', 'true');
                            if (menuOpen) closeMenu();
                        });
                        nav.appendChild(a);
                    }
                });

                document.body.appendChild(nav);

                document.addEventListener('click', function() {
                    if (menuOpen) closeMenu();
                });

                waitForReady();
            }

            buildNav();

            var _pushState = history.pushState.bind(history);
            history.pushState = function() {
                _pushState.apply(history, arguments);
                setTimeout(updateActiveState, 50);
                setTimeout(updateNavVisibility, 50);
            };
            var _replaceState = history.replaceState.bind(history);
            history.replaceState = function() {
                _replaceState.apply(history, arguments);
                setTimeout(updateActiveState, 50);
                setTimeout(updateNavVisibility, 50);
            };
            window.addEventListener('popstate', function() {
                setTimeout(updateActiveState, 50);
                setTimeout(updateNavVisibility, 50);
            });

            var navVisibilityThrottle = null;
            new MutationObserver(function() {
                if (!document.getElementById('__seanime_bottom_nav')) {
                    menuBtn = null;
                    buildNav();
                } else if (/\/manga\/entry/.test(window.location.pathname)) {
                    if (navVisibilityThrottle) return;
                    navVisibilityThrottle = setTimeout(function() {
                        navVisibilityThrottle = null;
                        updateNavVisibility();
                    }, 200);
                }
            }).observe(document.body, { childList: true, subtree: true });
        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }
}
