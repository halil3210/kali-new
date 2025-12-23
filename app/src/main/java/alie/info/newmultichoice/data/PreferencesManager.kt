package alie.info.newmultichoice.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Modern preferences management using DataStore (2025 best practice)
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "klcp_quiz_preferences")

class PreferencesManager(private val context: Context) {
    
    private val dataStore = context.dataStore
    
    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("selected_language")
        private val DARK_MODE_KEY = stringPreferencesKey("dark_mode") // "auto", "light", "dark"
        private val HAPTIC_FEEDBACK_KEY = booleanPreferencesKey("haptic_feedback")
        private val SOUND_EFFECTS_KEY = booleanPreferencesKey("sound_effects")
        private val CURRENT_QUESTION_INDEX_KEY = intPreferencesKey("current_question_index")
        private val QUIZ_IN_PROGRESS_KEY = booleanPreferencesKey("quiz_in_progress")
        private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        private val TOTAL_QUIZZES_TAKEN_KEY = intPreferencesKey("total_quizzes_taken")
        private val BEST_SCORE_KEY = intPreferencesKey("best_score")
        
        @Volatile
        private var INSTANCE: PreferencesManager? = null
        
        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PreferencesManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    // Language
    val languageFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: "en"
    }
    
    suspend fun setLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
        }
    }
    
    // Dark Mode
    val darkModeFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: "auto"
    }
    
    suspend fun setDarkMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = mode
        }
    }
    
    // Haptic Feedback
    val hapticFeedbackFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HAPTIC_FEEDBACK_KEY] ?: true
    }
    
    suspend fun setHapticFeedback(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAPTIC_FEEDBACK_KEY] = enabled
        }
    }
    
    // Sound Effects
    val soundEffectsFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SOUND_EFFECTS_KEY] ?: true
    }
    
    suspend fun setSoundEffects(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SOUND_EFFECTS_KEY] = enabled
        }
    }
    
    // Quiz Progress
    val quizInProgressFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[QUIZ_IN_PROGRESS_KEY] ?: false
    }
    
    val currentQuestionIndexFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[CURRENT_QUESTION_INDEX_KEY] ?: 0
    }
    
    suspend fun saveQuizProgress(questionIndex: Int, inProgress: Boolean) {
        dataStore.edit { preferences ->
            preferences[CURRENT_QUESTION_INDEX_KEY] = questionIndex
            preferences[QUIZ_IN_PROGRESS_KEY] = inProgress
        }
    }
    
    suspend fun clearQuizProgress() {
        dataStore.edit { preferences ->
            preferences.remove(CURRENT_QUESTION_INDEX_KEY)
            preferences[QUIZ_IN_PROGRESS_KEY] = false
        }
    }
    
    // Onboarding
    val onboardingCompletedFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED_KEY] ?: false
    }
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = completed
        }
    }
    
    // Statistics
    val totalQuizzesTakenFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[TOTAL_QUIZZES_TAKEN_KEY] ?: 0
    }
    
    suspend fun incrementTotalQuizzes() {
        dataStore.edit { preferences ->
            val current = preferences[TOTAL_QUIZZES_TAKEN_KEY] ?: 0
            preferences[TOTAL_QUIZZES_TAKEN_KEY] = current + 1
        }
    }
    
    val bestScoreFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[BEST_SCORE_KEY] ?: 0
    }
    
    suspend fun updateBestScore(score: Int) {
        dataStore.edit { preferences ->
            val currentBest = preferences[BEST_SCORE_KEY] ?: 0
            if (score > currentBest) {
                preferences[BEST_SCORE_KEY] = score
            }
        }
    }
}

