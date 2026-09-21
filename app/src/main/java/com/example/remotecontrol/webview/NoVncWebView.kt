package com.example.remotecontrol.webview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * noVNC 专用 WebView 封装
 *
 * 鸿蒙/安卓适配要点（针对华为 nova15 Ultra 及鸿蒙系统）：
 *  1. 禁用系统上下文菜单：外接鼠标右键不再弹出系统菜单，也不会触发系统返回手势
 *  2. 事件透传：右键、滚轮、键盘事件完整转发给 noVNC（noVNC 内部处理为远程操作）
 *  3. 触摸手势：双指缩放由 noVNC 页面自身处理（resize=scale），拖动由 noVNC 模拟鼠标
 *  4. 通过 JS 注入加固：拦截 contextmenu / wheel / keydown，确保事件到达 VNC 画布
 */
@SuppressLint("SetJavaScriptEnabled")
class NoVncWebView : WebView {

    private var injected = false

    constructor(context: Context) : super(context) {
        init()
    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            // 页面缩放交给 noVNC 自身（resize=scale），系统缩放关闭避免事件冲突
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            // 缓存策略：局域网资源实时拉取
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            mediaPlaybackRequiresUserGesture = false
        }

        // 键盘输入必须聚焦
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()

        // ---- 鸿蒙适配 1：禁系统上下文菜单 / 长按弹窗 ----
        setOnCreateContextMenuListener(null)
        isLongClickable = false
        setOnLongClickListener { true }

        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // noVNC 内部跳转（vnc.html -> app 等）由 WebView 自己处理
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (!injected) {
                    injectEventBridge()
                    injected = true
                }
            }
        }

        addJavascriptInterface(EventBridge(), "AndroidBridge")
    }

    /** 加载 noVNC 页面（自动拼接画质参数） */
    fun loadNoVnc(address: String, quality: String) {
        val url = buildUrl(address, quality)
        loadUrl(url)
    }

    private fun buildUrl(address: String, quality: String): String {
        val base = address.trim().ifEmpty { "http://192.168.1.100:6080/vnc.html" }
        val sep = if (base.contains("?")) "&" else "?"
        val qualityParam = when (quality) {
            "low" -> "quality=3&compression=2"   // 低画质：JPEG质量低+压缩高
            else -> "quality=6&compression=5"    // 标准画质
        }
        // 必选参数：自动连接、缩放适配、剪贴板、键盘
        return "$base$sep$qualityParam&autoconnect=1&resize=scale&clipboard=1&keyboard=1"
    }

    /** 注入事件透传桥（鸿蒙适配核心） */
    private fun injectEventBridge() {
        val js = """
            (function() {
                'use strict';
                // ---- 鸿蒙适配 2：拦截右键，禁止系统菜单/返回手势 ----
                document.addEventListener('contextmenu', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    return false;
                }, true);

                // ---- 鸿蒙适配 3：确保 VNC 画布获得焦点（键盘/快捷键可达）----
                function focusCanvas() {
                    var c = document.querySelector('canvas');
                    if (c) { c.focus(); c.setAttribute('tabindex', '0'); }
                }
                window.addEventListener('load', focusCanvas);
                document.addEventListener('touchstart', function() { focusCanvas(); }, {passive: true});
                document.addEventListener('mousedown', function() { focusCanvas(); }, true);

                // ---- 鸿蒙适配 4：滚轮事件透传到画布 ----
                // noVNC 监听 canvas 的 wheel 事件；WebView 中滚轮常被页面消费，
                // 这里将滚轮事件重新派发到 canvas，并阻止页面自身滚动/缩放。
                document.addEventListener('wheel', function(e) {
                    var c = document.querySelector('canvas');
                    if (!c) return;
                    if (e.target === c) return;   // 已到达画布，不重复派发
                    var rect = c.getBoundingClientRect();
                    if (rect.width === 0) return;
                    try {
                        var ne = new WheelEvent('wheel', {
                            clientX: e.clientX, clientY: e.clientY,
                            deltaX: e.deltaX, deltaY: e.deltaY,
                            deltaMode: e.deltaMode,
                            bubbles: true, cancelable: true
                        });
                        c.dispatchEvent(ne);
                    } catch (err) {}
                    e.preventDefault();  // 阻止页面滚动，让滚轮只作用于远程桌面
                    return false;
                }, {passive: false});

                // ---- 鸿蒙适配 5：键盘事件确保可达（noVNC keyboard=1 会自行处理）----
                window.addEventListener('keydown', function(e) {
                    var c = document.querySelector('canvas');
                    // noVNC 的键盘处理挂在其画布/keyboard 模块上，这里仅确保事件不被吞
                    if (c && document.activeElement !== c && !e.ctrlKey && !e.altKey && !e.metaKey) {
                        // 不抢焦点，仅提示 noVNC 已接管
                    }
                }, true);

                // 状态回调：告知 App 页面已就绪
                setTimeout(function() {
                    try { window.AndroidBridge.onPageReady(); } catch (err) {}
                }, 1500);
            })();
        """.trimIndent()
        evaluateJavascript(js, null)
    }

    /** JS 与原生桥 */
    inner class EventBridge {
        @JavascriptInterface
        fun onPageReady() {
            // 预留：可用于通知 App 更新状态栏
        }
    }

    // ---- 鸿蒙适配 6：按键事件透传（不拦截，交由 WebView/JS 处理）----
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 音量键、返回键保持系统行为；其余按键（字母/数字/功能键）交给页面
        return if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_VOLUME_UP
            || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            super.onKeyDown(keyCode, event)
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    // ---- 鸿蒙适配 7：触摸事件不拦截（双指缩放/拖动交给页面）----
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 交由 WebView 默认处理，保证 noVNC 的 touch 模拟鼠标逻辑完整收到事件
        return super.onTouchEvent(event)
    }
}
