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

    // ---- 服务器 CRUD（按稳定 id 定位，改名/增删不再受列表索引错位影响）----
    fun addServer(s: ServerConfig) =
        persist(config.copy(servers = config.servers + s))

    fun updateServer(id: String, s: ServerConfig) =
        persist(config.copy(servers = config.servers.map { if (it.id == id) s.copy(id = id) else it }))

    fun removeServer(id: String) =
        persist(config.copy(servers = config.servers.filterNot { it.id == id }))

    fun setServerShowOnHome(id: String, show: Boolean) =
        persist(config.copy(servers = config.servers.map { if (it.id == id) it.copy(showOnHome = show) else it }))

    // ---- 服务 CRUD ----
    fun addService(s: ServiceConfig) =
        persist(config.copy(services = config.services + s))

    fun updateService(id: String, s: ServiceConfig) =
        persist(config.copy(services = config.services.map { if (it.id == id) s.copy(id = id) else it }))

    fun removeService(id: String) =
        persist(config.copy(services = config.services.filterNot { it.id == id }))

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
                val parsedStatus = StatusParser.parseStatus(output)
                val parsedPorts = StatusParser.parseListeningPorts(output)
                // 值未变化不提交 state，避免每 10s 无谓重组打断滑动
                if (parsedStatus != status) status = parsedStatus
                if (parsedPorts != listeningPorts) listeningPorts = parsedPorts
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