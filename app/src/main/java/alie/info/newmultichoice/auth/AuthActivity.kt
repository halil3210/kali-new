package alie.info.newmultichoice.auth

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import alie.info.newmultichoice.MainActivity
import alie.info.newmultichoice.api.RetrofitClient
import alie.info.newmultichoice.databinding.ActivityAuthStartBinding
import alie.info.newmultichoice.databinding.DialogLoginBinding
import alie.info.newmultichoice.databinding.DialogRegisterBinding
import alie.info.newmultichoice.databinding.DialogVerificationBinding
import alie.info.newmultichoice.databinding.DialogLoadingBinding
import alie.info.newmultichoice.databinding.OverlayAppTransitionBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
// Google Sign-In REMOVED - Causes emulator crashes

class AuthActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    private var loginDialog: androidx.appcompat.app.AlertDialog? = null
    private var registerDialog: androidx.appcompat.app.AlertDialog? = null
    private var verificationDialog: androidx.appcompat.app.AlertDialog? = null
    private var loginDialogBinding: DialogLoginBinding? = null

    // Store user info for verification
    private var pendingUserId: String? = null
    private var pendingEmail: String? = null
    private var pendingToken: String? = null

    // Google Sign-In REMOVED - Causes emulator crashes

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen (Material You style)
        val splashScreen = installSplashScreen()
        
        // Check if we should show loading (from Sign Up flow)
        val showLoading = intent.getBooleanExtra("show_loading", false)
        
        // Keep splash screen visible for 3 seconds (or shorter if coming from Sign Up)
        var keepSplashOnScreen = true
        val splashDuration = if (showLoading) 1500L else 3000L
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        // Hide splash after duration using Handler (lifecycle-aware)
        Handler(Looper.getMainLooper()).postDelayed({
            keepSplashOnScreen = false
        }, splashDuration)
        
        super.onCreate(savedInstanceState)
        
        // Switch from Splash theme to normal theme (for Material Components)
        setTheme(alie.info.newmultichoice.R.style.Theme_KLCPQuiz)
        
        authManager = AuthManager.getInstance(this)
        
        // Google Sign-In REMOVED - Causes emulator crashes

        // Handle deep link if present
        handleDeepLink(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }
    
    private fun handleDeepLink(intent: Intent?) {
        val data: Uri? = intent?.data
        
        if (data != null) {
            alie.info.newmultichoice.utils.Logger.d("AuthActivity", "Deep link received: $data")
            
            // Extract token from URL
            val token = data.getQueryParameter("token")
            
            if (token != null) {
                // Verify email and auto-login
                verifyEmailAndLogin(token)
                return
            }
        }
        
        // No deep link - normal flow
        if (authManager.isLoggedIn()) {
            navigateToMain()
        } else {
            showStartScreen()
        }
    }
    
    private fun verifyEmailAndLogin(token: String) {
        alie.info.newmultichoice.utils.Logger.d("AuthActivity", "Verifying email with token: $token")
        
        // Show loading dialog
        val loadingDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Verifying Email...")
            .setMessage("Please wait while we verify your email address.")
            .setCancelable(false)
            .create()
        loadingDialog.show()
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAuthApiService().verifyEmailWithToken(token)
                
                loadingDialog.dismiss()
                
                if (response.isSuccessful) {
                    val body = response.body()
                    
                    if (body?.token != null && body.userId != null && body.email != null) {
                        // Auto-login with returned credentials
                        authManager.saveCredentials(body.token, body.userId, body.email)
                        
                        MaterialAlertDialogBuilder(this@AuthActivity)
                            .setTitle("Email Verified!")
                            .setMessage("Welcome, ${body.email}! Your email has been verified and you're now logged in.")
                            .setPositiveButton("Continue") { _, _ ->
                                navigateToMain()
                            }
                            .setCancelable(false)
                            .show()
                    } else {
                        // Verified but no auto-login data
                        MaterialAlertDialogBuilder(this@AuthActivity)
                            .setTitle("Email Verified!")
                            .setMessage(body?.message ?: "Your email has been verified. Please login.")
                            .setPositiveButton("Login") { _, _ ->
                                showLoginDialog()
                            }
                            .setCancelable(false)
                            .show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    MaterialAlertDialogBuilder(this@AuthActivity)
                        .setTitle("Verification Failed")
                        .setMessage(errorBody ?: "Could not verify email. The link may have expired.")
                        .setPositiveButton("OK") { _, _ ->
                            showLoginDialog()
                        }
                        .setCancelable(false)
                        .show()
                }
            } catch (e: Exception) {
                loadingDialog.dismiss()
                alie.info.newmultichoice.utils.Logger.e("AuthActivity", "Verification error", e)
                
                MaterialAlertDialogBuilder(this@AuthActivity)
                    .setTitle("Error")
                    .setMessage("Network error: ${e.message}")
                    .setPositiveButton("OK") { _, _ ->
                        showLoginDialog()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun showStartScreen() {
        val binding = ActivityAuthStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Google Sign-In Button REMOVED - Causes emulator crashes
        binding.googleSignInButton.visibility = View.GONE
        
        // Setup 3D rotating logo
        setup3DRotatingLogoStartScreen(binding)
        
        binding.signUpButton.setOnClickListener {
            showLoginDialog()
        }
        
        binding.guestButton.setOnClickListener {
            startGuestMode()
        }
    }
    
    /**
     * Setup 3D rotating logo in dialog (reusable for login/register dialogs)
     */
    private fun setup3DRotatingLogoDialog(
        logoContainer: View,
        rotationContainer: View,
        frontLogo: android.widget.ImageView,
        backLogo: android.widget.ImageView
    ) {
        // Set camera distance for 3D effect
        val cameraDistance = 8000f * resources.displayMetrics.density
        rotationContainer.cameraDistance = cameraDistance
        frontLogo.cameraDistance = cameraDistance
        backLogo.cameraDistance = cameraDistance
        
        // Initial fade in animation
        val fadeInAnimation = android.view.animation.AnimationUtils.loadAnimation(this, alie.info.newmultichoice.R.anim.fade_in_scale)
        logoContainer.startAnimation(fadeInAnimation)
        
        // Create 3D rotation animator
        val rotationAnimator = android.animation.ObjectAnimator.ofFloat(rotationContainer, "rotationY", 0f, 360f).apply {
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
        rotationContainer.postDelayed({
            rotationAnimator.start()
        }, 500)
    }
    
    /**
     * Setup 3D rotating logo on start screen
     */
    private fun setup3DRotatingLogoStartScreen(binding: ActivityAuthStartBinding) {
        val container = binding.logoRotationContainer
        val frontLogo = binding.appLogo
        val backLogo = binding.klcpLogo
        
        // Set camera distance for 3D effect
        val cameraDistance = 8000f * resources.displayMetrics.density
        container.cameraDistance = cameraDistance
        frontLogo.cameraDistance = cameraDistance
        backLogo.cameraDistance = cameraDistance
        
        // Initial fade in animation
        val fadeInAnimation = android.view.animation.AnimationUtils.loadAnimation(this, alie.info.newmultichoice.R.anim.fade_in_scale)
        binding.logoContainer.startAnimation(fadeInAnimation)
        
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
    
    // Google Sign-In REMOVED - Causes emulator crashes
    
    private fun startGuestMode() {
        // Mark as guest user
        authManager.setGuestMode(true)
        
        // Show upgrade prompt immediately
        showUpgradePrompt()
    }
    
    private fun showUpgradePrompt() {
        try {
            val dialogBinding = alie.info.newmultichoice.databinding.DialogUpgradePromptBinding.inflate(layoutInflater)
            
            val dialog = MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.root)
                .setCancelable(false)
                .create()
            
            dialogBinding.signUpButton.setOnClickListener {
                alie.info.newmultichoice.utils.Logger.d("AuthActivity", "Sign up button clicked")
                dialog.dismiss()
                
                // Show modern loading dialog
                val loadingBinding = DialogLoadingBinding.inflate(layoutInflater)
                loadingBinding.loadingText.text = "Loading..."
                loadingBinding.loadingSubtitle.text = "Please wait"
                
                val loadingDialog = MaterialAlertDialogBuilder(this, alie.info.newmultichoice.R.style.TransparentDialog)
                    .setView(loadingBinding.root)
                    .setCancelable(false)
                    .create()
                
                loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                loadingDialog.show()
                
                // Clear guest mode
                authManager.clearGuestMode()
                
                // Show login dialog after loading completes
                loadingBinding.root.postDelayed({
                    loadingDialog.dismiss()
                    showLoginDialog()
                }, 1000) // Wait for UI to be ready
            }
            
            dialogBinding.maybeLaterButton.setOnClickListener {
                alie.info.newmultichoice.utils.Logger.d("AuthActivity", "Maybe later button clicked")
                // Dismiss dialog
                dialog.dismiss()
                // Show transition overlay with app icon
                showTransitionOverlay()
            }
            
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            alie.info.newmultichoice.utils.Logger.d("AuthActivity", "Showing upgrade prompt dialog...")
            dialog.show()
        } catch (e: Exception) {
            alie.info.newmultichoice.utils.Logger.e("AuthActivity", "Error showing upgrade prompt", e)
            // Fallback: navigate to main activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    private fun showTransitionOverlay() {
        try {
            // Show overlay immediately on current activity
            val overlayBinding = OverlayAppTransitionBinding.inflate(layoutInflater)
            val overlayView = overlayBinding.root
            
            // Add overlay to window immediately
            val rootView = window.decorView.rootView as ViewGroup
            rootView.addView(overlayView)
            
            // Start animation immediately
            val appIcon = overlayBinding.appIconLarge
            
            // Fade in and scale up
            val fadeIn = ObjectAnimator.ofFloat(appIcon, "alpha", 0f, 1f)
            fadeIn.duration = 400
            fadeIn.interpolator = DecelerateInterpolator()
            
            val scaleUp = ObjectAnimator.ofFloat(appIcon, "scaleX", 0.5f, 1f)
            val scaleUpY = ObjectAnimator.ofFloat(appIcon, "scaleY", 0.5f, 1f)
            scaleUp.duration = 400
            scaleUpY.duration = 400
            scaleUp.interpolator = DecelerateInterpolator()
            scaleUpY.interpolator = DecelerateInterpolator()
            
            // Start MainActivity in background while overlay is visible
            // Use FLAG_ACTIVITY_NO_ANIMATION to prevent activity transition animation
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("skip_splash", true)
                flags = Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            
            // After showing icon, fade out
            fadeIn.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Hold for a moment, then fade out
                    appIcon.postDelayed({
                        val fadeOut = ObjectAnimator.ofFloat(overlayView, "alpha", 1f, 0f)
                        fadeOut.duration = 300
                        fadeOut.interpolator = AccelerateDecelerateInterpolator()
                        
                        fadeOut.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                // Remove overlay
                                rootView.removeView(overlayView)
                                // Finish activity after a small delay to prevent fragment reload
                                rootView.postDelayed({
                                    finish()
                                    overridePendingTransition(0, 0)
                                }, 100)
                            }
                        })
                        
                        fadeOut.start()
                    }, 2500) // Hold icon visible for 2500ms (2.5 seconds)
                }
            })
            
            // Start animations immediately
            fadeIn.start()
            scaleUp.start()
            scaleUpY.start()
            
        } catch (e: Exception) {
            alie.info.newmultichoice.utils.Logger.e("AuthActivity", "Error showing transition overlay", e)
            // Fallback: navigate directly
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("skip_splash", true)
            }
            startActivity(intent)
            finish()
        }
    }
    
    private fun showLoginDialog() {
        // Dismiss any existing dialogs first
        registerDialog?.dismiss()
        
        val binding = DialogLoginBinding.inflate(layoutInflater)
        loginDialogBinding = binding

        loginDialog = MaterialAlertDialogBuilder(this)
            .setView(binding.root)
            .setCancelable(false)
            .create()

        // Setup 3D rotating logo in login dialog
        setup3DRotatingLogoDialog(binding.logoContainer, binding.logoRotationContainer, binding.appLogo, binding.klcpLogo)

        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                showError(binding.errorText, "Please fill all fields")
                return@setOnClickListener
            }

            performLogin(binding, email, password)
        }

        binding.registerLink.setOnClickListener {
            // Dismiss login dialog and show register dialog
            loginDialog?.dismiss()
            // Small delay to ensure smooth transition
            Handler(Looper.getMainLooper()).postDelayed({
                showRegisterDialog()
            }, 50)
        }

        loginDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loginDialog?.show()
    }

    private fun showRegisterDialog() {
        // Dismiss any existing dialogs first
        loginDialog?.dismiss()
        
        val binding = DialogRegisterBinding.inflate(layoutInflater)

        registerDialog = MaterialAlertDialogBuilder(this)
            .setView(binding.root)
            .setCancelable(false)
            .create()

        // Setup 3D rotating logo in register dialog
        setup3DRotatingLogoDialog(binding.logoContainer, binding.logoRotationContainer, binding.appLogo, binding.klcpLogo)

        binding.registerButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()
            val confirmPassword = binding.confirmPasswordInput.text.toString()

            // Validation
            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                showError(binding.errorText, "Please fill all fields")
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError(binding.errorText, "Invalid email format")
                return@setOnClickListener
            }

            // Strong password validation
            val passwordError = validatePassword(password)
            if (passwordError != null) {
                showError(binding.errorText, passwordError)
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                showError(binding.errorText, "Passwords do not match")
                return@setOnClickListener
            }

            performRegistration(binding, email, password)
        }

        binding.loginLink.setOnClickListener {
            // Dismiss register dialog and show login dialog
            registerDialog?.dismiss()
            // Small delay to ensure smooth transition
            Handler(Looper.getMainLooper()).postDelayed({
                showLoginDialog()
            }, 50)
        }

        registerDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        registerDialog?.show()
    }

    private fun performLogin(binding: DialogLoginBinding, email: String, password: String) {
        // Show loading state
        binding.loginButton.isEnabled = false
        binding.loginButton.text = "Logging in..."
        binding.errorText.visibility = View.GONE

        // Show modern loading dialog
        val loadingBinding = DialogLoadingBinding.inflate(layoutInflater)
        loadingBinding.loadingText.text = "Logging in..."
        loadingBinding.loadingSubtitle.text = "Please wait"
        
        val loadingDialog = MaterialAlertDialogBuilder(this, alie.info.newmultichoice.R.style.TransparentDialog)
            .setView(loadingBinding.root)
            .setCancelable(false)
            .create()
        
        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog.show()

        lifecycleScope.launch {
            when (val result = authManager.login(email, password)) {
                is AuthResult.Success -> {
                    alie.info.newmultichoice.utils.Logger.d("AuthActivity", "Login successful")
                    loadingDialog.dismiss()
                    // Animate logo transition before navigating
                    animateLogoTransition(binding)
                }
                is AuthResult.Error -> {
                    alie.info.newmultichoice.utils.Logger.e("AuthActivity", "Login failed: ${result.message}")
                    loadingDialog.dismiss()
                    binding.loginButton.isEnabled = true
                    binding.loginButton.text = "Login"
                    showError(binding.errorText, result.message)
                }
            }
        }
    }

    private fun performRegistration(binding: DialogRegisterBinding, email: String, password: String) {
        // Show loading state
        binding.registerButton.isEnabled = false
        binding.registerButton.text = "Registering..."
        binding.errorText.visibility = View.GONE

        // Show modern loading dialog
        val loadingBinding = DialogLoadingBinding.inflate(layoutInflater)
        loadingBinding.loadingText.text = "Creating account..."
        loadingBinding.loadingSubtitle.text = "Please wait"
        
        val loadingDialog = MaterialAlertDialogBuilder(this, alie.info.newmultichoice.R.style.TransparentDialog)
            .setView(loadingBinding.root)
            .setCancelable(false)
            .create()
        
        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog.show()
        
        lifecycleScope.launch {
            when (val result = authManager.register(email, password)) {
                is AuthResult.Success -> {
                    alie.info.newmultichoice.utils.Logger.d("AuthActivity", "Registration successful")
                    loadingDialog.dismiss()
                    
                    // Store pending user info for verification
                    pendingUserId = result.data.userId
                    pendingEmail = result.data.email
                    pendingToken = result.data.token
                    
                    // Close register dialog and show verification dialog
                    registerDialog?.dismiss()
                    showVerificationDialog()
                }
                is AuthResult.Error -> {
                    alie.info.newmultichoice.utils.Logger.e("AuthActivity", "Registration failed: ${result.message}")
                    loadingDialog.dismiss()
                    binding.registerButton.isEnabled = true
                    binding.registerButton.text = "Create Account"
                    showError(binding.errorText, result.message)
                }
            }
        }
    }

    private fun showError(textView: android.widget.TextView, message: String) {
        textView.text = message
        textView.visibility = View.VISIBLE
    }
    
    /**
     * Validates password strength
     * Requirements:
     * - Minimum 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character
     */
    private fun validatePassword(password: String): String? {
        if (password.length < 8) {
            return "Password must be at least 8 characters"
        }
        if (!password.any { it.isUpperCase() }) {
            return "Password must contain at least one uppercase letter"
        }
        if (!password.any { it.isLowerCase() }) {
            return "Password must contain at least one lowercase letter"
        }
        if (!password.any { it.isDigit() }) {
            return "Password must contain at least one number"
        }
        if (!password.any { !it.isLetterOrDigit() }) {
            return "Password must contain at least one special character (!@#\$%^&*)"
        }
        return null // Password is valid
    }

    private fun animateLogoTransition(binding: DialogLoginBinding) {
        // Get the logo container from the dialog
        val logoContainer = binding.logoRotationContainer
        val appLogo = binding.appLogo
        val klcpLogo = binding.klcpLogo
        
        // Get screen dimensions
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()
        
        // Calculate the scale needed to fill the screen
        // Use the larger dimension to ensure full coverage
        val maxDimension = maxOf(screenWidth, screenHeight)
        val logoSize = 180f * resources.displayMetrics.density // Current logo size in pixels
        val scaleFactor = (maxDimension / logoSize) * 1.2f // Add 20% extra to ensure full coverage
        
        // Get the root view - use decorView to ensure it's on top of everything
        val rootView = window.decorView as ViewGroup
        
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
        
        // Create overlay view that covers the entire screen
        val overlayView = View(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFF1A1A1A.toInt())
            alpha = 0f
            // Set high elevation to ensure it's on top
            elevation = 10000f
            translationZ = 10000f
        }
        
        // Set camera distance for 3D effect
        val cameraDistance = 8000f * resources.displayMetrics.density
        
        // Create a copy of the logo container for animation
        val animatedLogoContainer = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                logoContainer.width,
                logoContainer.height
            )
            x = relativeStartX
            y = relativeStartY
            this.cameraDistance = cameraDistance
            // Set very high elevation to ensure it's on top of everything
            elevation = 10001f
            translationZ = 10001f
        }
        
        // Add overlay first, then logo container on top
        rootView.addView(overlayView)
        
        // Copy logos to animated container
        val animatedAppLogo = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                appLogo.width,
                appLogo.height,
                android.view.Gravity.CENTER
            )
            setImageDrawable(appLogo.drawable)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            this.cameraDistance = cameraDistance
        }
        
        val animatedKlcpLogo = ImageView(this).apply {
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
        
        // Continue 3D rotation during animation
        val rotationAnimator = ObjectAnimator.ofFloat(animatedLogoContainer, "rotationY", 0f, 360f).apply {
            duration = 4000 // 4 seconds for full rotation
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.LinearInterpolator()
        }
        
        // Update alpha values during rotation for smooth transition
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
        
        // Hide original logos and dismiss any dialogs to ensure clean transition
        logoContainer.alpha = 0f
        loginDialog?.dismiss()
        loginDialog = null
        
        // Start rotation animation
        rotationAnimator.start()
        
        // Animate overlay fade in - ensure it covers everything
        overlayView.animate()
            .alpha(1f)
            .setDuration(200)
            .withStartAction {
                // Bring overlay to front to ensure it covers everything
                overlayView.bringToFront()
                animatedLogoContainer.bringToFront()
            }
            .start()
        
        // Force layout to ensure positions are correct and bring to front
        animatedLogoContainer.post {
            // Ensure both views are on top of everything
            overlayView.bringToFront()
            animatedLogoContainer.bringToFront()
            
            // Get actual root view dimensions after layout
            val actualRootWidth = rootView.width.toFloat()
            val actualRootHeight = rootView.height.toFloat()
            
            // Calculate scaled dimensions
            val originalWidth = logoContainer.width.toFloat()
            val originalHeight = logoContainer.height.toFloat()
            val scaledWidth = originalWidth * scaleFactor
            val scaledHeight = originalHeight * scaleFactor
            
            // Target position: center of root view
            // Position the center of the scaled logo at the center of the screen
            val targetX = (actualRootWidth - scaledWidth) / 2f
            val targetY = (actualRootHeight - scaledHeight) / 2f
            
            // Calculate the center point of the screen
            val screenCenterX = actualRootWidth / 2f
            val screenCenterY = actualRootHeight / 2f
            
            // Calculate the center of the logo at start position
            val logoCenterStartX = relativeStartX + originalWidth / 2f
            val logoCenterStartY = relativeStartY + originalHeight / 2f
            
            // Calculate translation needed to move logo center to screen center
            // Add offset to compensate for drift to the left (move more to the right)
            val offsetX = 150f * resources.displayMetrics.density // Further increased offset for more correction
            val translationX = (screenCenterX - logoCenterStartX) + offsetX
            val translationY = screenCenterY - logoCenterStartY
            
            // Set pivot point to center of original logo for proper scaling
            animatedLogoContainer.pivotX = originalWidth / 2f
            animatedLogoContainer.pivotY = originalHeight / 2f
            
            // Debug: Log positions to verify
            alie.info.newmultichoice.utils.Logger.d("LogoAnimation", 
                "Start center: ($logoCenterStartX, $logoCenterStartY), " +
                "Screen center: ($screenCenterX, $screenCenterY), " +
                "Translation: ($translationX, $translationY), " +
                "Root: ${actualRootWidth}x${actualRootHeight}, " +
                "Scale: $scaleFactor")
            
            // Animate scale and translation together
            animatedLogoContainer.animate()
                .scaleX(scaleFactor)
                .scaleY(scaleFactor)
                .translationX(translationX)
                .translationY(translationY)
                .setDuration(2000) // 2 seconds
                .setInterpolator(DecelerateInterpolator())
                .withStartAction {
                    // Ensure it stays on top during animation
                    animatedLogoContainer.bringToFront()
                    overlayView.bringToFront()
                }
                .withEndAction {
                    // Fade out after reaching full size
                    animatedLogoContainer.animate()
                        .alpha(0f)
                        .setDuration(400)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .withEndAction {
                            // Stop rotation and remove views
                            rotationAnimator.cancel()
                            rootView.removeView(animatedLogoContainer)
                            rootView.removeView(overlayView)
                            navigateToMain()
                        }
                        .start()
                }
                .start()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("skip_splash", true)
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
        overridePendingTransition(0, 0)
    }
    
    private fun showVerificationDialog() {
        val binding = DialogVerificationBinding.inflate(layoutInflater)
        
        verificationDialog = MaterialAlertDialogBuilder(this)
            .setView(binding.root)
            .setCancelable(false)
            .create()
        
        // Update info text with email
        binding.infoText.text = "A 6-digit verification code has been sent to:\n${pendingEmail}"
        
        binding.verifyButton.setOnClickListener {
            val code = binding.codeInput.text.toString().trim()
            
            if (code.isEmpty()) {
                showError(binding.errorText, "Please enter the verification code")
                return@setOnClickListener
            }
            
            if (code.length != 6) {
                showError(binding.errorText, "Code must be 6 digits")
                return@setOnClickListener
            }
            
            performVerification(binding, code)
        }
        
        binding.resendButton.setOnClickListener {
            resendVerificationCode(binding)
        }
        
        binding.backToLoginLink.setOnClickListener {
            verificationDialog?.dismiss()
            pendingUserId = null
            pendingEmail = null
            pendingToken = null
            showLoginDialog()
        }
        
        verificationDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        verificationDialog?.show()
    }
    
    private fun performVerification(binding: DialogVerificationBinding, code: String) {
        val userId = pendingUserId
        if (userId == null) {
            showError(binding.errorText, "User ID not found. Please register again.")
            return
        }
        
        // Show loading
        binding.verifyButton.isEnabled = false
        binding.verifyButton.text = "Verifying..."
        binding.resendButton.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        binding.errorText.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val request = VerifyCodeRequest(userId, code)
                val response = RetrofitClient.getAuthApiService().verifyEmailWithCode(request)
                
                binding.progressBar.visibility = View.GONE
                
                if (response.isSuccessful) {
                    val body = response.body()
                    
                    if (body != null && body.token.isNotEmpty()) {
                        // Save credentials and navigate to main
                        authManager.saveCredentials(body.token, body.userId, body.email)
                        
                        verificationDialog?.dismiss()
                        
                        MaterialAlertDialogBuilder(this@AuthActivity)
                            .setTitle("Email Verified! ✓")
                            .setMessage("Welcome to KLCP Quiz!\nYour account is now active.")
                            .setPositiveButton("Start Learning") { _, _ ->
                                navigateToMain()
                            }
                            .setCancelable(false)
                            .show()
                    } else {
                        showError(binding.errorText, body?.message ?: "Verification failed")
                        binding.verifyButton.isEnabled = true
                        binding.verifyButton.text = "Verify Email"
                        binding.resendButton.isEnabled = true
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = when {
                        errorBody?.contains("expired", ignoreCase = true) == true -> "Code expired. Please request a new one."
                        errorBody?.contains("invalid", ignoreCase = true) == true -> "Invalid code. Please check and try again."
                        else -> errorBody ?: "Verification failed"
                    }
                    showError(binding.errorText, errorMessage)
                    binding.verifyButton.isEnabled = true
                    binding.verifyButton.text = "Verify Email"
                    binding.resendButton.isEnabled = true
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                alie.info.newmultichoice.utils.Logger.e("AuthActivity", "Verification error", e)
                showError(binding.errorText, "Network error: ${e.message}")
                binding.verifyButton.isEnabled = true
                binding.verifyButton.text = "Verify Email"
                binding.resendButton.isEnabled = true
            }
        }
    }
    
    private fun resendVerificationCode(binding: DialogVerificationBinding) {
        val userId = pendingUserId
        if (userId == null) {
            showError(binding.errorText, "User ID not found. Please register again.")
            return
        }
        
        binding.resendButton.isEnabled = false
        binding.resendButton.text = "Sending..."
        binding.errorText.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val request = ResendVerificationRequest(userId)
                val response = RetrofitClient.getAuthApiService().resendVerification(request)
                
                if (response.isSuccessful) {
                    binding.hintText.text = "New code sent! Check your email."
                    binding.hintText.setTextColor(resources.getColor(android.R.color.holo_green_light, null))
                } else {
                    val errorBody = response.errorBody()?.string()
                    showError(binding.errorText, errorBody ?: "Failed to resend code")
                }
            } catch (e: Exception) {
                alie.info.newmultichoice.utils.Logger.e("AuthActivity", "Resend error", e)
                showError(binding.errorText, "Network error: ${e.message}")
            } finally {
                binding.resendButton.isEnabled = true
                binding.resendButton.text = "Resend Code"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loginDialog?.dismiss()
        registerDialog?.dismiss()
        verificationDialog?.dismiss()
    }
}
