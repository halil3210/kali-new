package alie.info.newmultichoice.sync

import android.content.Context
import alie.info.newmultichoice.api.RetrofitClient
import alie.info.newmultichoice.api.models.*
import alie.info.newmultichoice.auth.AuthManager
import alie.info.newmultichoice.data.QuizRepository
import alie.info.newmultichoice.data.UserStats
import alie.info.newmultichoice.utils.DeviceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Manages synchronization between local database and server
 * 
 * Strategy: Offline-First with Background Sync
 * - App works fully offline
 * - Automatic sync when internet available
 * - Server is backup/cloud storage only
 */
class SyncManager(private val context: Context) {
    
    private val repository = QuizRepository.getInstance(context)
    private val apiService = RetrofitClient.getApiService()
    private val authManager = AuthManager.getInstance(context)
    
    // Get unique device ID
    private fun getDeviceId(): String {
        return DeviceUtils.getDeviceId(context)
    }
    
    /**
     * Upload local data to server
     */
    suspend fun uploadToServer(): SyncResult = withContext(Dispatchers.IO) {
        try {
            // Check server health first
            val healthCheck = apiService.healthCheck()
            if (!healthCheck.isSuccessful || healthCheck.body()?.status != "ok") {
                return@withContext SyncResult.Failure("Server not healthy")
            }
            
            alie.info.newmultichoice.utils.Logger.d("SyncManager", "✅ Server health OK (uptime: ${healthCheck.body()?.uptime}s)")
            
            // Get local data
            val userId = authManager.getUserId() ?: ""
            val userStats = repository.getUserStats().first()
            val sessions = repository.getRecentSessions(userId)
            
            // Convert to DTOs
            val statsDto = userStats?.toDto()
            val sessionDtos = sessions.map { it.toDto() }
            
            // Create sync request
            val request = SyncRequest(
                deviceId = getDeviceId(),
                userStats = statsDto,
                quizSessions = sessionDtos,
                lastSync = System.currentTimeMillis()
            )
            
            // Upload
            val response = apiService.uploadUserData(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                alie.info.newmultichoice.utils.Logger.d("SyncManager", "✅ Upload successful")
                SyncResult.Success("Data uploaded successfully")
            } else {
                SyncResult.Failure("Upload failed: ${response.message()}")
            }
            
        } catch (e: UnknownHostException) {
            alie.info.newmultichoice.utils.Logger.w("SyncManager", "❌ Server not reachable (DNS)")
            RetrofitClient.useIpFallback() // Try IP fallback
            SyncResult.Failure("Server not reachable")
        } catch (e: SocketTimeoutException) {
            alie.info.newmultichoice.utils.Logger.w("SyncManager", "❌ Connection timeout")
            SyncResult.Failure("Connection timeout")
        } catch (e: IOException) {
            alie.info.newmultichoice.utils.Logger.e("SyncManager", "❌ Network error", e)
            SyncResult.Failure("Network error: ${e.message}")
        } catch (e: Exception) {
            alie.info.newmultichoice.utils.Logger.e("SyncManager", "❌ Sync error", e)
            SyncResult.Failure("Sync error: ${e.message}")
        }
    }
    
    /**
     * Download data from server (restore backup)
     */
    suspend fun downloadFromServer(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val response = apiService.downloadUserData(getDeviceId())
            
            if (response.isSuccessful && response.body()?.success == true) {
                val downloadData = response.body()?.data
                
                if (downloadData == null) {
                    return@withContext SyncResult.Success("No server data available")
                }
                
                // Update local database with server data
                downloadData.userStats?.let { statsDto ->
                    val localStats = repository.getUserStats().first()
                    
                    // Only update if server data is newer or has better stats
                    if (shouldUpdateLocal(localStats, statsDto)) {
                        repository.updateStatsFromServer(statsDto)
                        alie.info.newmultichoice.utils.Logger.d("SyncManager", "✅ Downloaded ${downloadData.quizSessions?.size ?: 0} sessions")
                        SyncResult.Success("Data downloaded: ${downloadData.quizSessions?.size ?: 0} sessions")
                    } else {
                        SyncResult.Success("Local data is up-to-date")
                    }
                } ?: SyncResult.Success("No server stats available")
            } else {
                SyncResult.Failure("Download failed: ${response.message()}")
            }
            
        } catch (e: UnknownHostException) {
            alie.info.newmultichoice.utils.Logger.w("SyncManager", "❌ Server not reachable (DNS)")
            RetrofitClient.useSecondaryServer()
            SyncResult.Failure("Server not reachable")
        } catch (e: SocketTimeoutException) {
            alie.info.newmultichoice.utils.Logger.w("SyncManager", "❌ Connection timeout")
            SyncResult.Failure("Connection timeout")
        } catch (e: IOException) {
            SyncResult.Failure("Network error: ${e.message}")
        } catch (e: Exception) {
            SyncResult.Failure("Sync error: ${e.message}")
        }
    }
    
    /**
     * Bi-directional sync (smart merge)
     */
    suspend fun smartSync(): SyncResult = withContext(Dispatchers.IO) {
        try {
            // 1. Check server health
            val healthCheck = apiService.healthCheck()
            if (!healthCheck.isSuccessful) {
                return@withContext SyncResult.Failure("Server offline")
            }
            
            // 2. Get both local and server data
            val localStats = repository.getUserStats().first()
            val serverResponse = apiService.getUserStats(getDeviceId())
            
            if (!serverResponse.isSuccessful) {
                // No server data yet, upload local
                return@withContext uploadToServer()
            }
            
            val serverStats = serverResponse.body()?.data
            
            // 3. Merge strategy: Take the best of both
            val merged = mergeStats(localStats, serverStats)
            
            // 4. Update both local and server with merged data
            repository.updateStatsFromServer(merged)
            apiService.updateUserStats(merged, getDeviceId())
            
            SyncResult.Success("Sync completed successfully")
            
        } catch (e: Exception) {
            SyncResult.Failure("Sync error: ${e.message}")
        }
    }
    
    /**
     * Check if server is reachable
     */
    suspend fun isServerReachable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.healthCheck()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
    
    // Helper: Convert UserStats to DTO
    private fun UserStats.toDto() = UserStatsDto(
        totalQuizzesTaken = totalQuizzesTaken,
        totalQuestionsAnswered = totalQuestionsAnswered,
        totalCorrectAnswers = totalCorrectAnswers,
        overallAccuracy = overallAccuracy,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        highestUnlockedExam = highestUnlockedExam,
        unlockedAchievements = unlockedAchievements
    )
    
    // Helper: Convert QuizSession to DTO
    private fun alie.info.newmultichoice.data.QuizSession.toDto() = QuizSessionDto(
        sessionId = id,
        totalQuestions = totalQuestions,
        correctAnswers = correctAnswers,
        wrongAnswers = wrongAnswers,
        percentage = percentage,
        completedAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date(timestamp)),
        isCompleted = isCompleted
    )
    
    // Helper: Should update local with server data?
    private fun shouldUpdateLocal(local: UserStats?, server: UserStatsDto): Boolean {
        if (local == null) return true
        
        // Update if server has better progress
        return server.totalCorrectAnswers > local.totalCorrectAnswers ||
               server.longestStreak > local.longestStreak ||
               server.highestUnlockedExam > local.highestUnlockedExam
    }
    
    // Helper: Merge local and server stats (take best of both)
    private fun mergeStats(local: UserStats?, server: UserStatsDto?): UserStatsDto {
        if (local == null && server != null) return server
        if (local != null && server == null) return local.toDto()
        if (local == null && server == null) return UserStatsDto(0, 0, 0, 0f, 0, 0, 1, "")
        
        // Take the maximum/best values from both
        return UserStatsDto(
            totalQuizzesTaken = maxOf(local!!.totalQuizzesTaken, server!!.totalQuizzesTaken),
            totalQuestionsAnswered = maxOf(local.totalQuestionsAnswered, server.totalQuestionsAnswered),
            totalCorrectAnswers = maxOf(local.totalCorrectAnswers, server.totalCorrectAnswers),
            overallAccuracy = maxOf(local.overallAccuracy, server.overallAccuracy),
            currentStreak = maxOf(local.currentStreak, server.currentStreak),
            longestStreak = maxOf(local.longestStreak, server.longestStreak),
            highestUnlockedExam = maxOf(local.highestUnlockedExam, server.highestUnlockedExam),
            unlockedAchievements = mergeAchievements(local.unlockedAchievements, server.unlockedAchievements)
        )
    }
    
    // Helper: Merge achievement strings
    private fun mergeAchievements(local: String, server: String): String {
        val localSet = local.split(",").filter { it.isNotBlank() }.toSet()
        val serverSet = server.split(",").filter { it.isNotBlank() }.toSet()
        return (localSet + serverSet).joinToString(",")
    }
}

sealed class SyncResult {
    data class Success(val message: String) : SyncResult()
    data class Failure(val error: String) : SyncResult()
}

