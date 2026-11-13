package com.shici.app.ui.study

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shici.app.App
import com.shici.app.data.Srs
import com.shici.app.data.Word
import com.shici.app.databinding.ActivityStudyBinding
import com.shici.app.widget.WidgetUpdater
import kotlinx.coroutines.launch
import java.util.Locale

class StudyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudyBinding
    private val repo by lazy { (application as App).repo }

    private val queue = mutableListOf<Word>()
    private var index = 0
    private var flipped = false
    private var busy = false
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.card.cameraDistance = 8000f * resources.displayMetrics.density

        binding.btnClose.setOnClickListener { finish() }
        binding.card.setOnClickListener { toggleFlip() }
        binding.btnSpeak.setOnClickListener { speak() }
        binding.btnForgot.setOnClickListener { rate(Srs.RATING_FORGOT) }
        binding.btnVague.setOnClickListener { rate(Srs.RATING_VAGUE) }
        binding.btnKnow.setOnClickListener { rate(Srs.RATING_KNOW) }
        binding.btnFinishBack.setOnClickListener { finish() }

        initTts()

        lifecycleScope.launch {
            repo.seedIfNeeded()
            queue.clear()
            queue.addAll(repo.studyQueue())
            if (queue.isEmpty()) {
                showEmpty()
            } else {
                show(0)
            }
        }
    }

    /* ---------------- 卡片展示 ---------------- */

    private fun show(position: Int) {
        val word = queue.getOrNull(position) ?: return
        index = position
        flipped = false
        binding.front.visibility = View.VISIBLE
        binding.back.visibility = View.GONE
        binding.card.rotationY = 0f

        binding.tvWord.text = word.spelling
        binding.tvPhonetic.text = word.phonetic.ifBlank { " " }
        binding.tvMeaning.text = word.meaning
        binding.tvExample.text = word.example.ifBlank { "—" }
        binding.tvExampleCn.text = word.exampleCn
        binding.tvBox.text = if (word.box == 0) {
            "新词 · 第一次见面"
        } else {
            "记忆等级 L${word.box} · 下次复习 ${Srs.humanInterval(word.box, repo.settings().demoMode)}"
        }

        binding.tvCounter.text = "${position + 1}/${queue.size}"
        binding.pbProgress.progress = (position * 100) / queue.size.coerceAtLeast(1)

        if (repo.settings().autoSpeak) speak()
    }

    private fun toggleFlip() {
        val showBack = !flipped
        flipped = showBack
        val half = ObjectAnimator.ofFloat(binding.card, "rotationY", 0f, 90f).setDuration(140)
        half.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                binding.front.visibility = if (showBack) View.GONE else View.VISIBLE
                binding.back.visibility = if (showBack) View.VISIBLE else View.GONE
                ObjectAnimator.ofFloat(binding.card, "rotationY", -90f, 0f)
                    .setDuration(140)
                    .start()
            }
        })
        half.start()
    }

    /* ---------------- 评分 ---------------- */

    private fun rate(rating: Int) {
        if (busy) return
        val word = queue.getOrNull(index) ?: return
        busy = true

        lifecycleScope.launch {
            repo.rate(word, rating)
            busy = false
            if (index + 1 >= queue.size) {
                showDone()
            } else {
                show(index + 1)
            }
        }
    }

    private fun showDone() {
        binding.pbProgress.progress = 100
        binding.finishLayout.visibility = View.VISIBLE
        binding.tvFinishTitle.text = "今天的词都拾完了"
        binding.tvFinishInfo.text = "本轮共复习 ${queue.size} 个词\n桌面小组件已同步到最新进度"
        WidgetUpdater.refresh(this)
    }

    private fun showEmpty() {
        binding.finishLayout.visibility = View.VISIBLE
        binding.tvFinishTitle.text = "暂时没有要学的词"
        binding.tvFinishInfo.text = "今日任务已完成。可以去词库里翻翻生词，\n或者把桌面小组件加上，随手复习。"
        WidgetUpdater.refresh(this)
    }

    /* ---------------- 发音 ---------------- */

    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.language = Locale.US
            }
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
