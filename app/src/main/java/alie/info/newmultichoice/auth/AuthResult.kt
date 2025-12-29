package alie.info.newmultichoice.auth

/**
 * Enhanced AuthResult with specific error types
 */
sealed class AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>()
    data class Error(val error: AuthError) : AuthResult<Nothing>()

    /**
     * Specific error types for better error handling
     */
    sealed class AuthError {
        // Network errors
        data class NetworkError(val message: String, val isRetryable: Boolean = true) : AuthError()
        data class TimeoutError(val message: String = "Request timed out") : AuthError()
        data class NoInternetError(val message: String = "No internet connection") : AuthError()

        // Server errors
        data class ServerError(val code: Int, val message: String) : AuthError()
        data class MaintenanceError(val message: String = "Server is under maintenance") : AuthError()

        // Authentication errors
        data class InvalidCredentials(val message: String = "Invalid email or password") : AuthError()
        data class AccountLocked(val message: String = "Account is temporarily locked") : AuthError()
        data class EmailNotVerified(val message: String = "Please verify your email first") : AuthError()

        // Validation errors
        data class ValidationError(val field: String, val message: String) : AuthError()
        data class WeakPassword(val message: String = "Password is too weak") : AuthError()

        // Other errors
        data class UnknownError(val message: String, val cause: Throwable? = null) : AuthError()

        /**
         * User-friendly error message
         */
        fun getUserMessage(): String = when (this) {
            is NetworkError -> "Verbindungsproblem. Bitte prüfen Sie Ihre Internetverbindung."
            is TimeoutError -> "Verbindung zu langsam. Bitte versuchen Sie es später erneut."
            is NoInternetError -> "Keine Internetverbindung. Bitte gehen Sie online."
            is ServerError -> "Server-Fehler ($code). Bitte versuchen Sie es später erneut."
            is MaintenanceError -> "Server wird gewartet. Bitte versuchen Sie es später erneut."
            is InvalidCredentials -> "Falsche E-Mail oder Passwort. Bitte prüfen Sie Ihre Eingaben."
            is AccountLocked -> "Account vorübergehend gesperrt. Versuchen Sie es später erneut."
            is EmailNotVerified -> "Bitte bestätigen Sie zuerst Ihre E-Mail-Adresse."
            is ValidationError -> "$field: $message"
            is WeakPassword -> "Passwort ist zu schwach. Verwenden Sie mindestens 8 Zeichen."
            is UnknownError -> "Unbekannter Fehler. Bitte versuchen Sie es später erneut."
        }

        /**
         * Whether the user should retry the action
         */
        fun shouldRetry(): Boolean = when (this) {
            is NetworkError -> isRetryable
            is TimeoutError -> true
            is NoInternetError -> false // User muss Internet aktivieren
            is ServerError -> code >= 500 // 5xx Fehler sind retryable
            is MaintenanceError -> false
            is InvalidCredentials -> false
            is AccountLocked -> false
            is EmailNotVerified -> false
            is ValidationError -> false
            is WeakPassword -> false
            is UnknownError -> true
        }
    }
}