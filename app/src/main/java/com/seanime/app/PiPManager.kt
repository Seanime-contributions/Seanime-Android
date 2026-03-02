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
                    if (!window.location.pathname.includes('/entry')) return;

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
                        body > *:not(video):not([id="pip-video-wrapper"]) {
                            visibility: hidden !important;
                            pointer-events: none !important;
                        }
                        body {
                            background: black !important;
                            overflow: hidden !important;
                        }
                        video {
                            position: fixed !important;
                            top: 0 !important;
                            left: 0 !important;
                            width: 100vw !important;
                            height: 100vh !important;
                            z-index: 2147483647 !important;
                            background: black !important;
                            object-fit: contain !important;
                            visibility: visible !important;
                        }
                    `;
                    document.head.appendChild(style);

                    // Reparent the active video to body so it's not hidden by the
                    // "body > *:not(video)" rule that hides all other subtrees.
                    const video = document.querySelector('video');
                    if (video && video.parentElement !== document.body) {
                        const wrapper = document.createElement('div');
                        wrapper.id = 'pip-video-wrapper';
                        wrapper.style.cssText = [
                            'position:fixed',
                            'inset:0',
                            'z-index:2147483646',
                            'background:black',
                            'display:flex',
                            'align-items:center',
                            'justify-content:center'
                        ].join(';');
                        // Store a reference so we can restore later
                        video.__pipOriginalParent = video.parentElement;
                        video.__pipOriginalNextSibling = video.nextSibling;
                        wrapper.appendChild(video);
                        document.body.appendChild(wrapper);
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
                        if (origNext) {
                            origParent.insertBefore(video, origNext);
                        } else {
                            origParent.appendChild(video);
                        }
                        delete video.__pipOriginalParent;
                        delete video.__pipOriginalNextSibling;
                    }

                    // Remove the wrapper div
                    const wrapper = document.getElementById('pip-video-wrapper');
                    if (wrapper) wrapper.remove();

                    // Remove PiP styles
                    const style = document.getElementById('pip-overlay-style');
                    if (style) style.remove();
                    
                    // Restore pill
                    const pill = document.getElementById('android-floating-pill');
                    if (pill) pill.style.display = 'grid';
                    
                    window.dispatchEvent(new Event('resize'));
                })();
            """.trimIndent(), null)
        }
    }
}
