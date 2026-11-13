package com.shici.app

import android.app.Application
import com.shici.app.data.WordRepository
import com.shici.app.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {

    val repo: WordRepository by lazy { WordRepository(this) }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch { runCatching { repo.seedIfNeeded() } }
        WidgetUpdater.scheduleDailyRefresh(this)
    }
}
