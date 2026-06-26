package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ColabWebView(
    url: String,
    isDesktopMode: Boolean,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
    onProgressChanged: (Int) -> Unit,
    webViewProvider: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
                allowContentAccess = true
                allowFileAccess = true
            }

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)
        }
    }

    // Capture WebView reference to pass up
    LaunchedEffect(webView) {
        webViewProvider(webView)
    }

    // Handle Desktop/Mobile user-agent shifts
    LaunchedEffect(isDesktopMode) {
        webView.settings.userAgentString = if (isDesktopMode) {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        } else {
            null // Default Android user-agent
        }
        webView.reload()
    }

    // Handle URL updates
    LaunchedEffect(url) {
        if (webView.url != url) {
            webView.loadUrl(url)
        }
    }

    AndroidView(
        factory = {
            webView.apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { onPageStarted(it) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        url?.let { onPageFinished(it) }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val requestUrl = request?.url?.toString() ?: return false
                        // Keep navigation inside the webview
                        view?.loadUrl(requestUrl)
                        return true
                    }
                }

                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress)
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

// Javascript keyboard helper injections
fun injectTextHelper(webView: WebView?, text: String) {
    val js = """
        (function() {
            const el = document.activeElement;
            if (!el) return;
            if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
                const start = el.selectionStart;
                const end = el.selectionEnd;
                el.value = el.value.substring(0, start) + `$text` + el.value.substring(end);
                el.selectionStart = el.selectionEnd = start + `$text`.length;
                el.dispatchEvent(new Event('input', { bubbles: true }));
            } else if (el.contentEditable === 'true') {
                const selection = window.getSelection();
                if (selection.rangeCount > 0) {
                    const range = selection.getRangeAt(0);
                    range.deleteContents();
                    const textNode = document.createTextNode(`$text`);
                    range.insertNode(textNode);
                    range.setStartAfter(textNode);
                    range.setEndAfter(textNode);
                    selection.removeAllRanges();
                    selection.addRange(range);
                }
            }
        })();
    """.trimIndent()
    webView?.evaluateJavascript(js, null)
}

fun injectKeyboardShortcut(webView: WebView?, ctrlKey: Boolean, shiftKey: Boolean) {
    val js = """
        (function() {
            const el = document.activeElement || document.body;
            const eventOpts = {
                bubbles: true,
                cancelable: true,
                ctrlKey: $ctrlKey,
                shiftKey: $shiftKey,
                keyCode: 13,
                key: 'Enter',
                code: 'Enter',
                which: 13
            };
            el.dispatchEvent(new KeyboardEvent('keydown', eventOpts));
            el.dispatchEvent(new KeyboardEvent('keypress', eventOpts));
            el.dispatchEvent(new KeyboardEvent('keyup', eventOpts));
        })();
    """.trimIndent()
    webView?.evaluateJavascript(js, null)
}
