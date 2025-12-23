package alie.info.newmultichoice.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao {
    
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getUserStats(): Flow<UserStats?>
    
    @Query("SELECT * FROM user_stats WHERE id = 1")
    suspend fun getUserStatsOnce(): UserStats?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStats(stats: UserStats)
    
    @Query("UPDATE user_stats SET currentStreak = :streak, lastActivityDate = :date WHERE id = 1")
    suspend fun updateStreak(streak: Int, date: String)
    
    @Query("UPDATE user_stats SET todayQuestionCount = :count, lastGoalResetDate = :date WHERE id = 1")
    suspend fun updateDailyProgress(count: Int, date: String)
    
    @Query("UPDATE user_stats SET unlockedAchievements = :achievements WHERE id = 1")
    suspend fun updateAchievements(achievements: String)
    
    @Transaction
    suspend fun incrementQuestionCount() {
        val stats = getUserStatsOnce() ?: UserStats()
        insertOrUpdateStats(
            stats.copy(
                todayQuestionCount = stats.todayQuestionCount + 1,
                totalQuestionsAnswered = stats.totalQuestionsAnswered + 1
            )
        )
    }
}

