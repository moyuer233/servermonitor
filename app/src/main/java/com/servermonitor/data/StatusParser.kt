package com.servermonitor.data

import com.servermonitor.model.SystemStatus

object StatusParser {

    const val STATUS_COMMAND = """
echo '____CORES____'; nproc
echo '____CPU1____'; head -1 /proc/stat
sleep 1
echo '____CPU2____'; head -1 /proc/stat
echo '____MEM____'; grep -E '^MemTotal|^MemAvailable' /proc/meminfo
echo '____GPU____'; grep 'DRIVER' /sys/class/drm/card0/device/uevent 2>/dev/null
echo '____BTEMP____'; cat /sys/class/power_supply/battery/temp 2>/dev/null
echo '____CTEMP____'; cat /sys/class/thermal/thermal_zone3/temp 2>/dev/null
echo '____BATT____'; cat /sys/class/power_supply/battery/capacity 2>/dev/null
echo '____STATUS____'; cat /sys/class/power_supply/battery/status 2>/dev/null
echo '____PORTS____'; ss -tln 2>/dev/null | grep LISTEN
"""

    fun parseStatus(raw: String): SystemStatus {
        var cpuUsage = 0.0
        var cores = 0
        var memTotal = 0L
        var memAvail = 0L
        var gpu = ""
        var cpuTemp = 0.0
        var battTemp = 0.0
        var battPct = 0
        var charging = false
        var load1 = 0.0

        cores = section(raw, "____CORES____").toIntOrNull() ?: 0

        val cpu1 = parseCpuLine(section(raw, "____CPU1____"))
        val cpu2 = parseCpuLine(section(raw, "____CPU2____"))
        if (cpu1.size >= 4 && cpu2.size >= 4) {
            val idle1 = cpu1[3]
            val idle2 = cpu2[3]
            val total1 = cpu1.sum()
            val total2 = cpu2.sum()
            if (total2 > total1) {
                cpuUsage = (1.0 - (idle2 - idle1).toDouble() / (total2 - total1).toDouble()) * 100.0
            }
        }

        val memText = section(raw, "____MEM____")
        memText.lines().forEach { line ->
            when {
                line.startsWith("MemTotal") -> memTotal = line.extractLong()
                line.startsWith("MemAvailable") -> memAvail = line.extractLong()
            }
        }

        gpu = mapGpu(section(raw, "____GPU____"))

        cpuTemp = (section(raw, "____CTEMP____").toLongOrNull() ?: 0L) / 1000.0
        battTemp = (section(raw, "____BTEMP____").toLongOrNull() ?: 0L) / 10.0
        battPct = section(raw, "____BATT____").toIntOrNull() ?: 0
        val st = section(raw, "____STATUS____").lowercase()
        charging = st == "charging" || st == "full"

        return SystemStatus(
            cpuUsage = cpuUsage,
            cpuCores = cores,
            memoryTotalMb = memTotal / 1024,
            memoryUsedMb = (memTotal - memAvail).coerceAtLeast(0) / 1024,
            gpuName = gpu,
            cpuTempC = cpuTemp,
            batteryTempC = battTemp,
            batteryPercent = battPct,
            charging = charging,
            load1 = load1
        )
    }

    fun parseListeningPorts(raw: String): Set<Int> {
        val ports = mutableSetOf<Int>()
        val section = raw.substringAfter("____PORTS____", "")
        if (section.isBlank()) return ports
        section.lines().forEach { line ->
            if (!line.contains("LISTEN")) return@forEach
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size >= 4) {
                parts[3].substringAfterLast(':').toIntOrNull()?.let { ports.add(it) }
            }
        }
        return ports
    }

    private fun section(raw: String, marker: String): String {
        val idx = raw.indexOf(marker)
        if (idx < 0) return ""
        val start = idx + marker.length
        val end = raw.indexOf("____", start)
        return raw.substring(start, if (end > start) end else raw.length).trim()
    }

    private fun parseCpuLine(line: String): List<Long> {
        return line.substringAfter("cpu").trim()
            .split(Regex("\\s+"))
            .mapNotNull { it.toLongOrNull() }
    }

    private fun String.extractLong(): Long {
        return substringAfter(':').trim().replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
    }

    private fun mapGpu(driverLine: String): String {
        val driver = driverLine.substringAfter("DRIVER=").trim().lowercase()
        return when {
            driver.startsWith("pvrsrv") -> "PowerVR"
            driver.startsWith("mali") -> "Mali"
            driver.startsWith("adreno") || driver.startsWith("kgsl") -> "Adreno"
            driver.isNotBlank() -> driver
            else -> "无"
        }
    }
}