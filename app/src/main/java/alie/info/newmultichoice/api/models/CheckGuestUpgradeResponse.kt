package alie.info.newmultichoice.api.models

import com.google.gson.annotations.SerializedName

/**
 * Response for guest upgrade check
 */
data class CheckGuestUpgradeResponse(
    @SerializedName("message")
    val message: String? = null,
    
    @SerializedName("deviceId")
    val deviceId: String? = null,
    
    @SerializedName("totalQuestionsAnswered")
    val totalQuestionsAnswered: Int = 0,
    
    @SerializedName("shouldShowUpgradePrompt")
    val shouldShowUpgradePrompt: Boolean = false,
    
    @SerializedName("sessionCount")
    val sessionCount: Int = 0
)

