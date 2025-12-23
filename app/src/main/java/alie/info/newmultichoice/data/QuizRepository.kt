package alie.info.newmultichoice.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import alie.info.newmultichoice.api.models.UserStatsDto

/**
 * Repository for managing quiz data
 */
class QuizRepository(private val context: Context) {
    
    private val database = QuizDatabase.getDatabase(context)
    private val questionDao = database.questionDao()
    private val sessionDao = database.quizSessionDao()
    private val answerDao = database.userAnswerDao()
    private val statsDao = database.userStatsDao()
    
    /**
     * Initialize database with questions from JSON if not already loaded
     */
    suspend fun initializeQuestionsIfNeeded() {
        withContext(Dispatchers.IO) {
            val count = questionDao.getQuestionCount()
            if (count == 0) {
                loadQuestionsFromAssets()
            }
        }
    }
    
    /**
     * Load questions from assets/questions.json
     */
    private suspend fun loadQuestionsFromAssets() {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.assets.open("questions.json")
                val reader = InputStreamReader(inputStream)
                val gson = Gson()
                val type = object : TypeToken<List<QuestionJsonModel>>() {}.type
                val jsonQuestions: List<QuestionJsonModel> = gson.fromJson(reader, type)
                
                val questions = jsonQuestions.map { it.toQuestion() }
                questionDao.insertQuestions(questions)
                
                reader.close()
                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
                throw RuntimeException("Failed to load questions from assets", e)
            }
        }
    }
    
    /**
     * Get all questions
     */
    suspend fun getAllQuestions(): List<Question> {
        return withContext(Dispatchers.IO) {
            questionDao.getAllQuestions()
        }
    }
    
    /**
     * Get a specific question by ID
     */
    suspend fun getQuestionById(id: Int): Question? {
        return withContext(Dispatchers.IO) {
            questionDao.getQuestionById(id)
        }
    }
    
    /**
     * Save a quiz session
     */
    suspend fun saveSession(session: QuizSession): Long {
        return withContext(Dispatchers.IO) {
            sessionDao.insertSession(session)
        }
    }
    
    /**
     * Update existing session
     */
    suspend fun updateSession(session: QuizSession) {
        withContext(Dispatchers.IO) {
            sessionDao.updateSession(session)
        }
    }
    
    /**
     * Get recent quiz sessions for a user
     */
    suspend fun getRecentSessions(userId: String): List<QuizSession> {
        return withContext(Dispatchers.IO) {
            sessionDao.getRecentSessions(userId)
        }
    }
    
    /**
     * Save a user answer
     */
    suspend fun saveAnswer(answer: UserAnswer) {
        withContext(Dispatchers.IO) {
            answerDao.insertAnswer(answer)
        }
    }
    
    /**
     * Get all answers for a session
     */
    suspend fun getAnswersForSession(sessionId: Long): List<UserAnswer> {
        return withContext(Dispatchers.IO) {
            answerDao.getAnswersForSession(sessionId)
        }
    }
    
    /**
     * Get all questions that were answered incorrectly
     * Returns unique questions from all UserAnswer records where isCorrect = false
     */
    suspend fun getWronglyAnsweredQuestions(): List<Question> {
        return withContext(Dispatchers.IO) {
            // Get all wrong answers
            val wrongAnswers = answerDao.getIncorrectAnswers()
            
            // Get unique question IDs
            val wrongQuestionIds = wrongAnswers.map { it.questionId }.distinct()
            
            // Fetch the actual questions
            val questions = questionDao.getAllQuestions()
            questions.filter { it.id in wrongQuestionIds }
        }
    }
    
    /**
     * Save quiz session and all answers atomically
     * This ensures data consistency even if app crashes during save
     */
    suspend fun saveSessionWithAnswers(session: QuizSession, answers: List<UserAnswer>): Long {
        return withContext(Dispatchers.IO) {
            database.runInTransaction {
                // Save session first
                val sessionId = sessionDao.insertSession(session)

                // Save all answers with the correct session ID
                answers.forEach { answer ->
                    answerDao.insertAnswer(answer.copy(sessionId = sessionId))
                }

                sessionId
            }
        }
    }
    
    // ==================== STREAK & STATS FUNCTIONS ====================
    
    /**
     * Get user stats as Flow
     */
    fun getUserStats() = statsDao.getUserStats()
    
    /**
     * Update streak when user completes activity
     */
    suspend fun updateStreak() {
        withContext(Dispatchers.IO) {
            val stats = statsDao.getUserStatsOnce() ?: UserStats()
            val today = getCurrentDate()
            val yesterday = getYesterdayDate()
            
            when (stats.lastActivityDate) {
                today -> {
                    // Already updated today, do nothing
                }
                yesterday -> {
                    // Streak continues
                    val newStreak = stats.currentStreak + 1
                    val newLongest = maxOf(newStreak, stats.longestStreak)
                    statsDao.insertOrUpdateStats(
                        stats.copy(
                            currentStreak = newStreak,
                            longestStreak = newLongest,
                            lastActivityDate = today
                        )
                    )
                }
                else -> {
                    // Streak broken, start new
                    statsDao.insertOrUpdateStats(
                        stats.copy(
                            currentStreak = 1,
                            lastActivityDate = today
                        )
                    )
                }
            }
        }
    }
    
    /**
     * Update daily progress
     */
    suspend fun updateDailyProgress(questionsAnswered: Int) {
        withContext(Dispatchers.IO) {
            val stats = statsDao.getUserStatsOnce() ?: UserStats()
            val today = getCurrentDate()
            
            if (stats.lastGoalResetDate != today) {
                // New day, reset counter
                statsDao.updateDailyProgress(questionsAnswered, today)
            } else {
                // Same day, increment
                val newCount = stats.todayQuestionCount + questionsAnswered
                statsDao.updateDailyProgress(newCount, today)
            }
        }
    }
    
    /**
     * Update overall stats after quiz
     */
    suspend fun updateOverallStats(
        quizCount: Int = 1,
        questionsAnswered: Int,
        correctAnswers: Int,
        timeSpentMinutes: Int
    ) {
        withContext(Dispatchers.IO) {
            val stats = statsDao.getUserStatsOnce() ?: UserStats()
            
            val newTotalQuestions = stats.totalQuestionsAnswered + questionsAnswered
            val newTotalCorrect = stats.totalCorrectAnswers + correctAnswers
            val newAccuracy = if (newTotalQuestions > 0) {
                (newTotalCorrect.toFloat() / newTotalQuestions) * 100f
            } else 0f
            
            statsDao.insertOrUpdateStats(
                stats.copy(
                    totalQuizzesTaken = stats.totalQuizzesTaken + quizCount,
                    totalQuestionsAnswered = newTotalQuestions,
                    totalCorrectAnswers = newTotalCorrect,
                    overallAccuracy = newAccuracy,
                    totalTimeSpentMinutes = stats.totalTimeSpentMinutes + timeSpentMinutes
                )
            )
        }
    }
    
    /**
     * Unlock achievement
     */
    suspend fun unlockAchievement(achievementId: String) {
        withContext(Dispatchers.IO) {
            val stats = statsDao.getUserStatsOnce() ?: UserStats()
            val currentAchievements = stats.unlockedAchievements.split(",").filter { it.isNotEmpty() }.toMutableSet()
            
            if (!currentAchievements.contains(achievementId)) {
                currentAchievements.add(achievementId)
                statsDao.updateAchievements(currentAchievements.joinToString(","))
            }
        }
    }
    
    /**
     * Check if achievement is unlocked
     */
    suspend fun isAchievementUnlocked(achievementId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val stats = statsDao.getUserStatsOnce() ?: return@withContext false
            stats.unlockedAchievements.contains(achievementId)
        }
    }
    
    /**
     * Get current date in yyyy-MM-dd format
     */
    private fun getCurrentDate(): String {
        val calendar = java.util.Calendar.getInstance()
        return String.format(
            "%04d-%02d-%02d",
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }
    
    /**
     * Get yesterday's date
     */
    private fun getYesterdayDate(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -1)
        return String.format(
            "%04d-%02d-%02d",
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }
    
    /**
     * Unlock next exam after passing current one
     */
    suspend fun unlockNextExam(examNumber: Int) {
        withContext(Dispatchers.IO) {
            val stats = statsDao.getUserStatsOnce() ?: UserStats()
            if (examNumber >= stats.highestUnlockedExam) {
                // Unlock next exam
                statsDao.insertOrUpdateStats(
                    stats.copy(highestUnlockedExam = examNumber + 1)
                )
            }
        }
    }
    
    /**
     * Check if exam is unlocked
     */
    suspend fun isExamUnlocked(examNumber: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val stats = statsDao.getUserStatsOnce() ?: UserStats()
            
            // Special requirement for Exam 1: Need 50 correct answers in quiz
            if (examNumber == 1) {
                return@withContext stats.totalCorrectAnswers >= 50
            }
            
            // For other exams, check if previous exam was passed
            examNumber <= stats.highestUnlockedExam
        }
    }
    
    /**
     * Get total correct answers count
     */
    suspend fun getTotalCorrectAnswers(): Int {
        return withContext(Dispatchers.IO) {
            val stats = statsDao.getUserStatsOnce() ?: UserStats()
            stats.totalCorrectAnswers
        }
    }
    
    /**
     * Update local stats from server data
     */
    suspend fun updateStatsFromServer(serverStats: UserStatsDto) {
        withContext(Dispatchers.IO) {
            val currentStats = statsDao.getUserStatsOnce() ?: UserStats()
            
            val updatedStats = currentStats.copy(
                totalQuizzesTaken = maxOf(currentStats.totalQuizzesTaken, serverStats.totalQuizzesTaken),
                totalQuestionsAnswered = maxOf(currentStats.totalQuestionsAnswered, serverStats.totalQuestionsAnswered),
                totalCorrectAnswers = maxOf(currentStats.totalCorrectAnswers, serverStats.totalCorrectAnswers),
                overallAccuracy = maxOf(currentStats.overallAccuracy, serverStats.overallAccuracy),
                currentStreak = maxOf(currentStats.currentStreak, serverStats.currentStreak),
                longestStreak = maxOf(currentStats.longestStreak, serverStats.longestStreak),
                highestUnlockedExam = maxOf(currentStats.highestUnlockedExam, serverStats.highestUnlockedExam),
                unlockedAchievements = mergeAchievements(currentStats.unlockedAchievements, serverStats.unlockedAchievements)
            )
            
            statsDao.insertOrUpdateStats(updatedStats)
        }
    }
    
    private fun mergeAchievements(local: String, server: String): String {
        val localSet = local.split(",").filter { it.isNotBlank() }.toSet()
        val serverSet = server.split(",").filter { it.isNotBlank() }.toSet()
        return (localSet + serverSet).joinToString(",")
    }
    
    companion object {
        @Volatile
        private var INSTANCE: QuizRepository? = null
        
        fun getInstance(context: Context): QuizRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = QuizRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

