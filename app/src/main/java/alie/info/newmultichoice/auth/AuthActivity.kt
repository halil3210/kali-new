package alie.info.newmultichoice.auth

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import alie.info.newmultichoice.MainActivity
import alie.info.newmultichoice.api.RetrofitClient
import alie.info.newmultichoice.databinding.ActivityAuthStartBinding
import alie.info.newmultichoice.databinding.DialogLoginBinding
import alie.info.newmultichoice.databinding.DialogRegisterBinding
import alie.info.newmultichoice.databinding.DialogVerificationBinding
import alie.info.newmultichoice.databinding.OverlayAppTransitionBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Task

class AuthActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    private var loginDialog: androidx.appcompat.app.AlertDialog? = null
    private var registerDialog: androidx.appcompat.app.AlertDialog? = null
    private var verificationDialog: androidx.appcompat.app.AlertDialog? = null
    
    // Store user info for verification
    private var pendingUserId: String? = null
    private var pendingEmail: String? = null
    private var pendingToken: String? = null
    
    // Google Sign-In
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen (Material You style)
        val splashScreen = installSplashScreen()
        
        // Keep splash screen visible for 3 seconds
        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        
        // Hide splash after 3 seconds
        Thread {
            Thread.sleep(3000)
            keepSplashOnScreen = false
        }.start()
        
        super.onCreate(savedInstanceState)
        
        // Switch from Splash theme to normal theme (for Material Components)
        setTheme(alie.info.newmultichoice.R.style.Theme_KLCPQuiz)
        
        authManager = AuthManager.getInstance(this)
        
        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(alie.info.newmultichoice.R.string.default_web_client_id))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

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
            android.util.Log.d("AuthActivity", "Deep link received: $data")
            
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
        android.util.Log.d("AuthActivity", "Verifying email with token: $token")
        
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
                android.util.Log.e("AuthActivity", "Verification error", e)
                
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
        
        binding.googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
        
        binding.signUpButton.setOnClickListener {
            showLoginDialog()
        }
        
        binding.guestButton.setOnClickListener {
            startGuestMode()
        }
    }
    
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleGoogleSignInResult(task)
        }
    }
    
    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            account?.let {
                // Get user info from Google account
                val email = it.email ?: ""
                val idToken = it.idToken ?: ""
                val displayName = it.displayName ?: ""
                
                android.util.Log.d("AuthActivity", "Google Sign-In successful: $email")
                
                // Send to server for authentication
                lifecycleScope.launch {
                    authenticateWithGoogle(idToken, email, displayName)
                }
            }
        } catch (e: ApiException) {
            android.util.Log.e("AuthActivity", "Google Sign-In failed: code=${e.statusCode}, message=${e.message}", e)
            
            val errorMessage = when (e.statusCode) {
                CommonStatusCodes.NETWORK_ERROR -> "Network error. Please check your internet connection."
                CommonStatusCodes.INTERNAL_ERROR -> "Internal error. Please try again later."
                12501 -> {
                    // Sign-in was cancelled by user or configuration issue
                    val clientId = getString(alie.info.newmultichoice.R.string.default_web_client_id)
                    if (clientId == "YOUR_GOOGLE_WEB_CLIENT_ID_HERE" || clientId.isEmpty()) {
                        "Google Sign-In is not configured. Please contact the developer."
                    } else {
                        "Sign-in was cancelled or Google Sign-In is not properly configured. Please try again."
                    }
                }
                12500 -> "Sign-in failed. Please try again."
                10 -> {
                    // DEVELOPER_ERROR - Usually means OAuth client configuration issue
                    "Google Sign-In configuration error. Please ensure:\n" +
                    "1. Android OAuth Client is created in Google Cloud Console\n" +
                    "2. Package name matches: alie.info.newmultichoice\n" +
                    "3. SHA-1 fingerprint is registered\n" +
                    "4. Wait 5-10 minutes after creating the OAuth client\n" +
                    "5. Reinstall the app or clear Google Play Services cache"
                }
                else -> "Could not sign in with Google. Error code: ${e.statusCode}"
            }
            
            MaterialAlertDialogBuilder(this)
                .setTitle("Sign-In Failed")
                .setMessage(errorMessage)
                .setPositiveButton("OK", null)
                .show()
        }
    }
    
    private suspend fun authenticateWithGoogle(idToken: String, email: String, displayName: String) {
        try {
            android.util.Log.d("AuthActivity", "authenticateWithGoogle called: email=$email, idToken=${idToken.take(20)}...")
            
            val loadingDialog = MaterialAlertDialogBuilder(this)
                .setTitle("Signing in...")
                .setMessage("Please wait while we sign you in.")
                .setCancelable(false)
                .show()
            
            val apiService = RetrofitClient.getAuthApiService()
            val request = GoogleSignInRequest(
                idToken = idToken,
                email = email,
                displayName = displayName
            )
            
            val response = apiService.googleSignIn(request)
            
            loadingDialog.dismiss()
            
            if (response.isSuccessful) {
                val authResponse = response.body()
                if (authResponse != null && authResponse.token != null) {
                    // Save credentials
                    authManager.saveCredentials(
                        token = authResponse.token,
                        userId = authResponse.userId,
                        email = authResponse.email
                    )
                    authManager.setGuestMode(false)
                    
                    android.util.Log.d("AuthActivity", "Google Sign-In successful, navigating to MainActivity")
                    
                    // Navigate to main activity
                    navigateToMain()
                } else {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Sign-In Failed")
                        .setMessage("Invalid response from server.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("AuthActivity", "Google Sign-In server error: code=${response.code()}, body=$errorBody")
                MaterialAlertDialogBuilder(this)
                    .setTitle("Sign-In Failed")
                    .setMessage("Server error (${response.code()}): ${errorBody ?: "Could not sign in with Google."}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthActivity", "Google authentication error", e)
            MaterialAlertDialogBuilder(this)
                .setTitle("Error")
                .setMessage("Network error: ${e.message}")
                .setPositiveButton("OK", null)
                .show()
        }
    }
    
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
                android.util.Log.d("AuthActivity", "Sign up button clicked")
                dialog.dismiss()
                // Clear guest mode
                authManager.clearGuestMode()
                // Show login/register dialog
                showLoginDialog()
            }
            
            dialogBinding.maybeLaterButton.setOnClickListener {
                android.util.Log.d("AuthActivity", "Maybe later button clicked")
                // Dismiss dialog
                dialog.dismiss()
                // Show transition overlay with app icon
                showTransitionOverlay()
            }
            
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            android.util.Log.d("AuthActivity", "Showing upgrade prompt dialog...")
            dialog.show()
        } catch (e: Exception) {
            android.util.Log.e("AuthActivity", "Error showing upgrade prompt", e)
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
            android.util.Log.e("AuthActivity", "Error showing transition overlay", e)
            // Fallback: navigate directly
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("skip_splash", true)
            }
            startActivity(intent)
            finish()
        }
    }
    
    private fun showLoginDialog() {
        val binding = DialogLoginBinding.inflate(layoutInflater)

        loginDialog = MaterialAlertDialogBuilder(this)
            .setView(binding.root)
            .setCancelable(false)
            .create()

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
            loginDialog?.dismiss()
            showRegisterDialog()
        }

        loginDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loginDialog?.show()
    }

    private fun showRegisterDialog() {
        val binding = DialogRegisterBinding.inflate(layoutInflater)

        registerDialog = MaterialAlertDialogBuilder(this)
            .setView(binding.root)
            .setCancelable(false)
            .create()

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
            registerDialog?.dismiss()
            showLoginDialog()
        }

        registerDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        registerDialog?.show()
    }

    private fun performLogin(binding: DialogLoginBinding, email: String, password: String) {
        // Show loading
        binding.loginButton.isEnabled = false
        binding.loginButton.text = "Logging in..."

        lifecycleScope.launch {
            when (val result = authManager.login(email, password)) {
                is AuthResult.Success -> {
                    android.util.Log.d("AuthActivity", "Login successful")
                    loginDialog?.dismiss()
                    navigateToMain()
                }
                is AuthResult.Error -> {
                    android.util.Log.e("AuthActivity", "Login failed: ${result.message}")
                    binding.loginButton.isEnabled = true
                    binding.loginButton.text = "Login"
                    showError(binding.errorText, result.message)
                }
            }
        }
    }

    private fun performRegistration(binding: DialogRegisterBinding, email: String, password: String) {
        // Show loading
        binding.registerButton.isEnabled = false
        binding.registerButton.text = "Registering..."

        lifecycleScope.launch {
            when (val result = authManager.register(email, password)) {
                is AuthResult.Success -> {
                    android.util.Log.d("AuthActivity", "Registration successful")
                    
                    // Store pending user info for verification
                    pendingUserId = result.data.userId
                    pendingEmail = result.data.email
                    pendingToken = result.data.token
                    
                    // Close register dialog and show verification dialog
                    registerDialog?.dismiss()
                    showVerificationDialog()
                }
                is AuthResult.Error -> {
                    android.util.Log.e("AuthActivity", "Registration failed: ${result.message}")
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

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
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
                android.util.Log.e("AuthActivity", "Verification error", e)
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
                android.util.Log.e("AuthActivity", "Resend error", e)
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
