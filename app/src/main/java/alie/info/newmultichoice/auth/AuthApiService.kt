package alie.info.newmultichoice.auth

import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(
        @Body registerRequest: RegisterRequest
    ): Response<AuthResponse>

    @POST("api/auth/verify-email-token")
    suspend fun verifyEmailWithToken(
        @Body tokenRequest: TokenRequest
    ): Response<AuthResponse>

    @POST("api/auth/verify-email")
    suspend fun verifyEmail(
        @Body verifyRequest: VerifyRequest
    ): Response<VerifyResponse>

    @POST("api/auth/resend-verification")
    suspend fun resendVerificationCode(
        @Body resendRequest: ResendRequest
    ): Response<VerifyResponse>

    @DELETE("api/auth/account")
    suspend fun deleteAccount(
        @Header("Authorization") authHeader: String
    ): Response<DeleteResponse>
}

// Request/Response Models
data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String
)

data class TokenRequest(
    val token: String
)

data class VerifyRequest(
    val userId: String,
    val code: String
)

data class ResendRequest(
    val userId: String,
    val email: String
)

data class AuthResponse(
    val success: Boolean = false,
    val message: String = "",
    val token: String? = null,
    val userId: String? = null,
    val email: String? = null
)

data class VerifyResponse(
    val success: Boolean = false,
    val message: String = ""
)

data class DeleteResponse(
    val success: Boolean = false,
    val message: String = ""
)
