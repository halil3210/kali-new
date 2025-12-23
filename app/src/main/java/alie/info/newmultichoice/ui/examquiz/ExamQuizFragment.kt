package alie.info.newmultichoice.ui.examquiz

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import alie.info.newmultichoice.R
import alie.info.newmultichoice.databinding.DialogExamFailedBinding
import alie.info.newmultichoice.databinding.DialogExamPassedBinding
import alie.info.newmultichoice.databinding.FragmentExamBinding
import alie.info.newmultichoice.ui.quiz.QuizViewModel
import alie.info.newmultichoice.ui.quiz.QuizUiState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ExamQuizFragment : Fragment() {
    
    private var _binding: FragmentExamBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: QuizViewModel
    private val args: ExamQuizFragmentArgs by navArgs()
    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 90 * 60 * 1000L // 90 minutes
    private var resultDialog: androidx.appcompat.app.AlertDialog? = null // Store dialog reference
    private val optionButtons by lazy {
        listOf(binding.optionA, binding.optionB, binding.optionC, binding.optionD)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExamBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[QuizViewModel::class.java]
        
        // Show exam explanation first
        showExamExplanation()
        
        // Handle back press
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitConfirmation()
                }
            }
        )
        
        return binding.root
    }
    
    private fun showExamExplanation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("📝 ${args.examTitle}")
            .setMessage(
                "Exam Information:\n\n" +
                "• Difficulty: ${args.examDifficulty}\n" +
                "• 80 Questions\n" +
                "• 90 Minutes Time\n" +
                "• Passed: ≤10 Errors (70/80)\n" +
                "• Failed: >10 Errors\n\n" +
                "Good luck! 💪"
            )
            .setPositiveButton("Start Exam") { _, _ ->
                startExam()
            }
            .setNegativeButton("Cancel") { _, _ ->
                findNavController().navigateUp()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun startExam() {
        setupUI()
        viewModel.loadQuestions() // Load questions (will be limited to 80 in quiz logic)
        observeViewModel()
    }
    
    private fun setupUI() {
        // Start timer
        startTimer()
        
        // Setup option clicks
        optionButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                val answer = when (index) {
                    0 -> "A"
                    1 -> "B"
                    2 -> "C"
                    3 -> "D"
                    else -> ""
                }
                viewModel.selectAnswer(answer)
                viewModel.submitAnswer()
                
                // Auto-next after 1 second
                binding.root.postDelayed({
                    viewModel.nextQuestion()
                }, 1000)
            }
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (_binding == null) return@collect
                
                when (state) {
                    is QuizUiState.Loading -> {
                        // Show loading (no progressBar in exam layout)
                    }
                    is QuizUiState.Success -> {
                        updateQuestionUI(state)
                    }
                    is QuizUiState.QuizFinished -> {
                        countDownTimer?.cancel()
                        // Check pass/fail
                        val errors = state.wrongAnswers
                        if (errors <= 10) {
                            showPassedDialog(state.totalQuestions, state.correctAnswers, errors)
                        } else {
                            showFailedDialog(state.totalQuestions, state.correctAnswers, errors)
                        }
                    }
                    is QuizUiState.Error -> {
                        // Handle error
                    }
                }
            }
        }
    }
    
    private fun updateQuestionUI(state: QuizUiState.Success) {
        val question = state.currentQuestion
        
        // Limit to 80 questions
        val maxQuestions = 80
        if (state.currentQuestionIndex >= maxQuestions) {
            // Stop after 80 questions - trigger finish manually
            countDownTimer?.cancel()
            val errors = state.wrongAnswersCount
            if (errors <= 10) {
                showPassedDialog(maxQuestions, state.correctAnswersCount, errors)
            } else {
                showFailedDialog(maxQuestions, state.correctAnswersCount, errors)
            }
            return
        }
        
        // Update question counter
        binding.questionCounter.text = "${state.currentQuestionIndex + 1}/$maxQuestions"
        
        // Update question text (use English for now)
        binding.questionText.text = question.questionEn
        
        // Update options (use English for now)
        binding.optionA.text = "A) ${question.optionAEn}"
        binding.optionB.text = "B) ${question.optionBEn}"
        binding.optionC.text = "C) ${question.optionCEn}"
        binding.optionD.text = "D) ${question.optionDEn}"
        
        // Reset button states
        optionButtons.forEach { button ->
            button.isEnabled = true
            button.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), android.R.color.transparent)
            )
        }
        
        // Show feedback if answer submitted
        state.feedbackState?.let { feedback ->
            optionButtons.forEach { it.isEnabled = false }
            
            val correctIndex = when (feedback.correctAnswer) {
                "A" -> 0
                "B" -> 1
                "C" -> 2
                "D" -> 3
                else -> -1
            }
            
            if (correctIndex >= 0) {
                optionButtons[correctIndex].backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_green_light)
                )
            }
        }
    }
    
    private fun startTimer() {
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText()
            }
            
            override fun onFinish() {
                showTimeUpDialog()
            }
        }.start()
    }
    
    private fun updateTimerText() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        binding.timerText.text = String.format("%02d:%02d", minutes, seconds)
        
        // Warning color when < 5 minutes
        if (minutes < 5) {
            binding.timerText.setTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
            )
        }
    }
    
    private fun showTimeUpDialog() {
        countDownTimer?.cancel()
        val currentState = viewModel.uiState.value
        if (currentState is QuizUiState.Success) {
            val totalQuestions = currentState.totalQuestions
            val correctAnswers = currentState.correctAnswersCount
            val errors = currentState.wrongAnswersCount
            
            viewModel.saveSessionOnExit()
            
            // Check pass/fail
            if (errors <= 10) {
                showPassedDialog(totalQuestions, correctAnswers, errors)
            } else {
                showFailedDialog(totalQuestions, correctAnswers, errors)
            }
        } else {
            findNavController().navigateUp()
        }
    }
    
    private fun showPassedDialog(totalQuestions: Int, correctAnswers: Int, errors: Int) {
        val dialogBinding = DialogExamPassedBinding.inflate(layoutInflater)
        
        val percentage = (correctAnswers.toFloat() / totalQuestions.toFloat() * 100).toInt()
        
        dialogBinding.scoreText.text = "$correctAnswers / $totalQuestions"
        dialogBinding.percentageText.text = "$percentage%"
        dialogBinding.errorsText.text = "❌ Only $errors errors!"
        
        resultDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()
        
        dialogBinding.continueButton.setOnClickListener {
            resultDialog?.dismiss()
            
            // Unlock next exam with overlay
            viewLifecycleOwner.lifecycleScope.launch {
                val unlockService = alie.info.newmultichoice.services.UnlockCheckService.getInstance(requireContext())
                unlockService.unlockNextExam(
                    fragment = this@ExamQuizFragment,
                    currentExamNumber = args.examNumber,
                    score = percentage.toFloat(),
                    errors = errors,
                    maxErrors = 10,
                    passed = true,
                    onStartNextExam = {
                        // User can start next exam from exam list
                        android.util.Log.d("ExamQuizFragment", "Next exam unlocked!")
                        if (isAdded && !isDetached && view != null) {
                            try {
                                findNavController().navigateUp()
                            } catch (e: IllegalStateException) {
                                android.util.Log.e("ExamQuizFragment", "Navigation failed", e)
                            }
                        }
                    },
                    onLater = {
                        // Go back to exam list
                        if (isAdded && !isDetached && view != null) {
                            try {
                                findNavController().navigateUp()
                            } catch (e: IllegalStateException) {
                                android.util.Log.e("ExamQuizFragment", "Navigation failed", e)
                            }
                        }
                    }
                )
            }
        }
        
        resultDialog?.show()
    }
    
    private fun showFailedDialog(totalQuestions: Int, correctAnswers: Int, errors: Int) {
        val dialogBinding = DialogExamFailedBinding.inflate(layoutInflater)
        
        val percentage = (correctAnswers.toFloat() / totalQuestions.toFloat() * 100).toInt()
        
        dialogBinding.scoreText.text = "$correctAnswers / $totalQuestions"
        dialogBinding.percentageText.text = "$percentage%"
        dialogBinding.errorsText.text = "❌ $errors errors (Max: 10)"
        
        resultDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()
        
        dialogBinding.retryButton.setOnClickListener {
            resultDialog?.dismiss()
            // Restart exam
            viewModel.loadQuestions()
            timeLeftInMillis = 90 * 60 * 1000L
            startTimer()
        }
        
        dialogBinding.backButton.setOnClickListener {
            resultDialog?.dismiss()
            // Safe navigation
            if (isAdded && !isDetached && view != null) {
                try {
                    findNavController().navigateUp()
                } catch (e: IllegalStateException) {
                    android.util.Log.e("ExamQuizFragment", "Navigation failed", e)
                }
            }
        }
        
        resultDialog?.show()
    }
    
    private fun showExitConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("⚠️ Exam Invalid!")
            .setMessage("If you abort the exam, it will be counted as FAILED with 0 correct answers.\n\nDo you really want to abort?")
            .setPositiveButton("Continue", null) // Positive = Continue
            .setNegativeButton("Abort (0 Points)") { _, _ -> // Negative = Exit = FAILED
                abortExam()
            }
            .show()
    }
    
    private fun abortExam() {
        android.util.Log.d("ExamQuizFragment", "=== ABORTING EXAM ===")
        
        // Cancel timer first
        countDownTimer?.cancel()
        countDownTimer = null
        
        // Try to save session (ignore errors)
        try {
            viewModel.saveSessionOnExit()
        } catch (e: Exception) {
            android.util.Log.e("ExamQuizFragment", "Error saving session on abort", e)
        }
        
        // Show FAILED dialog
        showFailedDialogForAbort()
    }
    
    private fun showFailedDialogForAbort() {
        if (!isAdded || context == null) {
            android.util.Log.e("ExamQuizFragment", "Fragment not attached, navigating back directly")
            return
        }
        
        android.util.Log.d("ExamQuizFragment", "Showing failed dialog for abort")
        
        val dialogBinding = DialogExamFailedBinding.inflate(layoutInflater)
        
        dialogBinding.scoreText.text = "0 / 80"
        dialogBinding.percentageText.text = "0%"
        dialogBinding.errorsText.text = "❌ 80 errors (Max: 10)"
        
        resultDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()
        
        dialogBinding.retryButton.setOnClickListener {
            android.util.Log.d("ExamQuizFragment", "Retry button clicked")
            resultDialog?.dismiss()
            resultDialog = null
            // Restart exam
            timeLeftInMillis = 90 * 60 * 1000L
            viewModel.loadQuestions()
            startTimer()
        }
        
        dialogBinding.backButton.setOnClickListener {
            android.util.Log.d("ExamQuizFragment", "Back button clicked")
            resultDialog?.dismiss()
            resultDialog = null
            // Navigate back
            navigateBack()
        }
        
        resultDialog?.show()
        android.util.Log.d("ExamQuizFragment", "Failed dialog shown")
    }
    
    private fun navigateBack() {
        android.util.Log.d("ExamQuizFragment", "Navigating back...")
        if (!isAdded) {
            android.util.Log.e("ExamQuizFragment", "Fragment not added, cannot navigate")
            return
        }
        
        try {
            findNavController().navigateUp()
            android.util.Log.d("ExamQuizFragment", "Navigation successful")
        } catch (e: Exception) {
            android.util.Log.e("ExamQuizFragment", "Navigation failed: ${e.message}")
            try {
                activity?.finish()
            } catch (e2: Exception) {
                android.util.Log.e("ExamQuizFragment", "Activity finish also failed: ${e2.message}")
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        resultDialog?.dismiss()
        resultDialog = null
        _binding = null
    }
}

