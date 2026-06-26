package com.example.ui.screens

import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.NotebookEntity
import com.example.ui.components.ColabWebView
import com.example.ui.components.injectKeyboardShortcut
import com.example.ui.components.injectTextHelper
import com.example.ui.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColabScreen(
    viewModel: WorkspaceViewModel,
    modifier: Modifier = Modifier
) {
    val selectedUrl by viewModel.selectedNotebookUrl.collectAsState()
    val isDesktopMode by viewModel.isDesktopUserAgent.collectAsState()
    val notebooks by viewModel.notebooks.collectAsState()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf("Google Colab") }
    var pageProgress by remember { mutableStateOf(0) }
    var isPageLoading by remember { mutableStateOf(false) }

    var showBookmarkDialog by remember { mutableStateOf(false) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }

    var newTitle by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }

    val context = LocalContext.current

    val helperSymbols = listOf(
        "tab" to "    ",
        "{" to "{",
        "}" to "}",
        "[" to "[",
        "]" to "]",
        "(" to "(",
        ")" to ")",
        "=" to "=",
        "+" to "+",
        "-" to "-",
        "_" to "_",
        "*" to "*",
        "/" to "/",
        "%" to "%",
        ":" to ":",
        ";" to ";",
        "\"" to "\"",
        "'" to "'",
        "\\" to "\\",
        "|" to "|",
        "&" to "&",
        "^" to "^",
        "!" to "!",
        "~" to "~",
        "`" to "`",
        "<" to "<",
        ">" to ">",
        "?" to "?"
    )

    Column(modifier = modifier.fillMaxSize().background(Color(0xFFF8F9FF))) {
        // WebView Navigation Top Bar
        Surface(
            color = Color(0xFFF8F9FF),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Back
                    IconButton(
                        onClick = { webViewRef?.let { if (it.canGoBack()) it.goBack() } },
                        enabled = webViewRef?.canGoBack() == true
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back",
                            tint = if (webViewRef?.canGoBack() == true) Color(0xFF001A41) else Color(0xFFC4C6D0)
                        )
                    }

                    // Forward
                    IconButton(
                        onClick = { webViewRef?.let { if (it.canGoForward()) it.goForward() } },
                        enabled = webViewRef?.canGoForward() == true
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Go Forward",
                            tint = if (webViewRef?.canGoForward() == true) Color(0xFF001A41) else Color(0xFFC4C6D0)
                        )
                    }

                    // Refresh / Stop
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = Color(0xFF001A41)
                        )
                    }

                    // Address bar / Info
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E2EC)),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Secure",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = webViewRef?.url ?: selectedUrl,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF44474F)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Desktop mode indicator
                    IconButton(onClick = { viewModel.toggleUserAgent() }) {
                        Icon(
                            imageVector = if (isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.Smartphone,
                            contentDescription = "User Agent",
                            tint = if (isDesktopMode) Color(0xFF005AC1) else Color(0xFF001A41)
                        )
                    }

                    // Notebook Bookmarks Selector
                    IconButton(onClick = { showBookmarkDialog = true }) {
                        Icon(
                            Icons.Default.Bookmarks,
                            contentDescription = "Bookmarks",
                            tint = Color(0xFF001A41)
                        )
                    }
                }

                // Page loading linear indicator
                AnimatedVisibility(visible = isPageLoading && pageProgress < 100) {
                    LinearProgressIndicator(
                        progress = { pageProgress / 100f },
                        modifier = Modifier.fillMaxWidth().height(3.dp).padding(top = 4.dp),
                        color = Color(0xFF005AC1),
                        trackColor = Color(0xFFD8E2FF)
                    )
                }
            }
        }

        // Colab WebView Container
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ColabWebView(
                url = selectedUrl,
                isDesktopMode = isDesktopMode,
                onPageStarted = { url ->
                    isPageLoading = true
                    pageProgress = 15
                },
                onPageFinished = { url ->
                    isPageLoading = false
                    pageProgress = 100
                    webViewRef?.title?.let { pageTitle = it }
                },
                onProgressChanged = { progress ->
                    pageProgress = progress
                    if (progress >= 100) isPageLoading = false
                },
                webViewProvider = { webViewRef = it },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bottom Coding Helper Keyboard Bar
        Surface(
            color = Color(0xFFF3F3FA),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cell Execution Shortcuts (Orange / Amber Accent)
                    Button(
                        onClick = { injectKeyboardShortcut(webViewRef, ctrlKey = true, shiftKey = false) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD8E2FF)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF001A41))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run Cell", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF001A41)))
                    }

                    Button(
                        onClick = { injectKeyboardShortcut(webViewRef, ctrlKey = false, shiftKey = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run & Next", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }

                    // Divider
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFE1E2EC)))

                    // Text snippet/symbol keys
                    helperSymbols.forEach { (label, value) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E2EC)),
                            modifier = Modifier
                                .height(36.dp)
                                .clickable { injectTextHelper(webViewRef, value) }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF001A41)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Bookmarks Drawer/Dialog
    if (showBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { showBookmarkDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Saved Notebooks", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = {
                        newTitle = pageTitle
                        newUrl = webViewRef?.url ?: selectedUrl
                        showAddBookmarkDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Current Page", tint = Color(0xFF005AC1))
                    }
                }
            },
            text = {
                Box(modifier = Modifier.heightIn(max = 300.dp)) {
                    if (notebooks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No saved notebooks. Tap + to add the current notebook.", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(notebooks.size) { index ->
                                val notebook = notebooks[index]
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FF)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E2EC)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = notebook.title,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1C1B1F),
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = notebook.url,
                                                color = Color.Gray,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                              )
                                        }
                                        IconButton(onClick = {
                                            viewModel.openNotebookUrl(notebook.url)
                                            viewModel.markNotebookOpened(notebook.id)
                                            showBookmarkDialog = false
                                        }) {
                                            Icon(Icons.Default.Launch, contentDescription = "Open", tint = Color(0xFF005AC1))
                                        }
                                        IconButton(onClick = { viewModel.deleteNotebook(notebook) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookmarkDialog = false }) {
                    Text("Close", color = Color(0xFF005AC1))
                }
            },
            containerColor = Color(0xFFFFFFFF),
            titleContentColor = Color(0xFF1C1B1F),
            textContentColor = Color(0xFF44474F)
        )
    }

    // Add Bookmark Dialog
    if (showAddBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { showAddBookmarkDialog = false },
            title = { Text("Bookmark Notebook", color = Color(0xFF1C1B1F)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1C1B1F),
                            unfocusedTextColor = Color(0xFF1C1B1F),
                            focusedBorderColor = Color(0xFF005AC1),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUrl,
                        onValueChange = { newUrl = it },
                        label = { Text("Colab URL") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1C1B1F),
                            unfocusedTextColor = Color(0xFF1C1B1F),
                            focusedBorderColor = Color(0xFF005AC1),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotEmpty() && newUrl.isNotEmpty()) {
                            viewModel.addNotebook(newTitle, newUrl)
                            showAddBookmarkDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1))
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBookmarkDialog = false }) {
                    Text("Cancel", color = Color(0xFF44474F))
                }
            },
            containerColor = Color(0xFFFFFFFF)
        )
    }
}
