package com.example.remotecontrol

import android.content.Context
import android.content.SharedPreferences

/**
 * 设置持久化：保存 FastAPI 地址、noVNC 地址、画质选项
 */
object Prefs {
    private const val NAME = "remote_control_prefs"
    const val KEY_FASTAPI = "fastapi_url"
    const val KEY_NOVNC = "novnc_url"
    const val KEY_QUALITY = "quality" // "low" / "standard"

    private fun sp(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getFastApi(ctx: Context): String =
        sp(ctx).getString(KEY_FASTAPI, "http://192.168.1.100:8000") ?: ""

    fun getNoVnc(ctx: Context): String =
        sp(ctx).getString(KEY_NOVNC, "http://192.168.1.100:6080/vnc.html") ?: ""

    fun getQuality(ctx: Context): String =
        sp(ctx).getString(KEY_QUALITY, "standard") ?: "standard"

    fun save(ctx: Context, fastapi: String, novnc: String, quality: String) {
        sp(ctx).edit()
            .putString(KEY_FASTAPI, fastapi.trim())
            .putString(KEY_NOVNC, novnc.trim())
            .putString(KEY_QUALITY, quality)
            .apply()
    }
}
