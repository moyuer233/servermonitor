package com.servermonitor.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.servermonitor.model.AppConfig
import java.io.File

class ConfigManager(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private val configFile = File(context.filesDir, "config.json")

    fun load(): AppConfig {
        if (configFile.exists()) {
            runCatching { gson.fromJson(configFile.readText(), AppConfig::class.java) }
                .onSuccess { return it }
        }
        val fromAsset = runCatching {
            context.assets.open("default_config.json").bufferedReader().use { it.readText() }
        }.getOrNull()
        return fromAsset?.let {
            runCatching { gson.fromJson(it, AppConfig::class.java) }.getOrNull()
        } ?: AppConfig()
    }

    fun save(config: AppConfig) {
        runCatching { configFile.writeText(gson.toJson(config)) }
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
        return runCatching { gson.fromJson(text, AppConfig::class.java) }.getOrNull()
    }
}