package com.example.remotecontrol.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.remotecontrol.R

/**
 * Tab1：本地归档（预留空白占位页）
 * Tab2：归档审核（预留空白占位页）
 * 共用此布局，后续在对应业务迭代时填充内容。
 */
class PlaceholderFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_placeholder, container, false)
    }
}
