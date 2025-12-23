package alie.info.newmultichoice.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks user answers for each question in a session
 */
@Entity(tableName = "user_answers")
data class UserAnswer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val sessionId: Long,
    val questionId: Int,
    val userAnswer: String, // A, B, C, or D
    val correctAnswer: String,
    val isCorrect: Boolean,
    val timestamp: Long
)

