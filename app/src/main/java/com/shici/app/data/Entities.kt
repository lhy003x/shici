package com.shici.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 词书：四级 / 六级 / 考研，可继续扩展 */
@Entity(tableName = "book")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val desc: String,
)

/**
 * 单词。box = 0 表示还没学过；1..7 是 Leitner 记忆盒等级。
 * 每次复习按「忘了 / 模糊 / 认识」三档调整 box，并算出 nextReviewAt。
 */
@Entity(
    tableName = "word",
    indices = [Index("bookId"), Index("box"), Index("nextReviewAt")]
)
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val spelling: String,
    val phonetic: String,
    val meaning: String,
    val example: String,
    val exampleCn: String,
    val box: Int = 0,
    val nextReviewAt: Long = 0L,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val lastReviewAt: Long = 0L,
)

/** 复习流水，用于统计与打卡日历 */
@Entity(tableName = "review_log", indices = [Index("at"), Index("wordId")])
data class ReviewLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordId: Long,
    val rating: Int,
    val boxAfter: Int,
    val at: Long = System.currentTimeMillis(),
)
