package com.example.remotecontrol.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.remotecontrol.Prefs
import com.example.remotecontrol.R

/**
 * Tab4：设置页
 *  - FastAPI 地址（局域网内网 IP，例 http://192.168.x.x:8000）
 *  - noVNC 地址（例 http://192.168.x.x:6080/vnc.html）
 *  - 画质选项：低画质 / 标准画质
 *  - 保存按钮
 */
class SettingsFragment : Fragment() {

    private lateinit var etFastApi: EditText
    private lateinit var etNoVnc: EditText
    private lateinit var rgQuality: RadioGroup
    private lateinit var btnSave: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings, container, false)
        etFastApi = root.findViewById(R.id.et_fastapi)
        etNoVnc = root.findViewById(R.id.et_novnc)
        rgQuality = root.findViewById(R.id.rg_quality)
        btnSave = root.findViewById(R.id.btn_save)

        // 回填当前设置
        val ctx = requireContext()
        etFastApi.setText(Prefs.getFastApi(ctx))
        etNoVnc.setText(Prefs.getNoVnc(ctx))
        rgQuality.check(
            if (Prefs.getQuality(ctx) == "low") R.id.rb_quality_low
            else R.id.rb_quality_standard
        )

        btnSave.setOnClickListener {
            val quality = if (rgQuality.checkedRadioButtonId == R.id.rb_quality_low) "low" else "standard"
            Prefs.save(ctx, etFastApi.text.toString(), etNoVnc.text.toString(), quality)
            Toast.makeText(ctx, R.string.saved_toast, Toast.LENGTH_SHORT).show()
        }
        return root
    }
}
