package com.example.remotecontrol

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.remotecontrol.fragments.ControlFragment
import com.example.remotecontrol.fragments.PlaceholderFragment
import com.example.remotecontrol.fragments.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * 主界面：4 个底部 Tab 导航
 *  Tab1 本地归档（占位） / Tab2 归档审核（占位）
 *  Tab3 远程控制面板（核心） / Tab4 设置
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    private val fragments = mapOf(
        R.id.nav_archive to PlaceholderFragment(),
        R.id.nav_audit to PlaceholderFragment(),
        R.id.nav_control to ControlFragment(),
        R.id.nav_settings to SettingsFragment(),
    )

    private var currentTag: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            switchFragment(item.itemId)
            true
        }
        // 默认选中远程控制面板
        bottomNav.selectedItemId = R.id.nav_control
    }

    private fun switchFragment(id: Int) {
        val tag = id.toString()
        if (tag == currentTag) return
        val target = fragments[id] ?: return

        supportFragmentManager.beginTransaction().apply {
            // 隐藏当前
            currentTag?.let {
                supportFragmentManager.findFragmentByTag(it)?.let { f ->
                    hide(f)
                }
            }
            // 显示目标（首次 add）
            val existing = supportFragmentManager.findFragmentByTag(tag)
            if (existing == null) {
                add(R.id.fragment_container, target, tag)
            } else {
                show(existing)
            }
        }.commit()
        currentTag = tag
    }

    override fun onDestroy() {
        super.onDestroy()
        // 避免 WebView 内存泄漏
        fragments.values.forEach { f ->
            if (f is ControlFragment) {
                // 由 fragment 自身 onDestroyView 处理
            }
        }
    }
}
