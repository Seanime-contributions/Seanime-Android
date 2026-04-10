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
                [data-main-layout-content="true"] {
                    padding-bottom: 4.5rem !important;
                }

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
                    position: relative;
                    user-select: none;
                    -webkit-user-select: none;
                    touch-action: manipulation;
                }
                #__seanime_bottom_nav a svg,
                #__seanime_bottom_nav button svg {
                    width: 22px;
                    height: 22px;
                    flex-shrink: 0;
                }
                #__seanime_bottom_nav a:active,
                #__seanime_bottom_nav button:active {
                    color: rgba(255,255,255,0.7);
                }
                #__seanime_bottom_nav button[data-menu-open="true"] {
                    color: white;
                }

                /* ── Drag handle on nav items ── */
                .seanime-nav-drag-handle {
                    position: absolute;
                    top: 2px;
                    right: 2px;
                    width: 16px;
                    height: 16px;
                    display: none;
                    align-items: center;
                    justify-content: center;
                    color: rgba(255,255,255,0.7);
                    font-size: 11px;
                    line-height: 1;
                    cursor: grab;
                    z-index: 10;
                    pointer-events: auto;
                    touch-action: none;
                }
                .seanime-edit-mode .seanime-nav-drag-handle {
                    display: flex;
                }

                /* ── Nav drop target highlight ── */
                #__seanime_bottom_nav a.seanime-drop-target,
                #__seanime_bottom_nav button.seanime-drop-target {
                    background: rgba(255,255,255,0.1);
                    border-radius: 8px;
                }

                /* ── Floating menu ── */
                #android-floating-menu {
                    bottom: 4.5rem !important;
                    right: 1.25rem !important;
                }

                /* ── Wrench button ── */
                #__seanime_wrench_btn {
                    position: fixed;
                    z-index: 9100;
                    width: 36px;
                    height: 36px;
                    border-radius: 10px;
                    background: rgba(20,20,28,0.92);
                    border: 1px solid rgba(255,255,255,0.12);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: rgba(255,255,255,0.55);
                    cursor: pointer;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.5);
                    transition: color 0.15s, background 0.15s, border-color 0.15s, opacity 0.2s ease;
                    -webkit-tap-highlight-color: transparent;
                    touch-action: manipulation;
                    user-select: none;
                    -webkit-user-select: none;
                    opacity: 0;
                    pointer-events: none;
                    left: 1.25rem;
                    bottom: calc(4.5rem + env(safe-area-inset-bottom, 0px));
                }
                #__seanime_wrench_btn.wrench-visible {
                    opacity: 1;
                    pointer-events: auto;
                }
                #__seanime_wrench_btn.wrench-hidden-for-menu {
                    opacity: 0 !important;
                    pointer-events: none !important;
                }
                #__seanime_wrench_btn.wrench-active {
                    background: rgba(255,255,255,0.12);
                    border-color: rgba(255,255,255,0.3);
                    color: white;
                }

                /* ── Reset button ── */
                #__seanime_reset_btn {
                    position: fixed;
                    z-index: 9100;
                    width: 34px;
                    height: 34px;
                    border-radius: 999px;
                    background: rgba(20,20,28,0.92);
                    border: 1px solid rgba(239,68,68,0.9);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: rgba(239,68,68,0.95);
                    cursor: pointer;
                    box-shadow: 0 6px 18px rgba(0,0,0,0.55);
                    transition: opacity 0.2s ease, transform 0.22s cubic-bezier(0.2,0.9,0.2,1);
                    -webkit-tap-highlight-color: transparent;
                    touch-action: manipulation;
                    user-select: none;
                    -webkit-user-select: none;
                    opacity: 0;
                    pointer-events: none;
                    left: 1.25rem;
                    bottom: calc(4.5rem + env(safe-area-inset-bottom, 0px));
                    transform: translate(-14px, 14px) scale(0.95);
                }
                #__seanime_reset_btn.reset-visible {
                    opacity: 1;
                    pointer-events: auto;
                    transform: translate(44px, -44px) scale(1);
                }
                #__seanime_reset_btn.reset-hidden-for-menu {
                    opacity: 0 !important;
                    pointer-events: none !important;
                }

                /* ── Reset dialog ── */
                #__seanime_reset_dialog_overlay {
                    position: fixed;
                    inset: 0;
                    z-index: 999999;
                    background: rgba(0,0,0,0.55);
                    backdrop-filter: blur(8px);
                    -webkit-backdrop-filter: blur(8px);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    padding: 24px;
                    opacity: 0;
                    pointer-events: none;
                    transition: opacity 0.18s ease;
                }
                #__seanime_reset_dialog_overlay.visible {
                    opacity: 1;
                    pointer-events: auto;
                }
                #__seanime_reset_dialog {
                    width: min(360px, 92vw);
                    border-radius: 14px;
                    background: rgba(20,20,28,0.96);
                    border: 1px solid rgba(255,255,255,0.12);
                    box-shadow: 0 18px 50px rgba(0,0,0,0.65);
                    padding: 14px 14px 12px 14px;
                    transform: translateY(8px) scale(0.98);
                    transition: transform 0.18s ease;
                }
                #__seanime_reset_dialog_overlay.visible #__seanime_reset_dialog {
                    transform: translateY(0) scale(1);
                }
                #__seanime_reset_dialog_title {
                    color: rgba(255,255,255,0.92);
                    font-size: 15px;
                    font-weight: 700;
                    margin: 0 0 6px 0;
                    letter-spacing: 0.01em;
                }
                #__seanime_reset_dialog_desc {
                    color: rgba(255,255,255,0.65);
                    font-size: 13px;
                    font-weight: 500;
                    margin: 0 0 12px 0;
                    line-height: 1.35;
                }
                #__seanime_reset_dialog_actions {
                    display: flex;
                    gap: 10px;
                    justify-content: flex-end;
                }
                .__seanime_dialog_btn {
                    appearance: none;
                    border: 1px solid rgba(255,255,255,0.12);
                    background: rgba(255,255,255,0.06);
                    color: rgba(255,255,255,0.9);
                    font-size: 13px;
                    font-weight: 700;
                    padding: 10px 12px;
                    border-radius: 12px;
                    cursor: pointer;
                    -webkit-tap-highlight-color: transparent;
                }
                .__seanime_dialog_btn:active {
                    background: rgba(255,255,255,0.1);
                }
                .__seanime_dialog_btn_danger {
                    border-color: rgba(239,68,68,0.45);
                    background: rgba(239,68,68,0.14);
                    color: rgba(255,255,255,0.95);
                }
                .__seanime_dialog_btn_danger:active {
                    background: rgba(239,68,68,0.22);
                }

                /* ── Float drag handle ── */
                .float-drag-handle {
                    position: absolute;
                    top: 2px;
                    right: 2px;
                    width: 14px;
                    height: 14px;
                    display: none;
                    align-items: center;
                    justify-content: center;
                    color: rgba(255,255,255,0.7);
                    font-size: 10px;
                    line-height: 1;
                    cursor: grab;
                    z-index: 10;
                    pointer-events: auto;
                    touch-action: none;
                }
                .seanime-edit-mode .float-drag-handle {
                    display: flex;
                }

                /* ── Float drop target highlight ── */
                .float-nav-item.seanime-float-drop-target .float-nav-icon {
                    background: rgba(255,255,255,0.2) !important;
                    border-color: rgba(255,255,255,0.4) !important;
                }

                /* ── Float hidden state ── */
                .float-nav-item[data-hidden="true"] .float-nav-icon {
                    opacity: 0.3;
                }

                /* ── Drag ghost clone ── */
                .seanime-drag-ghost {
                    position: fixed;
                    z-index: 99999;
                    pointer-events: none;
                    opacity: 0.85;
                    transform: scale(1.1);
                    border-radius: 12px;
                    box-shadow: 0 8px 24px rgba(0,0,0,0.6);
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

            var TABLET_BREAKPOINT = 768;
            function isTablet() { return window.innerWidth >= TABLET_BREAKPOINT; }

            if (isTablet()) {
                window.addEventListener('resize', function() {
                    if (!isTablet()) {
                        window.__seanime_bottomnav_init = false;
                        setTimeout(function() { if (!window.__seanime_bottomnav_init) initNav(); }, 150);
                    }
                });
                return;
            }

            initNav();

            function initNav() {
                if (window.__seanime_bottomnav_init && document.getElementById('__seanime_bottom_nav')) return;
                window.__seanime_bottomnav_init = true;

                // ─────────────────────────────────────────────────────────────
                // STORAGE KEYS
                // ─────────────────────────────────────────────────────────────
                var LS_NAV_ORDER     = '__seanime_nav_order';
                var LS_NAV_PLACEMENT = '__seanime_nav_placement';
                var LS_NAV_ITEMS     = '__seanime_nav_items_full';
                var LS_FLOAT_HIDDEN  = '__seanime_float_hidden';
                var LS_FLOAT_ORDER   = '__seanime_float_order';

                // ─────────────────────────────────────────────────────────────
                // SVG CONSTANTS — defined once so they are always available
                // ─────────────────────────────────────────────────────────────
                var SVG_HAMBURGER = '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="6" x2="20" y2="6"/><line x1="4" y1="18" x2="20" y2="18"/></svg>';
                var SVG_CLOSE     = '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>';

                // ─────────────────────────────────────────────────────────────
                // NAV ITEM DEFINITIONS
                // ─────────────────────────────────────────────────────────────
                var DEFAULT_NAV_ITEMS = [
                    {
                        id: 'home',
                        href: '/',
                        label: 'Home',
                        matchExact: true,
                        placement: 'nav',
                        svg: '<svg stroke="currentColor" fill="currentColor" stroke-width="0" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg"><path fill="none" stroke-linecap="round" stroke-linejoin="round" stroke-width="32" d="M80 212v236a16 16 0 0 0 16 16h96V328a24 24 0 0 1 24-24h80a24 24 0 0 1 24 24v136h96a16 16 0 0 0 16-16V212"/><path fill="none" stroke-linecap="round" stroke-linejoin="round" stroke-width="32" d="M480 256 266.89 52c-5-5.28-16.69-5.34-21.78 0L32 256m368-77V64h-48v69"/></svg>'
                    },
                    {
                        id: 'lists',
                        href: '/lists',
                        label: 'My Lists',
                        matchExact: false,
                        placement: 'nav',
                        svg: '<svg stroke="currentColor" fill="currentColor" stroke-width="0" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M8 6V9H5V6H8ZM3 4V11H10V4H3ZM13 4H21V6H13V4ZM13 11H21V13H13V11ZM13 18H21V20H13V18ZM10.707 16.207L9.293 14.793 6 18.086 4.207 16.293 2.793 17.707 6 20.914Z"/></svg>'
                    },
                    {
                        id: 'discover',
                        href: '/discover',
                        label: 'Discover',
                        matchExact: false,
                        placement: 'nav',
                        svg: '<svg stroke="currentColor" fill="none" stroke-width="2" viewBox="0 0 24 24" stroke-linecap="round" stroke-linejoin="round" xmlns="http://www.w3.org/2000/svg"><path d="m16.24 7.76-1.804 5.411a2 2 0 0 1-1.265 1.265L7.76 16.24l1.804-5.411a2 2 0 0 1 1.265-1.265z"/><circle cx="12" cy="12" r="10"/></svg>'
                    },
                    {
                        id: 'search',
                        href: '/search',
                        label: 'Search',
                        matchExact: false,
                        placement: 'nav',
                        svg: '<svg stroke="currentColor" fill="none" stroke-width="2" viewBox="0 0 24 24" stroke-linecap="round" stroke-linejoin="round" xmlns="http://www.w3.org/2000/svg"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>'
                    },
                    {
                        id: 'menu',
                        label: 'Menu',
                        isMenu: true,
                        immovableToFloat: true,
                        placement: 'nav',
                        svg: SVG_HAMBURGER,
                        svgClose: SVG_CLOSE
                    }
                ];

                // ─────────────────────────────────────────────────────────────
                // PERSISTENCE HELPERS
                // ─────────────────────────────────────────────────────────────
                function normalizeNavItem(it) {
                    if (!it || !it.id) return null;
                    // For the menu item, always use the SVG constants so they
                    // survive localStorage round-trips intact.
                    var isMenu = !!it.isMenu;
                    return {
                        id: it.id,
                        href: it.href !== undefined ? it.href : null,
                        label: it.label || it.id,
                        matchExact: !!it.matchExact,
                        placement: it.placement || 'nav',
                        svg: isMenu ? SVG_HAMBURGER : (it.svg || ''),
                        svgClose: isMenu ? SVG_CLOSE : (it.svgClose || ''),
                        isMenu: isMenu,
                        immovableToFloat: !!it.immovableToFloat,
                        isSidebarItem: !!it.isSidebarItem,
                        sidebarType: it.sidebarType || null
                    };
                }

                function loadNavItems() {
                    try {
                        var savedFull = JSON.parse(localStorage.getItem(LS_NAV_ITEMS) || 'null');
                        if (Array.isArray(savedFull) && savedFull.length) {
                            var rebuilt = [];
                            savedFull.forEach(function(x) {
                                var n = normalizeNavItem(x);
                                if (n) rebuilt.push(n);
                            });
                            if (rebuilt.length) return rebuilt;
                        }
                    } catch(e) {}

                    var items = DEFAULT_NAV_ITEMS.map(function(d) { return Object.assign({}, d); });
                    try {
                        var savedPlacement = JSON.parse(localStorage.getItem(LS_NAV_PLACEMENT) || 'null');
                        if (savedPlacement) {
                            items.forEach(function(it) {
                                if (savedPlacement[it.id] !== undefined) it.placement = savedPlacement[it.id];
                            });
                        }
                        var savedOrder = JSON.parse(localStorage.getItem(LS_NAV_ORDER) || 'null');
                        if (Array.isArray(savedOrder)) {
                            var ordered = [];
                            savedOrder.forEach(function(id) {
                                for (var i = 0; i < items.length; i++) {
                                    if (items[i].id === id) { ordered.push(items[i]); break; }
                                }
                            });
                            items.forEach(function(it) {
                                var found = false;
                                ordered.forEach(function(o) { if (o.id === it.id) found = true; });
                                if (!found) ordered.push(it);
                            });
                            items = ordered;
                        }
                    } catch(e) {}
                    return items;
                }

                function saveNavState() {
                    try {
                        localStorage.setItem(LS_NAV_ITEMS, JSON.stringify(NAV_ITEMS));
                        localStorage.setItem(LS_NAV_ORDER, JSON.stringify(NAV_ITEMS.map(function(it) { return it.id; })));
                        var placement = {};
                        NAV_ITEMS.forEach(function(it) { placement[it.id] = it.placement; });
                        localStorage.setItem(LS_NAV_PLACEMENT, JSON.stringify(placement));
                    } catch(e) {}
                }

                function loadFloatHidden() {
                    try { return JSON.parse(localStorage.getItem(LS_FLOAT_HIDDEN) || '{}'); } catch(e) { return {}; }
                }
                function saveFloatHidden(map) {
                    try { localStorage.setItem(LS_FLOAT_HIDDEN, JSON.stringify(map)); } catch(e) {}
                }
                function loadFloatOrder() {
                    try { return JSON.parse(localStorage.getItem(LS_FLOAT_ORDER) || 'null'); } catch(e) { return null; }
                }
                function saveFloatOrder(arr) {
                    try { localStorage.setItem(LS_FLOAT_ORDER, JSON.stringify(arr)); } catch(e) {}
                }

                // ─────────────────────────────────────────────────────────────
                // STATE
                // ─────────────────────────────────────────────────────────────
                var NAV_ITEMS = loadNavItems();
                var menuOpen  = false;
                var editMode  = false;
                var menuBtn   = null;

                // floatSidebarItems: ordered list of sidebar items shown in float panel
                // Each: { label, href, type, svg, isCurrent }
                var floatSidebarItems = [];

                function getItem(id) {
                    for (var i = 0; i < NAV_ITEMS.length; i++) { if (NAV_ITEMS[i].id === id) return NAV_ITEMS[i]; }
                    return null;
                }
                function isActive(item) {
                    if (item.isMenu) return false;
                    var path = window.location.pathname;
                    if (item.matchExact) return path === item.href;
                    return path === item.href || path.indexOf(item.href + '/') === 0;
                }
                function suppressDefault(e) { e.preventDefault(); }

                // Returns the svg string for the hamburger icon — always reliable.
                function getMenuSvg() { return SVG_HAMBURGER; }
                // Returns the svg string for the close icon — always reliable.
                function getMenuCloseSvg() { return SVG_CLOSE; }

                function navItemToFloatPill(navItem) {
                    return {
                        type: 'link',
                        label: navItem.label,
                        href: navItem.href || '#',
                        svg: navItem.svg || '',
                        isCurrent: isActive(navItem)
                    };
                }

                // ─────────────────────────────────────────────────────────────
                // Helper: get set of labels that are currently in the bottom nav
                // (excludes the menu button itself)
                // ─────────────────────────────────────────────────────────────
                function getNavLabelsSet() {
                    var labels = new Set();
                    for (var i = 0; i < NAV_ITEMS.length; i++) {
                        var it = NAV_ITEMS[i];
                        if (it.placement === 'nav' && it.id !== 'menu') {
                            labels.add(it.label);
                        }
                    }
                    return labels;
                }

                // ─────────────────────────────────────────────────────────────
                // SIDEBAR SNAPSHOT (with automatic removal of items already in nav)
                // ─────────────────────────────────────────────────────────────
                function snapshotSidebarItems() {
                    var items = [];
                    var existing = {};
                    var navLabels = getNavLabelsSet();
                    
                    // First, collect all sidebar items with duplicate prevention
                    document.querySelectorAll('.UI-AppSidebar__sidebar a[data-vertical-menu-item-link]').forEach(function(a) {
                        if (a.style.display === 'none' && !a.dataset.hiddenByUs) return;
                        var label = a.getAttribute('data-vertical-menu-item-link');
                        var href  = a.getAttribute('href');
                        var isCurrent = a.getAttribute('data-current') === 'true';
                        var svgEl = a.querySelector('svg');
                        if (label && href && !existing[label] && !navLabels.has(label)) {
                            items.push({ type: 'link', label: label, href: href, svg: svgEl ? svgEl.outerHTML : '', isCurrent: isCurrent });
                            existing[label] = true;
                        }
                    });
                    document.querySelectorAll('.UI-AppSidebar__sidebar button[data-vertical-menu-item-button]').forEach(function(btn) {
                        if (btn.style.display === 'none' && !btn.dataset.hiddenByUs) return;
                        var label = btn.getAttribute('data-vertical-menu-item-button');
                        var svgEl = btn.querySelector('svg');
                        if (label && !existing[label] && !navLabels.has(label)) {
                            items.push({ type: 'button', label: label, svg: svgEl ? svgEl.outerHTML : '' });
                            existing[label] = true;
                        }
                    });

                    // Then add nav items that are in float placement, but only if not already in nav (should not happen) and not duplicate label
                    NAV_ITEMS.filter(function(it) { return it.placement === 'float' && !it.isMenu; }).forEach(function(navIt) {
                        if (!existing[navIt.label] && !navLabels.has(navIt.label)) {
                            items.push(navItemToFloatPill(navIt));
                            existing[navIt.label] = true;
                        }
                    });

                    return items;
                }

                function applyFloatOrder(snapshot) {
                    var order = loadFloatOrder();
                    if (!order) return snapshot;
                    var ordered = [];
                    order.forEach(function(lbl) {
                        for (var i = 0; i < snapshot.length; i++) {
                            if (snapshot[i].label === lbl) { ordered.push(snapshot[i]); break; }
                        }
                    });
                    snapshot.forEach(function(it) {
                        var found = false;
                        ordered.forEach(function(o) { if (o.label === it.label) found = true; });
                        if (!found) ordered.push(it);
                    });
                    return ordered;
                }

                function resetToDefaults() {
                    try {
                        localStorage.removeItem(LS_NAV_ORDER);
                        localStorage.removeItem(LS_NAV_PLACEMENT);
                        localStorage.removeItem(LS_NAV_ITEMS);
                        localStorage.removeItem(LS_FLOAT_HIDDEN);
                        localStorage.removeItem(LS_FLOAT_ORDER);
                    } catch(e) {}

                    NAV_ITEMS = DEFAULT_NAV_ITEMS.map(function(d) { return Object.assign({}, d); });
                    saveNavState();

                    saveFloatHidden({});
                    saveFloatOrder([]);

                    var snapshot = snapshotSidebarItems();
                    floatSidebarItems = applyFloatOrder(snapshot);
                    refreshNavBar();

                    if (menuOpen) closeMenu();
                    hideWrench();
                }

                // ─────────────────────────────────────────────────────────────
                // FLOATING MENU — normal (non-edit) open/close
                // ─────────────────────────────────────────────────────────────
                function openMenu() {
                    var menu = document.getElementById('android-floating-menu');
                    if (!menu) return;

                    var snapshot = snapshotSidebarItems();
                    floatSidebarItems = applyFloatOrder(snapshot);
                    var hiddenMap = loadFloatHidden();

                    menu.innerHTML = '';
                    // Reverse so last item in array appears at bottom (visually stacks upward)
                    var visible = floatSidebarItems.slice().reverse().filter(function(it) { return !hiddenMap[it.label]; });

                    visible.forEach(function(item) {
                        var row = document.createElement('div');
                        row.className = 'float-nav-item';

                        var tooltip = document.createElement('span');
                        tooltip.className = 'float-nav-tooltip';
                        tooltip.textContent = item.label;
                        tooltip.style.cssText = 'position:absolute;right:64px;background:rgba(15,15,15,0.95);color:white;font-size:13px;font-weight:500;padding:6px 12px;border-radius:8px;white-space:nowrap;border:1px solid rgba(255,255,255,0.1);backdrop-filter:blur(10px);pointer-events:none;opacity:0;transform:translateX(12px);transition:none;';

                        var iconWrap = document.createElement('div');
                        iconWrap.className = 'float-nav-icon' + (item.isCurrent ? ' float-nav-icon--active' : '');
                        iconWrap.innerHTML = item.svg || '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/></svg>';
                        var svgEl = iconWrap.querySelector('svg');
                        if (svgEl) {
                            svgEl.setAttribute('width', '22');
                            svgEl.setAttribute('height', '22');
                            svgEl.style.cssText = 'width:22px!important;height:22px!important;display:block!important;margin:0!important;';
                        }

                        row.appendChild(tooltip);
                        row.appendChild(iconWrap);
                        menu.appendChild(row);

                        row.addEventListener('click', function(e) {
                            e.stopPropagation();
                            if (editMode) return;
                            if (item.type === 'link' && (
                                window.location.pathname === item.href ||
                                window.location.pathname.indexOf(item.href + '/') === 0
                            )) { closeMenu(); return; }
                            closeMenu(function() {
                                if (item.type === 'link') {
                                    window.location.href = item.href;
                                } else {
                                    var t = document.querySelector('.UI-AppSidebar__sidebar button[data-vertical-menu-item-button="' + item.label.replace(/"/g, '\\"') + '"]');
                                    if (t) t.click();
                                }
                            });
                        });
                    });

                    menu.style.display = 'flex';
                    menu.style.pointerEvents = 'auto';
                    menuOpen = true;

                    showWrench();

                    if (menuBtn) {
                        menuBtn.innerHTML = getMenuCloseSvg() + '<span>Menu</span>';
                        menuBtn.setAttribute('data-menu-open', 'true');
                    }

                    // Animate: icons slide up first, tooltips slide out afterward
                    var rows = menu.querySelectorAll('.float-nav-item');
                    rows.forEach(function(el, i) {
                        var tip = el.querySelector('.float-nav-tooltip');
                        el.style.opacity = '0';
                        el.style.transform = 'translateY(10px)';

                        var iconDelay = i * 40;
                        setTimeout(function() {
                            el.style.transition = 'opacity 0.2s ease, transform 0.25s cubic-bezier(0.17,0.67,0.83,0.67)';
                            el.style.opacity = '1';
                            el.style.transform = 'translateY(0)';
                        }, iconDelay);

                        // Tooltip emerges from behind icon after icon has landed
                        var tipDelay = iconDelay + 200;
                        setTimeout(function() {
                            if (!tip) return;
                            tip.style.transition = 'opacity 0.18s ease, transform 0.22s cubic-bezier(0.34,1.4,0.64,1)';
                            tip.style.opacity = '1';
                            tip.style.transform = 'translateX(0)';
                        }, tipDelay);
                    });
                }

                function closeMenu(onComplete) {
                    var menu = document.getElementById('android-floating-menu');
                    menuOpen = false;

                    if (menuBtn) {
                        menuBtn.innerHTML = getMenuSvg() + '<span>Menu</span>';
                        menuBtn.setAttribute('data-menu-open', 'false');
                    }

                    if (!editMode) hideWrench();

                    if (!menu) { if (onComplete) onComplete(); return; }
                    menu.style.pointerEvents = 'none';

                    var rows = menu.querySelectorAll('.float-nav-item');
                    if (rows.length === 0) {
                        menu.style.display = 'none';
                        if (onComplete) onComplete();
                        return;
                    }
                    var reversed = Array.prototype.slice.call(rows).reverse();
                    reversed.forEach(function(el, i) {
                        setTimeout(function() {
                            el.style.transition = 'opacity 0.15s ease, transform 0.18s ease';
                            el.style.opacity = '0';
                            el.style.transform = 'translateY(8px)';
                        }, i * 30);
                    });
                    setTimeout(function() {
                        menu.style.display = 'none';
                        if (onComplete) onComplete();
                    }, (reversed.length - 1) * 30 + 180);
                }

                // ─────────────────────────────────────────────────────────────
                // SIDEBAR TRIGGERS
                // ─────────────────────────────────────────────────────────────
                function hideSidebarTriggers() {
                    if (isTablet()) return;
                    document.querySelectorAll('.UI-AppSidebarTrigger__trigger').forEach(function(btn) {
                        btn.dataset.hiddenByUs = 'true';
                        btn.style.setProperty('display', 'none', 'important');
                    });
                }
                function restoreSidebarTriggers() {
                    document.querySelectorAll('.UI-AppSidebarTrigger__trigger[data-hidden-by-us]').forEach(function(btn) {
                        delete btn.dataset.hiddenByUs;
                        btn.style.removeProperty('display');
                    });
                }

                function isReaderOpen() { return !!document.querySelector('[data-manga-reader-bar="true"]'); }
                function isDrawerOpen() {
                    if (document.querySelector('[data-vaul-drawer][data-state="open"]')) return true;
                    if (document.querySelector('.UI-Drawer__content[data-state="open"]')) return true;
                    if (document.querySelector('.UI-Modal__overlay[data-state="open"]')) return true;
                    if (document.querySelector('.UI-Modal__content[data-state="open"]')) return true;
                    return false;
                }
                function updateNavVisibility() {
                    var nav = document.getElementById('__seanime_bottom_nav');
                    if (!nav) return;
                    if (isTablet()) { nav.classList.remove('visible'); restoreSidebarTriggers(); hideWrench(); return; }
                    if (isReaderOpen() || isDrawerOpen()) {
                        nav.classList.remove('visible');
                        hideWrench();
                    } else if (document.querySelector('.UI-AppSidebar__sidebar')) {
                        nav.classList.add('visible');
                        if (menuOpen || editMode) showWrench(); else hideWrench();
                    }
                }
                function showNav() {
                    if (isTablet()) return;
                    var nav = document.getElementById('__seanime_bottom_nav');
                    if (nav && !isReaderOpen() && !isDrawerOpen()) nav.classList.add('visible');
                    hideSidebarTriggers();
                    if (menuOpen || editMode) showWrench(); else hideWrench();
                }

                // ─────────────────────────────────────────────────────────────
                // DRAWER OBSERVERS
                // ─────────────────────────────────────────────────────────────
                var drawerAttrObs = new MutationObserver(function(mutations) {
                    mutations.forEach(function(m) {
                        if (m.attributeName === 'data-state' &&
                            (m.target.hasAttribute('data-vaul-drawer') ||
                             m.target.classList.contains('UI-Drawer__content') ||
                             m.target.classList.contains('UI-Modal__overlay') ||
                             m.target.classList.contains('UI-Modal__content'))) {
                            updateNavVisibility();
                        }
                    });
                });
                function observeExistingDrawers() {
                    document.querySelectorAll('[data-vaul-drawer], .UI-Drawer__content, .UI-Modal__overlay, .UI-Modal__content').forEach(function(el) {
                        drawerAttrObs.observe(el, { attributes: true, attributeFilter: ['data-state'] });
                    });
                }
                new MutationObserver(function(mutations) {
                    var needsCheck = false;
                    mutations.forEach(function(m) {
                        m.addedNodes.forEach(function(node) {
                            if (node.nodeType !== 1) return;
                            if (node.hasAttribute('data-vaul-drawer') || node.classList.contains('UI-Drawer__content') || node.classList.contains('UI-Modal__overlay') || node.classList.contains('UI-Modal__content')) {
                                drawerAttrObs.observe(node, { attributes: true, attributeFilter: ['data-state'] });
                                needsCheck = true;
                            }
                            if (node.querySelectorAll) {
                                node.querySelectorAll('[data-vaul-drawer], .UI-Drawer__content, .UI-Modal__overlay, .UI-Modal__content').forEach(function(c) {
                                    drawerAttrObs.observe(c, { attributes: true, attributeFilter: ['data-state'] });
                                    needsCheck = true;
                                });
                            }
                        });
                    });
                    if (needsCheck) updateNavVisibility();
                }).observe(document.body, { childList: true, subtree: true });
                observeExistingDrawers();

                function waitForReady() {
                    if (document.querySelector('.UI-AppSidebar__sidebar')) { showNav(); return; }
                    var obs = new MutationObserver(function() {
                        if (document.querySelector('.UI-AppSidebar__sidebar')) { obs.disconnect(); showNav(); }
                    });
                    obs.observe(document.body, { childList: true, subtree: true });
                }
                function updateActiveState() {
                    var nav = document.getElementById('__seanime_bottom_nav');
                    if (!nav) return;
                    if (menuOpen) closeMenu();
                }

                // ═════════════════════════════════════════════════════════════
                // EDIT MODE
                // ═════════════════════════════════════════════════════════════
                function enterEditMode() {
                    editMode = true;

                    if (menuOpen) {
                        menuOpen = false;
                        var menu = document.getElementById('android-floating-menu');
                        if (menu) {
                            menu.style.display = 'flex';
                            menu.style.pointerEvents = 'auto';
                        }
                        if (menuBtn) {
                            menuBtn.innerHTML = getMenuSvg() + '<span>Menu</span>';
                            menuBtn.setAttribute('data-menu-open', 'false');
                        }
                    }

                    var nav    = document.getElementById('__seanime_bottom_nav');
                    var wrench = document.getElementById('__seanime_wrench_btn');
                    if (nav)    nav.classList.add('seanime-edit-mode');
                    if (wrench) wrench.classList.add('wrench-active');

                    var snapshot = snapshotSidebarItems();
                    floatSidebarItems = applyFloatOrder(snapshot);

                    buildEditFloatPanel();
                    showWrench();
                }

                function exitEditMode() {
                    editMode = false;

                    var nav    = document.getElementById('__seanime_bottom_nav');
                    var menu   = document.getElementById('android-floating-menu');
                    var wrench = document.getElementById('__seanime_wrench_btn');

                    if (nav)    nav.classList.remove('seanime-edit-mode');
                    if (wrench) wrench.classList.remove('wrench-active');
                    if (menu)   { menu.classList.remove('seanime-edit-mode'); menu.style.display = 'none'; menu.innerHTML = ''; }

                    saveFloatOrder(floatSidebarItems.map(function(it) { return it.label; }));
                    saveNavState();
                    refreshNavBar();

                    if (menuBtn) {
                        menuBtn.innerHTML = getMenuSvg() + '<span>Menu</span>';
                        menuBtn.setAttribute('data-menu-open', 'false');
                    }

                    if (menuOpen) showWrench(); else hideWrench();
                }

                function toggleEditMode() { if (editMode) exitEditMode(); else enterEditMode(); }

                // ─────────────────────────────────────────────────────────────
                // BUILD EDIT FLOAT PANEL
                // ─────────────────────────────────────────────────────────────
                function buildEditFloatPanel() {
                    var menu = document.getElementById('android-floating-menu');
                    if (!menu) return;
                    menu.innerHTML = '';
                    menu.style.display = 'flex';
                    menu.style.pointerEvents = 'auto';
                    menu.classList.add('seanime-edit-mode');

                    var hiddenMap = loadFloatHidden();

                    var reversed = floatSidebarItems.slice().reverse();

                    reversed.forEach(function(item) {
                        var row = document.createElement('div');
                        row.className = 'float-nav-item';
                        row.dataset.floatLabel = item.label;
                        if (hiddenMap[item.label]) row.setAttribute('data-hidden', 'true');

                        var tooltip = document.createElement('span');
                        tooltip.className = 'float-nav-tooltip';
                        tooltip.textContent = item.label;
                        tooltip.style.cssText = 'position:absolute;right:64px;background:rgba(15,15,15,0.95);color:white;font-size:13px;font-weight:500;padding:6px 12px;border-radius:8px;white-space:nowrap;border:1px solid rgba(255,255,255,0.1);backdrop-filter:blur(10px);pointer-events:none;';

                        var iconWrap = document.createElement('div');
                        iconWrap.className = 'float-nav-icon' + (item.isCurrent ? ' float-nav-icon--active' : '');
                        iconWrap.innerHTML = item.svg || '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/></svg>';
                        var svgEl = iconWrap.querySelector('svg');
                        if (svgEl) {
                            svgEl.setAttribute('width', '22');
                            svgEl.setAttribute('height', '22');
                            svgEl.style.cssText = 'width:22px!important;height:22px!important;display:block!important;margin:0!important;';
                        }

                        var handle = document.createElement('div');
                        handle.className = 'float-drag-handle';
                        handle.textContent = '\u28FF';
                        handle.setAttribute('aria-hidden', 'true');

                        row.appendChild(tooltip);
                        row.appendChild(iconWrap);
                        row.appendChild(handle);
                        menu.appendChild(row);

                        iconWrap.addEventListener('click', function(e) {
                            e.stopPropagation();
                            if (!editMode) return;
                            var isHidden = row.getAttribute('data-hidden') === 'true';
                            var next = !isHidden;
                            row.setAttribute('data-hidden', next.toString());
                            var hMap = loadFloatHidden();
                            hMap[item.label] = next;
                            saveFloatHidden(hMap);
                        });

                        row.addEventListener('contextmenu', suppressDefault);
                        handle.addEventListener('contextmenu', suppressDefault);

                        (function(capturedRow, capturedItem) {
                            handle.addEventListener('touchstart', function(e) {
                                e.preventDefault();
                                e.stopPropagation();
                                if (!editMode) return;
                                var t = e.touches[0];
                                startFloatDrag(capturedRow, capturedItem, t.clientX, t.clientY);
                            }, { passive: false });
                        })(row, item);
                    });
                }

                // ─────────────────────────────────────────────────────────────
                // BUILD / REFRESH NAV BAR
                // ─────────────────────────────────────────────────────────────
                function refreshNavBar() {
                    var nav = document.getElementById('__seanime_bottom_nav');
                    if (!nav) return;

                    while (nav.firstChild) nav.removeChild(nav.firstChild);
                    menuBtn = null;

                    NAV_ITEMS.filter(function(it) { return it.placement === 'nav'; }).forEach(function(item) {
                        var el;

                        if (item.isMenu) {
                            el = document.createElement('button');
                            el.setAttribute('data-nav-id', 'menu');
                            el.setAttribute('data-menu-open', 'false');
                            // Always use the constant SVG so it's correct on first render
                            el.innerHTML = getMenuSvg() + '<span>' + item.label + '</span>';
                            menuBtn = el;

                            el.addEventListener('click', function(e) {
                                e.stopPropagation();
                                if (editMode) return;
                                if (menuOpen) closeMenu(); else openMenu();
                            });

                        } else if (item.isSidebarItem) {
                            el = document.createElement('a');
                            el.href = item.href || '#';
                            el.setAttribute('data-nav-id', item.id);
                            el.setAttribute('data-active', 'false');
                            el.innerHTML = item.svg + '<span>' + item.label + '</span>';
                            var svgEl = el.querySelector('svg');
                            if (svgEl) {
                                svgEl.setAttribute('width', '22');
                                svgEl.setAttribute('height', '22');
                                svgEl.style.cssText = 'width:22px!important;height:22px!important;display:block!important;margin:0!important;';
                            }

                            el.addEventListener('click', function(e) {
                                if (editMode) { e.preventDefault(); e.stopPropagation(); return; }
                                if (item.sidebarType === 'link' && item.href) {
                                    window.location.href = item.href;
                                } else if (item.sidebarType === 'button') {
                                    var t = document.querySelector('.UI-AppSidebar__sidebar button[data-vertical-menu-item-button="' + item.label.replace(/"/g, '\\"') + '"]');
                                    if (t) t.click();
                                }
                            });

                        } else {
                            el = document.createElement('a');
                            el.href = item.href;
                            el.setAttribute('data-nav-id', item.id);
                            el.setAttribute('data-active', 'false');
                            el.innerHTML = item.svg + '<span>' + item.label + '</span>';

                            el.addEventListener('click', function(e) {
                                if (editMode) { e.preventDefault(); e.stopPropagation(); return; }
                                if (window.location.pathname === item.href ||
                                    (!item.matchExact && window.location.pathname.indexOf(item.href + '/') === 0)) {
                                    e.preventDefault();
                                    if (menuOpen) closeMenu();
                                    return;
                                }
                                if (menuOpen) closeMenu();
                            });
                        }

                        el.addEventListener('contextmenu', suppressDefault);

                        var handle = document.createElement('div');
                        handle.className = 'seanime-nav-drag-handle';
                        handle.textContent = '\u28FF';
                        handle.setAttribute('aria-hidden', 'true');
                        el.appendChild(handle);

                        handle.addEventListener('contextmenu', suppressDefault);
                        (function(capturedEl, capturedItem) {
                            handle.addEventListener('touchstart', function(e) {
                                e.preventDefault();
                                e.stopPropagation();
                                if (!editMode) return;
                                var t = e.touches[0];
                                startNavDrag(capturedEl, capturedItem, t.clientX, t.clientY);
                            }, { passive: false });
                        })(el, item);

                        nav.appendChild(el);
                    });

                    if (editMode) nav.classList.add('seanime-edit-mode');
                }

                // ═════════════════════════════════════════════════════════════
                // DRAG & DROP
                // ═════════════════════════════════════════════════════════════
                var dragState = null;

                function createGhost(sourceEl) {
                    var rect  = sourceEl.getBoundingClientRect();
                    var ghost = sourceEl.cloneNode(true);
                    ghost.className = 'seanime-drag-ghost';
                    ghost.style.width  = rect.width  + 'px';
                    ghost.style.height = rect.height + 'px';
                    ghost.style.left   = rect.left   + 'px';
                    ghost.style.top    = rect.top    + 'px';
                    ghost.querySelectorAll('.seanime-nav-drag-handle,.float-drag-handle').forEach(function(h) { h.remove(); });
                    document.body.appendChild(ghost);
                    return ghost;
                }
                function moveGhost(ghost, cx, cy) {
                    ghost.style.left = (cx - parseFloat(ghost.style.width)  / 2) + 'px';
                    ghost.style.top  = (cy - parseFloat(ghost.style.height) / 2) + 'px';
                }
                function clearDropTargets() {
                    document.querySelectorAll('.seanime-drop-target,.seanime-float-drop-target').forEach(function(el) {
                        el.classList.remove('seanime-drop-target', 'seanime-float-drop-target');
                    });
                }
                function navElAt(x, y) {
                    var nav = document.getElementById('__seanime_bottom_nav');
                    if (!nav) return null;
                    var best = null;
                    nav.querySelectorAll('a, button').forEach(function(el) {
                        var r = el.getBoundingClientRect();
                        if (x >= r.left && x <= r.right && y >= r.top && y <= r.bottom) best = el;
                    });
                    return best;
                }
                function floatRowAt(x, y) {
                    var best = null;
                    document.querySelectorAll('#android-floating-menu .float-nav-item').forEach(function(el) {
                        var r = el.getBoundingClientRect();
                        if (x >= r.left && x <= r.right && y >= r.top && y <= r.bottom) best = el;
                    });
                    return best;
                }
                function isOverNav(x, y) {
                    var nav = document.getElementById('__seanime_bottom_nav');
                    if (!nav) return false;
                    var r = nav.getBoundingClientRect();
                    return x >= r.left && x <= r.right && y >= r.top && y <= r.bottom;
                }
                function isOverFloatMenu(x, y) {
                    var m = document.getElementById('android-floating-menu');
                    if (!m || m.style.display === 'none') return false;
                    var r = m.getBoundingClientRect();
                    return x >= r.left && x <= r.right && y >= r.top && y <= r.bottom;
                }

                function startNavDrag(navEl, item, cx, cy) {
                    var ghost = createGhost(navEl);
                    navEl.style.opacity = '0.3';
                    dragState = { type: 'nav', item: item, originEl: navEl, ghost: ghost, moved: false };
                }
                function startFloatDrag(row, sidebarItem, cx, cy) {
                    var iconWrap = row.querySelector('.float-nav-icon');
                    if (!iconWrap) return;
                    var ghost = createGhost(iconWrap);
                    iconWrap.style.opacity = '0.3';
                    dragState = { type: 'float', sidebarItem: sidebarItem, originRow: row, originIcon: iconWrap, ghost: ghost, moved: false };
                }

                function onDragMove(cx, cy) {
                    if (!dragState) return;
                    dragState.moved = true;
                    moveGhost(dragState.ghost, cx, cy);
                    clearDropTargets();
                    if (dragState.type === 'nav') {
                        var nt = navElAt(cx, cy);
                        if (nt && nt !== dragState.originEl) nt.classList.add('seanime-drop-target');
                        var ft = floatRowAt(cx, cy);
                        if (ft) ft.classList.add('seanime-float-drop-target');
                    } else {
                        var ft2 = floatRowAt(cx, cy);
                        if (ft2 && ft2 !== dragState.originRow) ft2.classList.add('seanime-float-drop-target');
                        var nt2 = navElAt(cx, cy);
                        if (nt2) nt2.classList.add('seanime-drop-target');
                    }
                }

                function onDragEnd(cx, cy) {
                    if (!dragState) return;
                    var ds = dragState;
                    dragState = null;
                    clearDropTargets();
                    if (ds.ghost && ds.ghost.parentNode) ds.ghost.parentNode.removeChild(ds.ghost);
                    if (ds.originEl)   ds.originEl.style.opacity  = '';
                    if (ds.originIcon) ds.originIcon.style.opacity = '';
                    if (!ds.moved) return;

                    if (ds.type === 'nav') {
                        if (isOverNav(cx, cy)) {
                            var nt = navElAt(cx, cy);
                            if (nt && nt !== ds.originEl) {
                                var tid = nt.getAttribute('data-nav-id');
                                if (tid) reorderNavItems(ds.item.id, tid);
                            }
                        } else if (isOverFloatMenu(cx, cy) && !ds.item.immovableToFloat) {
                            swapNavItemWithFloat(ds.item, floatRowAt(cx, cy));
                        }
                    } else if (ds.type === 'float') {
                        if (isOverFloatMenu(cx, cy)) {
                            var ft = floatRowAt(cx, cy);
                            if (ft && ft !== ds.originRow) reorderFloatSidebarItems(ds.originRow, ft);
                        } else if (isOverNav(cx, cy)) {
                            var nt2 = navElAt(cx, cy);
                            if (nt2) {
                                var tnid = nt2.getAttribute('data-nav-id');
                                if (tnid && tnid !== 'menu') swapFloatWithNavItem(ds.sidebarItem, ds.originRow, tnid);
                            }
                        }
                    }
                }

                // ─────────────────────────────────────────────────────────────
                // REORDER / SWAP LOGIC
                // ─────────────────────────────────────────────────────────────
                function reorderNavItems(idA, idB) {
                    var ia = -1, ib = -1;
                    NAV_ITEMS.forEach(function(it, i) { if (it.id === idA) ia = i; if (it.id === idB) ib = i; });
                    if (ia === -1 || ib === -1) return;
                    var tmp = NAV_ITEMS[ia]; NAV_ITEMS[ia] = NAV_ITEMS[ib]; NAV_ITEMS[ib] = tmp;
                    saveNavState();
                    refreshNavBar();
                    if (editMode) {
                        var nav = document.getElementById('__seanime_bottom_nav');
                        if (nav) nav.classList.add('seanime-edit-mode');
                    }
                }

                function reorderFloatSidebarItems(fromRow, toRow) {
                    var fromLabel = fromRow.dataset.floatLabel;
                    var toLabel   = toRow.dataset.floatLabel;
                    var fi = -1, ti = -1;
                    floatSidebarItems.forEach(function(it, i) {
                        if (it.label === fromLabel) fi = i;
                        if (it.label === toLabel)   ti = i;
                    });
                    if (fi === -1 || ti === -1) return;
                    var tmp = floatSidebarItems[fi];
                    floatSidebarItems[fi] = floatSidebarItems[ti];
                    floatSidebarItems[ti] = tmp;
                    saveFloatOrder(floatSidebarItems.map(function(it) { return it.label; }));
                    buildEditFloatPanel();
                }

                function swapNavItemWithFloat(navItem, targetRow) {
                    var targetLabel = targetRow ? targetRow.dataset.floatLabel : null;
                    var sidebarItem = null;
                    if (targetLabel) {
                        floatSidebarItems.forEach(function(it) { if (it.label === targetLabel) sidebarItem = it; });
                    }
                    if (!sidebarItem && floatSidebarItems.length > 0) sidebarItem = floatSidebarItems[0];

                    var navIdx = -1;
                    NAV_ITEMS.forEach(function(it, i) { if (it.id === navItem.id) navIdx = i; });

                    if (!sidebarItem) {
                        navItem.placement = 'float';
                        saveNavState();
                        refreshNavBar();
                        if (editMode) { document.getElementById('__seanime_bottom_nav').classList.add('seanime-edit-mode'); buildEditFloatPanel(); }
                        return;
                    }

                    // Check if sidebar item already exists in nav (prevent duplicates)
                    var newNavId = '__float_' + sidebarItem.label.replace(/\W/g, '_');
                    var existingNavIdx = -1;
                    for (var i = 0; i < NAV_ITEMS.length; i++) { 
                        if (NAV_ITEMS[i].id === newNavId || (NAV_ITEMS[i].label === sidebarItem.label && NAV_ITEMS[i].isSidebarItem)) {
                            existingNavIdx = i;
                            break;
                        }
                    }
                    if (existingNavIdx !== -1) return; // Don't create duplicate

                    var synthetic = {
                        id: newNavId,
                        href: sidebarItem.href || null,
                        label: sidebarItem.label,
                        matchExact: false,
                        placement: 'nav',
                        isSidebarItem: true,
                        sidebarType: sidebarItem.type,
                        svg: sidebarItem.svg,
                        immovableToFloat: false
                    };

                    var movedOut = Object.assign({}, navItem);
                    movedOut.placement = 'float';

                    if (navIdx !== -1) NAV_ITEMS.splice(navIdx, 1, synthetic);
                    else NAV_ITEMS.push(synthetic);

                    // Remove the pill we are swapping in from floatSidebarItems, then add the swapped-out pill
                    floatSidebarItems = floatSidebarItems.filter(function(it) { return it.label !== sidebarItem.label; });
                    floatSidebarItems.push(navItemToFloatPill(movedOut));

                    saveNavState();
                    refreshNavBar();
                    if (editMode) { document.getElementById('__seanime_bottom_nav').classList.add('seanime-edit-mode'); buildEditFloatPanel(); }
                }

                function swapFloatWithNavItem(sidebarItem, originRow, targetNavId) {
                    var targetNavItem = getItem(targetNavId);
                    if (!targetNavItem || targetNavItem.immovableToFloat) return;

                    // Check if sidebar item already exists in nav (prevent duplicates)
                    var newNavId = '__float_' + sidebarItem.label.replace(/\W/g, '_');
                    var existingNavIdx = -1;
                    for (var i = 0; i < NAV_ITEMS.length; i++) { 
                        if (NAV_ITEMS[i].id === newNavId || (NAV_ITEMS[i].label === sidebarItem.label && NAV_ITEMS[i].isSidebarItem)) {
                            existingNavIdx = i;
                            break;
                        }
                    }
                    if (existingNavIdx !== -1) return; // Don't create duplicate

                    var targetIdx = -1;
                    NAV_ITEMS.forEach(function(it, i) { if (it.id === targetNavId) targetIdx = i; });
                    if (targetIdx === -1) return;

                    var synthetic = {
                        id: newNavId,
                        href: sidebarItem.href || null,
                        label: sidebarItem.label,
                        matchExact: false,
                        placement: 'nav',
                        isSidebarItem: true,
                        sidebarType: sidebarItem.type,
                        svg: sidebarItem.svg,
                        immovableToFloat: false
                    };

                    var movedOut = Object.assign({}, targetNavItem);
                    movedOut.placement = 'float';

                    NAV_ITEMS.splice(targetIdx, 1, synthetic);

                    // Remove the pill we are swapping in from floatSidebarItems, then add the swapped-out pill
                    floatSidebarItems = floatSidebarItems.filter(function(it) { return it.label !== sidebarItem.label; });
                    floatSidebarItems.push(navItemToFloatPill(movedOut));

                    saveNavState();
                    saveFloatOrder(floatSidebarItems.map(function(it) { return it.label; }));
                    refreshNavBar();
                    if (editMode) { document.getElementById('__seanime_bottom_nav').classList.add('seanime-edit-mode'); buildEditFloatPanel(); }
                }

                function updateWrenchPosition() {
                    var wrench = document.getElementById('__seanime_wrench_btn');
                    var nav    = document.getElementById('__seanime_bottom_nav');
                    if (!wrench || !nav) return;
                    wrench.style.left   = '1.25rem';
                    wrench.style.bottom = 'calc(4.5rem + env(safe-area-inset-bottom, 0px))';
                    wrench.style.removeProperty('right');
                }
                function buildWrenchBtn() {
                    if (document.getElementById('__seanime_wrench_btn')) return;
                    var wrench = document.createElement('div');
                    wrench.id  = '__seanime_wrench_btn';
                    wrench.setAttribute('role', 'button');
                    wrench.setAttribute('aria-label', 'Customize navigation');
                    wrench.innerHTML = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>';
                    wrench.addEventListener('contextmenu', suppressDefault);
                    wrench.addEventListener('touchstart', function(e) { e.stopPropagation(); }, { passive: true });
                    wrench.addEventListener('click', function(e) { e.stopPropagation(); toggleEditMode(); });
                    document.body.appendChild(wrench);
                    updateWrenchPosition();
                }

                function buildResetDialog() {
                    if (document.getElementById('__seanime_reset_dialog_overlay')) return;

                    var overlay = document.createElement('div');
                    overlay.id = '__seanime_reset_dialog_overlay';

                    var dialog = document.createElement('div');
                    dialog.id = '__seanime_reset_dialog';

                    var title = document.createElement('div');
                    title.id = '__seanime_reset_dialog_title';
                    title.textContent = 'Reset to defaults?';

                    var desc = document.createElement('div');
                    desc.id = '__seanime_reset_dialog_desc';
                    desc.textContent = 'This will restore the default bottom nav and reset your floating menu customizations.';

                    var actions = document.createElement('div');
                    actions.id = '__seanime_reset_dialog_actions';

                    var cancelBtn = document.createElement('button');
                    cancelBtn.className = '__seanime_dialog_btn';
                    cancelBtn.textContent = 'Cancel';

                    var yesBtn = document.createElement('button');
                    yesBtn.className = '__seanime_dialog_btn __seanime_dialog_btn_danger';
                    yesBtn.textContent = 'Yes';

                    actions.appendChild(cancelBtn);
                    actions.appendChild(yesBtn);

                    dialog.appendChild(title);
                    dialog.appendChild(desc);
                    dialog.appendChild(actions);

                    overlay.appendChild(dialog);
                    document.body.appendChild(overlay);

                    overlay.addEventListener('click', function() { hideResetDialog(); });
                    dialog.addEventListener('click', function(e) { e.stopPropagation(); });

                    cancelBtn.addEventListener('click', function(e) { e.stopPropagation(); hideResetDialog(); });
                    yesBtn.addEventListener('click', function(e) {
                        e.stopPropagation();
                        hideResetDialog();
                        resetToDefaults();
                    });

                    overlay.addEventListener('contextmenu', suppressDefault);
                    dialog.addEventListener('contextmenu', suppressDefault);
                }

                function showResetDialog() {
                    buildResetDialog();
                    var overlay = document.getElementById('__seanime_reset_dialog_overlay');
                    if (!overlay) return;
                    overlay.classList.add('visible');
                }

                function hideResetDialog() {
                    var overlay = document.getElementById('__seanime_reset_dialog_overlay');
                    if (!overlay) return;
                    overlay.classList.remove('visible');
                }

                function buildResetBtn() {
                    if (document.getElementById('__seanime_reset_btn')) return;
                    var reset = document.createElement('div');
                    reset.id  = '__seanime_reset_btn';
                    reset.setAttribute('role', 'button');
                    reset.setAttribute('aria-label', 'Reset navigation');
                    reset.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>';
                    reset.addEventListener('contextmenu', suppressDefault);
                    reset.addEventListener('touchstart', function(e) { e.stopPropagation(); }, { passive: true });
                    reset.addEventListener('click', function(e) {
                        e.stopPropagation();
                        if (editMode) return;
                        showResetDialog();
                    });
                    document.body.appendChild(reset);
                }

                function showWrench() {
                    var wrench = document.getElementById('__seanime_wrench_btn');
                    var reset  = document.getElementById('__seanime_reset_btn');
                    if (wrench) { wrench.classList.add('wrench-visible'); updateWrenchPosition(); }
                    if (reset)  { reset.classList.add('reset-visible'); }
                }
                function hideWrench() {
                    var wrench = document.getElementById('__seanime_wrench_btn');
                    var reset  = document.getElementById('__seanime_reset_btn');
                    if (wrench) wrench.classList.remove('wrench-visible');
                    if (reset)  reset.classList.remove('reset-visible');
                }

                // ═════════════════════════════════════════════════════════════
                // GLOBAL TOUCH EVENTS
                // ═════════════════════════════════════════════════════════════
                document.addEventListener('touchmove', function(e) {
                    if (!dragState) return;
                    e.preventDefault();
                    var t = e.touches[0];
                    onDragMove(t.clientX, t.clientY);
                }, { passive: false });
                document.addEventListener('touchend', function(e) {
                    if (!dragState) return;
                    var t = e.changedTouches[0];
                    onDragEnd(t.clientX, t.clientY);
                }, { passive: true });
                document.addEventListener('touchcancel', function() {
                    if (!dragState) return;
                    clearDropTargets();
                    if (dragState.ghost && dragState.ghost.parentNode) dragState.ghost.parentNode.removeChild(dragState.ghost);
                    if (dragState.originEl)   dragState.originEl.style.opacity  = '';
                    if (dragState.originIcon) dragState.originIcon.style.opacity = '';
                    dragState = null;
                }, { passive: true });

                // ═════════════════════════════════════════════════════════════
                // INITIAL BUILD
                // ═════════════════════════════════════════════════════════════
                function buildNav() {
                    if (isTablet()) return;
                    if (document.getElementById('__seanime_bottom_nav')) return;

                    if (!document.getElementById('android-floating-menu')) {
                        var floatMenu = document.createElement('div');
                        floatMenu.id = 'android-floating-menu';
                        floatMenu.style.cssText = 'position:fixed;z-index:999998;display:none;flex-direction:column;align-items:center;gap:12px;width:52px;';
                        document.body.appendChild(floatMenu);
                    }

                    if (!document.getElementById('__seanime_float_item_styles')) {
                        var fs = document.createElement('style');
                        fs.id  = '__seanime_float_item_styles';
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
                    nav.addEventListener('contextmenu', suppressDefault);
                    document.body.appendChild(nav);

                    refreshNavBar();

                    document.addEventListener('click', function(e) {
                        if (editMode) {
                            var nav2    = document.getElementById('__seanime_bottom_nav');
                            var float2  = document.getElementById('android-floating-menu');
                            var wrench2 = document.getElementById('__seanime_wrench_btn');
                            var reset2  = document.getElementById('__seanime_reset_btn');
                            if (nav2    && nav2.contains(e.target))    return;
                            if (float2  && float2.contains(e.target))  return;
                            if (wrench2 && wrench2.contains(e.target)) return;
                            if (reset2  && reset2.contains(e.target))  return;
                            exitEditMode();
                            return;
                        }
                        if (menuOpen) closeMenu();
                    });

                    buildWrenchBtn();
                    buildResetBtn();
                    buildResetDialog();
                    waitForReady();
                }

                buildNav();

                var resizeThrottle = null;
                window.addEventListener('resize', function() {
                    if (resizeThrottle) return;
                    resizeThrottle = setTimeout(function() {
                        resizeThrottle = null;
                        if (isTablet()) {
                            var nav = document.getElementById('__seanime_bottom_nav');
                            if (nav) nav.classList.remove('visible');
                            hideWrench();
                            restoreSidebarTriggers();
                        } else {
                            buildNav();
                            updateNavVisibility();
                            hideSidebarTriggers();
                            if (menuOpen || editMode) showWrench(); else hideWrench();
                            updateWrenchPosition();
                        }
                    }, 150);
                });

                var _push = history.pushState.bind(history);
                history.pushState = function() { _push.apply(history, arguments); setTimeout(updateActiveState, 50); setTimeout(updateNavVisibility, 50); };
                var _replace = history.replaceState.bind(history);
                history.replaceState = function() { _replace.apply(history, arguments); setTimeout(updateActiveState, 50); setTimeout(updateNavVisibility, 50); };
                window.addEventListener('popstate', function() { setTimeout(updateActiveState, 50); setTimeout(updateNavVisibility, 50); });

                var navVisThrottle = null;
                new MutationObserver(function() {
                    if (isTablet()) return;
                    if (!document.getElementById('__seanime_bottom_nav')) {
                        menuBtn = null;
                        buildNav();
                    } else if (/\/manga\/entry/.test(window.location.pathname)) {
                        if (navVisThrottle) return;
                        navVisThrottle = setTimeout(function() { navVisThrottle = null; updateNavVisibility(); }, 200);
                    }
                }).observe(document.body, { childList: true, subtree: true });

                var wrenchObs = new MutationObserver(function() {
                    if (document.querySelector('.UI-AppSidebar__sidebar')) { wrenchObs.disconnect(); if (menuOpen || editMode) showWrench(); else hideWrench(); }
                });
                wrenchObs.observe(document.body, { childList: true, subtree: true });
                if (document.querySelector('.UI-AppSidebar__sidebar')) { if (menuOpen || editMode) showWrench(); else hideWrench(); }
            }
        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }
}
