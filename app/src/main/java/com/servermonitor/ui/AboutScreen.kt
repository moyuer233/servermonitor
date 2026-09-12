package com.servermonitor.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.servermonitor.BuildConfig
import com.servermonitor.MainViewModel
import com.servermonitor.R
import kotlinx.coroutines.launch

// GitHub 显示昵称（「关于」页展示）；仓库链接使用真实用户名，避免 404
private const val GITHUB_DISPLAY_NAME = "awaZYLawa"
private const val GITHUB_REPO = "https://github.com/qwqZYLqwq/servermonitor"

private fun toast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(vm: MainViewModel) {
    var tab by rememberSaveable { mutableStateOf(0) } // 0=关于 1=设置

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("关于", fontWeight = FontWeight.SemiBold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("关于") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("设置") })
        }
        when (tab) {
            0 -> AboutTab(vm)
            1 -> SettingsTab(vm)
        }
    }
}

@Composable
private fun AboutTab(vm: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var updateDialog by remember { mutableStateOf<Pair<String, String>?>(null) } // (最新版本号, 下载页)

    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .size(112.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.avatar),
                contentDescription = "头像",
                modifier = Modifier.fillMaxWidth().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(GITHUB_DISPLAY_NAME, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "服务器状态监控工具",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))
        Text(
            "当前版本 v${BuildConfig.VERSION_NAME}  ·  构建 ${BuildConfig.GIT_COMMIT}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                if (checking) return@OutlinedButton
                checking = true
                scope.launch {
                    var result: Pair<String, String>? = null
                    var err: String? = null
                    try {
                        result = vm.fetchLatestRelease()
                    } catch (e: Exception) {
                        err = "检查更新失败：${e.message ?: "网络错误"}"
                    }
                    checking = false
                    when {
                        err != null -> toast(context, err)
                        result != null && vm.isNewer(result.first, BuildConfig.VERSION_NAME) ->
                            updateDialog = result
                        else -> toast(context, "已是最新版本")
                    }
                }
            }
        ) {
            if (checking) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(8.dp))
            } else {
                Icon(Icons.Filled.SystemUpdate, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
            }
            Text("检查更新")
        }

        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("开源", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Code, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { openUrl(GITHUB_REPO) }) { Text("GitHub 仓库") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.BugReport, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { openUrl("$GITHUB_REPO/issues") }) { Text("提交 Issue") }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    updateDialog?.let { (tag, url) ->
        AlertDialog(
            onDismissRequest = { updateDialog = null },
            title = { Text("发现新版本") },
            text = { Text("最新版本 v$tag，当前 v${BuildConfig.VERSION_NAME}。\n是否前往 GitHub 下载更新？") },
            confirmButton = {
                TextButton(onClick = {
                    updateDialog = null
                    openUrl(url)
                }) { Text("前往下载") }
            },
            dismissButton = { TextButton(onClick = { updateDialog = null }) { Text("稍后") } }
        )
    }
}

@Composable
private fun SettingsTab(vm: MainViewModel) {
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { vm.exportConfig(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.importConfig(it) } }

    val logLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri -> uri?.let { vm.exportLogs(it) { ok -> toast(context, if (ok) "日志已导出" else "导出失败") } } }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "可将服务器与服务配置导出为文件，或从文件加载。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { exportLauncher.launch("servermonitor_config.json") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Upload, contentDescription = null, Modifier.size(18.dp))
                    Text(" 导出配置到文件")
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, Modifier.size(18.dp))
                    Text(" 从文件加载配置")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(PaddingValues(16.dp)),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("开发者调试", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "导出本应用的运行日志（最近 3000 行），用于排查问题。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.BugReport, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { logLauncher.launch("servermonitor_log.txt") }) { Text("导出运行日志") }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("服务器刷新间隔", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "主页状态的更新间隔可在「服务器」页编辑对应服务器时设置（默认 10 秒，可调 2-300 秒）。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}