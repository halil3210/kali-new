package alie.info.newmultichoice.auth

import com.google.gson.annotations.SerializedName

// Request Models
data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("deviceId") val deviceId: String
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("deviceId") val deviceId: String
)

// Response Models
data class AuthResponse(
    @SerializedName("message") val message: String,
    @SerializedName("token") val token: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("email") val email: String,
    @SerializedName("isVerified") val isVerified: Boolean,
    @SerializedName("error") val error: String? = null
)

data class UserData(
    @SerializedName("id") val id: Int,
    @SerializedName("email") val email: String,
    @SerializedName("deviceId") val deviceId: String
)

data class UserInfoData(
    @SerializedName("id") val id: Int,
    @SerializedName("email") val email: String,
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("isVerified") val isVerified: Boolean,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("lastLogin") val lastLogin: Long?
)

