package alie.info.newmultichoice.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User Statistics and Streak Tracking
 */
@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey
    val id: Int = 1, // Single row for user stats
    
    // Streak tracking
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActivityDate: String = "", // Format: yyyy-MM-dd
    
    // Daily goals
    val dailyGoal: Int = 10, // questions per day
    val todayQuestionCount: Int = 0,
    val lastGoalResetDate: String = "",
    
    // Overall stats
    val totalQuizzesTaken: Int = 0,
    val totalQuestionsAnswered: Int = 0,
    val totalCorrectAnswers: Int = 0,
    val overallAccuracy: Float = 0f,
    
    // Time tracking
    val totalTimeSpentMinutes: Int = 0,
    val fastestQuizTimeSeconds: Int = Int.MAX_VALUE,
    
    // Achievements unlocked (comma-separated IDs)
    val unlockedAchievements: String = "",
    
    // Exam progress (highest unlocked exam number)
    val highestUnlockedExam: Int = 1 // Start with Exam 1 unlocked
)

