package alie.info.newmultichoice.ui.exam

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import alie.info.newmultichoice.R
import alie.info.newmultichoice.databinding.FragmentExamBinding
import alie.info.newmultichoice.ui.quiz.QuizViewModel
import alie.info.newmultichoice.ui.quiz.QuizUiState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ExamFragment : Fragment() {
    
    private var _binding: FragmentExamBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: QuizViewModel
    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 30 * 60 * 1000 // 30 minutes
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
        
        return binding.root
    }
    
    private fun showExamExplanation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🏃 Marathon Mode")
            .setMessage(
                "Welcome to Marathon Mode!\n\n" +
                "• ALL 375 Questions\n" +
                "• 30 Minutes Time Limit\n" +
                "• ~4.8 seconds per question!\n" +
                "• Extreme speed challenge!\n\n" +
                "Are you ready? 🔥"
            )
            .setPositiveButton("Start Marathon") { _, _ ->
                startExam()
            }
            .setNegativeButton("Cancel") { _, _ ->
                if (isAdded && !isDetached && view != null) {
                    try {
                        findNavController().navigateUp()
                    } catch (e: IllegalStateException) {
                        android.util.Log.e("ExamFragment", "Navigation failed", e)
                    }
                }
            }
            .setCancelable(false)
            .show()
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
        
        // Observe quiz state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Check if binding is still valid
                if (_binding == null) return@collect
                
                when (state) {
                    is QuizUiState.Loading -> {
                        // Show loading
                    }
                    is QuizUiState.Success -> {
                        updateUI(state)
                    }
                    is QuizUiState.QuizFinished -> {
                        // Navigate to results screen like normal quiz
                        navigateToResults(state.totalQuestions, state.correctAnswers, state.wrongAnswers)
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun startExam() {
        viewModel.startQuiz(practiceMode = false)
        setupUI()
    }
    
    private fun startTimer() {
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText()
            }
            
            override fun onFinish() {
                // Time's up! Finish exam
                showTimeUpDialog()
            }
        }.start()
    }
    
    private fun updateTimerText() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        binding.timerText.text = String.format("%02d:%02d", minutes, seconds)
        
        // Change color if time is running out (< 5 minutes)
        if (timeLeftInMillis < 5 * 60 * 1000) {
            binding.timerText.setTextColor(resources.getColor(R.color.error_red, null))
        }
    }
    
    private fun updateUI(state: QuizUiState.Success) {
        // Update counter
        binding.questionCounter.text = "${state.currentQuestionIndex + 1} / ${state.totalQuestions}"
        
        // Update question
        binding.questionText.text = if (state.language == "en") {
            state.currentQuestion.questionEn
        } else {
            state.currentQuestion.questionDe
        }
        
        // Update options
        val question = state.currentQuestion
        binding.optionA.text = "A) ${if (state.language == "en") question.optionAEn else question.optionADe}"
        binding.optionB.text = "B) ${if (state.language == "en") question.optionBEn else question.optionBDe}"
        binding.optionC.text = "C) ${if (state.language == "en") question.optionCEn else question.optionCDe}"
        binding.optionD.text = "D) ${if (state.language == "en") question.optionDEn else question.optionDDe}"
        
        // Reset radio buttons
        binding.optionsRadioGroup.clearCheck()
        optionButtons.forEach { button ->
            button.isEnabled = !state.isAnswerSubmitted
            button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            button.buttonTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), android.R.color.white)
            )
        }
    }
    
    private fun showTimeUpDialog() {
        countDownTimer?.cancel()
        
        // Get current state to show results
        val currentState = viewModel.uiState.value
        if (currentState is QuizUiState.Success) {
            navigateToResults(
                currentState.totalQuestions,
                currentState.correctAnswersCount,
                currentState.wrongAnswersCount
            )
        } else {
            // Fallback - safe navigation back
            if (isAdded && !isDetached && view != null) {
                try {
                    findNavController().navigateUp()
                } catch (e: IllegalStateException) {
                    android.util.Log.e("ExamFragment", "Navigation failed", e)
                }
            }
        }
    }
    
    private fun navigateToResults(totalQuestions: Int, correctAnswers: Int, wrongAnswers: Int) {
        countDownTimer?.cancel()
        
        // Safe navigation with fragment checks
        if (isAdded && !isDetached && view != null) {
            try {
                val action = ExamFragmentDirections.actionExamFragmentToResultFragment(
                    totalQuestions = totalQuestions,
                    correctAnswers = correctAnswers,
                    wrongAnswers = wrongAnswers,
                    isMarathon = true
                )
                findNavController().navigate(action)
            } catch (e: IllegalStateException) {
                android.util.Log.e("ExamFragment", "Navigation to results failed", e)
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        _binding = null
    }
}

