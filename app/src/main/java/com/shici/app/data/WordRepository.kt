package com.shici.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.time.LocalDate

/** 今日概览 */
data class DailyState(
    val totalWords: Int = 0,
    val newLeft: Int = 0,
    val dueLeft: Int = 0,
    val doneToday: Int = 0,
    val newTarget: Int = 0,
    val streak: Int = 0,
    val mastered: Int = 0,
) {
    val remaining: Int get() = newLeft + dueLeft
    val target: Int get() = doneToday + remaining
    val progress: Float
        get() = if (target <= 0) 1f else (doneToday.toFloat() / target).coerceIn(0f, 1f)
}

data class DayCount(val date: LocalDate, val count: Int)

data class WidgetProgress(val done: Int, val total: Int, val overdue: Int)

data class StatsSnapshot(
    val totalWords: Int = 0,
    val learning: Int = 0,
    val fragile: Int = 0,
    val mastered: Int = 0,
    val totalReviews: Int = 0,
    val accuracy: Int = 0,
    val streak: Int = 0,
    val week: List<DayCount> = emptyList(),
)

class WordRepository(context: Context) {

    private val app = context.applicationContext
    private val db = AppDatabase.get(app)
    private val bookDao = db.bookDao()
    private val wordDao = db.wordDao()
    private val logDao = db.logDao()
    private val settings = Settings(app)

    /* ---------------- 初始化 ---------------- */

    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        if (settings.seeded) return@withContext
        if (bookDao.count() > 0) {
            settings.seeded = true
            return@withContext
        }
        val json = app.assets.open("words.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val bookJson = arr.getJSONObject(i)
            val bookId = bookDao.insertAll(
                listOf(
                    Book(
                        code = bookJson.getString("code"),
                        name = bookJson.getString("name"),
                        desc = bookJson.getString("desc"),
                    )
                )
            ).first()
            val wordsJson = bookJson.getJSONArray("words")
            val words = ArrayList<Word>(wordsJson.length())
            for (j in 0 until wordsJson.length()) {
                val w = wordsJson.getJSONObject(j)
                words += Word(
                    bookId = bookId,
                    spelling = w.getString("w"),
                    phonetic = w.optString("p"),
                    meaning = w.getString("m"),
                    example = w.optString("e"),
                    exampleCn = w.optString("c"),
                )
            }
            wordDao.insertAll(words)
        }
        settings.seeded = true
    }

    /* ---------------- 首页 / 学习队列 ---------------- */

    suspend fun todayState(): DailyState = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val newTarget = settings.dailyNew
        val todayStart = Srs.startOfToday()
        val done = logDao.countSince(todayStart)
        val learnedToday = wordDao.countNewLearnedSince(todayStart)
        val newLeft = minOf(wordDao.countNew(), (newTarget - learnedToday).coerceAtLeast(0))
        val limit = settings.dailyReviewLimit
        val dueTotal = wordDao.countDue(now)
        val dueLeft = if (limit <= 0) dueTotal else minOf(dueTotal, limit)
        DailyState(
            totalWords = wordDao.total(),
            newLeft = newLeft,
            dueLeft = dueLeft,
            doneToday = done,
            newTarget = newTarget,
            streak = streak(),
            mastered = wordDao.countMastered(),
        )
    }

    /** 今日学习队列：先复习到期的，再补新词 */
    suspend fun studyQueue(): List<Word> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val due = wordDao.pickDue(now, settings.dailyReviewLimit.coerceAtLeast(1))
        val newWords = wordDao.pickNew(settings.dailyNew)
        (due + newWords).distinctBy { it.id }
    }

    /** 听写队列：到期 + 新词，若都没有则随机抽词 */
    suspend fun dictationQueue(limit: Int = 8): List<Word> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val due = wordDao.pickDue(now, limit)
        val newWords = wordDao.pickNew(limit)
        val picked = (due + newWords).distinctBy { it.id }.take(limit)
        picked.ifEmpty { wordDao.random(limit) }
    }

    suspend fun rate(word: Word, rating: Int): Word = withContext(Dispatchers.IO) {        val updated = Srs.apply(word, rating, settings.demoMode)
        wordDao.update(updated)
        logDao.insert(ReviewLog(wordId = word.id, rating = rating, boxAfter = updated.box))
        updated
    }

    suspend fun checkDue(word: Word): Boolean = withContext(Dispatchers.IO) {
        val latest = wordDao.byId(word.id) ?: return@withContext false
        latest.nextReviewAt <= System.currentTimeMillis()
    }

    /* ---------------- 词库 ---------------- */

    suspend fun books(): List<BookWithCount> = withContext(Dispatchers.IO) { bookDao.allWithCount() }

    suspend fun wordsOfBook(bookId: Long, limit: Int = 200): List<Word> =
        withContext(Dispatchers.IO) { wordDao.listByBook(bookId, limit) }

    suspend fun search(keyword: String): List<Word> = withContext(Dispatchers.IO) {
        if (keyword.isBlank()) emptyList() else wordDao.search(keyword.trim())
    }

    suspend fun hardWords(): List<Word> = withContext(Dispatchers.IO) { wordDao.hardWords() }

    suspend fun masteredWords(): List<Word> = withContext(Dispatchers.IO) { wordDao.mastered() }

    /* ---------------- 小组件 ---------------- */

    /** 小组件要展示的下一个词：优先到期的，其次新词 */
    suspend fun widgetWord(): Word? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        wordDao.pickDue(now, 1).firstOrNull() ?: wordDao.pickNew(1).firstOrNull()
    }

    suspend fun wordById(id: Long): Word? = withContext(Dispatchers.IO) { wordDao.byId(id) }

    suspend fun widgetProgress(): WidgetProgress = withContext(Dispatchers.IO) {
        val state = todayState()
        val overdue = wordDao.countOverdue(Srs.startOfToday())
        WidgetProgress(
            done = state.doneToday,
            total = maxOf(state.doneToday + state.remaining, 1),
            overdue = overdue,
        )
    }

    /* ---------------- 统计 ---------------- */

    suspend fun stats(): StatsSnapshot = withContext(Dispatchers.IO) {
        val since7 = Srs.startOfToday() - 6L * 24 * 60 * 60 * 1000
        val logs = logDao.since(since7)
        val byDay = logs.groupBy { Srs.dayOf(it.at) }.mapValues { it.value.size }
        val today = LocalDate.now()
        val week = (6 downTo 0).map { offset ->
            val d = today.minusDays(offset.toLong())
            DayCount(d, byDay[d] ?: 0)
        }
        val total = logDao.total()
        val correct = logs.count { it.rating == Srs.RATING_KNOW }
        StatsSnapshot(
            totalWords = wordDao.total(),
            learning = wordDao.countLearning(),
            fragile = wordDao.countFragile(),
            mastered = wordDao.countMastered(),
            totalReviews = total,
            accuracy = if (total == 0) 0 else (correct * 100 / maxOf(logs.size, 1)).coerceIn(0, 100),
            streak = streak(),
            week = week,
        )
    }

    suspend fun streak(): Int = withContext(Dispatchers.IO) {
        val days = logDao.allTimestamps().map { Srs.dayOf(it) }.toHashSet()
        if (days.isEmpty()) return@withContext 0
        var cursor = LocalDate.now()
        if (!days.contains(cursor)) {
            cursor = cursor.minusDays(1)
            if (!days.contains(cursor)) return@withContext 0
        }
        var count = 0
        while (days.contains(cursor)) {
            count++
            cursor = cursor.minusDays(1)
        }
        count
    }

    suspend fun resetProgress() = withContext(Dispatchers.IO) {
        val all = (1..3).flatMap { wordDao.listByBook(it.toLong(), 9999) }
        all.forEach { wordDao.update(it.copy(box = 0, nextReviewAt = 0, correctCount = 0, wrongCount = 0, lastReviewAt = 0)) }
        logDao.clear()
    }

    fun settings(): Settings = settings
}
