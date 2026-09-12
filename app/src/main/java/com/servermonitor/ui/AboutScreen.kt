package com.servermonitor.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.servermonitor.MainViewModel
import com.servermonitor.R

private const val GITHUB_USER = "qwqZYLqwq"
private const val GITHUB_REPO = "https://github.com/$GITHUB_USER/servermonitor"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(vm: MainViewModel) {
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { vm.exportConfig(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.importConfig(it) } }

    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("关于", fontWeight = FontWeight.SemiBold) })

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
            Text(GITHUB_USER, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "服务器状态监控工具",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(28.dp))

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

            Spacer(Modifier.height(16.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(PaddingValues(16.dp)), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
    }
}