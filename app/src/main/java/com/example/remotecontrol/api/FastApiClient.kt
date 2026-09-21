package com.example.remotecontrol.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * FastAPI 接口客户端（同步实现，调用处需切 IO 线程）
 * 接口：POST /start、POST /stop、GET /status
 */
object FastApiClient {

    /** 调用 FastAPI 接口，返回服务端 JSON 字符串 */
    private fun request(baseUrl: String, method: String, path: String, timeoutMs: Int = 8000): String {
        val url = URL(baseUrl.trimEnd('/') + path)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.doInput = true
            if (method == "POST") conn.doOutput = true
            conn.setRequestProperty("Accept", "application/json")

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            return text
        } finally {
            conn.disconnect()
        }
    }

    /** 启动归档任务（POST /start） */
    suspend fun start(baseUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val raw = request(baseUrl, "POST", "/start")
            val json = JSONObject(raw)
            val msg = json.optString("message", "已请求启动")
            Result.success(msg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 暂停归档任务（POST /stop） */
    suspend fun stop(baseUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val raw = request(baseUrl, "POST", "/stop")
            val json = JSONObject(raw)
            val msg = json.optString("message", "已请求暂停")
            Result.success(msg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 查询电脑状态（GET /status） */
    suspend fun status(baseUrl: String): Result<StatusInfo> = withContext(Dispatchers.IO) {
        try {
            val raw = request(baseUrl, "GET", "/status")
            val json = JSONObject(raw)
            Result.success(
                StatusInfo(
                    vnc = json.optBoolean("vnc", false),
                    novnc = json.optBoolean("novnc", false),
                    api = json.optBoolean("api", true),
                    novncUrl = json.optString("novnc_url", ""),
                    apiUrl = json.optString("api_url", ""),
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/** 电脑状态信息 */
data class StatusInfo(
    val vnc: Boolean,
    val novnc: Boolean,
    val api: Boolean,
    val novncUrl: String,
    val apiUrl: String,
) {
    fun describe(): String {
        val parts = mutableListOf<String>()
        parts += if (api) "控制服务:在线" else "控制服务:离线"
        parts += if (vnc) "VNC:在线" else "VNC:离线"
        parts += if (novnc) "投屏:在线" else "投屏:离线"
        return parts.joinToString("  ")
    }
}
