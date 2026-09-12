package com.servermonitor

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.servermonitor.data.ConfigManager
import com.servermonitor.data.StatusParser
import com.servermonitor.model.AppConfig
import com.servermonitor.model.ServerConfig
import com.servermonitor.model.ServiceConfig
import com.servermonitor.model.SystemStatus
import com.servermonitor.ssh.SshManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val configManager = ConfigManager(app)
    private val ssh = SshManager()

    var config by mutableStateOf(configManager.load())
        private set

    var status by mutableStateOf<SystemStatus?>(null)
        private set
    var listeningPorts by mutableStateOf<Set<Int>>(emptySet())
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun dismissError() { errorMessage = null }

    private fun persist(c: AppConfig) {
        config = c
        configManager.save(c)
    }

    // ---- 服务器 CRUD ----
    fun addServer(s: ServerConfig) {
        val c = config
        c.servers.add(s)
        persist(c)
    }

    fun updateServer(index: Int, s: ServerConfig) {
        val c = config
        if (index in c.servers.indices) c.servers[index] = s
        persist(c)
    }

    fun removeServer(index: Int) {
        val c = config
        if (index in c.servers.indices) c.servers.removeAt(index)
        persist(c)
    }

    fun setServerShowOnHome(index: Int, show: Boolean) {
        val c = config
        if (index in c.servers.indices) c.servers[index] = c.servers[index].copy(showOnHome = show)
        persist(c)
    }

    // ---- 服务 CRUD ----
    fun addService(s: ServiceConfig) {
        val c = config
        c.services.add(s)
        persist(c)
    }

    fun updateService(index: Int, s: ServiceConfig) {
        val c = config
        if (index in c.services.indices) c.services[index] = s
        persist(c)
    }

    fun removeService(index: Int) {
        val c = config
        if (index in c.services.indices) c.services.removeAt(index)
        persist(c)
    }

    // ---- 状态采集 ----
    fun refreshStatus() {
        val server = config.servers.firstOrNull { it.showOnHome }
        if (server == null) {
            errorMessage = "请先在「服务器」页添加并勾选服务器"
            return
        }
        viewModelScope.launch {
            isRefreshing = true
            errorMessage = null
            try {
                val output = withContext(Dispatchers.IO) {
                    ssh.execute(server, StatusParser.STATUS_COMMAND)
                }
                status = StatusParser.parseStatus(output)
                listeningPorts = StatusParser.parseListeningPorts(output)
            } catch (e: Exception) {
                errorMessage = "连接失败：${e.message ?: e.javaClass.simpleName}"
            } finally {
                isRefreshing = false
            }
        }
    }

    // ---- 服务控制 ----
    fun controlService(svc: ServiceConfig, action: String) {
        val server = config.servers.firstOrNull { it.showOnHome }
        if (server == null) {
            errorMessage = "请先在「服务器」页添加并勾选服务器"
            return
        }
        val command = when (action) {
            "start" -> svc.startCmd
            "stop" -> svc.stopCmd
            "restart" -> svc.restartCmd.ifBlank { "${svc.stopCmd} ; sleep 1 ; ${svc.startCmd}" }
            else -> return
        }
        if (command.isBlank()) {
            errorMessage = "「${svc.name}」未配置该命令"
            return
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { ssh.execute(server, command) }
                refreshStatus()
            } catch (e: Exception) {
                errorMessage = "操作失败：${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    // ---- 配置导入导出 ----
    fun exportConfig(uri: Uri) {
        val ok = configManager.exportTo(uri, config)
        if (!ok) errorMessage = "导出失败"
    }

    fun importConfig(uri: Uri) {
        val imported = configManager.importFrom(uri)
        if (imported == null) {
            errorMessage = "导入失败：无法解析该文件"
            return
        }
        persist(imported)
    }
}