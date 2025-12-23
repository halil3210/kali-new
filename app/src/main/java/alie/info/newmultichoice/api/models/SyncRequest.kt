package alie.info.newmultichoice.api.models

import com.google.gson.annotations.SerializedName

/**
 * Request model for syncing user data to server
 */
data class SyncRequest(
    @SerializedName("deviceId")
    val deviceId: String,
    
    @SerializedName("userStats")
    val userStats: UserStatsDto?,
    
    @SerializedName("quizSessions")
    val quizSessions: List<QuizSessionDto>?,
    
    @SerializedName("lastSync")
    val lastSync: Long
)

data class UserStatsDto(
    @SerializedName("totalQuizzesTaken")
    val totalQuizzesTaken: Int,
    
    @SerializedName("totalQuestionsAnswered")
    val totalQuestionsAnswered: Int,
    
    @SerializedName("totalCorrectAnswers")
    val totalCorrectAnswers: Int,
    
    @SerializedName("overallAccuracy")
    val overallAccuracy: Float,
    
    @SerializedName("currentStreak")
    val currentStreak: Int,
    
    @SerializedName("longestStreak")
    val longestStreak: Int,
    
    @SerializedName("highestUnlockedExam")
    val highestUnlockedExam: Int,
    
    @SerializedName("unlockedAchievements")
    val unlockedAchievements: String
)

data class QuizSessionDto(
    @SerializedName("sessionId")
    val sessionId: Long? = null,
    
    @SerializedName("totalQuestions")
    val totalQuestions: Int,
    
    @SerializedName("correctAnswers")
    val correctAnswers: Int,
    
    @SerializedName("wrongAnswers")
    val wrongAnswers: Int,
    
    @SerializedName("percentage")
    val percentage: Float,
    
    @SerializedName("completedAt")
    val completedAt: String? = null,
    
    @SerializedName("isCompleted")
    val isCompleted: Boolean = true
)

