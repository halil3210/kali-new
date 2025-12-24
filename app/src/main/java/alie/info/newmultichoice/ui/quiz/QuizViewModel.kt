package alie.info.newmultichoice.ui.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import alie.info.newmultichoice.auth.AuthManager
import alie.info.newmultichoice.data.Question
import alie.info.newmultichoice.data.QuizRepository
import alie.info.newmultichoice.data.QuizSession
import alie.info.newmultichoice.data.UserAnswer
import alie.info.newmultichoice.api.RetrofitClient
import alie.info.newmultichoice.api.models.CheckGuestUpgradeRequest
import alie.info.newmultichoice.utils.DeviceUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * ViewModel for managing quiz state and logic
 * Uses StateFlow for modern reactive state management
 */
class QuizViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = QuizRepository.getInstance(application)
    private val authManager = AuthManager.getInstance(application)
    
    // Modern StateFlow-based UI state
    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()
    
    // Internal state
    private var _questions = listOf<Question>()
    private var _currentQuestionIndex = 0
    private var _selectedAnswer: String? = null
    private var _isAnswerSubmitted = false
    private var _correctAnswersCount = 0
    private var _wrongAnswersCount = 0
    private var _currentLanguage = "en"

    // Data prefetching
    private var _nextQuestion: Question? = null
    
    private var sessionStartTime: Long = 0
    private var currentSessionId: Long = 0
    private val userAnswers = mutableListOf<UserAnswer>()
    
    private val _showSummaryDialog = MutableLiveData<Boolean>(false)
    val showSummaryDialog: LiveData<Boolean> = _showSummaryDialog
    
    // Should show upgrade prompt (from server)
    private val _shouldShowUpgradePrompt = MutableLiveData<Boolean>(false)
    val shouldShowUpgradePrompt: LiveData<Boolean> = _shouldShowUpgradePrompt
    
    // Backward compatibility LiveData
    private val _languageLiveData = MutableLiveData("en")
    val language: LiveData<String> = _languageLiveData
    
    private val _currentQuestionIndexLiveData = MutableLiveData(0)
    val currentQuestionIndex: LiveData<Int> = _currentQuestionIndexLiveData
    
    private val _currentQuestionLiveData = MutableLiveData<Question?>()
    val currentQuestion: LiveData<Question?> = _currentQuestionLiveData
    
    // Practice mode flag
    private var isPracticeMode = false
    
    // Demo mode flag (for guest users)
    private var isDemoMode = false
    private var demoQuestionLimit = 5
    
    // Guard to prevent multiple finishQuiz() calls
    private var isFinishingQuiz = false
    
    // Guard to prevent double stats update (finishQuiz + saveSessionOnExit)
    private var isQuizFinished = false
    
    init {
        // Questions werden erst geladen wenn startQuiz() aufgerufen wird
    }
    
    /**
     * Create a new quiz session - saves immediately to track started sessions
     */
    private fun createNewSession() {
        sessionStartTime = System.currentTimeMillis()
        userAnswers.clear()
        _correctAnswersCount = 0
        _wrongAnswersCount = 0
        
        // Save session immediately when quiz starts
        viewModelScope.launch {
            try {
                val totalQuestions = _questions.size
                val session = QuizSession(
                    userId = authManager.getUserId() ?: "",
                    timestamp = sessionStartTime,
                    totalQuestions = totalQuestions,
                    correctAnswers = 0,
                    wrongAnswers = 0,
                    percentage = 0f,
                    durationMinutes = 0,
                    language = _currentLanguage,
                    isCompleted = false // Session starts as incomplete
                )
                currentSessionId = repository.saveSession(session)
                alie.info.newmultichoice.utils.Logger.d("QuizViewModel", "New session created with ID: $currentSessionId")
            } catch (e: Exception) {
                android.util.Log.e("QuizViewModel", "Error creating session", e)
            }
        }
    }
    
    /**
     * Check and unlock achievements
     */
    private suspend fun checkAchievements(correct: Int, total: Int, percentage: Float, duration: Int) {
        try {
            // Get stats once (not as flow) - use first() instead of collect() to avoid blocking
            val statsFlow = repository.getUserStats()
            val userStats = statsFlow.firstOrNull()
            
            if (userStats != null) {
                // First Steps
                if (userStats.totalQuizzesTaken <= 1) {
                    repository.unlockAchievement("first_steps")
                }
                
                // Perfect Score
                if (correct == total && total > 0) {
                    repository.unlockAchievement("perfect_score")
                }
                
                // Speed Demon (under 5 minutes)
                if (duration < 5) {
                    repository.unlockAchievement("speed_demon")
                }
                
                // Master (90%+ accuracy)
                if (userStats.overallAccuracy >= 90f) {
                    repository.unlockAchievement("master")
                }
                
                // Dedicated (10+ day streak)
                if (userStats.currentStreak >= 10) {
                    repository.unlockAchievement("dedicated")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("QuizViewModel", "Error checking achievements", e)
        }
    }
    
    /**
     * Start quiz with specified mode
     * @param practiceMode if true, loads only wrongly answered questions
     * @param demoMode if true, limits to demoQuestionLimit questions
     * @param demoLimit number of questions for demo mode (default 5)
     */
    fun startQuiz(practiceMode: Boolean, demoMode: Boolean = false, demoLimit: Int = 5) {
        isPracticeMode = practiceMode
        isDemoMode = demoMode
        demoQuestionLimit = demoLimit
        loadQuestions()
        createNewSession()
    }
    
    /**
     * Load questions from repository
     */
    fun loadQuestions() {
        viewModelScope.launch {
            try {
                // Emit Loading state
                _uiState.value = QuizUiState.Loading
                
                // Initialize questions if needed
                repository.initializeQuestionsIfNeeded()
                
                // Load questions based on mode
                val loadedQuestions = if (isPracticeMode) {
                    repository.getWronglyAnsweredQuestions()
                } else {
                    repository.getAllQuestions()
                }
                
                // Limit questions for demo mode
                _questions = if (isDemoMode && loadedQuestions.size > demoQuestionLimit) {
                    loadedQuestions.take(demoQuestionLimit)
                } else {
                    loadedQuestions
                }
                
                if (loadedQuestions.isNotEmpty()) {
                    _currentQuestionIndex = 0
                    sessionStartTime = System.currentTimeMillis()

                    // Emit Success state with first question
                    emitSuccessState()

                    // Backward compatibility (extra safety)
                    _currentQuestionLiveData.value = loadedQuestions.getOrNull(0)
                    _currentQuestionIndexLiveData.value = 0
                    _languageLiveData.value = _currentLanguage

                    // Start prefetching next question
                    prefetchNextQuestion()
                } else {
                    // Emit Error state
                    val errorMessage = if (isPracticeMode) {
                        "No wrong answers found!\nStart a quiz first."
                    } else {
                        "No questions found"
                    }
                    _uiState.value = QuizUiState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _uiState.value = QuizUiState.Error("Error loading questions: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Helper function to emit Success state with current data
     */
    private fun emitSuccessState(feedbackState: FeedbackState? = null) {
        if (_questions.isEmpty()) return
        
        _uiState.value = QuizUiState.Success(
            currentQuestion = _questions[_currentQuestionIndex],
            currentQuestionIndex = _currentQuestionIndex,
            totalQuestions = _questions.size,
            selectedAnswer = _selectedAnswer,
            isAnswerSubmitted = _isAnswerSubmitted,
            correctAnswersCount = _correctAnswersCount,
            wrongAnswersCount = _wrongAnswersCount,
            quizCompleted = false,
            language = _currentLanguage,
            feedbackState = feedbackState
        )
    }
    
    /**
     * Select an answer
     */
    fun selectAnswer(answer: String) {
        if (!_isAnswerSubmitted) {
            _selectedAnswer = answer
            emitSuccessState()
        }
    }
    
    /**
     * Submit the selected answer
     * Calculates feedback and emits state with feedback information
     */
    fun submitAnswer() {
        val answer = _selectedAnswer ?: return
        val question = _questions.getOrNull(_currentQuestionIndex) ?: return
        
        _isAnswerSubmitted = true
        
        val isCorrect = answer == question.correct
        
        if (isCorrect) {
            _correctAnswersCount++
        } else {
            _wrongAnswersCount++
        }
        
        // Save answer for this session
        val userAnswer = UserAnswer(
            sessionId = currentSessionId,
            questionId = question.id,
            userAnswer = answer,
            correctAnswer = question.correct,
            isCorrect = isCorrect,
            timestamp = System.currentTimeMillis()
        )
        userAnswers.add(userAnswer)
        
        // Check if this is the last question
        val isLastQuestion = _currentQuestionIndex >= _questions.size - 1
        android.util.Log.d("QuizViewModel", "submitAnswer: isLastQuestion=$isLastQuestion, currentIndex=${_currentQuestionIndex}, totalQuestions=${_questions.size}")
        
        // If this is the last question, show "Finish" button instead of "Next"
        if (isLastQuestion) {
            android.util.Log.d("QuizViewModel", "*** LAST QUESTION DETECTED ***")
            android.util.Log.d("QuizViewModel", "Last question answered - showing Finish button")
            android.util.Log.d("QuizViewModel", "Current index: ${_currentQuestionIndex}, Questions size: ${_questions.size}")
            
            // Create feedback state for last question - show button as "Finish"
            val feedback = FeedbackState(
                isCorrect = isCorrect,
                correctAnswer = question.correct,
                explanation = if (_currentLanguage == "en") question.questionEn else question.questionDe,
                showNextButton = true, // Show button as "Finish" on last question
                autoProgressDelay = null // No auto-progress - user clicks "Finish" button
            )
            
            // Emit updated state with feedback
            emitSuccessState(feedbackState = feedback)
            
            android.util.Log.d("QuizViewModel", "Feedback state emitted with Finish button - waiting for user to click Finish")
        } else {
            // Create feedback state for non-last questions
            val feedback = FeedbackState(
                isCorrect = isCorrect,
                correctAnswer = question.correct,
                explanation = if (_currentLanguage == "en") question.questionEn else question.questionDe,
                showNextButton = !isCorrect, // Show button only for incorrect answers
                autoProgressDelay = if (isCorrect) 3000L else null // Auto-progress after 3s if correct
            )
            
            android.util.Log.d("QuizViewModel", "Feedback created: autoProgressDelay=${feedback.autoProgressDelay}")
            
            // Emit updated state with feedback
            emitSuccessState(feedbackState = feedback)
        }
    }
    
    /**
     * Move to next question
     */
    fun nextQuestion() {
        android.util.Log.d("QuizViewModel", "nextQuestion() called: currentIndex=${_currentQuestionIndex}, totalQuestions=${_questions.size}")

        if (_currentQuestionIndex < _questions.size - 1) {
            _currentQuestionIndex++
            _selectedAnswer = null
            _isAnswerSubmitted = false

            // Check if we have a prefetched next question
            if (_nextQuestion != null && _currentQuestionIndex < _questions.size) {
                // Use prefetched question if available and correct index
                val expectedIndex = _currentQuestionIndex
                if (_nextQuestion?.id == _questions.getOrNull(expectedIndex)?.id) {
                    android.util.Log.d("QuizViewModel", "Using prefetched question for index $expectedIndex")
                }
                _nextQuestion = null // Reset prefetch
            }

            // Emit updated state
            emitSuccessState()

            // Backward compatibility (extra safety with getOrNull)
            _currentQuestionIndexLiveData.value = _currentQuestionIndex
            _currentQuestionLiveData.value = _questions.getOrNull(_currentQuestionIndex)

            // Prefetch next question in background
            prefetchNextQuestion()
        } else {
            // Quiz completed - this is the last question
            android.util.Log.d("QuizViewModel", "Last question answered, finishing quiz...")
            android.util.Log.d("QuizViewModel", "Calling finishQuiz() from nextQuestion()")
            finishQuiz()
        }
    }
    
    /**
     * Prefetch next question in background for faster loading
     */
    private fun prefetchNextQuestion() {
        val nextIndex = _currentQuestionIndex + 1
        if (nextIndex < _questions.size && _nextQuestion == null) {
            viewModelScope.launch {
                try {
                    // Get next question from already loaded questions (no DB call needed)
                    _questions.getOrNull(nextIndex)?.let { nextQuestion ->
                        _nextQuestion = nextQuestion
                        android.util.Log.d("QuizViewModel", "Prefetched question ${nextQuestion.id} for index $nextIndex")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("QuizViewModel", "Error prefetching next question", e)
                }
            }
        }
    }

    /**
     * Go to previous question (for review)
     */
    fun previousQuestion() {
        if (_currentQuestionIndex > 0) {
            _currentQuestionIndex--
            _selectedAnswer = null
            _isAnswerSubmitted = false
            
            // Emit updated state
            emitSuccessState()
            
            // Backward compatibility
            _currentQuestionIndexLiveData.value = _currentQuestionIndex
            _currentQuestionLiveData.value = _questions[_currentQuestionIndex]
        }
    }
    
    /**
     * Toggle language between English and German
     */
    fun toggleLanguage() {
        _currentLanguage = if (_currentLanguage == "en") "de" else "en"
        _languageLiveData.value = _currentLanguage
        emitSuccessState()
    }
    
    /**
     * Set language
     */
    fun setLanguage(lang: String) {
        _currentLanguage = lang
        _languageLiveData.value = lang
        emitSuccessState()
    }
    
    /**
     * Finish quiz and save results
     * Uses atomic transaction to ensure data consistency
     */
    private fun finishQuiz() {
        android.util.Log.d("QuizViewModel", "*** finishQuiz() ENTRY POINT REACHED ***")
        android.util.Log.d("QuizViewModel", "finishQuiz() called - starting quiz finish process")
        
        // Guard: prevent multiple calls
        if (isFinishingQuiz) {
            android.util.Log.w("QuizViewModel", "finishQuiz() already in progress (isFinishingQuiz=$isFinishingQuiz), ignoring duplicate call")
            android.util.Log.w("QuizViewModel", "This is expected if finishQuiz() was called multiple times - first call will complete")
            return
        }
        isFinishingQuiz = true
        android.util.Log.d("QuizViewModel", "Guard set: isFinishingQuiz = true, proceeding with finishQuiz()")
        
        viewModelScope.launch {
            try {
                android.util.Log.d("QuizViewModel", "*** INSIDE finishQuiz() COROUTINE ***")
                android.util.Log.d("QuizViewModel", "=== FINISHING QUIZ ===")
                
                val totalQuestions = _questions.size
                val correct = _correctAnswersCount
                val wrong = _wrongAnswersCount
                val percentage = if (totalQuestions > 0) {
                    (correct.toFloat() / totalQuestions.toFloat()) * 100f
                } else {
                    0f
                }
                
                android.util.Log.d("QuizViewModel", "Total: $totalQuestions, Correct: $correct, Wrong: $wrong")
                
                val duration = ((System.currentTimeMillis() - sessionStartTime) / 60000).toInt()
                
                val session = QuizSession(
                    userId = authManager.getUserId() ?: "",
                    timestamp = System.currentTimeMillis(),
                    totalQuestions = totalQuestions,
                    correctAnswers = correct,
                    wrongAnswers = wrong,
                    percentage = percentage,
                    durationMinutes = duration,
                    language = _currentLanguage,
                    isCompleted = true // Quiz was fully completed
                )
                
                // ATOMIC TRANSACTION: Save session and all answers together
                android.util.Log.d("QuizViewModel", "Saving session and ${userAnswers.size} answers atomically...")
                currentSessionId = repository.saveSessionWithAnswers(session, userAnswers)
                android.util.Log.d("QuizViewModel", "Session and answers saved atomically with ID: $currentSessionId")
                
                // Save session to server (for both authenticated and guest users)
                android.util.Log.d("QuizViewModel", "Calling saveSessionToServer...")
                saveSessionToServer(session)
                android.util.Log.d("QuizViewModel", "saveSessionToServer called (async)")
                
                // Update streak and stats
                android.util.Log.d("QuizViewModel", "Updating streak and stats...")
                repository.updateStreak()
                repository.updateDailyProgress(totalQuestions)
                repository.updateOverallStats(
                    quizCount = 1,
                    questionsAnswered = totalQuestions,
                    correctAnswers = correct,
                    timeSpentMinutes = duration
                )
                android.util.Log.d("QuizViewModel", "Streak and stats updated")
                
                // Check for achievements
                android.util.Log.d("QuizViewModel", "Checking achievements...")
                try {
                    checkAchievements(correct, totalQuestions, percentage, duration)
                    android.util.Log.d("QuizViewModel", "Achievements checked successfully")
                } catch (e: Exception) {
                    android.util.Log.e("QuizViewModel", "Error in checkAchievements", e)
                    // Continue even if achievements check fails
                }
                
                // Emit QuizFinished state
                android.util.Log.d("QuizViewModel", "Emitting QuizFinished state...")
                _uiState.value = QuizUiState.QuizFinished(
                    totalQuestions = totalQuestions,
                    correctAnswers = correct,
                    wrongAnswers = wrong,
                    accuracy = percentage
                )
                android.util.Log.d("QuizViewModel", "QuizFinished state emitted")
                
                // If demo mode, check with server if upgrade prompt should be shown
                if (isDemoMode) {
                    android.util.Log.d("QuizViewModel", "Demo mode: Checking with server for upgrade prompt...")
                    checkGuestUpgradePrompt()
                    android.util.Log.d("QuizViewModel", "Upgrade prompt check completed")
                }
                
                // Show summary dialog
                android.util.Log.d("QuizViewModel", "*** ABOUT TO SET _showSummaryDialog.postValue(true) ***")
                _showSummaryDialog.postValue(true)
                android.util.Log.d("QuizViewModel", "*** _showSummaryDialog.postValue(true) CALLED ***")
                android.util.Log.d("QuizViewModel", "Current _showSummaryDialog value: ${_showSummaryDialog.value}")
                
                android.util.Log.d("QuizViewModel", "=== QUIZ FINISHED ===")
                
                // Mark quiz as finished to prevent saveSessionOnExit from updating stats again
                isQuizFinished = true
                android.util.Log.d("QuizViewModel", "Quiz marked as finished - saveSessionOnExit will be skipped")
            } catch (e: Exception) {
                android.util.Log.e("QuizViewModel", "*** ERROR IN finishQuiz() ***", e)
                android.util.Log.e("QuizViewModel", "Error finishing quiz", e)
                _uiState.value = QuizUiState.Error("Error saving: ${e.message}")
                e.printStackTrace()
            } finally {
                isFinishingQuiz = false
                android.util.Log.d("QuizViewModel", "finishQuiz() completed, guard reset")
            }
        }
    }
    
    fun resetSummaryDialog() {
        _showSummaryDialog.value = false
    }
    
    /**
     * Save session on abort/exit (called when user leaves quiz)
     */
    fun saveSessionOnExit() {
        if (currentSessionId == 0L) {
            android.util.Log.d("QuizViewModel", "No session to save on exit")
            return
        }
        
        viewModelScope.launch {
            try {
                android.util.Log.d("QuizViewModel", "=== SAVING SESSION ON EXIT ===")
                
                val totalAnswered = _correctAnswersCount + _wrongAnswersCount
                val correct = _correctAnswersCount
                val wrong = _wrongAnswersCount
                val percentage = if (totalAnswered > 0) {
                    (correct.toFloat() / totalAnswered.toFloat()) * 100f
                } else {
                    0f
                }
                
                val duration = ((System.currentTimeMillis() - sessionStartTime) / 60000).toInt()
                
                // Update the existing session as incomplete (aborted)
                val session = QuizSession(
                    id = currentSessionId,
                    userId = authManager.getUserId() ?: "",
                    timestamp = sessionStartTime,
                    totalQuestions = _questions.size,
                    correctAnswers = correct,
                    wrongAnswers = wrong,
                    percentage = percentage, // Percentage of answered questions, not total
                    durationMinutes = duration,
                    language = _currentLanguage,
                    isCompleted = false // Quiz was aborted, not fully completed
                )
                
                android.util.Log.d("QuizViewModel", "Updating session $currentSessionId: $correct correct, $wrong wrong")
                repository.updateSession(session)
                
                // Save all user answers
                userAnswers.forEach { answer ->
                    repository.saveAnswer(answer.copy(sessionId = currentSessionId))
                }
                
                // *** WICHTIG: Stats auch bei Abbruch aktualisieren! ***
                repository.updateOverallStats(
                    quizCount = 1,
                    questionsAnswered = totalAnswered,
                    correctAnswers = correct,
                    timeSpentMinutes = duration
                )
                
                android.util.Log.d("QuizViewModel", "Session saved on exit with $correct correct answers added to stats")

            } catch (e: Exception) {
                android.util.Log.e("QuizViewModel", "Error saving session on exit", e)
            }
        }
    }
    
    /**
     * Trigger summary dialog (for manual abort)
     */
    fun showSummaryOnAbort() {
        saveSessionOnExit()
        _showSummaryDialog.postValue(true)
    }
    
    /**
     * Reset quiz
     */
    /**
     * Save session to server (for both authenticated and guest users)
     */
    private fun saveSessionToServer(session: QuizSession) {
        viewModelScope.launch {
            try {
                val apiService = RetrofitClient.getApiService()
                val deviceId = DeviceUtils.getDeviceId(getApplication())
                
                // Convert to DTO - server expects session object with all fields
                // The API sends deviceId as query param, but we need to send session in body
                // Server will accept both formats now
                val sessionDto = alie.info.newmultichoice.api.models.QuizSessionDto(
                    sessionId = null,
                    totalQuestions = session.totalQuestions,
                    correctAnswers = session.correctAnswers,
                    wrongAnswers = session.wrongAnswers,
                    percentage = session.percentage,
                    completedAt = null,
                    isCompleted = session.isCompleted
                )
                
                android.util.Log.d("QuizViewModel", "Saving session to server: deviceId=$deviceId, totalQuestions=${session.totalQuestions}")
                
                // API sends deviceId as query param, session as body
                // Server now accepts both formats
                val response = apiService.saveQuizSession(sessionDto, deviceId)
                
                if (response.isSuccessful) {
                    android.util.Log.d("QuizViewModel", "Session saved to server successfully")
                } else {
                    android.util.Log.w("QuizViewModel", "Failed to save session to server: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("QuizViewModel", "Error saving session to server", e)
                // Don't fail the quiz if server save fails
            }
        }
    }
    
    /**
     * Check with server if guest user should see upgrade prompt
     */
    private fun checkGuestUpgradePrompt() {
        viewModelScope.launch {
            try {
                val apiService = RetrofitClient.getApiService()
                val deviceId = DeviceUtils.getDeviceId(getApplication())
                val request = CheckGuestUpgradeRequest(deviceId)
                
                android.util.Log.d("QuizViewModel", "Calling checkGuestUpgradePrompt API for device: $deviceId")
                
                val response = apiService.checkGuestUpgradePrompt(request)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        android.util.Log.d("QuizViewModel", "Server response: shouldShowUpgradePrompt=${body.shouldShowUpgradePrompt}, totalQuestionsAnswered=${body.totalQuestionsAnswered}")
                        _shouldShowUpgradePrompt.postValue(body.shouldShowUpgradePrompt)
                    } else {
                        android.util.Log.w("QuizViewModel", "Server response body is null")
                        _shouldShowUpgradePrompt.postValue(false)
                    }
                } else {
                    android.util.Log.e("QuizViewModel", "API call failed: ${response.code()}")
                    _shouldShowUpgradePrompt.postValue(false)
                }
            } catch (e: Exception) {
                android.util.Log.e("QuizViewModel", "Error checking guest upgrade prompt", e)
                // On error, don't show upgrade prompt
                _shouldShowUpgradePrompt.postValue(false)
            }
        }
    }
    
    fun resetQuiz() {
        _currentQuestionIndex = 0
        _selectedAnswer = null
        _isAnswerSubmitted = false
        _correctAnswersCount = 0
        _wrongAnswersCount = 0
        sessionStartTime = System.currentTimeMillis()
        currentSessionId = 0
        userAnswers.clear()
        
        // Emit loading state, then reload questions
        _uiState.value = QuizUiState.Loading
        
        // Backward compatibility
        _currentQuestionIndexLiveData.value = 0
        _currentQuestionLiveData.value = _questions.firstOrNull()
    }
    
    /**
     * Get current progress as percentage
     */
    fun getProgress(): Int {
        val total = _questions.size
        val current = _currentQuestionIndex + 1
        return if (total > 0) {
            ((current.toFloat() / total.toFloat()) * 100).toInt()
        } else {
            0
        }
    }
}

