package com.shici.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.shici.app.R
import com.shici.app.data.Word
import com.shici.app.data.WidgetProgress
import com.shici.app.data.WordRepository
import com.shici.app.ui.study.StudyActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 小组件的渲染中心。
 *
 * 设计要点：小组件不只是「展示」，三个按钮通过 PendingIntent 直接把评分写回 Room，
 * 整个过程不启动 Activity，所以点完立刻换下一个词。
 */
object WidgetUpdater {

    private val providers = listOf(
        WordWidgetProvider::class.java to R.layout.widget_word,
        MiniWidgetProvider::class.java to R.layout.widget_mini,
    )

    /** 从任意线程触发一次整体刷新 */
    fun refresh(context: Context) {
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { renderNow(app) }
        }
    }

    /** 读取数据并刷新所有已添加到桌面的实例 */
    suspend fun renderNow(context: Context) {
        val repo = WordRepository(context)
        val word = repo.widgetWord()
        val progress = repo.widgetProgress()
        val manager = AppWidgetManager.getInstance(context)

        providers.forEach { (cls, layout) ->
            val ids = manager.getAppWidgetIds(ComponentName(context, cls))
            if (ids == null || ids.isEmpty()) return@forEach
            val views = build(context, layout, word, progress)
            manager.updateAppWidget(ids, views)
        }
    }

    private fun build(
        context: Context,
        layout: Int,
        word: Word?,
        progress: WidgetProgress,
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, layout)
        val ratio = if (progress.total <= 0) 1f else {
            (progress.done.toFloat() / progress.total).coerceIn(0f, 1f)
        }
        val percent = (ratio * 100).toInt()

        rv.setOnClickPendingIntent(R.id.widgetRoot, openApp(context))

        if (word == null) {
            rv.setTextViewText(R.id.tvWidgetWord, "今日任务完成 🌿")
            rv.setTextViewText(R.id.tvWidgetPhonetic, "明天见")
            rv.setTextViewText(R.id.tvWidgetMeaning, "打开拾词，看看今天的收获")
            rv.setViewVisibility(R.id.actions, View.GONE)
            rv.setTextViewText(R.id.tvWidgetProgress, "完成")
        } else {
            rv.setTextViewText(R.id.tvWidgetWord, word.spelling)
            rv.setTextViewText(R.id.tvWidgetPhonetic, word.phonetic)
            rv.setTextViewText(R.id.tvWidgetMeaning, word.meaning)
            rv.setTextViewText(R.id.tvWidgetProgress, "${progress.done}/${progress.total}")
            rv.setViewVisibility(R.id.actions, View.VISIBLE)

            if (layout == R.layout.widget_word) {
                rv.setTextViewText(
                    R.id.tvWidgetExample,
                    word.example.ifBlank { "" },
                )
            }

            // 有欠账就变红；没欠账时按完成度 琥珀 → 绿
            val color = when {
                progress.overdue > 0 -> context.getColor(R.color.red)
                progress.done >= progress.total -> context.getColor(R.color.green)
                else -> context.getColor(R.color.amber)
            }
            runCatching { rv.setInt(R.id.tvWidgetProgress, "setTextColor", color) }

            if (layout == R.layout.widget_word) {
                rv.setOnClickPendingIntent(R.id.btnForgot, rate(context, word, 0))
                rv.setOnClickPendingIntent(R.id.btnVague, rate(context, word, 1))
                rv.setOnClickPendingIntent(R.id.btnKnow, rate(context, word, 2))
            } else {
                rv.setOnClickPendingIntent(R.id.btnForgot, rate(context, word, 0))
                rv.setOnClickPendingIntent(R.id.btnKnow, rate(context, word, 2))
            }
            rv.setOnClickPendingIntent(R.id.btnRefresh, refreshIntent(context))
        }

        if (layout == R.layout.widget_word) {
            rv.setProgressBar(R.id.pbWidget, 100, percent, false)
        }
        return rv
    }

    private fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, StudyActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun rate(context: Context, word: Word, rating: Int): PendingIntent {
        val intent = Intent(context, WordWidgetProvider::class.java).apply {
            action = Widgets.ACTION_RATE
            putExtra(Widgets.EXTRA_RATING, rating)
            putExtra(Widgets.EXTRA_WORD_ID, word.id)
        }
        return PendingIntent.getBroadcast(
            context,
            200 + rating,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun refreshIntent(context: Context): PendingIntent {
        val intent = Intent(context, WordWidgetProvider::class.java).apply {
            action = Widgets.ACTION_REFRESH
        }
        return PendingIntent.getBroadcast(
            context,
            300,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** 每天 00:05 自动换一批词 */
    fun scheduleDailyRefresh(context: Context) {
        val now = java.time.ZonedDateTime.now()
        var next = now.toLocalDate().plusDays(1).atTime(0, 5).atZone(now.zone)
        if (next.isBefore(now)) next = next.plusDays(1)
        val delay = next.toInstant().toEpochMilli() - System.currentTimeMillis()

        val request = androidx.work.PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
            24,
            java.util.concurrent.TimeUnit.HOURS,
        )
            .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "shici-widget-daily",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
