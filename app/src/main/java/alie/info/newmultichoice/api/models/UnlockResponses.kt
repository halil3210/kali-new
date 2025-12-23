package alie.info.newmultichoice.api.models

import com.google.gson.annotations.SerializedName

/**
 * Response for unlock status check
 */
data class UnlockStatusResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: UnlockStatusData?
)

data class UnlockStatusData(
    @SerializedName("marathonUnlocked")
    val marathonUnlocked: Boolean,
    
    @SerializedName("examsUnlocked")
    val examsUnlocked: Boolean,
    
    @SerializedName("currentProgress")
    val currentProgress: Int,
    
    @SerializedName("required")
    val required: Int,
    
    @SerializedName("remaining")
    val remaining: Int,
    
    @SerializedName("percentage")
    val percentage: Int? = null
)

/**
 * Response for exam unlock status
 */
data class ExamUnlockResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: ExamUnlockData?
)

data class ExamUnlockData(
    @SerializedName("examNumber")
    val examNumber: Int,
    
    @SerializedName("unlocked")
    val unlocked: Boolean,
    
    @SerializedName("reason")
    val reason: String,
    
    @SerializedName("highestUnlocked")
    val highestUnlocked: Int? = null,
    
    @SerializedName("correctAnswers")
    val correctAnswers: Int? = null
)

/**
 * Request to unlock next exam after passing
 */
data class UnlockExamRequest(
    @SerializedName("deviceId")
    val deviceId: String,
    
    @SerializedName("examNumber")
    val examNumber: Int,
    
    @SerializedName("score")
    val score: Int,
    
    @SerializedName("passed")
    val passed: Boolean
)

