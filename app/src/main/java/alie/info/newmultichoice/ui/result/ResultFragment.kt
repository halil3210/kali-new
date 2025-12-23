package alie.info.newmultichoice.ui.result

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import alie.info.newmultichoice.R
import alie.info.newmultichoice.databinding.FragmentResultBinding
import alie.info.newmultichoice.services.AchievementService
import alie.info.newmultichoice.services.UnlockCheckService
import alie.info.newmultichoice.services.SyncService
import kotlinx.coroutines.launch

class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!
    
    private val args: ResultFragmentArgs by navArgs()
    
    private lateinit var achievementService: AchievementService
    private lateinit var unlockCheckService: UnlockCheckService
    private lateinit var syncService: SyncService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        
        // Initialize services
        achievementService = AchievementService.getInstance(requireContext())
        unlockCheckService = UnlockCheckService.getInstance(requireContext())
        syncService = SyncService.getInstance(requireContext())
        
        setupUI()
        setupClickListeners()
        checkOverlays()
        
        return binding.root
    }
    
    private fun setupUI() {
        val totalQuestions = args.totalQuestions
        val correctAnswers = args.correctAnswers
        val wrongAnswers = args.wrongAnswers
        val isMarathon = args.isMarathon
        
        val percentage = if (totalQuestions > 0) {
            (correctAnswers.toFloat() / totalQuestions.toFloat()) * 100
        } else {
            0f
        }
        
        // Display score
        binding.scorePercentage.text = getString(R.string.percentage, percentage)
        binding.correctCount.text = correctAnswers.toString()
        binding.wrongCount.text = wrongAnswers.toString()
        
        // Change title and icon based on mode and performance
        val baseTitle = if (isMarathon) "Marathon Complete!" else getString(R.string.quiz_completed)
        
        if (percentage >= 80) {
            binding.resultIcon.setImageResource(android.R.drawable.star_big_on)
            binding.resultTitle.text = "$baseTitle 🎉"
        } else if (percentage >= 60) {
            binding.resultIcon.setImageResource(android.R.drawable.btn_star_big_on)
            binding.resultTitle.text = "$baseTitle 👍"
        } else {
            binding.resultIcon.setImageResource(android.R.drawable.ic_dialog_info)
            binding.resultTitle.text = baseTitle
        }
    }
    
    private fun setupClickListeners() {
        binding.shareButton.setOnClickListener {
            // Share result with context safety check
            val ctx = context ?: return@setOnClickListener
            val percentage = if (args.totalQuestions > 0) {
                (args.correctAnswers.toFloat() / args.totalQuestions.toFloat()) * 100f
            } else {
                0f
            }
            try {
                alie.info.newmultichoice.utils.ShareHelper.shareResult(
                    ctx,
                    args.correctAnswers,
                    args.totalQuestions,
                    percentage
                )
            } catch (e: Exception) {
                android.util.Log.e("ResultFragment", "Share failed", e)
            }
        }
        
        binding.tryAgainButton.setOnClickListener {
            // Navigate back to quiz with safety check
            if (isAdded && !isDetached && view != null) {
                try {
                    findNavController().navigate(R.id.action_resultFragment_to_quizFragment)
                } catch (e: IllegalStateException) {
                    android.util.Log.e("ResultFragment", "Navigation to quiz failed", e)
                }
            }
        }
        
        binding.backToHomeButton.setOnClickListener {
            // Navigate to home with safety check
            if (isAdded && !isDetached && view != null) {
                try {
                    findNavController().navigate(R.id.action_resultFragment_to_nav_home)
                } catch (e: IllegalStateException) {
                    android.util.Log.e("ResultFragment", "Navigation to home failed", e)
                }
            }
        }
    }

    /**
     * Check and show overlays for achievements, unlocks, and sync
     */
    private fun checkOverlays() {
        viewLifecycleOwner.lifecycleScope.launch {
            val totalQuestions = args.totalQuestions
            val correctAnswers = args.correctAnswers
            val isMarathon = args.isMarathon
            // val isExam = args.isExam // Not available in current args
            
            // 1. Check achievements (only for normal quiz, not marathon)
            if (!isMarathon) {
                achievementService.checkAchievementsAfterQuiz(
                    fragment = this@ResultFragment,
                    correctAnswers = correctAnswers,
                    totalQuestions = totalQuestions,
                    durationSeconds = 0 // TODO: Pass actual duration
                )
            }
            
            // 2. Check unlock status (Marathon/Exams)
            unlockCheckService.checkAndShowUnlockOverlay(
                fragment = this@ResultFragment,
                onMarathonUnlock = {
                    // User can navigate manually from home
                    android.util.Log.d("ResultFragment", "Marathon unlocked!")
                },
                onExamsUnlock = {
                    // User can navigate manually from home
                    android.util.Log.d("ResultFragment", "Exams unlocked!")
                }
            )
            
            // 3. Auto-sync data to server
            syncService.uploadAndShowOverlay(this@ResultFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

