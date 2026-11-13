package com.shici.app.ui.home

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.shici.app.App
import com.shici.app.R
import com.shici.app.databinding.FragmentHomeBinding
import com.shici.app.ui.study.StudyActivity
import com.shici.app.ui.study.SpellActivity
import com.shici.app.widget.WidgetUpdater
import com.shici.app.widget.WordWidgetProvider
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var binding: FragmentHomeBinding? = null
    private val repo get() = (requireActivity().application as App).repo

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentHomeBinding.bind(view)
        binding = b

        b.tvDate.text = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA))
        b.tvGreeting.text = greeting()

        b.btnStudy.setOnClickListener {
            startActivity(Intent(requireContext(), StudyActivity::class.java))
        }
        b.btnSpell.setOnClickListener {
            startActivity(Intent(requireContext(), SpellActivity::class.java))
        }
        b.btnAddWidget.setOnClickListener { pinWidget() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val b = binding ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            repo.seedIfNeeded()
            val s = repo.todayState()
            val hard = runCatching { repo.hardWords().size }.getOrDefault(0)

            b.tvTodayCount.text = "${s.remaining} 词"
            b.tvNewDue.text = "新词 ${s.newLeft} · 复习 ${s.dueLeft}"
            b.tvStreak.text = s.streak.toString()
            b.tvMastered.text = s.mastered.toString()
            b.tvHard.text = hard.toString()

            b.ring.progress = s.progress
            b.ring.setTexts("${s.doneToday}/${s.target}", "今日进度")

            b.btnStudy.text = when {
                s.remaining == 0 -> "今日任务已完成 · 再复习一轮"
                s.doneToday > 0 -> "继续学习（还剩 ${s.remaining} 词）"
                else -> "开始今日学习"
            }
            val pinSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                AppWidgetManager.getInstance(requireContext()).isRequestPinAppWidgetSupported
            b.btnAddWidget.text = if (pinSupported) "添加" else "预览"
            b.tvTip.text = if (s.totalWords > 0) {
                "词库共 ${s.totalWords} 词 · 按艾宾浩斯间隔自动安排复习"
            } else {
                "正在导入内置词库…"
            }

            // 首页每次回到前台，同步一次桌面小组件
            WidgetUpdater.refresh(requireContext())
        }
    }

    private fun greeting(): String {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 5..10 -> "早上好，先拾三个词再开始一天"
            in 11..13 -> "中午好，趁休息记几个词"
            in 14..18 -> "下午好，今天的词还记得吗"
            in 19..22 -> "晚上好，睡前复习最牢固"
            else -> "夜深了，记完这几个就去睡吧"
        }
    }

    private fun pinWidget() {
        val context = context ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = AppWidgetManager.getInstance(context)
            if (manager.isRequestPinAppWidgetSupported) {
                manager.requestPinAppWidget(
                    ComponentName(context, WordWidgetProvider::class.java),
                    null,
                    null,
                )
                return
            }
        }
        // 部分模拟器 / 定制桌面没有组件选择器，用同一份布局做预览
        startActivity(
            Intent(context, com.shici.app.ui.widget.WidgetPreviewActivity::class.java)
        )
    }
}
