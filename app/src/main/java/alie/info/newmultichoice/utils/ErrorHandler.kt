package alie.info.newmultichoice.utils

import alie.info.newmultichoice.auth.AuthResult
import android.content.Context
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import android.view.View

class ErrorHandler(private val context: Context) {

    fun handleAuthError(error: AuthResult.AuthError, fragment: Fragment, anchorView: View? = null) {
        when {
            // Zeige Snackbar für retryable Fehler
            error.shouldRetry() -> {
                showRetryableError(error, fragment, anchorView)
            }
            // Zeige Dialog für kritische Fehler
            else -> {
                showCriticalError(error, fragment)
            }
        }
    }

    private fun showRetryableError(error: AuthResult.AuthError, fragment: Fragment, anchorView: View?) {
        val message = error.getUserMessage()
        val snackbar = Snackbar.make(
            anchorView ?: fragment.requireView(),
            message,
            Snackbar.LENGTH_LONG
        )

        if (error.shouldRetry()) {
            snackbar.setAction("Retry") {
                // Hier könnte ein Retry-Callback aufgerufen werden
                Logger.d("ErrorHandler", "User requested retry")
            }
        }

        snackbar.show()
    }

    private fun showCriticalError(error: AuthResult.AuthError, fragment: Fragment) {
        MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle("Error")
            .setMessage(error.getUserMessage())
            .setPositiveButton("OK", null)
            .setNegativeButton("Report") { _, _ ->
                reportError(error)
            }
            .show()
    }

    private fun reportError(error: AuthResult.AuthError) {
        // Hier könnte ein Bug-Reporting-System integriert werden
        Logger.e("ErrorHandler", "User reported error: ${error.getUserMessage()}")
        Toast.makeText(context, "Error reported. Thank you!", Toast.LENGTH_SHORT).show()
    }

    fun handleNetworkError(fragment: Fragment, retryAction: (() -> Unit)? = null) {
        MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle("Connection Problem")
            .setMessage("Unable to connect to the server. Please check your internet connection and try again.")
            .setPositiveButton("Retry") { _, _ ->
                retryAction?.invoke()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun handleOfflineMode(fragment: Fragment) {
        MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle("Offline Mode")
            .setMessage("You're currently offline. Some features may not be available.")
            .setPositiveButton("Continue Offline", null)
            .setNegativeButton("Retry Connection") { _, _ ->
                // Hier könnte ein Connection-Check implementiert werden
            }
            .show()
    }
}
