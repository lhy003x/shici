package com.shici.app.ui.study

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import com.shici.app.App
import com.shici.app.data.Srs
import com.shici.app.data.Word
import com.shici.app.databinding.ActivitySpellBinding
import com.shici.app.widget.WidgetUpdater
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 拼写听写（自选功能）
 *
 * 听发音 → 输入拼写 → 判分。
 * 写对了按「认识」进入下一档记忆盒，写错了按「忘了」回到盒子 1，
 * 所以这个模式不是小游戏，它的结果会真正影响复习计划。
 */
class SpellActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySpellBinding
    private val repo by lazy { (application as App).repo }

    private val queue = mutableListOf<Word>()
    private var index = 0
    private var answered = false
    private var correctCount = 0
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpellBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnClose.setOnClickListener { finish() }
        binding.btnFinishBack.setOnClickListener { finish() }
        binding.btnPlay.setOnClickListener { speak() }
        binding.btnAction.setOnClickListener {
            if (answered) next() else submit()
        }
        binding.etSpell.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && !answered) {
                submit()
                true
            } else {
                false
            }
        }

        initTts()

        lifecycleScope.launch {
            repo.seedIfNeeded()
            queue.clear()
            queue.addAll(repo.dictationQueue(limit = 8))
            if (queue.isEmpty()) {
                showFinish(empty = true)
            } else {
                show(0)
            }
        }
    }

    private fun show(position: Int) {
        index = position
        answered = false
        val word = queue[position]

        binding.pbProgress.progress = (position * 100) / queue.size
        binding.tvCounter.text = "${position + 1}/${queue.size}"
        binding.tvMeaning.text = word.meaning
        binding.tvFeedback.visibility = View.GONE
        binding.etSpell.setText("")
        binding.etSpell.isEnabled = true
        binding.btnAction.text = "提交"

        binding.etSpell.post {
            binding.etSpell.requestFocus()
            getSystemService<InputMethodManager>()
                ?.showSoftInput(binding.etSpell, InputMethodManager.SHOW_IMPLICIT)
        }
        if (repo.settings().autoSpeak) speak()
    }

    private fun submit() {
        if (answered) return
        val typed = binding.etSpell.text?.toString()?.trim().orEmpty()
        if (typed.isEmpty()) {
            binding.tvFeedback.visibility = View.VISIBLE
            binding.tvFeedback.setTextColor(getColor(com.shici.app.R.color.amber))
            binding.tvFeedback.text = "先写点什么再提交吧"
            return
        }

        val word = queue[index]
        val correct = typed.equals(word.spelling, ignoreCase = true)
        answered = true
        if (correct) correctCount++

        binding.etSpell.isEnabled = false
        getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(binding.etSpell.windowToken, 0)

        binding.tvFeedback.visibility = View.VISIBLE
        if (correct) {
            binding.tvFeedback.setTextColor(getColor(com.shici.app.R.color.green))
            binding.tvFeedback.text = "✓ 写对了，记忆等级 +1"
        } else {
            binding.tvFeedback.setTextColor(getColor(com.shici.app.R.color.red))
            binding.tvFeedback.text = "✗ 正确拼写：${word.spelling}\n已放回复习队列，明天再见"
        }
        binding.btnAction.text = if (index + 1 >= queue.size) "完成" else "下一个"

        lifecycleScope.launch {
            repo.rate(word, if (correct) Srs.RATING_KNOW else Srs.RATING_FORGOT)
        }
    }

    private fun next() {
        if (index + 1 >= queue.size) {
            showFinish(empty = false)
        } else {
            show(index + 1)
        }
    }

    private fun showFinish(empty: Boolean) {
        binding.finishLayout.visibility = View.VISIBLE
        binding.btnAction.visibility = View.GONE
        if (empty) {
            binding.tvFinishTitle.text = "还没有可以听写的词"
            binding.tvFinishInfo.text = "先去学几个新词吧"
        } else {
            val total = queue.size
            val percent = correctCount * 100 / maxOf(total, 1)
            binding.tvFinishTitle.text = "听写完成 · $correctCount/$total"
            binding.tvFinishInfo.text = "正确率 $percent%\n写错的词已经回到第一批，明天会再出现"
        }
        WidgetUpdater.refresh(this)
    }

    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) tts?.language = Locale.US
            if (ttsReady) speak()
        }
    }

    private fun speak() {
        val word = queue.getOrNull(index) ?: return
        if (!ttsReady) return
        tts?.speak(word.spelling, TextToSpeech.QUEUE_FLUSH, null, word.spelling)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}
