package alie.info.newmultichoice.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import alie.info.newmultichoice.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AuthManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    init {
        _isLoggedIn.value = isLoggedIn()
    }

    companion object {
        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getString("token", null) != null
    }

    fun isGuestMode(): Boolean {
        return prefs.getBoolean("guest_mode", false)
    }

    fun setGuestMode(guestMode: Boolean) {
        prefs.edit().putBoolean("guest_mode", guestMode).apply()
    }

    fun clearGuestMode() {
        prefs.edit().remove("guest_mode").apply()
    }

    suspend fun login(email: String, password: String): AuthResult<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.getAuthApiService().login(LoginRequest(email, password))

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.token != null && body.userId != null && body.email != null) {
                        saveCredentials(body.token, body.userId, body.email)
                        _isLoggedIn.postValue(true)
                        AuthResult.Success(body)
                    } else {
                        AuthResult.Error(AuthResult.AuthError.ServerError(response.code(), "Invalid server response"))
                    }
                } else {
                    when (response.code()) {
                        400 -> AuthResult.Error(AuthResult.AuthError.ValidationError("credentials", "Invalid email or password format"))
                        401 -> AuthResult.Error(AuthResult.AuthError.InvalidCredentials())
                        429 -> AuthResult.Error(AuthResult.AuthError.ServerError(429, "Too many login attempts. Please wait."))
                        in 500..599 -> AuthResult.Error(AuthResult.AuthError.ServerError(response.code(), "Server error"))
                        else -> AuthResult.Error(AuthResult.AuthError.UnknownError("Login failed with code ${response.code()}"))
                    }
                }
            } catch (e: SocketTimeoutException) {
                AuthResult.Error(AuthResult.AuthError.TimeoutError())
            } catch (e: UnknownHostException) {
                AuthResult.Error(AuthResult.AuthError.NoInternetError())
            } catch (e: ConnectException) {
                AuthResult.Error(AuthResult.AuthError.NetworkError("Cannot connect to server", true))
            } catch (e: Exception) {
                AuthResult.Error(AuthResult.AuthError.UnknownError("Unexpected error: ${e.localizedMessage}", e))
            }
        }
    }

    suspend fun register(email: String, password: String): AuthResult<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.getAuthApiService().register(RegisterRequest(email, password))

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.userId != null && body.email != null) {
                        AuthResult.Success(body)
                    } else {
                        AuthResult.Error(AuthResult.AuthError.ServerError(response.code(), "Invalid server response"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    AuthResult.Error(AuthResult.AuthError.UnknownError(errorBody ?: "Registration failed"))
                }
            } catch (e: SocketTimeoutException) {
                AuthResult.Error(AuthResult.AuthError.TimeoutError())
            } catch (e: UnknownHostException) {
                AuthResult.Error(AuthResult.AuthError.NoInternetError())
            } catch (e: ConnectException) {
                AuthResult.Error(AuthResult.AuthError.NetworkError("Cannot connect to server", true))
            } catch (e: Exception) {
                AuthResult.Error(AuthResult.AuthError.UnknownError("Unexpected error: ${e.localizedMessage}", e))
            }
        }
    }

    fun saveCredentials(token: String, userId: String, email: String) {
        prefs.edit()
            .putString("token", token)
            .putString("userId", userId)
            .putString("email", email)
            .apply()
        _isLoggedIn.postValue(true)
    }

    fun logout() {
        prefs.edit()
            .remove("token")
            .remove("userId")
            .remove("email")
            .remove("guest_mode")
            .apply()
        _isLoggedIn.postValue(false)
    }

    suspend fun deleteAccount(): AuthResult<DeleteResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val token = prefs.getString("token", null)
                if (token == null) {
                    return@withContext AuthResult.Error(AuthResult.AuthError.InvalidCredentials("No authentication token found"))
                }

                val response = RetrofitClient.getAuthApiService().deleteAccount("Bearer $token")

                if (response.isSuccessful) {
                    logout()
                    AuthResult.Success(DeleteResponse(success = true, message = "Account deleted successfully"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    AuthResult.Error(AuthResult.AuthError.UnknownError(errorBody ?: "Account deletion failed"))
                }
            } catch (e: Exception) {
                AuthResult.Error(AuthResult.AuthError.NetworkError("Network error: ${e.message}", false))
            }
        }
    }

    fun getToken(): String? = prefs.getString("token", null)
    fun getUserId(): String? = prefs.getString("userId", null)
    fun getEmail(): String? = prefs.getString("email", null)
}
