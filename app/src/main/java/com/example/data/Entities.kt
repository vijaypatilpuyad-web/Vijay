package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,      // e.g. "Electrician Theory", "Employability Skills"
    val chapter: String,      // e.g. "Safety Rules & Tools", "Alternating Current"
    val questionTextEn: String,
    val questionTextMr: String,
    val optionAEn: String,
    val optionAMr: String,
    val optionBEn: String,
    val optionBMr: String,
    val optionCEn: String,
    val optionCMr: String,
    val optionDEn: String,
    val optionDMr: String,
    val correctOption: String, // "A", "B", "C", "D"
    val explanation: String,    // Bilingual description
    val difficulty: String     // "Easy", "Medium", "Hard"
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey val questionId: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val category: String, // e.g., "Safety Signs", "Formulae", "AC Circuits"
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "test_history")
data class TestHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val testTitle: String,
    val subjectName: String, // e.g. "Electrician Theory", "Full Trade Mock"
    val totalQuestions: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val unattemptedCount: Int,
    val scorePercentage: Float,
    val durationSeconds: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val dateString: String, // "yyyy-MM-dd"
    val questionsAttempted: Int,
    val correctAnswers: Int
)
