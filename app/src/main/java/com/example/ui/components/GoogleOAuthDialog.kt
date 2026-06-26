package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleOAuthDialog(
    clientId: String,
    redirectUri: String,
    onTokenCaptured: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Standard Google OAuth 2.0 Implicit Flow URL for web/mobile client
    val scope = "https://www.googleapis.com/auth/drive.readonly https://www.googleapis.com/auth/drive.metadata.readonly"
    val oauthUrl = "https://accounts.google.com/o/oauth2/v2/auth" +
            "?client_id=$clientId" +
            "&redirect_uri=$redirectUri" +
            "&response_type=token" +
            "&scope=${scope.replace(" ", "%20")}" +
            "&prompt=select_account"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                TopAppBar(
                    title = { Text("Google Sign-In", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                // WebView for OAuth login
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White)
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.useWideViewPort = true
                                
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        if (url != null && url.startsWith(redirectUri)) {
                                            // Intercept token from redirect URL fragment
                                            // Format: https://localhost/oauth2callback#access_token=ya29...&token_type=Bearer...
                                            val fragment = url.substringAfter("#", "")
                                            val params = fragment.split("&").associate {
                                                val parts = it.split("=")
                                                if (parts.size >= 2) parts[0] to parts[1] else "" to ""
                                            }
                                            val accessToken = params["access_token"]
                                            if (!accessToken.isNullOrEmpty()) {
                                                onTokenCaptured(accessToken)
                                            } else {
                                                // Try query parameter as backup
                                                val queryParams = url.substringAfter("?", "").split("&").associate {
                                                    val parts = it.split("=")
                                                    if (parts.size >= 2) parts[0] to parts[1] else "" to ""
                                                }
                                                val codeOrToken = queryParams["access_token"] ?: queryParams["code"]
                                                if (!codeOrToken.isNullOrEmpty()) {
                                                    onTokenCaptured(codeOrToken)
                                                }
                                            }
                                        }
                                    }

                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val requestUrl = request?.url?.toString() ?: return false
                                        if (requestUrl.startsWith(redirectUri)) {
                                            return true // let onPageStarted handle it
                                        }
                                        return false
                                    }
                                }
                                loadUrl(oauthUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
