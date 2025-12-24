package alie.info.newmultichoice.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import alie.info.newmultichoice.R
import alie.info.newmultichoice.auth.AuthManager
import alie.info.newmultichoice.data.QuizRepository
import alie.info.newmultichoice.databinding.FragmentHomeBinding
import alie.info.newmultichoice.databinding.DialogFirstLaunchBinding
import alie.info.newmultichoice.databinding.DialogLoadingBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: QuizRepository
    private lateinit var authManager: AuthManager
    private var backPressedTime: Long = 0
    private var isUIInitialized = false
    private lateinit var shimmerContainer: ShimmerFrameLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // If binding already exists and is attached, reuse it
        if (_binding != null) {
            return _binding!!.root
        }
        
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize shimmer container
        shimmerContainer = binding.shimmerContainer.root
        
        // Only initialize UI once (prevent double loading)
        if (!isUIInitialized) {
            repository = QuizRepository.getInstance(requireContext())
            authManager = AuthManager.getInstance(requireContext())
            
            // Check if first launch
            checkAndShowFirstLaunchDialog()

            // Show loading shimmer
            showLoading()

            setupUI()
            setupBackPressHandler()
            
            isUIInitialized = true
        }
    }
    
    private fun checkAndShowFirstLaunchDialog() {
        val sharedPrefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = sharedPrefs.getBoolean("is_first_launch", true)
        
        if (isFirstLaunch) {
            showFirstLaunchDialog()
            // Mark as not first launch
            sharedPrefs.edit().putBoolean("is_first_launch", false).apply()
        }
    }
    
    private fun showFirstLaunchDialog() {
        val dialogBinding = DialogFirstLaunchBinding.inflate(layoutInflater)
        
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.TransparentDialog)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()
        
        dialogBinding.getStartedButton.setOnClickListener {
            dialog.dismiss()
        }
        
        // Make dialog background transparent
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialog.show()
    }

    private fun showLoading() {
        shimmerContainer.visibility = View.VISIBLE
        binding.contentContainer.visibility = View.GONE
        shimmerContainer.startShimmer()
    }

    private fun hideLoading() {
        shimmerContainer.stopShimmer()
        shimmerContainer.visibility = View.GONE
        binding.contentContainer.visibility = View.VISIBLE
    }

    private fun setupBackPressHandler() {
        // Prevent back press from closing the app with a single press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Double tap to exit
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    // Second press within 2 seconds - exit app
                    requireActivity().finish()
                } else {
                    // First press - show toast
                    Snackbar.make(binding.root, "Press back again to exit", Snackbar.LENGTH_SHORT).show()
                }
                backPressedTime = System.currentTimeMillis()
            }
        })
    }
    
    private fun setupUI() {
        // Setup 3D rotating logo first (no animation delay)
        setup3DRotatingLogo()
        
        // Logo click listener - show KLCP info dialog
        binding.logoContainer.setOnClickListener {
            showKLCPInfoDialog()
        }
        
        // Set initial alpha to 0 and translate buttons up for smooth fade-in
        binding.logoContainer.alpha = 0f
        binding.streakCard.alpha = 0f
        
        // Buttons start slightly below and fade in
        binding.startQuizButton.alpha = 0f
        binding.startQuizButton.translationY = 30f
        binding.examModeButton.alpha = 0f
        binding.examModeButton.translationY = 30f
        binding.certificationExamsButton.alpha = 0f
        binding.certificationExamsButton.translationY = 30f
        
        // Subtle shine animation (background, doesn't block)
        val shineAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.shine_animation)
        binding.shineOverlay.startAnimation(shineAnimation)
        
        // Smooth simultaneous fade-in for all elements
        val fadeInDuration = 600L
        val fadeInInterpolator = android.view.animation.DecelerateInterpolator()
        
        // Logo fades in first
        binding.logoContainer.animate()
            .alpha(1f)
            .setDuration(fadeInDuration)
            .setInterpolator(fadeInInterpolator)
            .start()
        
        // Streak card fades in immediately after (minimal delay)
        binding.streakCard.animate()
            .alpha(1f)
            .setDuration(fadeInDuration)
            .setStartDelay(50)
            .setInterpolator(fadeInInterpolator)
            .start()
        
        // Buttons fade in together (smooth, no staggering)
        binding.startQuizButton.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(fadeInDuration)
            .setStartDelay(100)
            .setInterpolator(fadeInInterpolator)
            .start()
        
        binding.examModeButton.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(fadeInDuration)
            .setStartDelay(100)
            .setInterpolator(fadeInInterpolator)
            .start()
        
        binding.certificationExamsButton.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(fadeInDuration)
            .setStartDelay(100)
            .setInterpolator(fadeInInterpolator)
            .start()
        
        // Check unlock status when returning to home
        checkUnlockStatus()

        // Hide loading shimmer after animations are ready
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.delay(800) // Wait for animations to complete
            hideLoading()
        }

        // Observe user stats for streak display - use viewLifecycleOwner
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.getUserStats().collect { stats ->
                    // Check if binding is still valid before updating UI
                    _binding?.let { binding ->
                        updateStreakUI(stats)
                    }
                }
            }
        }
        
        // Check if guest mode
        val isGuest = authManager.isGuestMode()
        
        // Start Quiz button - alle Fragen
        binding.startQuizButton.setOnClickListener {
            if (isGuest) {
                // Guest users get demo quiz
                val action = HomeFragmentDirections.actionNavHomeToQuizFragment(practiceMode = false)
                findNavController().navigate(action)
            } else {
                val action = HomeFragmentDirections.actionNavHomeToQuizFragment(practiceMode = false)
                findNavController().navigate(action)
            }
        }
        
        // Marathon Mode button
        binding.examModeButton.setOnClickListener {
            if (isGuest) {
                showGuestUpgradePrompt("Marathon Mode")
            } else {
                checkUnlockAndNavigate(R.id.examFragment, "Marathon")
            }
        }
        
        // Certification Exams button
        binding.certificationExamsButton.setOnClickListener {
            if (isGuest) {
                showGuestUpgradePrompt("Certification Exams")
            } else {
                checkUnlockAndNavigate(R.id.nav_exam_list, "Certification Exams")
            }
        }
        
        // Disable buttons for guest users
        if (isGuest) {
            binding.examModeButton.alpha = 0.5f
            binding.certificationExamsButton.alpha = 0.5f
        }
    }
    
    private fun updateStreakUI(stats: alie.info.newmultichoice.data.UserStats?) {
        // Use safe binding access
        val binding = _binding ?: return
        
        // Check if guest mode - use demo limit (5) instead of unlock requirement (50)
        val isGuest = authManager.isGuestMode()
        val requiredForUnlock = if (isGuest) 5 else 50
        val correctAnswers = if (isGuest) {
            // For guests, show demo progress (0-5)
            0 // Always show 0/5 for guests
        } else {
            stats?.totalCorrectAnswers ?: 0
        }
        val isUnlocked = if (isGuest) false else (correctAnswers >= requiredForUnlock)
        
        if (stats == null && !isGuest) {
            // Show default values for first-time users (not guests)
            binding.streakText.text = "🎯 0 / 50 correct answers"
            binding.progressCountText.text = "0/50"
            binding.dailyProgressRing.max = 50
            binding.dailyProgressRing.progress = 0
            binding.dailyGoalText.text = "For Marathon & Exams"
            binding.longestStreakText.text = "50 more to unlock"
            
            // Disable buttons
            binding.examModeButton.alpha = 0.5f
            binding.certificationExamsButton.alpha = 0.5f
            return
        }
        
        // For guests, always show demo progress
        if (isGuest) {
            binding.streakText.text = "🎯 0 / 5 demo questions"
            binding.progressCountText.text = "0/5"
            binding.dailyProgressRing.max = 5
            binding.dailyProgressRing.progress = 0
            binding.dailyGoalText.text = "Sign up for full access"
            binding.longestStreakText.text = "Try 5 questions free"
            
            // Disable buttons
            binding.examModeButton.alpha = 0.5f
            binding.certificationExamsButton.alpha = 0.5f
            return
        }
        
        // Update with actual stats - show 50-question progress
        if (!isUnlocked) {
            binding.streakText.text = "🎯 $correctAnswers / $requiredForUnlock correct answers"
            val remaining = requiredForUnlock - correctAnswers
            binding.longestStreakText.text = "$remaining more to unlock"
            binding.dailyGoalText.text = "For Marathon & Exams"
            
            // Disable locked buttons
            binding.examModeButton.alpha = 0.5f
            binding.certificationExamsButton.alpha = 0.5f
        } else {
            binding.streakText.text = "✅ Marathon & Exams unlocked!"
            binding.longestStreakText.text = "All modes available! 🎉"
            binding.dailyGoalText.text = "Good luck! 💪"
            
            // Enable unlocked buttons
            binding.examModeButton.alpha = 1.0f
            binding.certificationExamsButton.alpha = 1.0f
        }
        
        // Update progress ring
        binding.progressCountText.text = "$correctAnswers/$requiredForUnlock"
        binding.dailyProgressRing.max = requiredForUnlock
        binding.dailyProgressRing.progress = correctAnswers.coerceAtMost(requiredForUnlock)
    }
    
    private fun checkUnlockAndNavigate(destinationId: Int, modeName: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val stats = repository.getUserStats().first()
            val correctAnswers = stats?.totalCorrectAnswers ?: 0
            
            if (correctAnswers >= 50) {
                // Unlocked - navigate
                findNavController().navigate(destinationId)
            } else {
                // Locked - show dialog
                val remaining = 50 - correctAnswers
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("🔒 $modeName Locked")
                    .setMessage(
                        "Answer $remaining more questions correctly in the Quiz to unlock $modeName!\n\n" +
                        "Current progress: $correctAnswers / 50 ✅"
                    )
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun showGuestUpgradePrompt(featureName: String) {
        val dialogBinding = alie.info.newmultichoice.databinding.DialogUpgradePromptBinding.inflate(layoutInflater)
        
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.TransparentDialog)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()
        
        dialogBinding.signUpButton.setOnClickListener {
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
            
            // Start AuthActivity and keep loading dialog visible
            val intent = android.content.Intent(requireContext(), alie.info.newmultichoice.auth.AuthActivity::class.java)
            intent.putExtra("show_loading", true)
            startActivity(intent)
            
            // Dismiss loading dialog after Activity transition completes
            loadingBinding.root.postDelayed({
                loadingDialog.dismiss()
            }, 1500) // Wait for Activity to fully load
        }
        
        dialogBinding.maybeLaterButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
    
    /**
     * Setup 3D rotating logo - App Icon on front, KLCP Logo on back
     */
    private fun setup3DRotatingLogo() {
        val container = binding.logoRotationContainer
        val frontLogo = binding.appLogo
        val backLogo = binding.klcpLogo
        
        // Set camera distance for 3D effect
        val cameraDistance = 8000f * resources.displayMetrics.density
        container.cameraDistance = cameraDistance
        frontLogo.cameraDistance = cameraDistance
        backLogo.cameraDistance = cameraDistance
        
        // Initial fade in animation
        val fadeInAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_scale)
        container.startAnimation(fadeInAnimation)
        
        // Create 3D rotation animator
        val rotationAnimator = android.animation.ObjectAnimator.ofFloat(container, "rotationY", 0f, 360f).apply {
            duration = 4000 // 4 seconds for full rotation
            repeatCount = android.animation.ValueAnimator.INFINITE
            interpolator = android.view.animation.LinearInterpolator()
        }
        
        // Update alpha values during rotation for smooth transition
        rotationAnimator.addUpdateListener { animator ->
            val rotation = animator.animatedValue as Float
            val normalizedRotation = (rotation % 360f)
            
            // Front logo visible from -90 to 90 degrees
            // Back logo visible from 90 to 270 degrees
            when {
                normalizedRotation < 90f -> {
                    // Front side visible
                    val alpha = 1f - (normalizedRotation / 90f) * 0.5f
                    frontLogo.alpha = alpha.coerceIn(0.5f, 1f)
                    backLogo.alpha = (normalizedRotation / 90f) * 0.5f
                }
                normalizedRotation < 180f -> {
                    // Back side becoming visible
                    val progress = (normalizedRotation - 90f) / 90f
                    frontLogo.alpha = 0.5f - progress * 0.5f
                    backLogo.alpha = 0.5f + progress * 0.5f
                }
                normalizedRotation < 270f -> {
                    // Back side visible
                    val progress = (normalizedRotation - 180f) / 90f
                    frontLogo.alpha = progress * 0.5f
                    backLogo.alpha = 1f - progress * 0.5f
                }
                else -> {
                    // Front side becoming visible again
                    val progress = (normalizedRotation - 270f) / 90f
                    frontLogo.alpha = 0.5f + progress * 0.5f
                    backLogo.alpha = 0.5f - progress * 0.5f
                }
            }
        }
        
        // Start rotation after initial fade in
        container.postDelayed({
            rotationAnimator.start()
        }, 500)
    }
    
    /**
     * Show KLCP certification information dialog
     */
    private fun showKLCPInfoDialog() {
        val dialogBinding = alie.info.newmultichoice.databinding.DialogKlcpInfoBinding.inflate(layoutInflater)
        
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.TransparentDialog)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()
        
        dialogBinding.closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        // Make dialog background transparent
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialog.show()
    }
    
    /**
     * Check unlock status when user returns to home screen
     * This catches unlocks from aborted quizzes
     */
    private fun checkUnlockStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            val unlockService = alie.info.newmultichoice.services.UnlockCheckService.getInstance(requireContext())
            unlockService.checkAndShowUnlockOverlay(
                fragment = this@HomeFragment,
                onMarathonUnlock = {
                    alie.info.newmultichoice.utils.Logger.d("HomeFragment", "Marathon unlocked!")
                },
                onExamsUnlock = {
                    alie.info.newmultichoice.utils.Logger.d("HomeFragment", "Exams unlocked!")
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}