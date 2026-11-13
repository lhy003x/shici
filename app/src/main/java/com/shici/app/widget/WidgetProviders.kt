package com.shici.app.widget

import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shici.app.data.WordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        WidgetUpdater.refresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Widgets.ACTION_RATE -> {
                val wordId = intent.getLongExtra(Widgets.EXTRA_WORD_ID, -1L)
                val rating = intent.getIntExtra(Widgets.EXTRA_RATING, 2)
                if (wordId <= 0) return
                val pending = goAsync()
                val appContext = context.applicationContext
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = WordRepository(appContext)
                        repo.wordById(wordId)?.let { repo.rate(it, rating) }
                        WidgetUpdater.renderNow(appContext)
                    } catch (_: Throwable) {
                        // 小组件更新失败不影响 App 本身
                    } finally {
                        pending.finish()
                    }
                }
            }

            Widgets.ACTION_REFRESH,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> WidgetUpdater.refresh(context)
        }
    }
}

class WordWidgetProvider : BaseWidgetProvider()

class MiniWidgetProvider : BaseWidgetProvider()

class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        WidgetUpdater.renderNow(applicationContext)
        return Result.success()
    }
}
