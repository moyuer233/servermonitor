package com.servermonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servermonitor.MainViewModel
import com.servermonitor.model.ServiceConfig
import com.servermonitor.model.SystemStatus
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel) {
    var editingService by remember { mutableStateOf<ServiceConfig?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.refreshStatus()
        while (true) {
            delay(10000)
            vm.refreshStatus()
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("服务器监控", fontWeight = FontWeight.SemiBold) },
            actions = {
                IconButton(onClick = { vm.refreshStatus() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                }
            }
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { StatusPanel(vm.status, vm.isRefreshing) }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("运行中的服务", style = MaterialTheme.typography.titleMedium)
                    FilledTonalButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("添加")
                    }
                }
            }

            if (vm.config.services.isEmpty()) {
                item {
                    Text(
                        "还没有配置服务，点击「添加」配置服务名称与命令。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                itemsIndexed(vm.config.services) { index, svc ->
                    ServiceItem(
                        svc = svc,
                        running = svc.port in vm.listeningPorts,
                        onStart = { vm.controlService(svc, "start") },
                        onStop = { vm.controlService(svc, "stop") },
                        onRestart = { vm.controlService(svc, "restart") },
                        onEdit = { editingService = svc },
                        onDelete = { vm.removeService(index) }
                    )
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (showAddDialog) {
        ServiceEditDialog(
            initial = null,
            onDismiss = { showAddDialog = false },
            onSave = {
                vm.addService(it)
                showAddDialog = false
            }
        )
    }

    editingService?.let { initial ->
        val idx = vm.config.services.indexOfFirst { it === initial }
        ServiceEditDialog(
            initial = initial,
            onDismiss = { editingService = null },
            onSave = { updated ->
                if (idx >= 0) vm.updateService(idx, updated) else vm.addService(updated)
                editingService = null
            }
        )
    }
}

@Composable
private fun StatusPanel(status: SystemStatus?, refreshing: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("系统状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            if (status == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (refreshing) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("正在连接服务器…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("暂无数据，点击右上角刷新", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                return@Column
            }

            val memPercent = if (status.memoryTotalMb > 0)
                (status.memoryUsedMb.toDouble() / status.memoryTotalMb * 100.0) else 0.0

            MetricRow("CPU", "${status.cpuUsage.toInt()}%", "${status.cpuCores} 核 · 负载 ${status.load1}")
            Progress(progress = status.cpuUsage)
            MetricRow("GPU", status.gpuName, "占用率不支持")
            Spacer(Modifier.height(8.dp))
            MetricRow("内存", "${status.memoryUsedMb} / ${status.memoryTotalMb} MB", "${memPercent.toInt()}%")
            Progress(progress = memPercent)
            Spacer(Modifier.height(8.dp))
            MetricRow("温度", "CPU ${Fmt.temp(status.cpuTempC)}", "电池 ${Fmt.temp(status.batteryTempC)}")
            MetricRow("电量", "${status.batteryPercent}%", if (status.charging) "充电中" else "未充电")
        }
    }
}

private object Fmt {
    fun temp(celsius: Double): String = if (celsius > 0) "${"%.1f".format(celsius)}℃" else "N/A"
}

@Composable
private fun MetricRow(label: String, value: String, sub: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(56.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun Progress(progress: Double) {
    LinearProgressIndicator(
        progress = { (progress / 100.0).toFloat().coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(6.dp)
    )
}

@Composable
private fun ServiceItem(
    svc: ServiceConfig,
    running: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(svc.name, fontWeight = FontWeight.SemiBold)
                    Text("端口 ${svc.port}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val dotColor = if (running) Color(0xFF2DD4A8) else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    Modifier.size(10.dp).background(dotColor, CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (running) "运行中" else "已停止",
                    style = MaterialTheme.typography.bodySmall,
                    color = dotColor
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onStart) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, Modifier.size(18.dp))
                    Text("启动")
                }
                OutlinedButton(onClick = onStop) {
                    Icon(Icons.Filled.Stop, contentDescription = null, Modifier.size(18.dp))
                    Text("停止")
                }
                OutlinedButton(onClick = onRestart) {
                    Icon(Icons.Filled.Replay, contentDescription = null, Modifier.size(18.dp))
                    Text("重启")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ServiceEditDialog(
    initial: ServiceConfig?,
    onDismiss: () -> Unit,
    onSave: (ServiceConfig) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var port by remember { mutableStateOf(if (initial == null) "" else initial.port.toString()) }
    var startCmd by remember { mutableStateOf(initial?.startCmd ?: "") }
    var stopCmd by remember { mutableStateOf(initial?.stopCmd ?: "") }
    var restartCmd by remember { mutableStateOf(initial?.restartCmd ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "添加服务" else "编辑服务") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("名称") }, singleLine = true)
                OutlinedTextField(
                    port, { port = it.filter { c -> c.isDigit() } }, label = { Text("端口") },
                    singleLine = true
                )
                OutlinedTextField(startCmd, { startCmd = it }, label = { Text("启动命令") }, minLines = 2)
                OutlinedTextField(stopCmd, { stopCmd = it }, label = { Text("停止命令") }, minLines = 2)
                OutlinedTextField(restartCmd, { restartCmd = it }, label = { Text("重启命令（留空=自动停后启）") }, minLines = 2)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(ServiceConfig(name.trim(), port.toIntOrNull() ?: 0, startCmd.trim(), stopCmd.trim(), restartCmd.trim()))
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}