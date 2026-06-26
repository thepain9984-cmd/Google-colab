package com.example.data.ssh

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties

data class ServerStats(
    val cpuUsage: Float,      // 0.0 to 1.0
    val memoryTotal: Float,    // in GB
    val memoryUsed: Float,     // in GB
    val diskTotal: Float,      // in GB
    val diskUsed: Float,       // in GB
    val gpuModel: String?,     // e.g., "NVIDIA RTX 4090"
    val gpuTemp: String?,      // e.g., "65°C"
    val gpuMemTotal: Float?,   // in GB
    val gpuMemUsed: Float?,    // in GB
    val osName: String         // e.g., "Ubuntu 22.04 LTS"
)

class SshManager {

    suspend fun testConnection(
        host: String,
        port: Int,
        username: String,
        passwordOrKey: String
    ): Boolean = withContext(Dispatchers.IO) {
        val jsch = JSch()
        var session: Session? = null
        try {
            session = jsch.getSession(username, host, port)
            session.setPassword(passwordOrKey)
            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            session.setConfig(config)
            session.connect(5000) // 5 second timeout
            session.isConnected
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            session?.disconnect()
        }
    }

    suspend fun executeCommand(
        host: String,
        port: Int,
        username: String,
        passwordOrKey: String,
        command: String
    ): String = withContext(Dispatchers.IO) {
        val jsch = JSch()
        var session: Session? = null
        var channel: ChannelExec? = null
        try {
            session = jsch.getSession(username, host, port)
            session.setPassword(passwordOrKey)
            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            session.setConfig(config)
            session.connect(8000)

            channel = session.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            val outputStream = ByteArrayOutputStream()
            val errorStream = ByteArrayOutputStream()
            channel.setOutputStream(outputStream)
            channel.setErrStream(errorStream)
            channel.connect(5000)

            val startTime = System.currentTimeMillis()
            while (!channel.isClosed && System.currentTimeMillis() - startTime < 15000) {
                delay(100)
            }
            outputStream.toString("UTF-8").ifEmpty { errorStream.toString("UTF-8") }
        } catch (e: Exception) {
            "Error executing command: ${e.localizedMessage}"
        } finally {
            channel?.disconnect()
            session?.disconnect()
        }
    }

    suspend fun fetchServerStats(
        host: String,
        port: Int,
        username: String,
        passwordOrKey: String
    ): ServerStats = withContext(Dispatchers.IO) {
        val osCommand = "uname -sr"
        val cpuCommand = "top -bn1 | grep 'Cpu(s)'"
        val memCommand = "free -m"
        val diskCommand = "df -m /"
        val gpuCommand = "nvidia-smi --query-gpu=gpu_name,temperature.gpu,memory.total,memory.used --format=csv,noheader,nounits 2>/dev/null || echo 'NONE'"

        val osRaw = executeCommand(host, port, username, passwordOrKey, osCommand).trim()
        val cpuRaw = executeCommand(host, port, username, passwordOrKey, cpuCommand).trim()
        val memRaw = executeCommand(host, port, username, passwordOrKey, memCommand).trim()
        val diskRaw = executeCommand(host, port, username, passwordOrKey, diskCommand).trim()
        val gpuRaw = executeCommand(host, port, username, passwordOrKey, gpuCommand).trim()

        // Parse OS name
        val osName = osRaw.ifEmpty { "Linux Server" }

        // Parse CPU (e.g. "%Cpu(s):  5.0 us,  2.0 sy,  0.0 ni, 93.0 id ...")
        var cpuUsage = 0.15f
        try {
            if (cpuRaw.contains("id")) {
                val idleMatch = Regex("([0-9.]+)\\s+id").find(cpuRaw)
                if (idleMatch != null) {
                    val idle = idleMatch.groupValues[1].toFloat()
                    cpuUsage = (100f - idle) / 100f
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Parse Memory (e.g. "Mem:          32000        12000")
        var memTotal = 16.0f
        var memUsed = 4.0f
        try {
            val lines = memRaw.lines()
            val memLine = lines.firstOrNull { it.contains("Mem:") }
            if (memLine != null) {
                val parts = memLine.split(Regex("\\s+")).filter { it.isNotEmpty() }
                if (parts.size >= 3) {
                    val totalM = parts[1].toFloat()
                    val usedM = parts[2].toFloat()
                    memTotal = totalM / 1024f
                    memUsed = usedM / 1024f
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Parse Disk (e.g. "/dev/sda1       240000     120000")
        var diskTotal = 500f
        var diskUsed = 120f
        try {
            val lines = diskRaw.lines()
            if (lines.size >= 2) {
                val dataLine = lines[1]
                val parts = dataLine.split(Regex("\\s+")).filter { it.isNotEmpty() }
                if (parts.size >= 4) {
                    val totalM = parts[1].toFloat()
                    val usedM = parts[2].toFloat()
                    diskTotal = totalM / 1024f
                    diskUsed = usedM / 1024f
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Parse GPU (e.g. "NVIDIA GeForce RTX 4090, 62, 24564, 4120" or "NONE")
        var gpuModel: String? = null
        var gpuTemp: String? = null
        var gpuMemTotal: Float? = null
        var gpuMemUsed: Float? = null

        if (gpuRaw.isNotEmpty() && gpuRaw != "NONE" && !gpuRaw.contains("Error")) {
            try {
                val parts = gpuRaw.split(",").map { it.trim() }
                if (parts.size >= 4) {
                    gpuModel = parts[0]
                    gpuTemp = "${parts[1]}°C"
                    gpuMemTotal = parts[2].toFloatOrNull()?.let { it / 1024f }
                    gpuMemUsed = parts[3].toFloatOrNull()?.let { it / 1024f }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        ServerStats(
            cpuUsage = cpuUsage.coerceIn(0f, 1f),
            memoryTotal = memTotal,
            memoryUsed = memUsed,
            diskTotal = diskTotal,
            diskUsed = diskUsed,
            gpuModel = gpuModel,
            gpuTemp = gpuTemp,
            gpuMemTotal = gpuMemTotal,
            gpuMemUsed = gpuMemUsed,
            osName = osName
        )
    }
}
