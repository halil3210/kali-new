package alie.info.newmultichoice.ui.quiz

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.RadioButton
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import alie.info.newmultichoice.R
import alie.info.newmultichoice.data.PreferencesManager
import alie.info.newmultichoice.data.Question
import alie.info.newmultichoice.databinding.FragmentQuizBinding
import alie.info.newmultichoice.databinding.DialogQuizSummaryBinding
import alie.info.newmultichoice.databinding.DialogUpgradePromptBinding
import alie.info.newmultichoice.databinding.DialogLoadingBinding
import alie.info.newmultichoice.auth.AuthActivity
import alie.info.newmultichoice.auth.AuthManager
import alie.info.newmultichoice.utils.HapticFeedbackHelper
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: QuizViewModel
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var hapticHelper: HapticFeedbackHelper
    private lateinit var authManager: AuthManager
    
    private val optionButtons = mutableListOf<RadioButton>()
    
    private var hapticEnabled = true
    private val autoNextHandler = Handler(Looper.getMainLooper())
    
    // Dialog reference to dismiss on destroy
    private var summaryDialog: androidx.appcompat.app.AlertDialog? = null
    
    // Local state for backward compatibility
    private var isAnswerSubmittedLocal = false
    private var totalQuestionsLocal = 0
    private var correctAnswersLocal = 0
    private var wrongAnswersLocal = 0
    private var selectedAnswerLocal: String? = null
    
    // Navigation arguments
    private val args: QuizFragmentArgs by navArgs()
    
    // Demo mode flag
    private var isDemoMode = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[QuizViewModel::class.java]
        preferencesManager = PreferencesManager.getInstance(requireContext())
        hapticHelper = HapticFeedbackHelper.getInstance(requireContext())
        authManager = AuthManager.getInstance(requireContext())
        
        // Check for demo mode from activity intent OR guest mode
        val intentDemoMode = activity?.intent?.getBooleanExtra("demo_mode", false) ?: false
        val isGuest = authManager.isGuestMode()
        isDemoMode = intentDemoMode || isGuest
        val demoLimit = activity?.intent?.getIntExtra("demo_questions", 5) ?: 5
        
        alie.info.newmultichoice.utils.Logger.d("QuizFragment", "Demo mode: $isDemoMode (intent: $intentDemoMode, guest: $isGuest)")
        
        // Load preferences
        loadPreferences()
        
        // Start quiz with specified mode
        val practiceMode = args.practiceMode
        viewModel.startQuiz(practiceMode, isDemoMode, demoLimit)
        
        setupUI()
        observeViewModel()
        setupBackPressHandler()
        
        // Animate entrance
        animateEntrance()
        
        return binding.root
    }
    
    private fun setupBackPressHandler() {
        // Handle back button press
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                alie.info.newmultichoice.utils.Logger.d("QuizFragment", "Back pressed - showing exit confirmation")
                // Show confirmation dialog first, like in Exams
                showExitConfirmation()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }
    
    private fun showExitConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Leave Marathon?")
            .setMessage("Do you really want to abort the Marathon?\n\nYour answers so far will be saved and shown in the summary.")
            .setPositiveButton("Continue", null) // Stay in quiz
            .setNegativeButton("Exit and Show Summary") { _, _ ->
                // Show summary with current stats
                viewModel.showSummaryOnAbort()
            }
            .show()
    }
    
    private fun loadPreferences() {
        lifecycleScope.launch {
            // Load language
            val language = preferencesManager.languageFlow.first()
            viewModel.setLanguage(language)
            
            // Load haptic setting
            hapticEnabled = preferencesManager.hapticFeedbackFlow.first()
        }
    }
    
    private fun animateEntrance() {
        // Animate question text
        binding.questionText.alpha = 0f
        binding.questionText.translationY = -50f
        binding.questionText.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(OvershootInterpolator())
            .start()
        
        // Animate options smoothly one after another
        optionButtons.forEachIndexed { index, button ->
            button.alpha = 0f
            button.translationY = 30f
            button.scaleX = 0.9f
            button.scaleY = 0.9f
            
            button.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setStartDelay((index * 150).toLong()) // 150ms delay between each option
                .setInterpolator(OvershootInterpolator())
                .start()
        }
    }
    
    private fun setupUI() {
        // Collect option buttons
        optionButtons.addAll(
            listOf(
                binding.optionA,
                binding.optionB,
                binding.optionC,
                binding.optionD
            )
        )
        
        // Setup click listeners for radio buttons - direct submit
        optionButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (!isAnswerSubmittedLocal) {
                    selectAnswer(index)
                    // Automatically submit answer
                    viewModel.submitAnswer()
                    isAnswerSubmittedLocal = true
                }
            }
        }
        
        // Next/Finish button - changes behavior on last question
        binding.nextButton.setOnClickListener {
            // Check if this is the last question
            val currentIndex = viewModel.currentQuestionIndex.value ?: 0
            val isLastQuestion = currentIndex >= totalQuestionsLocal - 1
            
            if (isLastQuestion) {
                alie.info.newmultichoice.utils.Logger.d("QuizFragment", "Finish button clicked - calling finishQuiz()")
                // Call finishQuiz directly (we need to expose it or use a different approach)
                // Since finishQuiz is private, we'll use nextQuestion which calls finishQuiz for last question
                viewModel.nextQuestion() // This will call finishQuiz() internally for last question
            } else {
                alie.info.newmultichoice.utils.Logger.d("QuizFragment", "Next button clicked - calling nextQuestion()")
                viewModel.nextQuestion()
            }
        }
        
        // Previous button
        binding.previousButton.setOnClickListener {
            viewModel.previousQuestion()
        }
        
        // Language toggle
        binding.languageToggleButton.setOnClickListener {
            viewModel.toggleLanguage()
        }
    }
    
    private fun selectAnswer(index: Int) {
        val answer = when (index) {
            0 -> "A"
            1 -> "B"
            2 -> "C"
            3 -> "D"
            else -> ""
        }
        viewModel.selectAnswer(answer)
        
        // Haptic feedback
        if (hapticEnabled) {
            hapticHelper.mediumFeedback(optionButtons[index])
        }
    }
    
    private fun observeViewModel() {
        // Observe modern UI state (StateFlow)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                handleUiState(state)
            }
        }
        
        // Observe current question (backward compatibility)
        viewModel.currentQuestion.observe(viewLifecycleOwner) { question ->
            question?.let {
                displayQuestion(it)
            }
        }
        
        // Observe summary dialog trigger
        viewModel.showSummaryDialog.observe(viewLifecycleOwner) { show ->
            alie.info.newmultichoice.utils.Logger.d("QuizFragment", "*** OBSERVER TRIGGERED: showSummaryDialog changed to: $show ***")
            alie.info.newmultichoice.utils.Logger.d("QuizFragment", "showSummaryDialog changed: $show, isDemoMode: $isDemoMode")
            if (show) {
                alie.info.newmultichoice.utils.Logger.d("QuizFragment", "*** SHOW IS TRUE - CALLING showQuizSummaryDialog() ***")
                showQuizSummaryDialog()
                viewModel.resetSummaryDialog()
            } else {
                alie.info.newmultichoice.utils.Logger.d("QuizFragment", "*** SHOW IS FALSE - NOT SHOWING DIALOG ***")
            }
        }
        
        // Observe question index
        viewModel.currentQuestionIndex.observe(viewLifecycleOwner) { index ->
            // SOFORT UI zurücksetzen beim Fragenwechsel
            resetUI()
            isAnswerSubmittedLocal = false
            
            binding.questionCounter.text = getString(R.string.question_counter, index + 1, totalQuestionsLocal)
            
            // Reset progress bar color to default when loading new question
            context?.let { ctx ->
                binding.progressIndicator.setIndicatorColor(
                    ContextCompat.getColor(ctx, R.color.md_theme_light_primary)
                )
            }
        }
        
        // Observe language
        viewModel.language.observe(viewLifecycleOwner) { language ->
            // Update button text to show current language
            binding.languageToggleButton.text = if (language == "en") "EN" else "DE"
            // Update question display
            viewModel.currentQuestion.value?.let { displayQuestion(it) }
        }
        
        // Old observers removed - now handled by modern UIState
        
        // Old loading and error observers removed - now handled by modern UIState
    }
    
    /**
     * Handle feedback state (STATE-DRIVEN UI)
     * All feedback logic is now driven by ViewModel state
     */
    private fun handleFeedbackState(feedback: FeedbackState, question: Question, selectedAnswer: String?) {
        // Animate progress bar
        animateProgressBarForAnswer(feedback.isCorrect)
        
        // Show feedback card
        binding.feedbackCard.visibility = View.VISIBLE
        
        // Animate feedback entrance
        binding.feedbackCard.alpha = 0f
        binding.feedbackCard.translationY = 50f
        binding.feedbackCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(OvershootInterpolator())
            .start()
        
        // Haptic feedback
        if (hapticEnabled) {
            if (feedback.isCorrect) {
                hapticHelper.successFeedback(binding.feedbackCard)
            } else {
                hapticHelper.errorFeedback(binding.feedbackCard)
            }
        }
        
        // Set feedback content
        if (feedback.isCorrect) {
            binding.feedbackTitle.text = getString(R.string.correct) + " ✅"
            binding.feedbackMessage.text = getString(R.string.correct_answer_message)
            binding.feedbackCard.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.success_green)
            )
        } else {
            binding.feedbackTitle.text = getString(R.string.incorrect) + " ❌"
            binding.feedbackMessage.text = getString(R.string.incorrect_answer_message, feedback.correctAnswer)
            binding.feedbackCard.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.error_red)
            )
        }
        
        // Highlight wrong answer in red (if incorrect)
        if (!feedback.isCorrect && selectedAnswer != null) {
            val selectedIndex = when (selectedAnswer) {
                "A" -> 0; "B" -> 1; "C" -> 2; "D" -> 3
                else -> -1
            }
            if (selectedIndex >= 0) {
                animateWrongAnswer(selectedIndex)
            }
        }
        
        // Highlight correct answer in green (always show)
        val correctIndex = when (feedback.correctAnswer) {
            "A" -> 0; "B" -> 1; "C" -> 2; "D" -> 3
            else -> -1
        }
        if (correctIndex >= 0) {
            animateCorrectAnswer(correctIndex)
        }
        
        // Next button is always visible now
        // (removed visibility logic)
        
        // Auto-progress if specified (only for non-last questions)
        feedback.autoProgressDelay?.let { delay ->
            // Check if this is the last question - don't auto-progress on last question
            val currentIndex = viewModel.currentQuestionIndex.value ?: 0
            val isLastQuestion = currentIndex >= totalQuestionsLocal - 1
            
            if (!isLastQuestion) {
                alie.info.newmultichoice.utils.Logger.d("QuizFragment", "Auto-progress scheduled: delay=$delay ms")
                autoNextHandler.postDelayed({
                    alie.info.newmultichoice.utils.Logger.d("QuizFragment", "Auto-progress executing: calling nextQuestion()")
                    viewModel.nextQuestion()
                }, delay)
            } else {
                alie.info.newmultichoice.utils.Logger.d("QuizFragment", "Last question - no auto-progress, user must click Finish button")
            }
        } ?: run {
            alie.info.newmultichoice.utils.Logger.d("QuizFragment", "No auto-progress: autoProgressDelay is null")
        }
        
        // Update Next button text based on current question index
        val currentIndex = viewModel.currentQuestionIndex.value ?: 0
        val isLastQuestion = currentIndex >= totalQuestionsLocal - 1
        binding.nextButton.text = if (isLastQuestion) {
            getString(R.string.finish_quiz)
        } else {
            getString(R.string.next_question)
        }
        
        // Scroll to bottom if incorrect
        if (!feedback.isCorrect) {
            binding.root.post {
                binding.root.fullScroll(View.FOCUS_DOWN)
            }
        }
    }
    
    /**
     * Handle modern UI state changes
     */
    private fun handleUiState(state: QuizUiState) {
        when (state) {
            is QuizUiState.Loading -> {
                // Show loading indicator
                binding.loadingIndicator.visibility = View.VISIBLE
                binding.questionText.visibility = View.GONE
                binding.optionsRadioGroup.visibility = View.GONE
                binding.feedbackCard.visibility = View.GONE
            }
            
            is QuizUiState.Success -> {
                // Hide loading, show content
                binding.loadingIndicator.visibility = View.GONE
                binding.questionText.visibility = View.VISIBLE
                binding.optionsRadioGroup.visibility = View.VISIBLE
                
                // Store locally for backward compatibility
                totalQuestionsLocal = state.totalQuestions
                correctAnswersLocal = state.correctAnswersCount
                wrongAnswersLocal = state.wrongAnswersCount
                selectedAnswerLocal = state.selectedAnswer
                
                // Update progress indicator
                val progress = if (state.totalQuestions > 0) {
                    ((state.currentQuestionIndex + 1) * 100) / state.totalQuestions
                } else {
                    0
                }
                binding.progressIndicator.setProgressCompat(progress, true)
                
                // Update question counter
                binding.questionCounter.text = getString(
                    R.string.question_counter,
                    state.currentQuestionIndex + 1,
                    state.totalQuestions
                )
                
                // Update navigation buttons visibility
                binding.previousButton.isEnabled = state.currentQuestionIndex > 0
                binding.previousButton.alpha = if (state.currentQuestionIndex > 0) 1.0f else 0.3f
                
                // Update Next button text - show "Finish" on last question
                val isLastQuestion = state.currentQuestionIndex >= state.totalQuestions - 1
                binding.nextButton.text = if (isLastQuestion) {
                    getString(R.string.finish_quiz)
                } else {
                    getString(R.string.next_question)
                }
                
                // Handle answer submission with feedback state
                if (state.isAnswerSubmitted && !isAnswerSubmittedLocal) {
                    isAnswerSubmittedLocal = true
                    optionButtons.forEach { it.isEnabled = false }
                }
                
                // Handle feedback state (STATE-DRIVEN UI)
                state.feedbackState?.let { feedback ->
                    handleFeedbackState(feedback, state.currentQuestion, state.selectedAnswer)
                }
            }
            
            is QuizUiState.Error -> {
                // Show error message
                binding.loadingIndicator.visibility = View.GONE
                binding.feedbackCard.visibility = View.VISIBLE
                binding.feedbackTitle.text = getString(R.string.error)
                binding.feedbackMessage.text = state.message
                context?.let { ctx ->
                    binding.feedbackCard.setCardBackgroundColor(
                        ContextCompat.getColor(ctx, R.color.error_red)
                    )
                }
                
                // Optionally navigate back after showing error (with safety check)
                binding.root.postDelayed({
                    if (isAdded && !isDetached && view != null) {
                        try {
                            findNavController().navigateUp()
                        } catch (e: IllegalStateException) {
                            android.util.Log.e("QuizFragment", "Navigation failed after error", e)
                        }
                    }
                }, 3000)
            }
            
            is QuizUiState.QuizFinished -> {
                // Quiz completed - summary dialog will be shown via LiveData observer
                alie.info.newmultichoice.utils.Logger.d("QuizFragment", "Quiz finished: ${state.correctAnswers}/${state.totalQuestions}, isDemoMode: $isDemoMode")
                // Upgrade prompt will be shown based on server response via LiveData observer
            }
        }
    }
    
    private fun displayQuestion(question: Question) {
        val language = viewModel.language.value ?: "en"
        
        if (language == "en") {
            binding.questionText.text = question.questionEn
            binding.optionA.text = "A) ${question.optionAEn}"
            binding.optionB.text = "B) ${question.optionBEn}"
            binding.optionC.text = "C) ${question.optionCEn}"
            binding.optionD.text = "D) ${question.optionDEn}"
        } else {
            binding.questionText.text = question.questionDe
            binding.optionA.text = "A) ${question.optionADe}"
            binding.optionB.text = "B) ${question.optionBDe}"
            binding.optionC.text = "C) ${question.optionCDe}"
            binding.optionD.text = "D) ${question.optionDDe}"
        }
    }
    
    // Old showFeedback() removed - replaced by handleFeedbackState() (STATE-DRIVEN UI)
    
    private fun resetUI() {
        // Hide feedback card
        binding.feedbackCard.visibility = View.GONE
        binding.feedbackCard.alpha = 1f
        binding.feedbackCard.translationY = 0f

        // Reset all option buttons
        optionButtons.forEach { button ->
            button.isChecked = false
            button.isEnabled = true
            button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            button.buttonTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), android.R.color.white)
            )
            button.alpha = 1f
            button.translationY = 0f
            button.scaleX = 1f
            button.scaleY = 1f
            button.clearAnimation()
        }

        // Next button stays visible (no hiding)

        // Reset progress bar color to default
        binding.progressIndicator.setIndicatorColor(
            ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary)
        )
        binding.progressIndicator.setProgressCompat(0, false)
    }
    
    private fun animateProgressBarForAnswer(isCorrect: Boolean) {
        // Reset progress to 0
        binding.progressIndicator.setProgressCompat(0, false)

        // Set color based on correctness
        val color = if (isCorrect) {
            ContextCompat.getColor(requireContext(), R.color.success_green)
        } else {
            ContextCompat.getColor(requireContext(), R.color.error_red)
        }
        binding.progressIndicator.setIndicatorColor(color)

        // Animate from 0 to 100
        binding.progressIndicator.setProgressCompat(100, true)

        // Reset to normal color after 2 seconds
        binding.root.postDelayed({
            binding.progressIndicator.setIndicatorColor(
                ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary)
            )
        }, 2000)
    }
    
    private fun animateCorrectAnswer(index: Int) {
        // Keep white color for better readability with green glow
        optionButtons[index].setTextColor(
            ContextCompat.getColor(requireContext(), R.color.success_green_light)
        )
        
        // Change radio button tint to green
        optionButtons[index].buttonTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.success_green)
        )
        
        // Pulse animation
        val scaleX = ObjectAnimator.ofFloat(optionButtons[index], "scaleX", 1f, 1.05f, 1f)
        val scaleY = ObjectAnimator.ofFloat(optionButtons[index], "scaleY", 1f, 1.05f, 1f)
        scaleX.duration = 400
        scaleY.duration = 400
        scaleX.start()
        scaleY.start()
    }
    
    private fun animateWrongAnswer(index: Int) {
        // Keep white color for better readability with red glow
        optionButtons[index].setTextColor(
            ContextCompat.getColor(requireContext(), R.color.error_red_light)
        )
        
        // Change radio button tint to red
        optionButtons[index].buttonTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.error_red)
        )
        
        // Shake animation
        val shake = ObjectAnimator.ofFloat(
            optionButtons[index],
            "translationX",
            0f, -25f, 25f, -25f, 25f, -15f, 15f, -5f, 5f, 0f
        )
        shake.duration = 500
        shake.start()
    }
    
    private fun navigateToResults() {
        val action = QuizFragmentDirections.actionQuizFragmentToResultFragment(
            totalQuestions = totalQuestionsLocal,
            correctAnswers = correctAnswersLocal,
            wrongAnswers = wrongAnswersLocal
        )
        findNavController().navigate(action)
    }

    private fun showQuizSummaryDialog() {
        alie.info.newmultichoice.utils.Logger.d("QuizFragment", "=== CREATING SUMMARY DIALOG ===")
        alie.info.newmultichoice.utils.Logger.d("QuizFragment", "isDemoMode: $isDemoMode, isAdded: $isAdded, isDetached: $isDetached")
        
        if (!isAdded || isDetached || view == null) {
            android.util.Log.e("QuizFragment", "Fragment not attached, cannot show summary dialog")
            return
        }
        
        val dialogBinding = DialogQuizSummaryBinding.inflate(layoutInflater)
        
        val correct = correctAnswersLocal
        val wrong = wrongAnswersLocal
        val total = correct + wrong
        val accuracy = if (total > 0) (correct.toDouble() / total.toDouble()) * 100.0 else 0.0
        
        alie.info.newmultichoice.utils.Logger.d("QuizFragment", "Dialog stats: $correct / $total (${accuracy}%)")
        
        dialogBinding.scoreText.text = "$correct / $total"
        dialogBinding.accuracyText.text = getString(R.string.session_accuracy, accuracy)
        dialogBinding.correctCountText.text = getString(R.string.questions_correct, correct)
        dialogBinding.incorrectCountText.text = getString(R.string.questions_incorrect, wrong)
        
        // Update close button text for demo mode
        if (isDemoMode) {
            dialogBinding.closeButton.text = "Continue"
        }
        
        summaryDialog = MaterialAlertDialogBuilder(requireContext(), R.style.TransparentDialog)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()
        
        summaryDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialogBinding.closeButton.setOnClickListener {
            alie.info.newmultichoice.utils.Logger.d("QuizFragment", "Close button clicked, isDemoMode: $isDemoMode")
            
            // Safely dismiss dialog
            try {
                summaryDialog?.dismiss()
            } catch (e: Exception) {
                android.util.Log.e("QuizFragment", "Error dismissing summary dialog", e)
            }
            summaryDialog = null
            
            // Use viewLifecycleOwner to ensure we're on the correct lifecycle
            viewLifecycleOwner.lifecycleScope.launch {
                kotlinx.coroutines.delay(200) // Small delay to ensure dialog is dismissed
                
                // Check if fragment is still valid before navigation
                if (!isAdded || isDetached || view == null) {
                    android.util.Log.w("QuizFragment", "Fragment no longer attached, skipping navigation")
                    return@launch
                }
                
                // For demo mode, show upgrade prompt after summary dialog
                if (isDemoMode) {
                    alie.info.newmultichoice.utils.Logger.d("QuizFragment", "Demo mode: Showing upgrade prompt after summary")
                    if (isAdded && !isDetached && view != null) {
                        try {
                            showUpgradePrompt()
                        } catch (e: Exception) {
                            android.util.Log.e("QuizFragment", "Error showing upgrade prompt", e)
                        }
                    }
                } else {
                    alie.info.newmultichoice.utils.Logger.d("QuizFragment", "Not demo mode, navigating to home")
                    // Navigate back to home - only if fragment is still attached
                    if (isAdded && !isDetached && view != null) {
                        try {
                            findNavController().navigate(R.id.nav_home)
                        } catch (e: IllegalStateException) {
                            android.util.Log.e("QuizFragment", "Navigation failed: Fragment not attached", e)
                        } catch (e: Exception) {
                            android.util.Log.e("QuizFragment", "Navigation error", e)
                        }
                    }
                }
            }
        }
        
        alie.info.newmultichoice.utils.Logger.d("QuizFragment", "Showing summary dialog...")
        try {
            summaryDialog?.show()
            alie.info.newmultichoice.utils.Logger.d("QuizFragment", "Summary dialog shown successfully")
        } catch (e: Exception) {
            android.util.Log.e("QuizFragment", "Error showing summary dialog", e)
        }
    }
    
    private fun showUpgradePrompt() {
        alie.info.newmultichoice.utils.Logger.d("QuizFragment", "showUpgradePrompt() called")
        
        if (!isAdded || isDetached || view == null) {
            android.util.Log.e("QuizFragment", "Fragment not attached, cannot show upgrade prompt")
            return
        }
        
        try {
            val dialogBinding = DialogUpgradePromptBinding.inflate(layoutInflater)
            
            val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.TransparentDialog)
                .setView(dialogBinding.root)
                .setCancelable(false)
                .create()
            
            dialogBinding.signUpButton.setOnClickListener {
                alie.info.newmultichoice.utils.Logger.d("QuizFragment", "Sign up button clicked")
                dialog.dismiss()
                
                // Show modern loading dialog
                val loadingBinding = alie.info.newmultichoice.databinding.DialogLoadingBinding.inflate(layoutInflater)
                loadingBinding.loadingText.text = "Loading..."
                loadingBinding.loadingSubtitle.text = "Please wait"
                
                val loadingDialog = MaterialAlertDialogBuilder(requireContext(), R.style.TransparentDialog)
                    .setView(loadingBinding.root)
                    .setCancelable(false)
                    .create()
                
                loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                loadingDialog.show()
                
                // Clear guest mode
                authManager.clearGuestMode()
                
                // Start AuthActivity and keep loading dialog visible
                val intent = Intent(requireContext(), AuthActivity::class.java)
                intent.putExtra("show_loading", true)
                startActivity(intent)
                
                // Dismiss loading dialog after Activity transition completes
                loadingBinding.root.postDelayed({
                    loadingDialog.dismiss()
                    requireActivity().finish()
                }, 1500) // Wait for Activity to fully load
            }
            
            dialogBinding.maybeLaterButton.setOnClickListener {
                alie.info.newmultichoice.utils.Logger.d("QuizFragment", "Maybe later button clicked")
                dialog.dismiss()
                // Navigate back to home
                if (isAdded && !isDetached && view != null) {
                    try {
                        findNavController().navigate(R.id.nav_home)
                    } catch (e: IllegalStateException) {
                        android.util.Log.e("QuizFragment", "Navigation failed", e)
                    }
                }
            }
            
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            alie.info.newmultichoice.utils.Logger.d("QuizFragment", "Showing upgrade prompt dialog...")
            dialog.show()
            alie.info.newmultichoice.utils.Logger.d("QuizFragment", "Upgrade prompt dialog shown")
        } catch (e: Exception) {
            android.util.Log.e("QuizFragment", "Error showing upgrade prompt", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel pending auto-next actions
        autoNextHandler.removeCallbacksAndMessages(null)
        // Dismiss any open dialogs to prevent crashes
        summaryDialog?.dismiss()
        summaryDialog = null
        _binding = null
    }
}

