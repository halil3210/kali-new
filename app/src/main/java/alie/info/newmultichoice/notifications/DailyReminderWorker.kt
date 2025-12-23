package alie.info.newmultichoice.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import alie.info.newmultichoice.data.QuizRepository
import kotlinx.coroutines.flow.first

class DailyReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val repository = QuizRepository.getInstance(context)
            val notificationManager = StreakNotificationManager(context)
            
            // Get user stats
            val stats = repository.getUserStats().first()
            
            if (stats != null) {
                val questionsRemaining = stats.dailyGoal - stats.todayQuestionCount
                
                // Send notification if user hasn't completed daily goal
                if (questionsRemaining > 0) {
                    if (stats.currentStreak > 0) {
                        notificationManager.showStreakReminderNotification(stats.currentStreak)
                    } else {
                        notificationManager.showDailyGoalReminderNotification(questionsRemaining)
                    }
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

