package com.shici.app.ui.widget

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shici.app.App
import com.shici.app.R
import com.shici.app.data.WidgetProgress
import com.shici.app.data.Word
import com.shici.app.databinding.ActivityWidgetPreviewBinding
import kotlinx.coroutines.launch

/**
 * 小组件预览页。
 *
 * 直接复用 @layout/widget_word —— 和桌面小组件是同一份布局，
 * 三个按钮调用的是同一个仓储方法，所以这里看到的交互与真机桌面一致。
 * 部分模拟器（如雷电的定制 Launcher3）没有组件选择器，用它来兜底演示。
 */
class WidgetPreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWidgetPreviewBinding
    private val repo by lazy { (application as App).repo }
    private var current: Word? = null
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWidgetPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnClose.setOnClickListener { finish() }
        binding.widgetCard.btnForgot.setOnClickListener { rate(0) }
        binding.widgetCard.btnVague.setOnClickListener { rate(1) }
        binding.widgetCard.btnKnow.setOnClickListener { rate(2) }
        binding.widgetCard.btnRefresh.setOnClickListener { render() }
        binding.widgetCard.widgetRoot.setOnClickListener { /* 预览页内不跳转 */ }

        render()
    }

    private fun rate(rating: Int) {
        val word = current ?: return
        if (busy) return
        busy = true
        lifecycleScope.launch {
            repo.rate(word, rating)
            busy = false
            fadeCard()
            render()
        }
    }

    private fun fadeCard() {
        binding.widgetCard.root.alpha = 0.2f
        binding.widgetCard.root.animate().alpha(1f).setDuration(220).start()
    }

    private fun render() {
        lifecycleScope.launch {
            val word = repo.widgetWord()
            val progress = repo.widgetProgress()
            current = word
            bind(word, progress)
        }
    }

    private fun bind(word: Word?, progress: WidgetProgress) {
        val w = binding.widgetCard
        if (word == null) {
            w.tvWidgetWord.text = "今日任务完成 🌿"
            w.tvWidgetPhonetic.text = "明天见"
            w.tvWidgetMeaning.text = "打开拾词，看看今天的收获"
            w.tvWidgetExample.text = ""
            w.actions.visibility = View.GONE
            w.tvWidgetProgress.text = "完成"
            w.pbWidget.progress = 100
            binding.tvHint.text = "已经把今天的词都拾完了"
            return
        }

        w.actions.visibility = View.VISIBLE
        w.tvWidgetWord.text = word.spelling
        w.tvWidgetPhonetic.text = word.phonetic
        w.tvWidgetMeaning.text = word.meaning
        w.tvWidgetExample.text = word.example
        w.tvWidgetProgress.text = "${progress.done}/${progress.total}"

        val ratio = if (progress.total <= 0) 1f else {
            (progress.done.toFloat() / progress.total).coerceIn(0f, 1f)
        }
        w.pbWidget.progress = (ratio * 100).toInt()
        w.tvWidgetProgress.setTextColor(
            getColor(
                when {
                    progress.overdue > 0 -> R.color.red
                    progress.done >= progress.total -> R.color.green
                    else -> R.color.amber
                }
            )
        )
        binding.tvHint.text = "真机上：长按桌面 → 小组件 → 拾词"
    }
}
