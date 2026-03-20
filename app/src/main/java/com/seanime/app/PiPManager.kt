package com.seanime.app

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Intent
import android.os.Build
import android.util.Rational
import android.webkit.JavascriptInterface
import android.webkit.WebView

class PiPManager(private val activity: Activity, private val webView: WebView) {

    inner class PiPBridge {
        @JavascriptInterface
        fun enterPiP() {
            activity.runOnUiThread {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val params = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .build()
                    activity.enterPictureInPictureMode(params)
                }
            }
        }

        @JavascriptInterface
        fun exitPiP() {
            activity.runOnUiThread {
                val intent = Intent(activity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                activity.startActivity(intent)
            }
        }
    }

    fun registerBridge() {
        webView.addJavascriptInterface(PiPBridge(), "AndroidBridge")
    }

    fun injectHijacker() {
        val js = """
            (function() {
                window.__androidPiPActive = window.__androidPiPActive || false;

                function hijackVideoControls() {
                    const buttons = document.querySelectorAll('button[data-vc-element="control-button"]:not([data-pip-hijacked])');
                    
                    buttons.forEach(btn => {
                        const isPiPButton = btn.querySelector('path[d^="M11 19h-6"]'); 
                        
                        if (isPiPButton) {
                            btn.setAttribute('data-pip-hijacked', 'true');
                            btn.addEventListener('click', (e) => {
                                e.preventDefault();
                                e.stopPropagation();
                                e.stopImmediatePropagation();
                                
                                if (window.__androidPiPActive) {
                                    AndroidBridge.exitPiP();
                                } else {
                                    AndroidBridge.enterPiP();
                                }
                            }, true); 
                        }
                    });
                }

                const observer = new MutationObserver(() => hijackVideoControls());
                observer.observe(document.body, { childList: true, subtree: true });
                hijackVideoControls();
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    fun onPiPModeChanged(isInPictureInPictureMode: Boolean) {
        if (isInPictureInPictureMode) {
            webView.evaluateJavascript("""
                (function() {
                    window.__androidPiPActive = true;
                    
                    const pill = document.getElementById('android-floating-pill');
                    const menu = document.getElementById('android-floating-menu');
                    if (pill) pill.style.setProperty('display', 'none', 'important');
                    if (menu) menu.style.setProperty('display', 'none', 'important');
                    
                    let style = document.getElementById('pip-overlay-style') || document.createElement('style');
                    style.id = 'pip-overlay-style';
                    style.innerHTML = `
                        html, body {
                            width: 100% !important;
                            height: 100% !important;
                            margin: 0 !important;
                            padding: 0 !important;
                            background: black !important;
                            overflow: hidden !important;
                        }
                        body > *:not(video) {
                            display: none !important;
                            visibility: hidden !important;
                            pointer-events: none !important;
                        }
                        video {
                            position: fixed !important;
                            top: 0 !important;
                            left: 0 !important;
                            width: 100% !important;
                            height: 100% !important;
                            object-fit: fill !important;
                            background: black !important;
                            z-index: 9999999 !important;
                            margin: 0 !important;
                            padding: 0 !important;
                            border: none !important;
                            visibility: visible !important;
                        }
                    `;
                    document.head.appendChild(style);

                    // Move video directly to body
                    const video = document.querySelector('video');
                    if (video) {
                        // Store original position
                        video.__pipOriginalParent = video.parentElement;
                        video.__pipOriginalNextSibling = video.nextSibling;
                        
                        // Move directly to body
                        if (video.parentElement !== document.body) {
                            document.body.appendChild(video);
                        }
                    }
                })();
            """.trimIndent(), null)
        } else {
            webView.evaluateJavascript("""
                (function() {
                    window.__androidPiPActive = false;
                    
                    // Restore video to its original position in the DOM
                    const video = document.querySelector('video');
                    if (video && video.__pipOriginalParent) {
                        const origParent = video.__pipOriginalParent;
                        const origNext = video.__pipOriginalNextSibling;
                        
                        // Clear all inline styles from video
                        video.removeAttribute('style');
                        
                        if (origNext) {
                            origParent.insertBefore(video, origNext);
                        } else {
                            origParent.appendChild(video);
                        }
                        delete video.__pipOriginalParent;
                        delete video.__pipOriginalNextSibling;
                    }

                    // Remove PiP styles completely
                    const style = document.getElementById('pip-overlay-style');
                    if (style) {
                        style.innerHTML = '';
                        style.remove();
                    }
                    
                    // Force restore html and body to normal by removing inline styles
                    document.documentElement.removeAttribute('style');
                    document.body.removeAttribute('style');
                    
                    // Restore pill
                    const pill = document.getElementById('android-floating-pill');
                    if (pill) {
                        pill.style.removeProperty('display');
                        pill.style.display = 'grid';
                    }
                    
                    // Reset viewport zoom
                    const viewport = document.querySelector('meta[name="viewport"]');
                    if (viewport) {
                        viewport.setAttribute('content', 'width=device-width, initial-scale=1, viewport-fit=cover');
                    }
                    
                    // Reset document scroll
                    window.scrollTo(0, 0);
                    document.documentElement.scrollTop = 0;
                    document.body.scrollTop = 0;
                    
                    // Trigger layout recalculation after a small delay
                    setTimeout(() => {
                        window.dispatchEvent(new Event('resize'));
                    }, 100);
                })();
            """.trimIndent(), null)
        }
    }
}
