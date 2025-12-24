package alie.info.newmultichoice

import android.animation.AnimatorInflater
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import alie.info.newmultichoice.databinding.DialogLoadingBinding
import androidx.core.view.WindowCompat
import androidx.core.view.GravityCompat
import alie.info.newmultichoice.auth.AuthActivity
import alie.info.newmultichoice.auth.AuthManager
import alie.info.newmultichoice.auth.ProfileManager
import alie.info.newmultichoice.utils.VersionManager
import alie.info.newmultichoice.utils.VersionCheckResult
import alie.info.newmultichoice.databinding.ActivityMainBinding
import alie.info.newmultichoice.databinding.DialogProfileBinding
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var gestureDetector: GestureDetector
    private lateinit var authManager: AuthManager
    private lateinit var profileManager: ProfileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen (Material You style)
        val splashScreen = installSplashScreen()
        
        // Skip splash screen if requested (e.g., from guest mode)
        val skipSplash = intent.getBooleanExtra("skip_splash", false)
        
        if (skipSplash) {
            // Immediately hide splash screen
            splashScreen.setKeepOnScreenCondition { false }
        } else {
            // Keep splash screen visible for 3 seconds
            var keepSplashOnScreen = true
            splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

            // Hide splash after 3 seconds using Handler (lifecycle-aware)
            Handler(Looper.getMainLooper()).postDelayed({
                keepSplashOnScreen = false
            }, 3000)
        }
        
        super.onCreate(savedInstanceState)
        
        // Override activity transition animation if coming from AuthActivity
        if (skipSplash) {
            overridePendingTransition(0, 0)
        }
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize AuthManager and ProfileManager
        authManager = AuthManager.getInstance(this)
        profileManager = ProfileManager.getInstance(this)

        // No toolbar/actionbar
        supportActionBar?.hide()

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        
        // Request notification permission (Android 13+)
        alie.info.newmultichoice.utils.NotificationPermissionHelper.requestNotificationPermission(this)
        
        // Schedule background sync - TEMPORARILY DISABLED TO PREVENT EMULATOR CRASHES
        // alie.info.newmultichoice.sync.SyncWorker.schedulePeriodicSync(this)
        
        // Check for demo mode
        val isDemoMode = intent.getBooleanExtra("demo_mode", false)
        if (isDemoMode) {
            // Navigate directly to quiz with demo mode after a short delay
            lifecycleScope.launch {
                kotlinx.coroutines.delay(500) // Small delay to ensure navigation is ready
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                val action = alie.info.newmultichoice.ui.home.HomeFragmentDirections
                    .actionNavHomeToQuizFragment(practiceMode = false)
                navController.navigate(action)
            }
        }
        
        // Check if should navigate directly to home (from guest mode transition)
        // Note: Home is already the default destination, so we don't need to navigate again
        // The navigateToHome extra is just for clarity, but navigation happens automatically
        
        // Setup navigation without toolbar
        // Empty set = all destinations are top-level, left swipe always opens drawer
        appBarConfiguration = AppBarConfiguration(
            setOf(), // Empty = all screens allow drawer swipe
            drawerLayout
        )
        navView.setupWithNavController(navController)
        
        // Disable drawer swipe-to-open (we want explicit control)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        
        // Setup profile header click
        setupProfileHeader(navView, drawerLayout)
        
        // Handle menu item clicks
        navView.setNavigationItemSelectedListener { menuItem ->
            // Check if guest mode - show upgrade prompt for all menu items except home
            if (authManager.isGuestMode() && menuItem.itemId != R.id.nav_home) {
                drawerLayout.closeDrawer(navView)
                showGuestUpgradePrompt()
                return@setNavigationItemSelectedListener false
            }
            
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    navController.navigate(R.id.nav_home)
                    drawerLayout.closeDrawer(navView)
                    true
                }
                R.id.nav_practice_wrong -> {
                    // Navigate to quiz with practice mode
                    navController.navigate(R.id.quizFragment, Bundle().apply {
                        putBoolean("practiceMode", true)
                    })
                    drawerLayout.closeDrawer(navView)
                    true
                }
                R.id.nav_sessions -> {
                    navController.navigate(R.id.nav_sessions)
                    drawerLayout.closeDrawer(navView)
                    true
                }
                R.id.nav_statistics -> {
                    navController.navigate(R.id.nav_statistics)
                    drawerLayout.closeDrawer(navView)
                    true
                }
                R.id.nav_achievements -> {
                    navController.navigate(R.id.nav_achievements)
                    drawerLayout.closeDrawer(navView)
                    true
                }
                R.id.nav_exam_list -> {
                    navController.navigate(R.id.nav_exam_list)
                    drawerLayout.closeDrawer(navView)
                    true
                }
                R.id.nav_kali_tools -> {
                    navController.navigate(R.id.nav_kali_tools)
                    drawerLayout.closeDrawer(navView)
                    true
                }
                R.id.nav_faq -> {
                    navController.navigate(R.id.nav_faq)
                    drawerLayout.closeDrawer(navView)
                    true
                }
                R.id.nav_logout -> {
                    drawerLayout.closeDrawer(navView)
                    showLogoutDialog()
                    true
                }
                R.id.nav_delete_account -> {
                    drawerLayout.closeDrawer(navView)
                    showDeleteAccountDialog()
                    true
                }
                else -> false
            }
        }
        
        // Setup menu bubble to open drawer with hover animation
        binding.appBarMain.menuBubble.setOnClickListener { view ->
            // Animate click
            view.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(100)
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
            
            if (drawerLayout.isDrawerOpen(navView)) {
                drawerLayout.closeDrawer(navView)
            } else {
                drawerLayout.openDrawer(navView)
            }
        }
        
        // Hide menu bubble when in quiz
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.quizFragment -> {
                    binding.appBarMain.menuBubble.visibility = View.GONE
                }
                else -> {
                    binding.appBarMain.menuBubble.visibility = View.VISIBLE
                }
            }
        }
        
        // Setup custom gesture detector
        setupGestureDetector(drawerLayout, navController)

        // Check version compatibility in background
        checkVersionCompatibility()
    }
    
    private fun setupGestureDetector(drawerLayout: DrawerLayout, navController: androidx.navigation.NavController) {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100
            
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Right swipe (left to right) = DISABLED (no menu opening)
                            // User must use menu button instead
                            return false
                        } else {
                            // Left swipe (right to left) = Go Back
                            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                                // If drawer is open, close it
                                drawerLayout.closeDrawer(GravityCompat.START)
                                return true
                            }
                            
                            // Check if we're on the home screen
                            val currentDestination = navController.currentDestination?.id
                            if (currentDestination == R.id.nav_home) {
                                // On home screen - do nothing (don't close app, don't navigate)
                                return false // Let other handlers deal with it
                            }
                            
                            // Not on home screen - navigate back if possible
                            if (navController.previousBackStackEntry != null) {
                                navController.navigateUp()
                                return true
                            }
                            
                            return false
                        }
                    }
                }
                return false
            }
        })
    }
    
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    // No options menu needed without action bar
    
    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.logout_title)
            .setMessage(R.string.logout_message)
            .setPositiveButton(R.string.confirm) { _, _ ->
                performLogout()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showDeleteAccountDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_account_title)
            .setMessage(R.string.delete_account_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                performDeleteAccount()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun performLogout() {
        authManager.logout()
        navigateToAuth()
    }
    
    private fun performDeleteAccount() {
        lifecycleScope.launch {
            try {
                val result = authManager.deleteAccount()
                when (result) {
                    is alie.info.newmultichoice.auth.AuthResult.Success -> {
                        Snackbar.make(binding.root, "Account deleted", Snackbar.LENGTH_SHORT).show()
                        navigateToAuth()
                    }
                    is alie.info.newmultichoice.auth.AuthResult.Error -> {
                        Snackbar.make(binding.root, "Error: ${result.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }
    
    private fun navigateToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun setupProfileHeader(navView: NavigationView, drawerLayout: DrawerLayout) {
        val headerView = navView.getHeaderView(0)
        val profileName = headerView.findViewById<android.widget.TextView>(R.id.profileName)
        val profileEmail = headerView.findViewById<android.widget.TextView>(R.id.profileEmail)
        
        // Setup 3D rotating logo in header
        setup3DRotatingLogoHeader(headerView)
        
        // Update header with current profile info
        updateProfileHeader(profileName, profileEmail)
        
        // Make entire header clickable
        headerView.setOnClickListener {
            drawerLayout.closeDrawer(navView)
            showProfileDialog()
        }
    }
    
    /**
     * Setup 3D rotating logo in navigation drawer header
     */
    private fun setup3DRotatingLogoHeader(headerView: View) {
        val container = headerView.findViewById<View>(R.id.logoRotationContainer)
        val frontLogo = headerView.findViewById<android.widget.ImageView>(R.id.appLogo)
        val backLogo = headerView.findViewById<android.widget.ImageView>(R.id.klcpLogo)
        
        if (container == null || frontLogo == null || backLogo == null) return
        
        // Set camera distance for 3D effect
        val cameraDistance = 8000f * resources.displayMetrics.density
        container.cameraDistance = cameraDistance
        frontLogo.cameraDistance = cameraDistance
        backLogo.cameraDistance = cameraDistance
        
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
        
        // Start rotation
        container.postDelayed({
            rotationAnimator.start()
        }, 500)
    }
    
    private fun updateProfileHeader(nameView: android.widget.TextView, emailView: android.widget.TextView) {
        val name = profileManager.getName()
        val email = authManager.getEmail()
        
        if (!name.isNullOrBlank()) {
            nameView.text = name
        } else {
            nameView.text = "Tap to edit profile"
        }
        
        emailView.text = email ?: ""
    }
    
    private fun showProfileDialog() {
        val dialogBinding = DialogProfileBinding.inflate(layoutInflater)
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()
        
        // Pre-fill current values
        dialogBinding.nameInput.setText(profileManager.getName() ?: "")
        val age = profileManager.getAge()
        if (age > 0) {
            dialogBinding.ageInput.setText(age.toString())
        }
        dialogBinding.emailInput.setText(authManager.getEmail() ?: "")
        
        dialogBinding.saveButton.setOnClickListener {
            val name = dialogBinding.nameInput.text.toString().trim()
            val ageText = dialogBinding.ageInput.text.toString().trim()
            
            if (name.isBlank()) {
                dialogBinding.nameInput.error = "Name is required"
                return@setOnClickListener
            }
            
            profileManager.setName(name)
            if (ageText.isNotBlank()) {
                profileManager.setAge(ageText.toIntOrNull() ?: 0)
            }
            
            // Update header
            val navView: NavigationView = binding.navView
            val headerView = navView.getHeaderView(0)
            val profileName = headerView.findViewById<android.widget.TextView>(R.id.profileName)
            val profileEmail = headerView.findViewById<android.widget.TextView>(R.id.profileEmail)
            updateProfileHeader(profileName, profileEmail)
            
            Snackbar.make(binding.root, "Profile saved", Snackbar.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        
        dialogBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
    
    private fun showGuestUpgradePrompt() {
        try {
            val dialogBinding = alie.info.newmultichoice.databinding.DialogUpgradePromptBinding.inflate(layoutInflater)
            
            val dialog = MaterialAlertDialogBuilder(this, R.style.TransparentDialog)
                .setView(dialogBinding.root)
                .setCancelable(true)
                .create()
            
            dialogBinding.signUpButton.setOnClickListener {
                dialog.dismiss()
                
                // Show modern loading dialog
                val loadingBinding = alie.info.newmultichoice.databinding.DialogLoadingBinding.inflate(layoutInflater)
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
                
                // Start AuthActivity and keep loading dialog visible
                val intent = Intent(this, AuthActivity::class.java)
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
        } catch (e: Exception) {
            alie.info.newmultichoice.utils.Logger.e("MainActivity", "Error showing guest upgrade prompt", e)
        }
    }
    
    private fun showTransitionOverlay(onComplete: (() -> Unit)? = null) {
        try {
            // Show overlay immediately - don't wait
            val overlayBinding = alie.info.newmultichoice.databinding.OverlayAppTransitionBinding.inflate(layoutInflater)
            val overlayView = overlayBinding.root
            
            // Add overlay to window immediately (before anything else is visible)
            val rootView = window.decorView.rootView as ViewGroup
            rootView.addView(overlayView)
            
            // Start animation
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
                                // Call completion callback
                                onComplete?.invoke()
                            }
                        })
                        
                        fadeOut.start()
                    }, 600) // Hold icon visible for 600ms
                }
            })
            
            // Start animations
            fadeIn.start()
            scaleUp.start()
            scaleUpY.start()
        } catch (e: Exception) {
            alie.info.newmultichoice.utils.Logger.e("MainActivity", "Error showing transition overlay", e)
            // Call completion even on error
            onComplete?.invoke()
        }
    }

    @Suppress("DEPRECATION")
    private fun finishWithTransition() {
        finish()
        // Use deprecated API with suppression since it's still needed for older Android versions
        overridePendingTransition(0, 0)
    }

    private fun checkVersionCompatibility() {
        lifecycleScope.launch {
            when (val result = VersionManager.checkVersionCompatibility()) {
                is VersionCheckResult.Compatible -> {
                    // Everything is fine, no action needed
                    alie.info.newmultichoice.utils.Logger.d("MainActivity", "App version is compatible with server")
                }
                is VersionCheckResult.UpdateRecommended -> {
                    showVersionDialog(
                        title = "Update Empfohlen",
                        message = result.message,
                        updateUrl = result.updateUrl,
                        isRequired = false
                    )
                }
                is VersionCheckResult.Incompatible -> {
                    showVersionDialog(
                        title = "Update Erforderlich",
                        message = result.message,
                        updateUrl = result.updateUrl,
                        isRequired = result.updateRequired
                    )
                }
                is VersionCheckResult.Error -> {
                    // Log error but don't show dialog for network issues
                    alie.info.newmultichoice.utils.Logger.w("MainActivity", "Version check failed: ${result.message}")
                }
            }
        }
    }

    private fun showVersionDialog(title: String, message: String, updateUrl: String?, isRequired: Boolean) {
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(!isRequired)

        if (updateUrl != null) {
            builder.setPositiveButton("Update") { _, _ ->
                // Open update URL
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(updateUrl))
                startActivity(intent)
            }
        }

        if (!isRequired) {
            builder.setNegativeButton("Später", null)
        }

        builder.show()
    }
}