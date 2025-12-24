package alie.info.newmultichoice.api.models

import com.google.gson.annotations.SerializedName

/**
 * Version compatibility check response
 */
data class VersionResponse(
    @SerializedName("serverVersion")
    val serverVersion: String,

    @SerializedName("minSupportedAppVersion")
    val minSupportedAppVersion: String,

    @SerializedName("recommendedAppVersion")
    val recommendedAppVersion: String,

    @SerializedName("isCompatible")
    val isCompatible: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("updateRequired")
    val updateRequired: Boolean,

    @SerializedName("updateUrl")
    val updateUrl: String? = null
)
