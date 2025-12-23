package alie.info.newmultichoice.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a quiz session result
 */
@Entity(tableName = "quiz_sessions")
data class QuizSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val userId: String = "", // User ID from auth server
    val timestamp: Long,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val percentage: Float,
    val durationMinutes: Int,
    val language: String, // "en" or "de"
    val isCompleted: Boolean = false // true if quiz was fully completed, false if aborted
)

