package com.servermonitor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.servermonitor.MainViewModel

@Composable
fun MainScreen(vm: MainViewModel) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val saveableStateHolder = rememberSaveableStateHolder()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(vm.errorMessage) {
        vm.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.dismissError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "主页") },
                    label = { Text("主页") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Dns, contentDescription = "服务器") },
                    label = { Text("服务器") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Info, contentDescription = "关于") },
                    label = { Text("关于") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            saveableStateHolder.SaveableStateProvider(selectedTab) {
                when (selectedTab) {
                    0 -> HomeScreen(vm)
                    1 -> ServerScreen(vm)
                    2 -> AboutScreen(vm)
                }
            }
        }
    }
}