package com.servermonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.servermonitor.ui.MainScreen
import com.servermonitor.ui.theme.ServerMonitorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ServerMonitorTheme {
                val vm: MainViewModel = viewModel()
                MainScreen(vm)
            }
        }
    }
}