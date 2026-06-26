package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.DriveApiService
import com.example.data.api.DriveFile
import com.example.data.database.AppDatabase
import com.example.data.database.NotebookEntity
import com.example.data.database.ServerEntity
import com.example.data.repository.AppRepository
import com.example.data.ssh.ServerStats
import com.example.data.ssh.SshManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

enum class AppTab {
    COLAB, DRIVE, SERVERS, TEMPLATES
}

class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(database.serverDao(), database.notebookDao())
    private val sshManager = SshManager()

    // UI States
    val currentTab = MutableStateFlow(AppTab.COLAB)
    val selectedNotebookUrl = MutableStateFlow("https://colab.research.google.com/")
    val isDesktopUserAgent = MutableStateFlow(true)

    // Notebooks State
    val notebooks: StateFlow<List<NotebookEntity>> = repository.allNotebooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Servers State
    val servers: StateFlow<List<ServerEntity>> = repository.allServers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Server & SSH Console State
    val activeServer = MutableStateFlow<ServerEntity?>(null)
    val activeServerStats = MutableStateFlow<ServerStats?>(null)
    val isLoadingStats = MutableStateFlow(false)
    val statsError = MutableStateFlow<String?>(null)

    val commandInput = MutableStateFlow("")
    val terminalLogs = MutableStateFlow<List<String>>(listOf("Terminal ready. Select a server to connect via SSH."))
    val isExecutingCommand = MutableStateFlow(false)

    // Google Drive Sync State
    val googleAccessToken = MutableStateFlow<String?>(null)
    val isGoogleDriveSignedIn = MutableStateFlow(false)
    val driveFiles = MutableStateFlow<List<DriveFile>>(emptyList())
    val isLoadingDrive = MutableStateFlow(false)
    val driveError = MutableStateFlow<String?>(null)
    val driveSearchQuery = MutableStateFlow("")

    // Settings & Custom Client Config for Developers
    val customClientId = MutableStateFlow("1073860012579-q7g37f9re2idvphgh805h7tbe7t86v8s.apps.googleusercontent.com")
    val customRedirectUri = MutableStateFlow("https://localhost/oauth2callback")

    // Retrofit client for Drive
    private val driveApiService: DriveApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/drive/v3/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(DriveApiService::class.java)
    }

    init {
        viewModelScope.launch {
            repository.seedDefaultNotebooksIfNeeded()
        }
    }

    // Google Colab WebView helpers
    fun openNotebookUrl(url: String) {
        selectedNotebookUrl.value = url
        currentTab.value = AppTab.COLAB
    }

    fun toggleUserAgent() {
        isDesktopUserAgent.value = !isDesktopUserAgent.value
    }

    // Add / Delete Notebook Bookmarks
    fun addNotebook(title: String, url: String, category: String = "My Notebooks") {
        viewModelScope.launch {
            repository.insertNotebook(
                NotebookEntity(title = title, url = url, category = category)
            )
        }
    }

    fun deleteNotebook(notebook: NotebookEntity) {
        viewModelScope.launch {
            repository.deleteNotebook(notebook)
        }
    }

    fun markNotebookOpened(id: Int) {
        viewModelScope.launch {
            repository.updateNotebookLastOpened(id, System.currentTimeMillis())
        }
    }

    // Google Drive file management
    fun signInGoogleDrive(token: String) {
        googleAccessToken.value = token
        isGoogleDriveSignedIn.value = true
        fetchDriveFiles()
    }

    fun signOutGoogleDrive() {
        googleAccessToken.value = null
        isGoogleDriveSignedIn.value = false
        driveFiles.value = emptyList()
        driveError.value = null
    }

    fun fetchDriveFiles() {
        val token = googleAccessToken.value ?: return
        viewModelScope.launch {
            isLoadingDrive.value = true
            driveError.value = null
            try {
                // Build a query: list both folders and colab files or search matching term
                val search = driveSearchQuery.value
                val baseQuery = "mimeType = 'application/vnd.google-apps.folder' or mimeType = 'application/x-ipynb+json' or name contains '.ipynb'"
                val query = if (search.isNotEmpty()) {
                    "($baseQuery) and name contains '$search'"
                } else {
                    baseQuery
                }

                val response = withContext(Dispatchers.IO) {
                    driveApiService.listFiles(
                        authHeader = "Bearer $token",
                        query = query
                    )
                }
                driveFiles.value = response.files
            } catch (e: Exception) {
                driveError.value = "Failed to load Drive files: ${e.localizedMessage}"
                e.printStackTrace()
            } finally {
                isLoadingDrive.value = false
            }
        }
    }

    // Server SSH Management
    fun selectServer(server: ServerEntity?) {
        activeServer.value = server
        activeServerStats.value = null
        statsError.value = null
        if (server != null) {
            terminalLogs.value = listOf("Connected to server info terminal for ${server.name} (${server.host}). Ready to run commands.")
            fetchActiveServerStats()
        } else {
            terminalLogs.value = listOf("Terminal ready. Select a server to connect via SSH.")
        }
    }

    fun addServer(name: String, host: String, port: Int, username: String, authType: String, passwordOrKey: String, notes: String) {
        viewModelScope.launch {
            repository.insertServer(
                ServerEntity(
                    name = name,
                    host = host,
                    port = port,
                    username = username,
                    authType = authType,
                    passwordOrKey = passwordOrKey,
                    notes = notes
                )
            )
        }
    }

    fun deleteServer(server: ServerEntity) {
        viewModelScope.launch {
            if (activeServer.value?.id == server.id) {
                selectServer(null)
            }
            repository.deleteServer(server)
        }
    }

    fun fetchActiveServerStats() {
        val server = activeServer.value ?: return
        viewModelScope.launch {
            isLoadingStats.value = true
            statsError.value = null
            try {
                // If the host is "localhost" or "simulation", or if JSch fails or we want a fallback simulator,
                // we can also generate rich stats so the user gets immediate beautiful GPU dashboards
                if (server.host.lowercase() == "demo" || server.host.lowercase() == "localhost" || server.host.lowercase() == "simulation") {
                    simulateStats()
                } else {
                    val stats = sshManager.fetchServerStats(
                        host = server.host,
                        port = server.port,
                        username = server.username,
                        passwordOrKey = server.passwordOrKey
                    )
                    activeServerStats.value = stats
                    terminalLogs.value = terminalLogs.value + "System: Retrieved latest performance logs and NVIDIA GPU diagnostics successfully."
                }
            } catch (e: Exception) {
                statsError.value = "SSH metrics fetch failed: ${e.localizedMessage}. Using simulated fallback."
                simulateStats() // fallback to let them explore GPU dashboard
            } finally {
                isLoadingStats.value = false
            }
        }
    }

    private fun simulateStats() {
        // High fidelity simulated stats for DEMO/SIMULATION
        val rand = kotlin.random.Random.Default
        activeServerStats.value = ServerStats(
            cpuUsage = rand.nextFloat() * (0.8f - 0.3f) + 0.3f,
            memoryTotal = 32.0f,
            memoryUsed = rand.nextFloat() * (26.0f - 12.0f) + 12.0f,
            diskTotal = 1000.0f,
            diskUsed = 432.5f,
            gpuModel = "NVIDIA RTX 4090 (24GB)",
            gpuTemp = "${(60..78).random()}°C",
            gpuMemTotal = 24.0f,
            gpuMemUsed = rand.nextFloat() * (21.5f - 8.0f) + 8.0f,
            osName = "Ubuntu 22.04 LTS (Kernel 5.15.0-generic)"
        )
    }

    fun executeSshCommand() {
        val server = activeServer.value ?: return
        val cmd = commandInput.value.trim()
        if (cmd.isEmpty()) return

        terminalLogs.value = terminalLogs.value + "$ ${server.username}@mobile-ssh: $cmd"
        commandInput.value = ""
        isExecutingCommand.value = true

        viewModelScope.launch {
            try {
                val output = withContext(Dispatchers.IO) {
                    if (server.host.lowercase() == "demo" || server.host.lowercase() == "localhost" || server.host.lowercase() == "simulation") {
                        simulateCommandExecution(cmd)
                    } else {
                        sshManager.executeCommand(
                            host = server.host,
                            port = server.port,
                            username = server.username,
                            passwordOrKey = server.passwordOrKey,
                            command = cmd
                        )
                    }
                }
                terminalLogs.value = terminalLogs.value + output.lines().filter { it.isNotEmpty() }
            } catch (e: Exception) {
                terminalLogs.value = terminalLogs.value + "Error: ${e.localizedMessage}"
            } finally {
                isExecutingCommand.value = false
            }
        }
    }

    private fun simulateCommandExecution(cmd: String): String {
        val lower = cmd.lowercase()
        return when {
            lower.contains("nvidia-smi") -> {
                """
                +-----------------------------------------------------------------------------+
                | NVIDIA-SMI 525.60.13    Driver Version: 525.60.13    CUDA Version: 12.0     |
                |-------------------------------+----------------------+----------------------+
                | GPU  Name        Persistence-M| Bus-Id        Disp.A | Volatile Uncorr. ECC |
                | Fan  Temp  Perf  Pwr:Usage/Cap|         Memory-Usage | GPU-Util  Compute M. |
                |                               |                      |               MIG M. |
                |===============================+======================+======================|
                |   0  NVIDIA GeForce ...  On   | 00000000:01:00.0  On |                  N/A |
                | 31%   65C    P2   145W / 450W |  14320MiB / 24576MiB |     78%      Default |
                |                               |                      |                  N/A |
                +-------------------------------+----------------------+----------------------+
                """.trimIndent()
            }
            lower.contains("top") || lower.contains("htop") -> {
                """
                top - 05:12:43 up 12 days,  3:12,  1 user,  load average: 1.43, 1.21, 0.98
                Tasks: 210 total,   2 running, 208 sleeping,   0 stopped,   0 zombie
                %Cpu(s): 12.5 us,  3.2 sy,  0.0 ni, 84.3 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st
                MiB Mem :  32120.5 total,   4120.1 free,  18450.4 used,   9550.0 buff/cache
                MiB Swap:  16384.0 total,  15210.5 free,   1173.5 used.  13120.2 avail Mem 
                """.trimIndent()
            }
            lower.contains("free") -> {
                "              total        used        free      shared  buff/cache   available\n" +
                "Mem:          32120       18450        4120         128        9550       13120\n" +
                "Swap:         16384        1173       15210"
            }
            lower.contains("df") -> {
                "Filesystem     1M-blocks   Used Available Use% Mounted on\n" +
                "/dev/sda1        1024000 442880    581120  44% /"
            }
            lower.contains("uname") -> {
                "Linux colab-server-gpu-box-1 5.15.0-88-generic #98-Ubuntu SMP Mon Oct 2 15:18:56 UTC 2023 x86_64 x86_64 x86_64 GNU/Linux"
            }
            lower.contains("python") || lower.contains("run") || lower.contains("train") -> {
                "Epoch 1/10: [==============================] - Loss: 0.423 - Accuracy: 0.842\n" +
                "Epoch 2/10: [==============================] - Loss: 0.212 - Accuracy: 0.918\n" +
                "Epoch 3/10: [==============================] - Loss: 0.154 - Accuracy: 0.945\n" +
                "Training complete. Checkpoints saved to ./models/checkpoints/"
            }
            lower.contains("help") || lower.contains("list") -> {
                "Standard Linux shell enabled. Recommended commands to try: 'nvidia-smi', 'top', 'free -m', 'df -h', 'uname -a'."
            }
            else -> {
                "Command executed successfully on remote server: exit code 0."
            }
        }
    }
}
