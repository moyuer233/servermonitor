package com.servermonitor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.servermonitor.MainViewModel
import com.servermonitor.model.ServerConfig
import com.servermonitor.ui.theme.SurfaceDark
import com.servermonitor.ui.theme.SurfaceVariant
import com.servermonitor.ui.theme.TextPrimary
import com.servermonitor.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ServerScreen(vm: MainViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingServer by remember { mutableStateOf<ServerConfig?>(null) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("服务器", fontWeight = FontWeight.SemiBold) },
            actions = {
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "添加服务器")
                }
            }
        )

        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            if (vm.config.servers.isEmpty()) {
                item {
                    Text(
                        "还没有服务器，点击右上角「添加」。\n填一次后即可在主页查看状态。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                itemsIndexed(vm.config.servers, key = { _, s -> s.id }) { _, server ->
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, SurfaceVariant)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(server.name, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text(
                                        "${server.username}@${server.host}:${server.port}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        if (server.useIpv6) "IPv6" else "IPv4",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { editingServer = server }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = TextSecondary)
                                }
                                IconButton(onClick = { vm.removeServer(server.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("在主页显示", Modifier.weight(1f), color = TextSecondary)
                                Switch(
                                    checked = server.showOnHome,
                                    onCheckedChange = { vm.setServerShowOnHome(server.id, it) }
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }

    if (showAddDialog) {
        ServerEditDialog(
            initial = null,
            onDismiss = { showAddDialog = false },
            onSave = {
                vm.addServer(it)
                showAddDialog = false
            }
        )
    }

    editingServer?.let { initial ->
        ServerEditDialog(
            initial = initial,
            onDismiss = { editingServer = null },
            onSave = { updated ->
                vm.updateServer(initial.id, updated)
                editingServer = null
            }
        )
    }
}

@Composable
private fun ServerEditDialog(
    initial: ServerConfig?,
    onDismiss: () -> Unit,
    onSave: (ServerConfig) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var host by remember { mutableStateOf(initial?.host ?: "") }
    var port by remember { mutableStateOf(if (initial == null) "22" else initial.port.toString()) }
    var username by remember { mutableStateOf(initial?.username ?: "root") }
    var privateKey by remember { mutableStateOf(initial?.privateKey ?: "") }
    var useIpv6 by remember { mutableStateOf(initial?.useIpv6 ?: true) }
    var showOnHome by remember { mutableStateOf(initial?.showOnHome ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "添加服务器" else "编辑服务器") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("名称") }, singleLine = true)
                OutlinedTextField(host, { host = it }, label = { Text("地址（IPv4 / IPv6）") }, singleLine = true)
                OutlinedTextField(
                    port, { port = it.filter { c -> c.isDigit() } }, label = { Text("端口") },
                    singleLine = true
                )
                OutlinedTextField(username, { username = it }, label = { Text("用户名") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("IPv6", Modifier.weight(1f))
                    Switch(checked = useIpv6, onCheckedChange = { useIpv6 = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("在主页显示", Modifier.weight(1f))
                    Switch(checked = showOnHome, onCheckedChange = { showOnHome = it })
                }
                OutlinedTextField(
                    privateKey, { privateKey = it }, label = { Text("SSH 私钥") },
                    minLines = 5
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    ServerConfig(
                        name = name.trim(),
                        host = host.trim(),
                        port = port.toIntOrNull() ?: 22,
                        username = username.trim(),
                        privateKey = privateKey.trim(),
                        useIpv6 = useIpv6,
                        showOnHome = showOnHome
                    )
                )
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}