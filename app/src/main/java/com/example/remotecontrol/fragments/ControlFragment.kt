package com.example.remotecontrol.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.remotecontrol.Prefs
import com.example.remotecontrol.R
import com.example.remotecontrol.api.FastApiClient
import com.example.remotecontrol.webview.NoVncWebView
import kotlinx.coroutines.launch

/**
 * Tab3：远程控制面板（核心页）
 *  - 顶部 3 按钮：启动归档任务 / 暂停归档任务 / 刷新电脑状态（调用 FastAPI）
 *  - 下方 WebView 嵌入 noVNC 页面（鸿蒙适配见 NoVncWebView）
 */
class ControlFragment : Fragment() {

    private lateinit var btnStart: Button
    private lateinit var btnPause: Button
    private lateinit var btnRefresh: Button
    private lateinit var tvStatus: TextView
    private lateinit var webView: NoVncWebView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_control, container, false)
        btnStart = root.findViewById(R.id.btn_start)
        btnPause = root.findViewById(R.id.btn_pause)
        btnRefresh = root.findViewById(R.id.btn_refresh)
        tvStatus = root.findViewById(R.id.tv_status)
        webView = root.findViewById(R.id.webview_novnc) as NoVncWebView

        btnStart.setOnClickListener { callApi("start") }
        btnPause.setOnClickListener { callApi("stop") }
        btnRefresh.setOnClickListener { refreshStatus() }
        return root
    }

    override fun onResume() {
        super.onResume()
        // 每次回到本页重新加载 noVNC（地址/画质可能已改）
        loadNoVnc()
        refreshStatus()
    }

    private fun loadNoVnc() {
        val ctx = requireContext()
        val addr = Prefs.getNoVnc(ctx)
        val quality = Prefs.getQuality(ctx)
        tvStatus.text = getString(R.string.status_connecting)
        webView.loadNoVnc(addr, quality)
    }

    /** 调用 FastAPI 接口 */
    private fun callApi(action: String) {
        val ctx = requireContext()
        val base = Prefs.getFastApi(ctx)
        if (base.isBlank()) {
            Toast.makeText(ctx, "请先在设置页填写 FastAPI 地址", Toast.LENGTH_SHORT).show()
            return
        }
        tvStatus.text = if (action == "start") "正在请求启动…" else "正在请求暂停…"
        lifecycleScope.launch {
            val result = if (action == "start") {
                FastApiClient.start(base)
            } else {
                FastApiClient.stop(base)
            }
            result.onSuccess { msg ->
                tvStatus.text = msg
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                tvStatus.text = getString(R.string.status_failed)
                Toast.makeText(ctx, "请求失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** 刷新电脑状态 */
    private fun refreshStatus() {
        val ctx = requireContext()
        val base = Prefs.getFastApi(ctx)
        if (base.isBlank()) {
            tvStatus.text = getString(R.string.status_failed)
            return
        }
        lifecycleScope.launch {
            FastApiClient.status(base)
                .onSuccess { info ->
                    tvStatus.text = info.describe()
                    // 若 noVNC 服务未就绪，提示刷新投屏
                    if (!info.novnc) {
                        Toast.makeText(ctx, "电脑 noVNC 未就绪，请确认 start_remote.bat 已运行", Toast.LENGTH_LONG).show()
                    }
                }
                .onFailure { e ->
                    tvStatus.text = getString(R.string.status_failed)
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        webView.destroy()
    }
}
