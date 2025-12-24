package alie.info.newmultichoice.api

import alie.info.newmultichoice.api.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API Service for KLCP Quiz Server
 * 
 * Server URL: http://192.168.178.27:3000 (Heimnetzwerk)
 *             http://192.168.44.1:3000 (WiFi Access Point)
 */
interface ApiService {
    
    // ============================================
    // HEALTH & STATUS
    // ============================================
    
    @GET("api/health")
    suspend fun healthCheck(): Response<HealthResponse>

    @GET("api/version/check")
    suspend fun checkVersionCompatibility(
        @Query("appVersion") appVersion: String,
        @Query("platform") platform: String = "android"
    ): Response<VersionResponse>
    
    // ============================================
    // SYNC ENDPOINTS
    // ============================================
    
    @POST("api/sync/upload")
    suspend fun uploadUserData(
        @Body request: SyncRequest
    ): Response<SyncResponse>
    
    @GET("api/sync/download/{deviceId}")
    suspend fun downloadUserData(
        @Path("deviceId") deviceId: String
    ): Response<SyncDownloadResponse>
    
    // ============================================
    // USER STATS & UNLOCK STATUS
    // ============================================
    
    @GET("api/stats/user/{deviceId}")
    suspend fun getUserStats(
        @Path("deviceId") deviceId: String
    ): Response<ApiResponse<UserStatsDto>>
    
    @POST("api/stats/update")
    suspend fun updateUserStats(
        @Body stats: UserStatsDto,
        @Query("deviceId") deviceId: String
    ): Response<ApiResponse<Boolean>>
    
    @GET("api/stats/unlock-status/{deviceId}")
    suspend fun getUnlockStatus(
        @Path("deviceId") deviceId: String
    ): Response<UnlockStatusResponse>
    
    @GET("api/stats/exam-unlock/{deviceId}/{examNumber}")
    suspend fun getExamUnlockStatus(
        @Path("deviceId") deviceId: String,
        @Path("examNumber") examNumber: Int
    ): Response<ExamUnlockResponse>
    
    @POST("api/stats/unlock-exam")
    suspend fun unlockExam(
        @Body request: UnlockExamRequest
    ): Response<ApiResponse<String>>
    
    // ============================================
    // QUIZ SESSIONS
    // ============================================
    
    @GET("api/sessions/{deviceId}")
    suspend fun getQuizSessions(
        @Path("deviceId") deviceId: String,
        @Query("limit") limit: Int = 50
    ): Response<ApiResponse<List<QuizSessionDto>>>
    
    @POST("api/sessions/save")
    suspend fun saveQuizSession(
        @Body session: QuizSessionDto,
        @Query("deviceId") deviceId: String
    ): Response<ApiResponse<Long>>
    
    @POST("api/sessions/check-guest-upgrade")
    suspend fun checkGuestUpgradePrompt(
        @Body request: CheckGuestUpgradeRequest
    ): Response<CheckGuestUpgradeResponse>
    
    // ============================================
    // BACKUP & RESTORE
    // ============================================
    
    @POST("api/backup/create")
    suspend fun createBackup(
        @Body request: SyncRequest
    ): Response<ApiResponse<String>>
    
    @GET("api/backup/restore/{deviceId}")
    suspend fun restoreBackup(
        @Path("deviceId") deviceId: String
    ): Response<SyncResponse>
}

