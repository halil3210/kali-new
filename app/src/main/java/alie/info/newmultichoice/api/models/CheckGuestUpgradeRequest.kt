package alie.info.newmultichoice.api.models

import com.google.gson.annotations.SerializedName

/**
 * Request to check if guest user should see upgrade prompt
 */
data class CheckGuestUpgradeRequest(
    @SerializedName("deviceId")
    val deviceId: String
)

