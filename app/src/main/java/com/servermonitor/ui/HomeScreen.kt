package com.servermonitor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servermonitor.MainViewModel
import com.servermonitor.model.ServiceConfig
import com.servermonitor.ui.theme.FlClashGreen
import com.servermonitor.ui.theme.SurfaceDark
import com.servermonitor.ui.theme.SurfaceVariant
import com.servermonitor.ui.theme.TextPrimary
import com.servermonitor.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: MainViewModel) {
    var editingService by remember { mutableStateOf<ServiceConfig?>(null) }
    var deletingService by remember { mutableStateOf<ServiceConfig?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val activeId = vm.activeServer?.id
    LaunchedEffect(activeId) {
        vm.refreshStatus()
        while (true) {
            val intervalMs = vm.activeServer?.refreshSeconds?.coerceIn(2, 300)?.times(1000L) ?: 10000L
            delay(intervalMs)
            vm.refreshStatus()
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("服务器监控", fontWeight = FontWeight.SemiBold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            actions = {
                IconButton(onClick = { vm.refreshStatus() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                }
            }
        )

        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 状态区：独立重组域，数据刷新不波及服务列表
                item(key = "stats") { StatGrid(vm) }

            item(key = "services_header") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("运行中的服务", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    FilledTonalButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(18.dp))
                        Text("添加")
                    }
                }
            }

            if (vm.config.services.isEmpty()) {
                item(key = "services_empty") {
                    Text(
                        "还没有配置服务，点击「添加」配置服务名称与命令。",
                        color = TextSecondary
                    )
                }
            } else {
                items(vm.config.services, key = { it.id }) { svc ->
                    ServiceItem(
                        vm = vm,
                        svc = svc,
                        onStart = { vm.controlService(svc, "start") },
                        onStop = { vm.controlService(svc, "stop") },
                        onRestart = { vm.controlService(svc, "restart") },
                        onEdit = { editingService = svc },
                        onDelete = { deletingService = svc }
                    )
                }
            }
        }
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
        ServiceEditDialog(
            initial = initial,
            onDismiss = { editingService = null },
            onSave = { updated ->
                vm.updateService(initial.id, updated)
                editingService = null
            }
        )
    }

    deletingService?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingService = null },
            title = { Text("删除服务卡片") },
            text = { Text("确定要删除「${target.name}」的管理卡片吗？此操作不会影响服务器上的服务本身。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.removeService(target.id)
                    deletingService = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingService = null }) { Text("取消") } }
        )
    }
}

/** 2×2 状态指标卡网格。所有实时数据只在这里读取，独立于服务列表重组。 */
@Composable
private fun StatGrid(vm: MainViewModel) {
    val status = vm.status
    val refreshing = vm.isRefreshing

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                Modifier.weight(1f),
                Icons.Filled.Speed,
                "CPU",
                if (status == null) "--" else "${status.cpuUsage.toInt()}%",
                if (status == null) "等待数据" else "${status.cpuCores} 核 · 负载 ${status.load1}",
                progress = status?.cpuUsage?.let { (it / 100.0).toFloat().coerceIn(0f, 1f) }
            )
            StatCard(
                Modifier.weight(1f),
                Icons.Filled.Memory,
                "内存",
                if (status == null) "--" else "${status.memoryUsedMb} / ${status.memoryTotalMb}MB",
                if (status == null) "等待数据" else pct(status.memoryUsedMb, status.memoryTotalMb),
                progress = status?.let { (it.memoryUsedMb.toDouble() / (it.memoryTotalMb.coerceAtLeast(1))).toFloat().coerceIn(0f, 1f) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                Modifier.weight(1f),
                Icons.Filled.DeviceThermostat,
                "温度",
                if (status == null) "--" else temp(status.cpuTempC),
                if (status == null) "等待数据" else "电池 ${temp(status.batteryTempC)}"
            )
            StatCard(
                Modifier.weight(1f),
                if (status?.charging == true) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryFull,
                if (status?.charging == true) "电量 · 已充电" else "电量",
                if (status == null) "--" else "${status.batteryPercent}%",
                if (status == null) "等待数据" else if (status.charging) "充电中" else "未充电",
                progress = status?.let { (it.batteryPercent / 100f).coerceIn(0f, 1f) }
            )
        }
        if (status == null && refreshing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = FlClashGreen)
                Spacer(Modifier.width(10.dp))
                Text("正在连接服务器…", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

private fun pct(used: Long, total: Long): String =
    if (total <= 0) "0%" else "${(used.toDouble() / total * 100).toInt()}%"

private fun temp(c: Double): String = if (c > 0) "${"%.1f".format(c)}℃" else "N/A"

@Composable
private fun StatCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    sub: String,
    progress: Float? = null
) {
    Surface(
        modifier.height(100.dp),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, SurfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = FlClashGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = FlClashGreen,
                    trackColor = SurfaceVariant
                )
            } else {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** 扁平化服务管理卡片：固定高度 + 运行状态在卡片内部读取（对应 FlClash 重绘隔离）。 */
@Composable
private fun ServiceItem(
    vm: MainViewModel,
    svc: ServiceConfig,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val running = svc.port in vm.listeningPorts
    val dotColor = if (running) FlClashGreen else TextSecondary

    Surface(
        Modifier.fillMaxWidth().height(132.dp),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, SurfaceVariant)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(svc.name, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("端口 ${svc.port}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Box(Modifier.size(8.dp).background(dotColor, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (running) "运行中" else "已停止",
                    style = MaterialTheme.typography.bodySmall,
                    color = dotColor
                )
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ServiceAction("启动", Icons.Filled.PlayArrow, onStart)
                ServiceAction("停止", Icons.Filled.Stop, onStop)
                ServiceAction("重启", Icons.Filled.Replay, onRestart)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = TextSecondary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ServiceAction(label: String, icon: ImageVector, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 10.dp)) {
        Icon(icon, contentDescription = null, Modifier.size(16.dp), tint = FlClashGreen)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
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
                onSave(
                    ServiceConfig(
                        name = name.trim(),
                        port = port.toIntOrNull() ?: 0,
                        startCmd = startCmd.trim(),
                        stopCmd = stopCmd.trim(),
                        restartCmd = restartCmd.trim()
                    )
                )
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}