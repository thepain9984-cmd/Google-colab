package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.ServerEntity
import com.example.ui.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    viewModel: WorkspaceViewModel,
    modifier: Modifier = Modifier
) {
    val servers by viewModel.servers.collectAsState()
    val activeServer by viewModel.activeServer.collectAsState()
    val activeStats by viewModel.activeServerStats.collectAsState()
    val isLoadingStats by viewModel.isLoadingStats.collectAsState()
    val statsError by viewModel.statsError.collectAsState()

    val commandInput by viewModel.commandInput.collectAsState()
    val terminalLogs by viewModel.terminalLogs.collectAsState()
    val isExecutingCommand by viewModel.isExecutingCommand.collectAsState()

    var showAddServerDialog by remember { mutableStateOf(false) }
    var activeSubTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Console

    // Form states
    var serverName by remember { mutableStateOf("") }
    var serverHost by remember { mutableStateOf("") }
    var serverPort by remember { mutableStateOf("22") }
    var serverUsername by remember { mutableStateOf("") }
    var serverPassword by remember { mutableStateOf("") }
    var serverNotes by remember { mutableStateOf("") }

    val shortcutCommands = listOf(
        "nvidia-smi" to "nvidia-smi",
        "top" to "top -bn1 | head -n 10",
        "free -m" to "free -m",
        "df -h" to "df -h /",
        "uname -a" to "uname -a",
        "CUDA Check" to "python3 -c \"import torch; print('CUDA Available:', torch.cuda.is_available())\"",
        "HuggingFace Test" to "python3 -c \"import transformers; print('HF Version:', transformers.__version__)\""
    )

    // Seed a mock server in viewmodel if none exist
    LaunchedEffect(servers) {
        if (servers.isEmpty()) {
            viewModel.addServer(
                name = "Demo PyTorch GPU Box",
                host = "simulation",
                port = 22,
                username = "colab_developer",
                authType = "PASSWORD",
                passwordOrKey = "secret",
                notes = "High performance workstation with 1x RTX 4090 GPU configured for mobile testing."
            )
        }
    }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFFF8F9FF))) {
        // App bar
        TopAppBar(
            title = { Text("Remote Server Manager", fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F)) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FF)),
            actions = {
                IconButton(onClick = { showAddServerDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Server", tint = Color(0xFF005AC1))
                }
            }
        )

        // Selected Server Selector Header
        Surface(
            color = Color(0xFFF8F9FF),
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = Color(0xFF005AC1))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeServer?.name ?: "No Active Connection",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F),
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (activeServer != null) "${activeServer!!.username}@${activeServer!!.host}:${activeServer!!.port}" else "Select a remote server below to establish SSH",
                            color = Color(0xFF44474F),
                            fontSize = 12.sp
                        )
                    }

                    if (activeServer != null) {
                        Button(
                            onClick = { viewModel.selectServer(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Disconnect", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (activeServer != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    // Switcher between Dashboard (0) and Console (1)
                    TabRow(
                        selectedTabIndex = activeSubTab,
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF005AC1),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[activeSubTab]),
                                color = Color(0xFF005AC1)
                            )
                        }
                    ) {
                        Tab(
                            selected = activeSubTab == 0,
                            onClick = { activeSubTab = 0 },
                            text = { Text("System Dashboard", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            selectedContentColor = Color(0xFF005AC1),
                            unselectedContentColor = Color(0xFF44474F)
                        )
                        Tab(
                            selected = activeSubTab == 1,
                            onClick = { activeSubTab = 1 },
                            text = { Text("SSH Console Shell", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            selectedContentColor = Color(0xFF005AC1),
                            unselectedContentColor = Color(0xFF44474F)
                        )
                    }
                }
            }
        }

        if (activeServer == null) {
            // Server Selection Page
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    "Your Remote Servers",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B1F),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    items(servers) { server ->
                        val isDemo = server.host == "simulation"
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E2EC)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectServer(server) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDemo) Color(0xFFD8E2FF) else Color(0xFFF3F3FA))
                                ) {
                                    Icon(
                                        imageVector = if (isDemo) Icons.Default.Science else Icons.Default.Computer,
                                        contentDescription = null,
                                        tint = if (isDemo) Color(0xFF005AC1) else Color(0xFF44474F)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(server.name, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F), fontSize = 15.sp)
                                        if (isDemo) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFD8E2FF))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("Local Sim Mode", color = Color(0xFF001A41), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Text("${server.username}@${server.host}:${server.port}", color = Color(0xFF44474F), fontSize = 12.sp)
                                    if (server.notes.isNotEmpty()) {
                                        Text(
                                            server.notes,
                                            color = Color(0xFF44474F),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }

                                IconButton(onClick = { viewModel.deleteServer(server) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Server", tint = Color.Red)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                // Onboarding tip
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E2EC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF005AC1))
                        Text(
                            "Connect your remote servers (e.g. AWS EC2, Paperspace, or local rigs) via secure SSH tunnel to monitor your AI training progress and execute terminal commands from your phone.",
                            fontSize = 12.sp,
                            color = Color(0xFF44474F),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            // Selected Server Workspace
            if (activeSubTab == 0) {
                // Dashboard View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .background(Color(0xFFF8F9FF)),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val server = activeServer!!
                    val isDemo = server.host == "simulation"

                    // Header Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Live Server Performance",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F),
                            fontSize = 15.sp
                        )
                        IconButton(onClick = { viewModel.fetchActiveServerStats() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh stats", tint = Color(0xFF005AC1))
                        }
                    }

                    if (isLoadingStats) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(color = Color(0xFF005AC1))
                                Text("Fetching system metrics...", color = Color.Gray)
                            }
                        }
                    } else {
                        val stats = activeStats
                        if (stats == null) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                    Text("No metrics retrieved. Tap refresh.", color = Color.Gray)
                                    Button(onClick = { viewModel.fetchActiveServerStats() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1))) {
                                        Text("Fetch Metrics", color = Color.White)
                                    }
                                }
                            }
                        } else {
                            // Render Beautiful Dashboard metrics
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // OS & Uptime Banner
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E2EC)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF005AC1))
                                            Column {
                                                Text("Operating System Environment", color = Color.Gray, fontSize = 11.sp)
                                                Text(stats.osName, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F), fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }

                                // CPU and RAM widgets
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // CPU
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E2EC)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("CPU LOAD", fontWeight = FontWeight.Bold, color = Color(0xFF44474F), fontSize = 11.sp)
                                                Text("${(stats.cpuUsage * 100).toInt()}%", fontWeight = FontWeight.Black, color = Color(0xFF1C1B1F), fontSize = 24.sp)
                                                LinearProgressIndicator(
                                                    progress = { stats.cpuUsage },
                                                    color = if (stats.cpuUsage > 0.8f) Color.Red else Color(0xFF005AC1),
                                                    trackColor = Color(0xFFE1E2EC),
                                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                                )
                                            }
                                        }

                                        // RAM
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E2EC)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("MEMORY USAGE", fontWeight = FontWeight.Bold, color = Color(0xFF44474F), fontSize = 11.sp)
                                                Text("${stats.memoryUsed.toInt()} / ${stats.memoryTotal.toInt()} GB", fontWeight = FontWeight.Black, color = Color(0xFF1C1B1F), fontSize = 20.sp)
                                                val memRatio = stats.memoryUsed / stats.memoryTotal
                                                LinearProgressIndicator(
                                                    progress = { memRatio },
                                                    color = if (memRatio > 0.85f) Color.Red else Color(0xFF005AC1),
                                                    trackColor = Color(0xFFE1E2EC),
                                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                                )
                                            }
                                        }
                                    }
                                }

                                // Storage widget
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E2EC)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text("ROOT DISK SPACE", fontWeight = FontWeight.Bold, color = Color(0xFF44474F), fontSize = 11.sp)
                                                Text("${stats.diskUsed.toInt()} GB / ${stats.diskTotal.toInt()} GB", color = Color(0xFF1C1B1F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            val diskRatio = stats.diskUsed / stats.diskTotal
                                            LinearProgressIndicator(
                                                progress = { diskRatio },
                                                color = Color(0xFF005AC1),
                                                trackColor = Color(0xFFE1E2EC),
                                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                            )
                                        }
                                    }
                                }

                                // NVIDIA GPU Monitor (ML Specific Card!)
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)), // Elegant high density background
                                        shape = RoundedCornerShape(16.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xA010B981)), // Emerald green tint border
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = Color(0xFF10B981))
                                                    Text("NVIDIA GPU ENGINE", fontWeight = FontWeight.Black, color = Color(0xFF10B981), fontSize = 14.sp)
                                                }
                                                if (stats.gpuTemp != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFFE6F4EA))
                                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("TEMP: ${stats.gpuTemp}", color = Color(0xFF137333), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }

                                            if (stats.gpuModel == null) {
                                                Text("No NVIDIA graphics engines detected on this server.", color = Color(0xFF44474F), fontSize = 12.sp)
                                            } else {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(stats.gpuModel, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F), fontSize = 15.sp)

                                                    if (stats.gpuMemUsed != null && stats.gpuMemTotal != null) {
                                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                            Text("VRAM ALLOCATION", color = Color.Gray, fontSize = 11.sp)
                                                            Text(
                                                                text = String.format("%.2f / %.2f GB", stats.gpuMemUsed, stats.gpuMemTotal),
                                                                color = Color(0xFF1C1B1F),
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                        val gpuRatio = stats.gpuMemUsed / stats.gpuMemTotal
                                                        LinearProgressIndicator(
                                                            progress = { gpuRatio },
                                                            color = Color(0xFF10B981),
                                                            trackColor = Color(0xFFE1E2EC),
                                                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Interactive Shell Terminal View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .background(Color(0xFFF8F9FF)),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Terminal display log card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Deep navy slate for gorgeous shell look
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(terminalLogs) { log ->
                                Text(
                                    text = log,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = when {
                                        log.startsWith("$") -> Color(0xFF38BDF8) // Command
                                        log.startsWith("Error") -> Color.Red     // Error
                                        log.startsWith("System") -> Color(0xFF10B981) // Stats system success
                                        else -> Color.White // Normal output
                                    },
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Quick Command shortcuts scroll row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        shortcutCommands.forEach { (label, command) ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E2EC)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .height(30.dp)
                                    .clickable {
                                        viewModel.commandInput.value = command
                                        viewModel.executeSshCommand()
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxHeight().padding(horizontal = 10.dp)
                                ) {
                                    Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF005AC1), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Terminal Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commandInput,
                            onValueChange = { viewModel.commandInput.value = it },
                            placeholder = { Text("Type custom bash shell command...", color = Color.Gray, fontSize = 13.sp) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF1C1B1F), fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF1C1B1F),
                                unfocusedTextColor = Color(0xFF1C1B1F),
                                focusedBorderColor = Color(0xFF005AC1),
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { viewModel.executeSshCommand() },
                            enabled = commandInput.isNotEmpty() && !isExecutingCommand,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (commandInput.isNotEmpty()) Color(0xFF005AC1) else Color(0xFFE1E2EC))
                        ) {
                            if (isExecutingCommand) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = if (commandInput.isNotEmpty()) Color.White else Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Server Dialog
    if (showAddServerDialog) {
        AlertDialog(
            onDismissRequest = { showAddServerDialog = false },
            title = { Text("Configure SSH GPU Server", color = Color(0xFF1C1B1F)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = serverName,
                        onValueChange = { serverName = it },
                        label = { Text("Display Name (e.g., RTX 4090 Box)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1C1B1F),
                            unfocusedTextColor = Color(0xFF1C1B1F),
                            focusedBorderColor = Color(0xFF005AC1),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = serverHost,
                            onValueChange = { serverHost = it },
                            label = { Text("Server Host / IP") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF1C1B1F),
                                unfocusedTextColor = Color(0xFF1C1B1F),
                                focusedBorderColor = Color(0xFF005AC1),
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier.weight(1.5f)
                        )
                        OutlinedTextField(
                            value = serverPort,
                            onValueChange = { serverPort = it },
                            label = { Text("Port") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF1C1B1F),
                                unfocusedTextColor = Color(0xFF1C1B1F),
                                focusedBorderColor = Color(0xFF005AC1),
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier.weight(0.7f)
                        )
                    }

                    OutlinedTextField(
                        value = serverUsername,
                        onValueChange = { serverUsername = it },
                        label = { Text("SSH Username") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1C1B1F),
                            unfocusedTextColor = Color(0xFF1C1B1F),
                            focusedBorderColor = Color(0xFF005AC1),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = serverPassword,
                        onValueChange = { serverPassword = it },
                        label = { Text("Password or Private Key") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1C1B1F),
                            unfocusedTextColor = Color(0xFF1C1B1F),
                            focusedBorderColor = Color(0xFF005AC1),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = serverNotes,
                        onValueChange = { serverNotes = it },
                        label = { Text("Brief notes / description") },
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
                        if (serverName.isNotEmpty() && serverHost.isNotEmpty() && serverUsername.isNotEmpty()) {
                            viewModel.addServer(
                                name = serverName,
                                host = serverHost,
                                port = serverPort.toIntOrNull() ?: 22,
                                username = serverUsername,
                                authType = "PASSWORD",
                                passwordOrKey = serverPassword,
                                notes = serverNotes
                            )
                            // Clear form
                            serverName = ""
                            serverHost = ""
                            serverPort = "22"
                            serverUsername = ""
                            serverPassword = ""
                            serverNotes = ""
                            showAddServerDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1))
                ) {
                    Text("Add Server", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddServerDialog = false }) {
                    Text("Cancel", color = Color(0xFF44474F))
                }
            },
            containerColor = Color(0xFFFFFFFF)
        )
    }
}
