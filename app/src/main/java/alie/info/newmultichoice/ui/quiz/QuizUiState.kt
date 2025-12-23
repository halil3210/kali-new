package alie.info.newmultichoice.ui.quiz

import alie.info.newmultichoice.data.Question

/**
 * Represents the UI state of the Quiz screen
 * Using sealed interface ensures all possible states are handled
 */
sealed interface QuizUiState {
    
    /**
     * Initial loading state
     */
    data object Loading : QuizUiState
    
    /**
     * Quiz is ready and active
     */
    data class Success(
        val currentQuestion: Question,
        val currentQuestionIndex: Int,
        val totalQuestions: Int,
        val selectedAnswer: String?,
        val isAnswerSubmitted: Boolean,
        val correctAnswersCount: Int,
        val wrongAnswersCount: Int,
        val quizCompleted: Boolean,
        val language: String,
        // Feedback state
        val feedbackState: FeedbackState? = null
    ) : QuizUiState
    
    /**
     * Error state with message
     */
    data class Error(
        val message: String
    ) : QuizUiState
    
    /**
     * Quiz completed - show summary
     */
    data class QuizFinished(
        val totalQuestions: Int,
        val correctAnswers: Int,
        val wrongAnswers: Int,
        val accuracy: Float
    ) : QuizUiState
}

/**
 * Represents feedback state after answer submission
 */
data class FeedbackState(
    val isCorrect: Boolean,
    val correctAnswer: String,
    val explanation: String?,
    val showNextButton: Boolean,
    val autoProgressDelay: Long? = null // milliseconds until auto-progress (null = manual)
)

