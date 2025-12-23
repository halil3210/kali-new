package alie.info.newmultichoice.services

import android.content.Context
import androidx.fragment.app.Fragment
import alie.info.newmultichoice.api.RetrofitClient
import alie.info.newmultichoice.data.QuizRepository
import alie.info.newmultichoice.ui.overlays.OverlayManager
import alie.info.newmultichoice.utils.DeviceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service to check unlock status and show appropriate overlays
 * Integrates with API endpoints
 */
class UnlockCheckService(private val context: Context) {

    private val apiService = RetrofitClient.getApiService()
    private val repository = QuizRepository.getInstance(context)
    
    companion object {
        private const val PREF_NAME = "unlock_overlays_shown"
        private const val KEY_MARATHON_SHOWN = "marathon_shown"
        private const val KEY_EXAMS_SHOWN = "exams_shown"
        
        @Volatile
        private var INSTANCE: UnlockCheckService? = null
        
        fun getInstance(context: Context): UnlockCheckService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UnlockCheckService(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    /**
     * Check if Marathon/Exams should be unlocked
     * Calls: GET /api/stats/unlock-status/:deviceId
     * Shows overlay if newly unlocked
     */
    suspend fun checkAndShowUnlockOverlay(
        fragment: Fragment,
        onMarathonUnlock: () -> Unit = {},
        onExamsUnlock: () -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        try {
            val deviceId = DeviceUtils.getDeviceId(context)
            val response = apiService.getUnlockStatus(deviceId)
            
            if (response.isSuccessful) {
                val data = response.body()?.data
                
                if (data != null) {
                    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    
                    // Check Marathon unlock
                    if (data.marathonUnlocked && !prefs.getBoolean(KEY_MARATHON_SHOWN, false)) {
                        withContext(Dispatchers.Main) {
                            OverlayManager.showMarathonUnlockedOverlay(
                                fragment = fragment,
                                correctAnswers = data.currentProgress,
                                accuracy = (data.currentProgress.toFloat() / data.required * 100f),
                                onStartMarathon = onMarathonUnlock
                            )
                        }
                        prefs.edit().putBoolean(KEY_MARATHON_SHOWN, true).apply()
                        android.util.Log.d("UnlockCheck", "✅ Marathon overlay shown")
                    }
                    
                    // Check Exams unlock
                    if (data.examsUnlocked && !prefs.getBoolean(KEY_EXAMS_SHOWN, false)) {
                        withContext(Dispatchers.Main) {
                            OverlayManager.showExamsUnlockedOverlay(
                                fragment = fragment,
                                correctAnswers = data.currentProgress,
                                onViewExams = onExamsUnlock
                            )
                        }
                        prefs.edit().putBoolean(KEY_EXAMS_SHOWN, true).apply()
                        android.util.Log.d("UnlockCheck", "✅ Exams overlay shown")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("UnlockCheck", "Error checking unlock status", e)
        }
    }
    
    /**
     * Check if specific exam is unlocked
     * Calls: GET /api/stats/exam-unlock/:deviceId/:examNumber
     */
    suspend fun checkExamUnlock(examNumber: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val deviceId = DeviceUtils.getDeviceId(context)
                val response = apiService.getExamUnlockStatus(deviceId, examNumber)
                
                if (response.isSuccessful) {
                    response.body()?.data?.unlocked ?: false
                } else {
                    false
                }
            } catch (e: Exception) {
                android.util.Log.e("UnlockCheck", "Error checking exam $examNumber unlock", e)
                false
            }
        }
    }
    
    /**
     * Unlock next exam after passing
     * Calls: POST /api/stats/unlock-exam
     * Shows overlay for next exam
     */
    suspend fun unlockNextExam(
        fragment: Fragment,
        currentExamNumber: Int,
        score: Float,
        errors: Int,
        maxErrors: Int,
        passed: Boolean,
        onStartNextExam: () -> Unit = {},
        onLater: () -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        try {
            val deviceId = DeviceUtils.getDeviceId(context)
            val request = alie.info.newmultichoice.api.models.UnlockExamRequest(
                deviceId = deviceId,
                examNumber = currentExamNumber,
                score = score.toInt(),
                passed = passed
            )
            
            val response = apiService.unlockExam(request)
            
            if (response.isSuccessful && passed) {
                val nextExamNumber = currentExamNumber + 1
                
                withContext(Dispatchers.Main) {
                    OverlayManager.showExamNextUnlockedOverlay(
                        fragment = fragment,
                        passedExamNumber = currentExamNumber,
                        nextExamNumber = nextExamNumber,
                        score = score,
                        errors = errors,
                        maxErrors = maxErrors,
                        onStartNextExam = onStartNextExam,
                        onLater = onLater
                    )
                }
                
                android.util.Log.d("UnlockCheck", "✅ Exam $nextExamNumber unlocked")
            }
        } catch (e: Exception) {
            android.util.Log.e("UnlockCheck", "Error unlocking next exam", e)
        }
    }
    
    /**
     * Reset shown overlays (for testing)
     */
    fun resetShownOverlays() {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}

