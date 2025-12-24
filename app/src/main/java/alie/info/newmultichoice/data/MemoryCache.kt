package alie.info.newmultichoice.data

import android.util.LruCache
import alie.info.newmultichoice.data.Question
import alie.info.newmultichoice.data.UserStats

/**
 * Simple memory cache for frequently accessed data
 * Uses LRU (Least Recently Used) eviction policy
 */
class MemoryCache {

    // Cache for questions (by ID) - increased size for better performance
    private val questionCache = object : LruCache<Int, Question>(100) {
        override fun sizeOf(key: Int, value: Question): Int = 1
    }

    // Cache for question lists by exam/practice mode
    private val examQuestionsCache = mutableMapOf<String, Pair<List<Question>, Long>>()
    private var practiceQuestionsCache: Pair<List<Question>, Long>? = null
    private var practiceCacheTime: Long = 0

    // Cache for user stats
    private var userStatsCache: UserStats? = null
    private var statsCacheTime: Long = 0
    private val STATS_CACHE_DURATION = 45_000L // 45 seconds (increased)

    // Cache for all questions list
    private var allQuestionsCache: List<Question>? = null
    private var questionsCacheTime: Long = 0
    private val QUESTIONS_CACHE_DURATION = 120_000L // 2 minutes (increased)

    companion object {
        @Volatile
        private var INSTANCE: MemoryCache? = null

        fun getInstance(): MemoryCache {
            return INSTANCE ?: synchronized(this) {
                val instance = MemoryCache()
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Cache a question
     */
    fun putQuestion(question: Question) {
        questionCache.put(question.id, question)
    }

    /**
     * Get a cached question
     */
    fun getQuestion(id: Int): Question? {
        return questionCache.get(id)
    }

    /**
     * Cache user stats with timestamp
     */
    fun putUserStats(stats: UserStats) {
        userStatsCache = stats
        statsCacheTime = System.currentTimeMillis()
    }

    /**
     * Get cached user stats if not expired
     */
    fun getUserStats(): UserStats? {
        val currentTime = System.currentTimeMillis()
        return if (currentTime - statsCacheTime < STATS_CACHE_DURATION) {
            userStatsCache
        } else {
            null // Cache expired
        }
    }

    /**
     * Cache all questions list with timestamp
     */
    fun putAllQuestions(questions: List<Question>) {
        allQuestionsCache = questions
        questionsCacheTime = System.currentTimeMillis()

        // Also cache individual questions for faster access
        questions.forEach { putQuestion(it) }
    }

    /**
     * Get cached questions list if not expired
     */
    fun getAllQuestions(): List<Question>? {
        val currentTime = System.currentTimeMillis()
        return if (currentTime - questionsCacheTime < QUESTIONS_CACHE_DURATION) {
            allQuestionsCache
        } else {
            null // Cache expired
        }
    }

    /**
     * Cache questions for specific exam
     */
    fun putExamQuestions(examId: String, questions: List<Question>) {
        examQuestionsCache[examId] = Pair(questions, System.currentTimeMillis())
    }

    /**
     * Get cached exam questions
     */
    fun getExamQuestions(examId: String): List<Question>? {
        val cached = examQuestionsCache[examId]
        return cached?.let { (questions, cacheTime) ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - cacheTime < QUESTIONS_CACHE_DURATION) {
                questions
            } else {
                examQuestionsCache.remove(examId) // Remove expired cache
                null
            }
        }
    }

    /**
     * Cache practice questions (wrong answers)
     */
    fun putPracticeQuestions(questions: List<Question>) {
        practiceQuestionsCache = Pair(questions, System.currentTimeMillis())
    }

    /**
     * Get cached practice questions
     */
    fun getPracticeQuestions(): List<Question>? {
        val cached = practiceQuestionsCache
        return cached?.let { (questions, cacheTime) ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - cacheTime < QUESTIONS_CACHE_DURATION) {
                questions
            } else {
                practiceQuestionsCache = null // Clear expired cache
                null
            }
        }
    }

    /**
     * Clear all cached data
     */
    fun clear() {
        questionCache.evictAll()
        userStatsCache = null
        statsCacheTime = 0
        allQuestionsCache = null
        questionsCacheTime = 0
        examQuestionsCache.clear()
        practiceQuestionsCache = null
        practiceCacheTime = 0
    }

    /**
     * Clear expired cache entries
     */
    fun clearExpired() {
        val currentTime = System.currentTimeMillis()

        // Clear expired exam caches
        examQuestionsCache.entries.removeIf { (_, pair) ->
            currentTime - pair.second >= QUESTIONS_CACHE_DURATION
        }

        // Clear expired practice cache
        practiceQuestionsCache?.let { (_, cacheTime) ->
            if (currentTime - cacheTime >= QUESTIONS_CACHE_DURATION) {
                practiceQuestionsCache = null
            }
        }

        // Clear expired stats cache
        if (userStatsCache != null && currentTime - statsCacheTime >= STATS_CACHE_DURATION) {
            userStatsCache = null
            statsCacheTime = 0
        }

        // Clear expired questions cache
        if (allQuestionsCache != null && currentTime - questionsCacheTime >= QUESTIONS_CACHE_DURATION) {
            allQuestionsCache = null
            questionsCacheTime = 0
        }
    }

    /**
     * Get cache statistics for debugging
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "questions_cached" to questionCache.size(),
            "questions_max" to questionCache.maxSize(),
            "exam_caches" to examQuestionsCache.size,
            "practice_cached" to (practiceQuestionsCache != null),
            "stats_cached" to (userStatsCache != null),
            "all_questions_cached" to (allQuestionsCache != null),
            "total_cache_entries" to (questionCache.size() + examQuestionsCache.size + (if (practiceQuestionsCache != null) 1 else 0))
        )
    }
}
