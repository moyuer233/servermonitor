package com.servermonitor.model

import java.util.UUID

data class ServerConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val host: String = "",
    val port: Int = 22,
    val username: String = "root",
    val privateKey: String = "",
    val useIpv6: Boolean = true,
    val showOnHome: Boolean = true
)

data class ServiceConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val port: Int = 0,
    val startCmd: String = "",
    val stopCmd: String = "",
    val restartCmd: String = ""
)

data class AppConfig(
    val servers: List<ServerConfig> = emptyList(),
    val services: List<ServiceConfig> = emptyList()
)

data class SystemStatus(
    val cpuUsage: Double = 0.0,
    val cpuCores: Int = 0,
    val memoryTotalMb: Long = 0,
    val memoryUsedMb: Long = 0,
    val gpuName: String = "",
    val cpuTempC: Double = 0.0,
    val batteryTempC: Double = 0.0,
    val batteryPercent: Int = 0,
    val charging: Boolean = false,
    val load1: Double = 0.0
)