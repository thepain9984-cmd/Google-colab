package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.PlayCircleFilled
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.screens.ColabScreen
import com.example.ui.screens.DriveScreen
import com.example.ui.screens.ServersScreen
import com.example.ui.screens.TemplatesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.WorkspaceViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: WorkspaceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = false) { // High Density light design theme
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFFF8F9FF),
                    bottomBar = {
                        AppNavigationBar(viewModel = viewModel)
                    },
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        val activeTab by viewModel.currentTab.collectAsState()

                        // 1. Colab Workspace Tab (Always kept alive in layout tree to prevent WebView reload!)
                        ColabScreen(
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = if (activeTab == AppTab.COLAB) 1f else 0f
                                    translationX = if (activeTab == AppTab.COLAB) 0f else 20000f
                                }
                        )

                        // 2. Drive Sync Explorer Tab
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = if (activeTab == AppTab.DRIVE) 1f else 0f
                                    translationX = if (activeTab == AppTab.DRIVE) 0f else 20000f
                                }
                        ) {
                            if (activeTab == AppTab.DRIVE) {
                                DriveScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                            }
                        }

                        // 3. Remote Server Management Tab
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = if (activeTab == AppTab.SERVERS) 1f else 0f
                                    translationX = if (activeTab == AppTab.SERVERS) 0f else 20000f
                                }
                        ) {
                            if (activeTab == AppTab.SERVERS) {
                                ServersScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                            }
                        }

                        // 4. Code & Template Library Tab
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = if (activeTab == AppTab.TEMPLATES) 1f else 0f
                                    translationX = if (activeTab == AppTab.TEMPLATES) 0f else 20000f
                                }
                        ) {
                            if (activeTab == AppTab.TEMPLATES) {
                                TemplatesScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigationBar(viewModel: WorkspaceViewModel) {
    val activeTab by viewModel.currentTab.collectAsState()

    NavigationBar(
        containerColor = Color(0xFFF3F3FA), // High Density Bottom Nav Background
        contentColor = Color(0xFF1C1B1F)
    ) {
        // Colab
        NavigationBarItem(
            selected = activeTab == AppTab.COLAB,
            onClick = { viewModel.currentTab.value = AppTab.COLAB },
            icon = {
                Icon(
                    imageVector = if (activeTab == AppTab.COLAB) Icons.Filled.PlayCircleFilled else Icons.Outlined.PlayCircleFilled,
                    contentDescription = "Colab Workspace"
                )
            },
            label = { Text("Workspace") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF001A41),
                selectedTextColor = Color(0xFF001A41),
                indicatorColor = Color(0xFFD8E2FF), // High Density Active Pill Indicator
                unselectedIconColor = Color(0xFF44474F),
                unselectedTextColor = Color(0xFF44474F)
            )
        )

        // Drive Sync
        NavigationBarItem(
            selected = activeTab == AppTab.DRIVE,
            onClick = { viewModel.currentTab.value = AppTab.DRIVE },
            icon = {
                Icon(
                    imageVector = if (activeTab == AppTab.DRIVE) Icons.Filled.Cloud else Icons.Outlined.Cloud,
                    contentDescription = "Drive Sync"
                )
            },
            label = { Text("Drive") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF001A41),
                selectedTextColor = Color(0xFF001A41),
                indicatorColor = Color(0xFFD8E2FF),
                unselectedIconColor = Color(0xFF44474F),
                unselectedTextColor = Color(0xFF44474F)
            )
        )

        // Servers
        NavigationBarItem(
            selected = activeTab == AppTab.SERVERS,
            onClick = { viewModel.currentTab.value = AppTab.SERVERS },
            icon = {
                Icon(
                    imageVector = if (activeTab == AppTab.SERVERS) Icons.Filled.Dns else Icons.Outlined.Dns,
                    contentDescription = "SSH Servers"
                )
            },
            label = { Text("SSH Servers") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF001A41),
                selectedTextColor = Color(0xFF001A41),
                indicatorColor = Color(0xFFD8E2FF),
                unselectedIconColor = Color(0xFF44474F),
                unselectedTextColor = Color(0xFF44474F)
            )
        )

        // Templates
        NavigationBarItem(
            selected = activeTab == AppTab.TEMPLATES,
            onClick = { viewModel.currentTab.value = AppTab.TEMPLATES },
            icon = {
                Icon(
                    imageVector = if (activeTab == AppTab.TEMPLATES) Icons.Filled.Code else Icons.Outlined.Code,
                    contentDescription = "Code Hub"
                )
            },
            label = { Text("Code Hub") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF001A41),
                selectedTextColor = Color(0xFF001A41),
                indicatorColor = Color(0xFFD8E2FF),
                unselectedIconColor = Color(0xFF44474F),
                unselectedTextColor = Color(0xFF44474F)
            )
        )
    }
}
