package com.servermonitor.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.servermonitor.model.AppConfig
import com.servermonitor.model.ServerConfig
import com.servermonitor.model.ServiceConfig
import java.io.File
import java.util.UUID

class ConfigManager(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private val configFile = File(context.filesDir, "config.json")

    fun load(): AppConfig {
        val loaded = if (configFile.exists()) {
            runCatching { gson.fromJson(configFile.readText(), AppConfig::class.java) }.getOrNull()
        } else {
            val fromAsset = runCatching {
                context.assets.open("default_config.json").bufferedReader().use { it.readText() }
            }.getOrNull()
            fromAsset?.let {
                runCatching { gson.fromJson(it, AppConfig::class.java) }.getOrNull()
            }
        } ?: AppConfig()
        return ensureIds(loaded)
    }

    fun save(config: AppConfig) {
        runCatching { configFile.writeText(gson.toJson(config)) }
    }

    /** 老版本配置没有 id 字段（Gson 反序列化为 null），这里补上稳定 UUID 并落盘，保证改名/删除按 id 定位可靠 */
    private fun ensureIds(config: AppConfig): AppConfig {
        var changed = false
        val servers: List<ServerConfig> = config.servers.map { s ->
            if (s.id.isNullOrBlank()) { changed = true; s.copy(id = UUID.randomUUID().toString()) } else s
        }
        val services: List<ServiceConfig> = config.services.map { s ->
            if (s.id.isNullOrBlank()) { changed = true; s.copy(id = UUID.randomUUID().toString()) } else s
        }
        if (changed) {
            val fixed = AppConfig(servers, services)
            save(fixed)
            return fixed
        }
        return config
    }

    fun exportTo(uri: Uri, config: AppConfig): Boolean {
        return runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.use {
                it.write(gson.toJson(config).toByteArray(Charsets.UTF_8))
                it.flush()
            } ?: return false
            true
        }.getOrDefault(false)
    }

    fun importFrom(uri: Uri): AppConfig? {
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull() ?: return null
        return runCatching { ensureIds(gson.fromJson(text, AppConfig::class.java)) }.getOrNull()
    }
}