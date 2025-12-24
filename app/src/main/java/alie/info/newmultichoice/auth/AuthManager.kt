package alie.info.newmultichoice.auth

import android.content.Context
import android.content.SharedPreferences
import alie.info.newmultichoice.api.RetrofitClient
import alie.info.newmultichoice.utils.DeviceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = 
        context.getSharedPreferences("klcp_auth", Context.MODE_PRIVATE)
    
    private val authApi: AuthApiService by lazy {
        RetrofitClient.getAuthApiService()
    }

    companion object {
        @Volatile
        private var INSTANCE: AuthManager? = null
        
        private const val PREF_AUTH_TOKEN = "auth_token"
        private const val PREF_USER_ID = "user_id"
        private const val PREF_USER_EMAIL = "user_email"
        private const val PREF_IS_LOGGED_IN = "is_logged_in"
        private const val PREF_IS_GUEST = "is_guest"

        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    // Check if user is logged in
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(PREF_IS_LOGGED_IN, false) && getToken() != null
    }

    // Get JWT token
    fun getToken(): String? {
        return prefs.getString(PREF_AUTH_TOKEN, null)
    }

    // Get user email
    fun getEmail(): String? {
        return prefs.getString(PREF_USER_EMAIL, null)
    }

    // Get user ID
    fun getUserId(): String? {
        return prefs.getString(PREF_USER_ID, null)
    }

    // Logout
    fun logout() {
        prefs.edit().clear().apply()
        android.util.Log.d("AuthManager", "User logged out")
    }
    
    // Save credentials (for deep link auto-login)
    fun saveCredentials(token: String, userId: String, email: String) {
        prefs.edit()
            .putString(PREF_AUTH_TOKEN, token)
            .putString(PREF_USER_ID, userId)
            .putString(PREF_USER_EMAIL, email)
            .putBoolean(PREF_IS_LOGGED_IN, true)
            .remove(PREF_IS_GUEST) // Clear guest mode when saving credentials
            .apply()
        android.util.Log.d("AuthManager", "Credentials saved for: $email")
    }
    
    // Save auth token (for Google Sign-In)
    fun saveAuthToken(token: String) {
        prefs.edit()
            .putString(PREF_AUTH_TOKEN, token)
            .putBoolean(PREF_IS_LOGGED_IN, true)
            .apply()
        android.util.Log.d("AuthManager", "Auth token saved")
    }
    
    // Save user ID (for Google Sign-In)
    fun saveUserId(userId: String) {
        prefs.edit()
            .putString(PREF_USER_ID, userId)
            .apply()
        android.util.Log.d("AuthManager", "User ID saved: $userId")
    }

    // Register new user
    suspend fun register(email: String, password: String): AuthResult<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val deviceId = DeviceUtils.getDeviceId(context)
                val request = RegisterRequest(email, password, deviceId)
                
                android.util.Log.d("AuthManager", "📝 Registering user: $email")
                
                val response = authApi.register(request)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.error == null) {
                        // Save auth data and clear guest mode
                        prefs.edit()
                            .putString(PREF_AUTH_TOKEN, body.token)
                            .putString(PREF_USER_ID, body.userId)
                            .putString(PREF_USER_EMAIL, body.email)
                            .putBoolean(PREF_IS_LOGGED_IN, true)
                            .remove(PREF_IS_GUEST) // Clear guest mode on successful registration
                            .apply()
                        
                        android.util.Log.d("AuthManager", "✅ Registration successful: ${body.email}")
                        AuthResult.Success(body)
                    } else {
                        val error = body?.error ?: "Registration failed"
                        android.util.Log.e("AuthManager", "❌ Registration error: $error")
                        AuthResult.Error(error)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("AuthManager", "❌ HTTP ${response.code()}: $errorBody")
                    AuthResult.Error(parseErrorMessage(errorBody))
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthManager", "❌ Registration exception", e)
                AuthResult.Error("Network error: ${e.message}")
            }
        }
    }

    // Login user
    suspend fun login(email: String, password: String): AuthResult<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val deviceId = DeviceUtils.getDeviceId(context)
                val request = LoginRequest(email, password, deviceId)
                
                android.util.Log.d("AuthManager", "🔐 Logging in user: $email")
                
                val response = authApi.login(request)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.error == null) {
                        // Save auth data and clear guest mode
                        prefs.edit()
                            .putString(PREF_AUTH_TOKEN, body.token)
                            .putString(PREF_USER_ID, body.userId)
                            .putString(PREF_USER_EMAIL, body.email)
                            .putBoolean(PREF_IS_LOGGED_IN, true)
                            .remove(PREF_IS_GUEST) // Clear guest mode on successful login
                            .apply()
                        
                        android.util.Log.d("AuthManager", "✅ Login successful: ${body.email}")
                        AuthResult.Success(body)
                    } else {
                        val error = body?.error ?: "Login failed"
                        android.util.Log.e("AuthManager", "❌ Login error: $error")
                        AuthResult.Error(error)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("AuthManager", "❌ HTTP ${response.code()}: $errorBody")
                    AuthResult.Error(parseErrorMessage(errorBody))
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthManager", "❌ Login exception", e)
                AuthResult.Error("Network error: ${e.message}")
            }
        }
    }

    // Delete account
    suspend fun deleteAccount(): AuthResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getToken()
                if (token == null) {
                    return@withContext AuthResult.Error("Not logged in")
                }
                
                android.util.Log.d("AuthManager", "🗑️ Deleting account...")
                
                val response = authApi.deleteAccount("Bearer $token")
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.error == null) {
                        // Clear local auth data
                        logout()
                        android.util.Log.d("AuthManager", "✅ Account deleted successfully")
                        AuthResult.Success(true)
                    } else {
                        val error = body?.error ?: "Delete failed"
                        android.util.Log.e("AuthManager", "❌ Delete error: $error")
                        AuthResult.Error(error)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("AuthManager", "❌ HTTP ${response.code()}: $errorBody")
                    AuthResult.Error(parseErrorMessage(errorBody))
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthManager", "❌ Delete account exception", e)
                AuthResult.Error("Network error: ${e.message}")
            }
        }
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "Unknown error"
        return try {
            val gson = com.google.gson.Gson()
            val error = gson.fromJson(errorBody, ErrorResponse::class.java)
            error.error ?: error.message ?: "Unknown error"
        } catch (e: Exception) {
            errorBody
        }
    }
    
    // Guest mode methods
    fun setGuestMode(isGuest: Boolean) {
        prefs.edit().putBoolean(PREF_IS_GUEST, isGuest).apply()
    }
    
    fun isGuestMode(): Boolean {
        return prefs.getBoolean(PREF_IS_GUEST, false)
    }
    
    fun clearGuestMode() {
        prefs.edit().remove(PREF_IS_GUEST).apply()
    }
}

// Result wrapper
sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
}

// Error response model
data class ErrorResponse(
    val error: String? = null,
    val message: String? = null
)
