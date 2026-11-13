package com.shici.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 简化版 Leitner / 艾宾浩斯调度。
 *
 * 三档反馈：
 *  - 忘了(0)：回到盒子 1
 *  - 模糊(1)：原地停留（未学过则进入盒子 1）
 *  - 认识(2)：升一级，最高盒子 7
 *
 * 间隔按盒子取 1/2/4/7/15/30/60 天；演示模式下把「天」换成「分钟」，
 * 方便答辩现场看到间隔真的在变。
 */
object Srs {

    const val RATING_FORGOT = 0
    const val RATING_VAGUE = 1
    const val RATING_KNOW = 2

    val LABELS = arrayOf("忘了", "模糊", "认识")

    private val DAYS = intArrayOf(0, 1, 2, 4, 7, 15, 30, 60)
    private const val MINUTE = 60_000L
    private const val DAY = 24 * 60 * MINUTE

    fun nextBox(current: Int, rating: Int): Int = when (rating) {
        RATING_FORGOT -> 1
        RATING_VAGUE -> if (current <= 0) 1 else current
        else -> (current + 1).coerceAtMost(7)
    }

    fun intervalMillis(box: Int, demo: Boolean): Long {
        val unit = DAYS[box.coerceIn(0, 7)]
        return if (demo) unit * MINUTE else unit * DAY
    }

    fun apply(word: Word, rating: Int, demo: Boolean, now: Long = System.currentTimeMillis()): Word {
        val box = nextBox(word.box, rating)
        return word.copy(
            box = box,
            nextReviewAt = now + intervalMillis(box, demo),
            lastReviewAt = now,
            correctCount = if (rating == RATING_KNOW) word.correctCount + 1 else word.correctCount,
            wrongCount = if (rating == RATING_FORGOT) word.wrongCount + 1 else word.wrongCount,
        )
    }

    /** 卡片上显示「下次复习：…」 */
    fun humanInterval(box: Int, demo: Boolean): String {
        val unit = DAYS[box.coerceIn(0, 7)]
        return if (demo) "$unit 分钟后" else "$unit 天后"
    }

    fun startOfToday(zone: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

    fun dayOf(ts: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(ts).atZone(zone).toLocalDate()
}
