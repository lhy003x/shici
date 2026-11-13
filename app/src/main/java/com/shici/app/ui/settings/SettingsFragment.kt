package com.shici.app.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shici.app.App
import com.shici.app.R
import com.shici.app.databinding.FragmentSettingsBinding
import com.shici.app.widget.WidgetUpdater
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var binding: FragmentSettingsBinding? = null
    private val repo get() = (requireActivity().application as App).repo

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentSettingsBinding.bind(view)
        binding = b
        val settings = repo.settings()

        b.sbDailyNew.progress = settings.dailyNew - 5
        b.tvDailyValue.text = "${settings.dailyNew} 词/天"
        b.sbDailyNew.setOnSeekBarChangeListener(object :
            android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, value: Int, fromUser: Boolean) {
                val count = value + 5
                b.tvDailyValue.text = "$count 词/天"
                if (fromUser) settings.dailyNew = count
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
        })

        b.swDemo.isChecked = settings.demoMode
        b.swDemo.setOnCheckedChangeListener { _, checked -> settings.demoMode = checked }

        b.swAutoSpeak.isChecked = settings.autoSpeak
        b.swAutoSpeak.setOnCheckedChangeListener { _, checked -> settings.autoSpeak = checked }

        b.btnReset.setOnClickListener { confirmReset() }
    }

    private fun confirmReset() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("清空学习进度？")
            .setMessage("所有单词会回到「未学」，统计与打卡记录一并清除，词库本身保留。")
            .setNegativeButton("取消", null)
            .setPositiveButton("清空") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repo.resetProgress()
                    WidgetUpdater.refresh(requireContext())
                    Toast.makeText(requireContext(), "已重置", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
