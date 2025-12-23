package alie.info.newmultichoice.auth

import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {
    
    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>
    
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>
    
    @DELETE("api/auth/delete-account")
    suspend fun deleteAccount(
        @Header("Authorization") token: String
    ): Response<DeleteAccountResponse>
    
    @GET("api/auth/verify-app")
    suspend fun verifyEmailWithToken(
        @Query("token") token: String
    ): Response<AuthResponse>
    
    @POST("api/auth/verify-code")
    suspend fun verifyEmailWithCode(
        @Body request: VerifyCodeRequest
    ): Response<AuthResponse>
    
    @POST("api/auth/resend-verification")
    suspend fun resendVerification(
        @Body request: ResendVerificationRequest
    ): Response<ResendVerificationResponse>
    
    @POST("api/auth/google-signin")
    suspend fun googleSignIn(
        @Body request: GoogleSignInRequest
    ): Response<AuthResponse>
}

data class DeleteAccountResponse(
    val message: String? = null,
    val error: String? = null
)

data class VerifyCodeRequest(
    val userId: String,
    val code: String
)

data class ResendVerificationRequest(
    val userId: String
)

data class ResendVerificationResponse(
    val message: String? = null,
    val error: String? = null
)

data class GoogleSignInRequest(
    val idToken: String,
    val email: String,
    val displayName: String
)

