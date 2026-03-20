package com.seanime.app

import android.webkit.WebView

object UIHomePatch {

    fun inject(webView: WebView) {
        injectHomeStyles(webView)
        // Removed carousel and toggle injections
    }

    private fun injectHomeStyles(webView: WebView) {
        val js = """
        (function() {
            if (document.getElementById('__seanime_home_styles')) return;

            var s = document.createElement('style');
            s.id = '__seanime_home_styles';
            s.textContent = `
                /* ── Banner: shrink on mobile so it doesn't eat half the screen ── */
                [data-library-header-container="true"] {
                    height: 14rem !important;
                }
                /* Reduce banner bottom gradient/shadow height */
                [data-library-header-banner-bottom-gradient="true"] {
                    height: 8rem !important;
                }
                [data-layout-header-background-gradient="true"] {
                    height: 4rem !important;
                }
                /* Compensate top padding containers that assume full banner height */
                [data-library-toolbar-top-padding="true"].h-28 {
                    height: 4rem !important;
                }

                /* ── Top navbar: hide desktop nav links, keep the action icons ── */
                [data-top-menu="true"] {
                    display: none !important;
                }
                /* Remove the spacer so action icons sit flush right */
                [data-top-navbar-content-separator="true"] {
                    display: none !important;
                }

                /* ── Currently watching section: allow horizontal overflow ── */
                [data-library-collection-list-item-media-card-lazy-grid][data-list-type="CURRENT"] {
                    overflow: visible !important;
                }
                /* The page wrapper must not clip the carousel */
                [data-page-wrapper="true"] {
                    overflow: visible !important;
                }

                /* ── Continue watching carousel: compact card height ── */
                [data-episode-card="true"] [data-episode-card-image-container="true"] {
                    aspect-ratio: 16/7 !important;
                }

                /* ── Episode card subtitle: single line, smaller ── */
                [data-episode-card-subtitle="true"] {
                    font-size: 0.78rem !important;
                }
                [data-episode-card-subtitle="true"] span.flex-none {
                    font-size: 0.85rem !important;
                }
                [data-episode-card-title="true"] {
                    font-size: 0.92rem !important;
                    width: 100% !important;
                }

                /* ── Library grid: 2-col tighter gap on mobile ── */
                [data-media-card-grid="true"] {
                    gap: 0.65rem !important;
                }

                /* ── Media card: slightly reduce cover aspect so more fit ── */
                .media-entry-card__body {
                    aspect-ratio: 6/8 !important;
                }

                /* ── Card title: tighter ── */
                [data-media-entry-card-title-section="true"] {
                    padding-top: 0.3rem !important;
                }
                [data-media-entry-card-title-section-title="true"] {
                    font-size: 0.78rem !important;
                    line-height: 1.2 !important;
                }
                [data-media-entry-card-title-section-year-season="true"] {
                    font-size: 0.7rem !important;
                }

                /* ── Genre tabs: smaller pills on mobile ── */
                .UI-StaticTabs__trigger {
                    height: 2.2rem !important;
                    padding-left: 0.85rem !important;
                    padding-right: 0.85rem !important;
                    font-size: 0.82rem !important;
                }

                /* ── Section headings: tighter margin ── */
                [data-continue-watching-container="true"] h2,
                [data-library-collection-list-item-header="true"] h2 {
                    font-size: 1.1rem !important;
                }

                /* ── Grid/Carousel transition animations ── */
                [data-library-collection-list-item-media-card-lazy-grid][data-list-type="CURRENT"] [data-media-card-grid="true"] {
                    transition: opacity 0.3s ease !important;
                }

                /* Hide grid when not ready – enables fade out */
                [data-library-collection-list-item-media-card-lazy-grid][data-list-type="CURRENT"]:not([data-grid-ready]) [data-media-card-grid="true"] {
                    opacity: 0 !important;
                }

                [data-library-collection-list-item-media-card-lazy-grid][data-list-type="CURRENT"][data-view-mode="grid"][data-grid-ready="true"] [data-media-card-grid="true"] {
                    opacity: 1 !important;
                }

                [data-library-collection-list-item-media-card-lazy-grid][data-list-type="CURRENT"][data-view-mode="carousel"][data-grid-ready="true"] [data-media-card-grid="true"] {
                    opacity: 1 !important;
                }

                /* ── Grid view styles - force 2 columns ── */
                [data-library-collection-list-item-media-card-lazy-grid][data-list-type="CURRENT"][data-view-mode="grid"] [data-media-card-grid="true"] {
                    display: grid !important;
                    grid-template-columns: repeat(2, 1fr) !important;
                    gap: 0.65rem !important;
                    padding: 0.25rem 1rem 1rem 1rem !important;
                    overflow: visible !important;
                    flex-direction: unset !important;
                    flex-wrap: unset !important;
                    scroll-snap-type: unset !important;
                    -webkit-overflow-scrolling: unset !important;
                }

                [data-library-collection-list-item-media-card-lazy-grid][data-list-type="CURRENT"][data-view-mode="grid"] [data-media-entry-card-container="true"] {
                    flex: unset !important;
                    flex-basis: unset !important;
                    width: 100% !important;
                    max-width: unset !important;
                    scroll-snap-align: unset !important;
                }

                /* ── View toggle button - only show background in grid mode ── */
                #__seanime_cw_view_toggle {
                    background-color: transparent !important;
                    transition: background-color 0.3s ease !important;
                }

                #__seanime_cw_view_toggle:hover {
                    background-color: rgba(255, 255, 255, 0.15) !important;
                }

                #__seanime_cw_view_toggle:active {
                    background-color: rgba(255, 255, 255, 0.25) !important;
                }

                #__seanime_cw_view_toggle[data-view-mode="grid"] {
                    background-color: rgba(255, 255, 255, 0.2) !important;
                }

                #__seanime_cw_view_toggle[data-view-mode="grid"]:hover {
                    background-color: rgba(255, 255, 255, 0.3) !important;
                }

                #__seanime_cw_view_toggle[data-view-mode="grid"]:active {
                    background-color: rgba(255, 255, 255, 0.35) !important;
                }

                #__seanime_cw_view_toggle svg {
                    width: 20px;
                    height: 20px;
                }
            `;
            document.head.appendChild(s);
        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }
}