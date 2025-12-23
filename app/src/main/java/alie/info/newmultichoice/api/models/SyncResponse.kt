package alie.info.newmultichoice.api.models

import com.google.gson.annotations.SerializedName

/**
 * Response model from server sync upload
 */
data class SyncResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: SyncData?
)

data class SyncData(
    @SerializedName("statsUpdated")
    val statsUpdated: Boolean,
    
    @SerializedName("sessionsCreated")
    val sessionsCreated: Int
)

/**
 * Response model from server sync download
 */
data class SyncDownloadResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: SyncDownloadData?
)

data class SyncDownloadData(
    @SerializedName("userStats")
    val userStats: UserStatsDto?,
    
    @SerializedName("quizSessions")
    val quizSessions: List<QuizSessionDto>?,
    
    @SerializedName("serverTimestamp")
    val serverTimestamp: Long
)

data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: T?,
    
    @SerializedName("message")
    val message: String? = null,
    
    @SerializedName("error")
    val error: String? = null
)

