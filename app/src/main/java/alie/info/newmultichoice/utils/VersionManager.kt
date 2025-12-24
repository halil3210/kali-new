package alie.info.newmultichoice.utils

import alie.info.newmultichoice.api.RetrofitClient
import alie.info.newmultichoice.api.models.VersionResponse
import retrofit2.Response

/**
 * Manages app-server version compatibility
 */
object VersionManager {

    private const val CURRENT_APP_VERSION = "1.0.0"

    /**
     * Check if app version is compatible with server
     */
    suspend fun checkVersionCompatibility(): VersionCheckResult {
        return try {
            val response = RetrofitClient.getApiService().checkVersionCompatibility(
                appVersion = CURRENT_APP_VERSION
            )

            if (response.isSuccessful) {
                val versionResponse = response.body()
                versionResponse?.let { version ->
                    Logger.d("VersionManager", "Version check successful: ${version.isCompatible}")

                    if (!version.isCompatible) {
                        VersionCheckResult.Incompatible(
                            message = version.message,
                            updateRequired = version.updateRequired,
                            updateUrl = version.updateUrl
                        )
                    } else if (version.updateRequired) {
                        VersionCheckResult.UpdateRecommended(
                            message = version.message,
                            updateUrl = version.updateUrl
                        )
                    } else {
                        VersionCheckResult.Compatible
                    }
                } ?: VersionCheckResult.Error("Invalid server response")
            } else {
                Logger.w("VersionManager", "Version check failed: ${response.code()}")
                VersionCheckResult.Error("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            Logger.e("VersionManager", "Version check exception", e)
            VersionCheckResult.Error("Network error: ${e.message}")
        }
    }

    /**
     * Get current app version
     */
    fun getCurrentAppVersion(): String = CURRENT_APP_VERSION
}

/**
 * Result of version compatibility check
 */
sealed class VersionCheckResult {
    object Compatible : VersionCheckResult()
    data class UpdateRecommended(val message: String, val updateUrl: String?) : VersionCheckResult()
    data class Incompatible(val message: String, val updateRequired: Boolean, val updateUrl: String?) : VersionCheckResult()
    data class Error(val message: String) : VersionCheckResult()
}
