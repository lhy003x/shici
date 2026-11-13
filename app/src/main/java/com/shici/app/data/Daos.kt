package com.shici.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM book ORDER BY id")
    suspend fun all(): List<Book>

    @Query("SELECT * FROM book WHERE id = :id")
    suspend fun byId(id: Long): Book?

    @Query("SELECT COUNT(*) FROM book")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<Book>): List<Long>

    @Query(
        "SELECT b.*, (SELECT COUNT(*) FROM word w WHERE w.bookId = b.id) AS wordCount " +
            "FROM book b ORDER BY b.id"
    )
    suspend fun allWithCount(): List<BookWithCount>
}

data class BookWithCount(
    val id: Long,
    val code: String,
    val name: String,
    val desc: String,
    val wordCount: Int,
)

@Dao
interface WordDao {

    @Query("SELECT COUNT(*) FROM word")
    suspend fun total(): Int

    @Query("SELECT COUNT(*) FROM word WHERE box = 0")
    suspend fun countNew(): Int

    @Query("SELECT COUNT(*) FROM word WHERE box > 0")
    suspend fun countLearning(): Int

    @Query("SELECT COUNT(*) FROM word WHERE box > 0 AND nextReviewAt <= :now")
    suspend fun countDue(now: Long): Int

    @Query("SELECT COUNT(*) FROM word WHERE box > 0 AND nextReviewAt < :before")
    suspend fun countOverdue(before: Long): Int

    @Query("SELECT COUNT(*) FROM word WHERE box >= 5")
    suspend fun countMastered(): Int

    @Query("SELECT * FROM word WHERE box = 0 ORDER BY id LIMIT :limit")
    suspend fun pickNew(limit: Int): List<Word>

    @Query(
        "SELECT * FROM word WHERE box > 0 AND nextReviewAt <= :now " +
            "ORDER BY nextReviewAt LIMIT :limit"
    )
    suspend fun pickDue(now: Long, limit: Int): List<Word>

    @Query("SELECT * FROM word WHERE id = :id")
    suspend fun byId(id: Long): Word?

    @Query("SELECT * FROM word WHERE bookId = :bookId ORDER BY id LIMIT :limit")
    suspend fun listByBook(bookId: Long, limit: Int): List<Word>

    @Query("SELECT * FROM word WHERE spelling LIKE '%' || :q || '%' ORDER BY spelling LIMIT 100")
    suspend fun search(q: String): List<Word>

    @Query("SELECT * FROM word WHERE box >= 5 ORDER BY lastReviewAt DESC")
    suspend fun mastered(): List<Word>

    @Query("SELECT * FROM word WHERE wrongCount > 0 ORDER BY wrongCount DESC, id")
    suspend fun hardWords(): List<Word>

    @Update
    suspend fun update(word: Word)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<Word>)

    @Query("SELECT COUNT(*) FROM word WHERE box >= 1 AND box <= 3")
    suspend fun countFragile(): Int

    @Query("SELECT * FROM word ORDER BY RANDOM() LIMIT :limit")
    suspend fun random(limit: Int): List<Word>

    /** 今天第一次见到的词（首次复习发生在今天） */
    @Query(
        "SELECT COUNT(*) FROM word WHERE lastReviewAt >= :since " +
            "AND (correctCount + wrongCount) = 1"
    )
    suspend fun countNewLearnedSince(since: Long): Int
}

@Dao
interface LogDao {
    @Insert
    suspend fun insert(log: ReviewLog): Long

    @Query("SELECT * FROM review_log WHERE at >= :since ORDER BY at")
    suspend fun since(since: Long): List<ReviewLog>

    @Query("SELECT COUNT(*) FROM review_log WHERE at >= :since")
    suspend fun countSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM review_log WHERE rating = 2 AND at >= :since")
    suspend fun correctSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM review_log")
    suspend fun total(): Int

    @Query("DELETE FROM review_log")
    suspend fun clear()

    @Query("SELECT at FROM review_log ORDER BY at DESC")
    suspend fun allTimestamps(): List<Long>
}
