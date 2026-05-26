package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY id ASC")
    fun getAllQuestions(): Flow<List<Question>>

    @Query("SELECT * FROM questions WHERE subject = :subject ORDER BY id ASC")
    fun getQuestionsBySubject(subject: String): Flow<List<Question>>

    @Query("SELECT * FROM questions WHERE subject = :subject AND chapter = :chapter ORDER BY id ASC")
    fun getQuestionsByChapter(subject: String, chapter: String): Flow<List<Question>>

    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    suspend fun getQuestionById(id: Long): Question?

    @Query("SELECT * FROM questions ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomQuestions(limit: Int): List<Question>

    @Query("SELECT * FROM questions WHERE subject = :subject ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomQuestionsBySubject(subject: String, limit: Int): List<Question>

    @Query("SELECT DISTINCT subject FROM questions")
    fun getSubjects(): Flow<List<String>>

    @Query("SELECT DISTINCT chapter FROM questions WHERE subject = :subject")
    fun getChaptersBySubject(subject: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: Question): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<Question>)

    @Delete
    suspend fun deleteQuestion(question: Question)

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun deleteQuestionById(id: Long)

    @Query("SELECT COUNT(*) FROM questions")
    fun getQuestionCountFlow(): Flow<Int>
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE questionId = :questionId")
    suspend fun removeBookmark(questionId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE questionId = :questionId)")
    fun isBookmarkedFlow(questionId: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE questionId = :questionId)")
    suspend fun isBookmarked(questionId: Long): Boolean
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY lastUpdated DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE category = :category ORDER BY lastUpdated DESC")
    fun getNotesByCategory(category: String): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Delete
    suspend fun deleteNote(note: Note)
}

@Dao
interface TestHistoryDao {
    @Query("SELECT * FROM test_history ORDER BY timestamp DESC")
    fun getAllTestHistory(): Flow<List<TestHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestHistory(test: TestHistory): Long

    @Query("DELETE FROM test_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)
}

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_progress ORDER BY dateString ASC")
    fun getAllProgress(): Flow<List<UserProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: UserProgress)

    @Query("SELECT * FROM user_progress WHERE dateString = :dateString LIMIT 1")
    suspend fun getProgressForDate(dateString: String): UserProgress?
}
