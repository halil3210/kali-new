package alie.info.newmultichoice.services

import android.content.Context
import androidx.fragment.app.Fragment
import alie.info.newmultichoice.data.QuizRepository
import alie.info.newmultichoice.ui.overlays.OverlayManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Service for checking and unlocking achievements
 * Shows achievement overlays when conditions are met
 */
class AchievementService(private val context: Context) {

    private val repository = QuizRepository.getInstance(context)
    
    companion object {
        private const val PREF_NAME = "achievements"
        
        @Volatile
        private var INSTANCE: AchievementService? = null
        
        fun getInstance(context: Context): AchievementService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AchievementService(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    /**
     * Check all achievements after quiz completion
     */
    suspend fun checkAchievementsAfterQuiz(
        fragment: Fragment,
        correctAnswers: Int,
        totalQuestions: Int,
        durationSeconds: Int
    ) {
        checkFirstSteps(fragment)
        checkPerfectScore(fragment, correctAnswers, totalQuestions)
        checkSpeedDemon(fragment, durationSeconds)
        checkMaster(fragment)
    }
    
    /**
     * Check "First Steps" achievement
     * Unlock after completing first quiz
     */
    private suspend fun checkFirstSteps(fragment: Fragment) {
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            if (!prefs.getBoolean("first_steps", false)) {
                val stats = repository.getUserStats().first()
                if (stats != null && stats.totalQuizzesTaken >= 1) {
                    withContext(Dispatchers.Main) {
                        OverlayManager.showAchievementUnlockedOverlay(
                            fragment = fragment,
                            achievementName = OverlayManager.Achievements.FIRST_STEPS.name,
                            achievementDescription = OverlayManager.Achievements.FIRST_STEPS.description,
                            achievementIcon = OverlayManager.Achievements.FIRST_STEPS.icon,
                            xpReward = OverlayManager.Achievements.FIRST_STEPS.xpReward
                        )
                    }
                    prefs.edit().putBoolean("first_steps", true).apply()
                    repository.unlockAchievement("first_steps")
                }
            }
        }
    }
    
    /**
     * Check "Perfect Score" achievement
     * Unlock after getting 100% in a quiz
     */
    private suspend fun checkPerfectScore(fragment: Fragment, correctAnswers: Int, totalQuestions: Int) {
        if (correctAnswers == totalQuestions) {
            withContext(Dispatchers.IO) {
                val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                if (!prefs.getBoolean("perfect_score", false)) {
                    withContext(Dispatchers.Main) {
                        OverlayManager.showAchievementUnlockedOverlay(
                            fragment = fragment,
                            achievementName = OverlayManager.Achievements.PERFECT_SCORE.name,
                            achievementDescription = OverlayManager.Achievements.PERFECT_SCORE.description,
                            achievementIcon = OverlayManager.Achievements.PERFECT_SCORE.icon,
                            xpReward = OverlayManager.Achievements.PERFECT_SCORE.xpReward
                        )
                    }
                    prefs.edit().putBoolean("perfect_score", true).apply()
                    repository.unlockAchievement("perfect_score")
                }
            }
        }
    }
    
    /**
     * Check "Speed Demon" achievement
     * Unlock after completing quiz in under 5 minutes
     */
    private suspend fun checkSpeedDemon(fragment: Fragment, durationSeconds: Int) {
        if (durationSeconds < 300) { // 5 minutes
            withContext(Dispatchers.IO) {
                val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                if (!prefs.getBoolean("speed_demon", false)) {
                    withContext(Dispatchers.Main) {
                        OverlayManager.showAchievementUnlockedOverlay(
                            fragment = fragment,
                            achievementName = OverlayManager.Achievements.SPEED_DEMON.name,
                            achievementDescription = OverlayManager.Achievements.SPEED_DEMON.description,
                            achievementIcon = OverlayManager.Achievements.SPEED_DEMON.icon,
                            xpReward = OverlayManager.Achievements.SPEED_DEMON.xpReward
                        )
                    }
                    prefs.edit().putBoolean("speed_demon", true).apply()
                    repository.unlockAchievement("speed_demon")
                }
            }
        }
    }
    
    /**
     * Check "Master" achievement
     * Unlock after reaching 90%+ overall accuracy
     */
    private suspend fun checkMaster(fragment: Fragment) {
        withContext(Dispatchers.IO) {
            val stats = repository.getUserStats().first()
            if (stats != null && stats.overallAccuracy >= 90f) {
                val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                if (!prefs.getBoolean("master", false)) {
                    withContext(Dispatchers.Main) {
                        OverlayManager.showAchievementUnlockedOverlay(
                            fragment = fragment,
                            achievementName = OverlayManager.Achievements.MASTER.name,
                            achievementDescription = OverlayManager.Achievements.MASTER.description,
                            achievementIcon = OverlayManager.Achievements.MASTER.icon,
                            xpReward = OverlayManager.Achievements.MASTER.xpReward
                        )
                    }
                    prefs.edit().putBoolean("master", true).apply()
                    repository.unlockAchievement("master")
                }
            }
        }
    }
    
    /**
     * Check "Dedicated" achievement
     * Unlock after reaching 10-day streak
     */
    suspend fun checkDedicated(fragment: Fragment) {
        withContext(Dispatchers.IO) {
            val stats = repository.getUserStats().first()
            if (stats != null && stats.currentStreak >= 10) {
                val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                if (!prefs.getBoolean("dedicated", false)) {
                    withContext(Dispatchers.Main) {
                        OverlayManager.showAchievementUnlockedOverlay(
                            fragment = fragment,
                            achievementName = OverlayManager.Achievements.DEDICATED.name,
                            achievementDescription = OverlayManager.Achievements.DEDICATED.description,
                            achievementIcon = OverlayManager.Achievements.DEDICATED.icon,
                            xpReward = OverlayManager.Achievements.DEDICATED.xpReward
                        )
                    }
                    prefs.edit().putBoolean("dedicated", true).apply()
                    repository.unlockAchievement("dedicated")
                }
            }
        }
    }
}

