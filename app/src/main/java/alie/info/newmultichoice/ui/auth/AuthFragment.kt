package alie.info.newmultichoice.ui.auth

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import alie.info.newmultichoice.R
import alie.info.newmultichoice.auth.VerifyRequest
import alie.info.newmultichoice.api.RetrofitClient
import alie.info.newmultichoice.auth.AuthManager
import alie.info.newmultichoice.auth.AuthResult
import alie.info.newmultichoice.auth.ResendRequest
import alie.info.newmultichoice.auth.TokenRequest
import alie.info.newmultichoice.databinding.*
import alie.info.newmultichoice.utils.HapticFeedbackHelper
import alie.info.newmultichoice.utils.NotificationPermissionHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class AuthFragment : Fragment() {

    private lateinit var authManager: AuthManager
    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private var verificationDialog: androidx.appcompat.app.AlertDialog? = null

    private var pendingUserId: String? = null
    private var pendingEmail: String? = null

    private var isAnimationRunning = false
    private var hasNavigatedToHome = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        android.util.Log.d("AuthFragment", "🔄 onViewCreated - Fragment: ${this.hashCode()}, savedInstanceState: $savedInstanceState")

        authManager = AuthManager.getInstance(requireContext())

        // Reset navigation flag when fragment is recreated
        hasNavigatedToHome = false

        binding.signUpButton.setOnClickListener { showRegisterDialog() }
        binding.guestButton.setOnClickListener {
            authManager.setGuestMode(true)
            navigateToHome()
        }

        NotificationPermissionHelper.requestNotificationPermission(requireActivity())
        handleDeepLink(requireActivity().intent)

        // Setup rotating logo like in home fragment
        setupRotatingLogo()

        if (savedInstanceState == null) showStartScreen()
    }

    private fun showStartScreen() {
        binding.root.visibility = View.VISIBLE
    }

    /**
     * Setup 3D rotating logo like in home fragment
     * Starts rotating immediately when fragment is created
     */
    private fun setupRotatingLogo() {
        try {
            val logoContainer = binding.logoContainer
            val logoRotationContainer = binding.logoRotationContainer
            val appLogo = binding.appLogo

            // Set camera distance for 3D effect
            val cameraDistance = 8000f * resources.displayMetrics.density
            logoRotationContainer.cameraDistance = cameraDistance
            appLogo.cameraDistance = cameraDistance

            // Create 3D rotation animator (same as home fragment)
            val rotationAnimator = android.animation.ObjectAnimator.ofFloat(logoRotationContainer, "rotationY", 0f, 360f).apply {
                duration = 4000 // 4 seconds for full rotation
                repeatCount = android.animation.ValueAnimator.INFINITE
                interpolator = android.view.animation.LinearInterpolator()
            }

            // Start rotation immediately
            rotationAnimator.start()

            android.util.Log.d("AuthFragment", "🎡 Rotating logo started in auth fragment")

        } catch (e: Exception) {
            android.util.Log.e("AuthFragment", "Error setting up rotating logo", e)
        }
    }

    /**
     * Setup rotating logo for dialog overlays
     * Same animation as home fragment but for dialog contexts
     */
    private fun setupDialogRotatingLogo(logoRotationContainer: android.widget.FrameLayout, appLogo: android.widget.ImageView) {
        try {
            // Set camera distance for 3D effect
            val cameraDistance = 8000f * resources.displayMetrics.density
            logoRotationContainer.cameraDistance = cameraDistance
            appLogo.cameraDistance = cameraDistance

            // Create 3D rotation animator (same as home fragment)
            val rotationAnimator = android.animation.ObjectAnimator.ofFloat(logoRotationContainer, "rotationY", 0f, 360f).apply {
                duration = 4000 // 4 seconds for full rotation
                repeatCount = android.animation.ValueAnimator.INFINITE
                interpolator = android.view.animation.LinearInterpolator()
            }

            // Start rotation immediately
            rotationAnimator.start()

            android.util.Log.d("AuthFragment", "🎡 Rotating logo started in dialog")

        } catch (e: Exception) {
            android.util.Log.e("AuthFragment", "Error setting up dialog rotating logo", e)
        }
    }

    private fun navigateToHome() {
        android.util.Log.d("AuthFragment", "🏠 NAVIGATING TO HOME - hasNavigatedToHome: $hasNavigatedToHome")
        if (!hasNavigatedToHome) {
            hasNavigatedToHome = true
            findNavController().navigate(R.id.action_authFragment_to_nav_home)
            android.util.Log.d("AuthFragment", "✅ NAVIGATION TRIGGERED")
        } else {
            android.util.Log.d("AuthFragment", "⏭️  NAVIGATION SKIPPED (already navigated)")
        }
    }

    private fun handleDeepLink(intent: Intent?) {
        val token = intent?.data?.getQueryParameter("token") ?: return
        verifyEmailAndLogin(token)
    }

    private fun verifyEmailAndLogin(token: String) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Verifying email")
            .setMessage("Please wait…")
            .setCancelable(false)
            .create()
        dialog.show()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAuthApiService()
                    .verifyEmailWithToken(TokenRequest(token))

                dialog.dismiss()

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    authManager.saveCredentials(body.token ?: "", body.userId ?: "", body.email ?: "")
                    navigateToHome()
                } else {
                    showErrorDialog("Verification failed")
                }
            } catch (e: Exception) {
                showErrorDialog(e.message ?: "Network error")
            }
        }
    }

    private fun showLoginDialog() {
        // Erstelle fullscreen Overlay statt Dialog
        val binding = DialogLoginBinding.inflate(layoutInflater)
        val overlayView = binding.root

        // Setup rotating logo in dialog
        setupDialogRotatingLogo(binding.logoRotationContainer, binding.appLogo)

        // Get activity root view for fullscreen overlay
        val activity = requireActivity()
        val rootView = activity.window.decorView.findViewById<android.view.ViewGroup>(android.R.id.content)

        // Erstelle fullscreen Overlay Layout Parameter
        val overlayLayoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Setze Overlay als fullscreen
        overlayView.layoutParams = overlayLayoutParams

        // Erstelle dunklen Hintergrund für fullscreen Effekt
        val backgroundView = android.view.View(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#B3000000")) // Semi-transparent black
            layoutParams = overlayLayoutParams
        }

        // Erstelle Container für Overlay und Hintergrund
        val container = android.widget.FrameLayout(requireContext()).apply {
            layoutParams = overlayLayoutParams
            addView(backgroundView)
            addView(overlayView)
        }

        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val pass = binding.passwordInput.text.toString()

            if (email.isBlank() || pass.isBlank()) {
                showError(binding.errorText, "Missing credentials")
                return@setOnClickListener
            }

            performLogin(binding, email, pass)
        }

        binding.registerLink.setOnClickListener {
            // Entferne Login Overlay
            try {
                rootView.removeView(container)
            } catch (e: Exception) {
                android.util.Log.e("AuthFragment", "Error removing login overlay", e)
            }
            // Zeige Register Overlay
            showRegisterDialog()
        }

        // Füge Container zur Root View hinzu
        rootView.addView(container)
    }

    private fun performLogin(binding: DialogLoginBinding, email: String, password: String) {
        binding.loginButton.isEnabled = false

        lifecycleScope.launch {
            when (val result = authManager.login(email, password)) {
                is AuthResult.Success -> {
                    animateLoginSuccess(binding)
                }
                is AuthResult.Error -> {
                    binding.loginButton.isEnabled = true
                    showError(binding.errorText, result.error.getUserMessage())
                }
            }
        }
    }



    private fun animateLoginSuccess(binding: DialogLoginBinding) {
        android.util.Log.d("AuthFragment", "🎬 STARTING LOGIN ANIMATION - Fragment: ${this.hashCode()}")

        // Get the logo container from the dialog
        val logoContainer = binding.logoRotationContainer
        val appLogo = binding.appLogo
        val klcpLogo = binding.klcpLogo

        // Get screen dimensions and density (store at beginning to avoid context issues during animation)
        val displayMetrics = requireContext().resources.displayMetrics
        val density = displayMetrics.density
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()

        // Calculate the scale needed to fill the screen
        val maxDimension = maxOf(screenWidth, screenHeight)
        val logoSize = 180f * density
        val scaleFactor = (maxDimension / logoSize) * 1.2f

        android.util.Log.d("AuthFragment", "📐 Scale factor: $scaleFactor, Screen: ${screenWidth}x${screenHeight}")

        // Get the root view
        val rootView = requireActivity().window.decorView as ViewGroup

        // Get root view location and dimensions
        val rootLocation = IntArray(2)
        rootView.getLocationOnScreen(rootLocation)
        val rootWidth = rootView.width.toFloat()
        val rootHeight = rootView.height.toFloat()

        // Get the center position of the logo in screen coordinates
        val location = IntArray(2)
        logoContainer.getLocationOnScreen(location)
        val logoStartX = location[0].toFloat()
        val logoStartY = location[1].toFloat()

        // Calculate relative start position within root view
        val relativeStartX = logoStartX - rootLocation[0]
        val relativeStartY = logoStartY - rootLocation[1]

        // Calculate translation needed to move logo center to screen center
        val screenCenterX = rootWidth / 2f
        val screenCenterY = rootHeight / 2f
        val logoCenterStartX = relativeStartX + logoContainer.width / 2f
        val logoCenterStartY = relativeStartY + logoContainer.height / 2f

        // Translation to screen center
        val translationToCenter = (screenCenterX - logoCenterStartX) to (screenCenterY - logoCenterStartY)
        
        // Calculate home logo position (top center of screen, with some margin)
        // Home logo is at top with 16dp margin + 180dp container centered
        val homeLogoTopMargin = 60f * density + 16f * density // paddingTop + marginTop from fragment_home.xml
        val homeLogoCenterX = screenCenterX // centered horizontally
        val homeLogoCenterY = homeLogoTopMargin + (180f * density / 2f) // top margin + half container height
        
        // Translation from screen center to home logo position
        val translationToHome = (homeLogoCenterX - screenCenterX) to (homeLogoCenterY - screenCenterY)

        // Create overlay view
        val overlayView = View(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFF0F172A.toInt())
            alpha = 0f
            elevation = 10000f
            translationZ = 10000f
        }

        // Set camera distance for 3D effect
        val cameraDistance = 8000f * density

        // Create a copy of the logo container for animation
        val animatedLogoContainer = FrameLayout(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                logoContainer.width,
                logoContainer.height
            )
            x = relativeStartX
            y = relativeStartY
            this.cameraDistance = cameraDistance
            elevation = 10001f
            translationZ = 10001f
        }

        // Add overlay first, then logo container on top
        rootView.addView(overlayView)

        // Copy logos to animated container
        val animatedAppLogo = ImageView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                appLogo.width,
                appLogo.height,
                android.view.Gravity.CENTER
            )
            setImageDrawable(appLogo.drawable)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            this.cameraDistance = cameraDistance
        }

        val animatedKlcpLogo = ImageView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                klcpLogo.width,
                klcpLogo.height,
                android.view.Gravity.CENTER
            )
            setImageDrawable(klcpLogo.drawable)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            alpha = klcpLogo.alpha
            this.cameraDistance = cameraDistance
        }

        animatedLogoContainer.addView(animatedAppLogo)
        animatedLogoContainer.addView(animatedKlcpLogo)
        rootView.addView(animatedLogoContainer)

        // Start 3D rotation
        val rotationAnimator = ObjectAnimator.ofFloat(animatedLogoContainer, "rotationY", 0f, 360f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.LinearInterpolator()
        }

        // Update alpha values during rotation
        rotationAnimator.addUpdateListener { animator ->
            val rotation = animator.animatedValue as Float
            val normalizedRotation = (rotation % 360f)

            when {
                normalizedRotation < 90f -> {
                    val alpha = 1f - (normalizedRotation / 90f) * 0.5f
                    animatedAppLogo.alpha = alpha.coerceIn(0.5f, 1f)
                    animatedKlcpLogo.alpha = (normalizedRotation / 90f) * 0.5f
                }
                normalizedRotation < 180f -> {
                    val progress = (normalizedRotation - 90f) / 90f
                    animatedAppLogo.alpha = 0.5f - progress * 0.5f
                    animatedKlcpLogo.alpha = 0.5f + progress * 0.5f
                }
                normalizedRotation < 270f -> {
                    val progress = (normalizedRotation - 180f) / 90f
                    animatedAppLogo.alpha = progress * 0.5f
                    animatedKlcpLogo.alpha = 1f - progress * 0.5f
                }
                else -> {
                    val progress = (normalizedRotation - 270f) / 90f
                    animatedAppLogo.alpha = 0.5f + progress * 0.5f
                    animatedKlcpLogo.alpha = 0.5f - progress * 0.5f
                }
            }
        }

        // Hide original logos
        logoContainer.alpha = 0f

        // Start rotation
        rotationAnimator.start()

        // Animate overlay fade in
        overlayView.animate()
            .alpha(1f)
            .setDuration(200)
            .withStartAction {
                overlayView.bringToFront()
                animatedLogoContainer.bringToFront()
            }
            .start()

        // Force layout and animate scale and translation
        animatedLogoContainer.post {
            // Phase 1: Position to center first
            android.util.Log.d("AuthFragment", "📍 Phase 1: Moving to screen center")
            animatedLogoContainer.animate()
                .translationX(translationToCenter.first)
                .translationY(translationToCenter.second)
                .setDuration(800)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    // Then scale
                    animatedLogoContainer.animate()
                        .scaleX(scaleFactor)
                        .scaleY(scaleFactor)
                        .setDuration(2000)
                        .setInterpolator(DecelerateInterpolator())
                        .withStartAction {
                            animatedLogoContainer.bringToFront()
                            overlayView.bringToFront()
                        }
                        .withEndAction {
                            // Phase 2: Move to home logo position and shrink
                            android.util.Log.d("AuthFragment", "🎯 Phase 2: Moving to home logo position")
                            
                            // Stop rotation for smooth transition
                            rotationAnimator.cancel()
                            
                            // Calculate final scale (home logo is 100dp, we're at 180dp * scaleFactor)
                            val currentSize = 180f * density * scaleFactor
                            val targetSize = 100f * density
                            val finalScale = targetSize / currentSize
                            
                            animatedLogoContainer.animate()
                                .scaleX(finalScale)
                                .scaleY(finalScale)
                                .translationX(translationToHome.first)
                                .translationY(translationToHome.second)
                                .setDuration(1000)
                                .setInterpolator(AccelerateDecelerateInterpolator())
                                .withStartAction {
                                    // Remove login overlay and navigate to home
                                    try {
                                        val activity = requireActivity()
                                        val contentRoot = activity.window.decorView.findViewById<android.view.ViewGroup>(android.R.id.content)
                                        // Find and remove fullscreen container (Login/Register Overlays)
                                        for (i in 0 until contentRoot.childCount) {
                                            val child = contentRoot.getChildAt(i)
                                            if (child is android.widget.FrameLayout &&
                                                child.tag == null && // Our overlays have no tag
                                                child.childCount >= 2) { // Background + Content View
                                                contentRoot.removeView(child)
                                                break
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("AuthFragment", "Error removing login overlay", e)
                                    }
                                    
                                    // Navigate to home immediately so it loads in background
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        navigateToHome()
                                    }, 100)
                                }
                                .withEndAction {
                                    // Phase 3: Fade out and remove
                                    android.util.Log.d("AuthFragment", "✨ Phase 3: Fading out")
                                    
                                    animatedLogoContainer.animate()
                                        .alpha(0f)
                                        .scaleX(0f)
                                        .scaleY(0f)
                                        .setDuration(400)
                                        .withEndAction {
                                            // Remove animated logo
                                            Handler(Looper.getMainLooper()).postDelayed({
                                                try {
                                                    rootView.removeView(animatedLogoContainer)
                                                } catch (e: Exception) {
                                                    // View already removed
                                                }
                                            }, 100)
                                        }
                                        .start()

                                    // Fade out overlay
                                    overlayView.animate()
                                        .alpha(0f)
                                        .setDuration(500)
                                        .withEndAction {
                                            rootView.removeView(overlayView)
                                        }
                                .start()
                        }
                        .start()
                }
                .start()
        }
    }

    private fun showRegisterDialog() {
        // Erstelle fullscreen Overlay statt Dialog
        val binding = DialogRegisterBinding.inflate(layoutInflater)
        val overlayView = binding.root

        // Setup rotating logo in dialog
        setupDialogRotatingLogo(binding.logoRotationContainer, binding.appLogo)

        // Get activity root view for fullscreen overlay
        val activity = requireActivity()
        val rootView = activity.window.decorView.findViewById<android.view.ViewGroup>(android.R.id.content)

        // Erstelle fullscreen Overlay Layout Parameter
        val overlayLayoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Setze Overlay als fullscreen
        overlayView.layoutParams = overlayLayoutParams

        // Erstelle dunklen Hintergrund für fullscreen Effekt
        val backgroundView = android.view.View(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#B3000000")) // Semi-transparent black
            layoutParams = overlayLayoutParams
        }

        // Erstelle Container für Overlay und Hintergrund
        val container = android.widget.FrameLayout(requireContext()).apply {
            layoutParams = overlayLayoutParams
            addView(backgroundView)
            addView(overlayView)
        }

        binding.registerButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val pass = binding.passwordInput.text.toString()
            val confirm = binding.confirmPasswordInput.text.toString()

            if (email.isBlank() || pass.isBlank() || confirm.isBlank()) {
                showError(binding.errorText, "All fields required")
                return@setOnClickListener
            }

            if (pass != confirm) {
                showError(binding.errorText, "Passwords do not match")
                return@setOnClickListener
            }

            performRegistration(binding, email, pass)
        }

        binding.loginLink.setOnClickListener {
            // Entferne Register Overlay
            try {
                rootView.removeView(container)
            } catch (e: Exception) {
                android.util.Log.e("AuthFragment", "Error removing register overlay", e)
            }
            // Zeige Login Overlay
            showLoginDialog()
        }

        // Füge Container zur Root View hinzu
        rootView.addView(container)
    }

    private fun performRegistration(binding: DialogRegisterBinding, email: String, password: String) {
        lifecycleScope.launch {
            val result = authManager.register(email, password)
            if (result is AuthResult.Success) {
                pendingEmail = email
                pendingUserId = result.data.userId
                showVerificationDialog()
            } else {
                showError(binding.errorText, "Registration failed")
            }
        }
    }

    private fun showVerificationDialog() {
        val binding = DialogVerificationBinding.inflate(layoutInflater)

        verificationDialog = MaterialAlertDialogBuilder(requireContext(), R.style.TransparentDialog)
            .setView(binding.root)
            .setCancelable(false)
            .create()

        binding.verifyButton.setOnClickListener {
            val code = binding.codeInput.text.toString()
            if (code.length != 6) {
                showError(binding.errorText, "Invalid code")
                return@setOnClickListener
            }
            verifyCode(binding, code)
        }

        verificationDialog?.show()
    }

    private fun verifyCode(binding: DialogVerificationBinding, code: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAuthApiService()
                    .verifyEmail(VerifyRequest(pendingUserId ?: "", code))

                if (response.isSuccessful) {
                    navigateToHome()
                } else {
                    showError(binding.errorText, "Invalid code")
                }
            } catch (e: Exception) {
                showError(binding.errorText, "Network error")
            }
        }
    }

    private fun showError(view: android.widget.TextView, msg: String) {
        view.text = msg
        view.visibility = View.VISIBLE
        HapticFeedbackHelper.getInstance(requireContext()).errorFeedback(view)
    }

    private fun showErrorDialog(msg: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Error")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        verificationDialog?.dismiss()
    }
}
