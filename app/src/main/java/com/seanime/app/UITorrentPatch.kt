package com.seanime.app

import android.webkit.WebView

object UITorrentPatch {

    fun inject(webView: WebView) {
        injectTorrentPatch(webView)
    }

    private fun injectTorrentPatch(webView: WebView) {
        val js = """
        (function() {
            if (window.__seanime_torrent_patch_init) return;
            window.__seanime_torrent_patch_init = true;

            // ── Inject base styles once ───────────────────────────────────────
            if (!document.getElementById('__seanime_torrent_patch_styles')) {
                var s = document.createElement('style');
                s.id = '__seanime_torrent_patch_styles';
                s.textContent = [
                    '[data-torrent-preview-item="true"][data-seanime-patched="true"] {',
                    '  padding: 0 !important;',
                    '  border-radius: 12px !important;',
                    '  overflow: hidden !important;',
                    '  min-height: 72px !important;',
                    '  display: block !important;',
                    '}',

                    '[data-torrent-preview-item="true"][data-seanime-patched="true"] [data-torrent-preview-item-image-container="true"] {',
                    '  max-width: none !important;',
                    '  width: 100% !important;',
                    '  position: absolute !important;',
                    '  inset: 0 !important;',
                    '  z-index: 0 !important;',
                    '}',

                    '[data-torrent-preview-item="true"][data-seanime-patched="true"] [data-torrent-preview-item-image="true"] {',
                    '  opacity: 0.15 !important;',
                    '}',

                    '[data-torrent-preview-item="true"][data-seanime-patched="true"] [data-torrent-preview-item-image-end-gradient="true"] {',
                    '  display: none !important;',
                    '}',

                    '[data-torrent-preview-item="true"][data-seanime-patched="true"] [data-torrent-preview-item-content="true"] {',
                    '  position: relative !important;',
                    '  z-index: 2 !important;',
                    '  display: flex !important;',
                    '  flex-direction: row !important;',
                    '  align-items: center !important;',
                    '  gap: 0 !important;',
                    '  padding: 10px 48px 10px 12px !important;',
                    '  min-height: 72px !important;',
                    '}',

                    '[data-torrent-preview-item="true"][data-seanime-patched="true"] [data-torrent-preview-item-release-info-container="true"] {',
                    '  display: none !important;',
                    '}',

                    '[data-torrent-preview-item="true"][data-seanime-patched="true"] [data-torrent-preview-item-metadata="true"] {',
                    '  display: flex !important;',
                    '  flex-direction: column !important;',
                    '  justify-content: center !important;',
                    '  gap: 0 !important;',
                    '  min-width: 0 !important;',
                    '  width: 100% !important;',
                    '}',

                    '[data-torrent-preview-item="true"][data-seanime-patched="true"] [data-torrent-preview-item-subtitle="true"] {',
                    '  display: none !important;',
                    '}',

                    '[data-torrent-preview-item="true"][data-seanime-patched="true"] [data-torrent-preview-item-subcontent="true"] {',
                    '  display: none !important;',
                    '}',

                    '[data-torrent-preview-item="true"][data-seanime-patched="true"] [data-torrent-preview-item-title="true"] {',
                    '  font-size: 0.9rem !important;',
                    '  font-weight: 600 !important;',
                    '  max-width: 100% !important;',
                    '  line-height: 1.3 !important;',
                    '  display: flex !important;',
                    '  flex-direction: row !important;',
                    '  align-items: center !important;',
                    '  flex-wrap: nowrap !important;',
                    '  overflow: hidden !important;',
                    '  gap: 0 !important;',
                    '}',

                    '[data-torrent-preview-item="true"][data-seanime-patched="true"] [data-torrent-preview-item-title="true"] > :first-child,',
                    '[data-torrent-preview-item="true"][data-seanime-patched="true"] [data-torrent-preview-item-title="true"] > text {',
                    '  overflow: hidden !important;',
                    '  text-overflow: ellipsis !important;',
                    '  white-space: nowrap !important;',
                    '  min-width: 0 !important;',
                    '  flex-shrink: 1 !important;',
                    '}',

                    '[data-torrent-preview-item="true"][data-seanime-patched="true"] [data-torrent-preview-item-title="true"] [data-torrent-preview-item-confirmed-badge="true"] {',
                    '  margin-left: 6px !important;',
                    '  flex-shrink: 0 !important;',
                    '}',
                    '  position: absolute !important;',
                    '  right: 0 !important;',
                    '  top: 0 !important;',
                    '  bottom: 0 !important;',
                    '  width: 48px !important;',
                    '  display: flex !important;',
                    '  align-items: center !important;',
                    '  justify-content: center !important;',
                    '  z-index: 10 !important;',
                    '}',

                    '[data-torrent-preview-item-open-in-browser-button="true"] {',
                    '  width: 34px !important;',
                    '  height: 34px !important;',
                    '  border-radius: 9px !important;',
                    '  background: rgba(255,255,255,0.05) !important;',
                    '  border: 1px solid rgba(255,255,255,0.09) !important;',
                    '  display: flex !important;',
                    '  align-items: center !important;',
                    '  justify-content: center !important;',
                    '  padding: 0 !important;',
                    '}',

                    '[data-torrent-preview-item="true"][data-is-selected="true"][data-seanime-patched="true"] {',
                    '  border-color: rgba(99,102,241,0.55) !important;',
                    '  background: rgba(99,102,241,0.07) !important;',
                    '}',

                    '[data-torrent-preview-item="true"][data-is-best-release="true"][data-seanime-patched="true"] {',
                    '  border-color: rgba(34,197,94,0.4) !important;',
                    '}'
                ].join('\n');
                document.head.appendChild(s);
            }

            // ── Patch a single card ───────────────────────────────────────────
            function patchCard(card) {
                if (card.dataset.seanimePatched === 'true') return;
                card.dataset.seanimePatched = 'true';

                var displayName   = card.dataset.displayName  || '';
                var releaseGroup  = card.dataset.releaseGroup  || '';
                var torrentName   = card.dataset.torrentName   || '';
                var isBestRelease = card.dataset.isBestRelease === 'true';

                // Resolution
                var resBadge = card.querySelector('[data-torrent-item-resolution-badge="true"]');
                var resolution = resBadge ? resBadge.textContent.trim() : '';

                // Seeders: find the numeric span inside the seeders badge
                var seederCount = '';
                var seedersBadge = card.querySelector('[data-torrent-item-seeders-badge="true"]');
                if (seedersBadge) {
                    seedersBadge.querySelectorAll('span').forEach(function(sp) {
                        var t = sp.textContent.trim();
                        if (!seederCount && /^\d[\d,]*${'$'}/.test(t)) seederCount = t;
                    });
                }

                // File size: any text node matching GiB / MiB
                var fileSize = '';
                card.querySelectorAll('p, span').forEach(function(el) {
                    if (!fileSize && /\d+(\.\d+)?\s*(GiB|MiB|MB|GB)/.test(el.textContent)) {
                        fileSize = el.textContent.trim();
                    }
                });

                // Date: text containing "ago"
                var dateStr = '';
                card.querySelectorAll('p, span').forEach(function(el) {
                    if (!dateStr && /\d+\s+(day|hour|minute|week|month)s?\s+ago/.test(el.textContent)) {
                        // strip leading calendar icon text if any
                        dateStr = el.textContent.replace(/[\u{1F4C5}\u{1F5D3}]/gu, '').trim();
                    }
                });

                // ── Build stat bar ────────────────────────────────────────────
                var meta = card.querySelector('[data-torrent-preview-item-metadata="true"]');
                if (!meta) return;

                // Remove previously injected nodes (re-patch guard)
                var oldBar = meta.querySelector('[data-seanime-torrent-bar]');
                if (oldBar) meta.removeChild(oldBar);
                var oldFn = meta.querySelector('[data-seanime-torrent-filename]');
                if (oldFn) meta.removeChild(oldFn);

                var bar = document.createElement('div');
                bar.setAttribute('data-seanime-torrent-bar', 'true');
                bar.style.cssText = 'display:flex;flex-direction:row;align-items:center;flex-wrap:nowrap;gap:0;margin-top:4px;min-width:0;overflow:hidden;';

                function pill(text, bg, border, color) {
                    var sp = document.createElement('span');
                    sp.textContent = text;
                    sp.style.cssText = [
                        'font-size:0.62rem',
                        'font-weight:700',
                        'letter-spacing:0.04em',
                        'text-transform:uppercase',
                        'padding:2px 6px',
                        'border-radius:5px',
                        'margin-right:6px',
                        'flex-shrink:0',
                        'white-space:nowrap',
                        'background:' + bg,
                        'border:1px solid ' + border,
                        'color:' + color
                    ].join(';');
                    return sp;
                }

                function stat(text, color, bold) {
                    var sp = document.createElement('span');
                    sp.textContent = text;
                    sp.style.cssText = 'font-size:0.7rem;font-weight:' + (bold ? '700' : '500') + ';color:' + color + ';white-space:nowrap;flex-shrink:0;';
                    return sp;
                }

                function dot() {
                    var sp = document.createElement('span');
                    sp.textContent = '·';
                    sp.style.cssText = 'font-size:0.7rem;color:rgba(255,255,255,0.2);margin:0 5px;flex-shrink:0;';
                    return sp;
                }

                // Release group pill
                if (releaseGroup) {
                    bar.appendChild(pill(
                        releaseGroup,
                        isBestRelease ? 'rgba(34,197,94,0.12)' : 'rgba(255,255,255,0.07)',
                        isBestRelease ? 'rgba(34,197,94,0.35)' : 'rgba(255,255,255,0.1)',
                        isBestRelease ? 'rgb(134,239,172)' : 'rgba(255,255,255,0.5)'
                    ));
                }

                // Resolution pill
                if (resolution) {
                    bar.appendChild(pill(
                        resolution,
                        isBestRelease ? 'rgba(34,197,94,0.12)' : 'rgba(99,102,241,0.14)',
                        isBestRelease ? 'rgba(34,197,94,0.35)' : 'rgba(99,102,241,0.32)',
                        isBestRelease ? 'rgb(134,239,172)' : 'rgb(165,167,255)'
                    ));
                }

                // Seeders in the stat bar
                if (seederCount) {
                    bar.appendChild(stat('▲ ' + seederCount, 'rgb(129,140,248)', true));
                }

                // ── Inject fileSize + date into the title line ────────────────
                var titleEl = card.querySelector('[data-torrent-preview-item-title="true"]');
                if (titleEl) {
                    // Remove previously injected title extras
                    titleEl.querySelectorAll('[data-seanime-title-extra]').forEach(function(el) {
                        el.parentNode.removeChild(el);
                    });

                    // Wrap bare text node in a truncating span (once only)
                    if (!titleEl.querySelector('[data-seanime-title-name]')) {
                        var nameSpan = document.createElement('span');
                        nameSpan.setAttribute('data-seanime-title-name', 'true');
                        nameSpan.style.cssText = 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;min-width:0;flex-shrink:1;';
                        // Move all non-element child nodes (raw text) into the span
                        Array.prototype.slice.call(titleEl.childNodes).forEach(function(node) {
                            if (node.nodeType === 3) nameSpan.appendChild(node);
                        });
                        // Shorten "Episode N" → "Ep. N"
                        nameSpan.textContent = nameSpan.textContent.replace(/\bEpisode\s+(\d)/g, 'Ep. ${'$'}1');
                        titleEl.insertBefore(nameSpan, titleEl.firstChild);
                    }

                    function titleExtra(text, color) {
                        var sp = document.createElement('span');
                        sp.setAttribute('data-seanime-title-extra', 'true');
                        sp.textContent = text;
                        sp.style.cssText = 'font-size:0.6rem;font-weight:500;color:' + color + ';white-space:nowrap;flex-shrink:0;';
                        return sp;
                    }

                    function titleDot() {
                        var sp = document.createElement('span');
                        sp.setAttribute('data-seanime-title-extra', 'true');
                        sp.textContent = '·';
                        sp.style.cssText = 'font-size:0.6rem;color:rgba(255,255,255,0.2);margin:0 4px;flex-shrink:0;';
                        return sp;
                    }

                    var extrasAdded = 0;
                    if (fileSize) {
                        titleEl.appendChild(titleDot());
                        titleEl.appendChild(titleExtra(fileSize, 'rgba(255,255,255,0.55)'));
                        extrasAdded++;
                    }
                    if (dateStr) {
                        titleEl.appendChild(titleDot());
                        titleEl.appendChild(titleExtra(dateStr, 'rgba(255,255,255,0.28)'));
                    }
                }

                // Filename row
                var fnRow = document.createElement('p');
                fnRow.setAttribute('data-seanime-torrent-filename', 'true');
                fnRow.textContent = torrentName;
                fnRow.style.cssText = 'font-size:0.62rem;color:rgba(255,255,255,0.2);margin-top:3px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:100%;';

                meta.appendChild(bar);
                meta.appendChild(fnRow);
            }

            // ── Patch all existing cards ──────────────────────────────────────
            function patchAll() {
                document.querySelectorAll('[data-torrent-preview-item="true"]').forEach(patchCard);
            }

            patchAll();

            // ── Watch for new cards (SPA navigation / lazy rendering) ─────────
            new MutationObserver(function(mutations) {
                var found = false;
                mutations.forEach(function(mutation) {
                    mutation.addedNodes.forEach(function(node) {
                        if (node.nodeType !== 1) return;
                        if (node.dataset && node.dataset.torrentPreviewItem === 'true') {
                            patchCard(node);
                        } else if (node.querySelectorAll) {
                            var cards = node.querySelectorAll('[data-torrent-preview-item="true"]');
                            if (cards.length) { cards.forEach(patchCard); found = true; }
                        }
                    });
                });
            }).observe(document.body, { childList: true, subtree: true });

        })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }
}
