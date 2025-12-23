package alie.info.newmultichoice.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.HapticFeedbackConstants

/**
 * Modern haptic feedback helper for 2025
 */
class HapticFeedbackHelper(private val context: Context) {
    
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    
    /**
     * Light tap feedback for button presses
     */
    fun lightTap(view: View? = null) {
        view?.performHapticFeedback(
            HapticFeedbackConstants.VIRTUAL_KEY,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }
    
    /**
     * Medium feedback for selections
     */
    fun mediumFeedback(view: View? = null) {
        view?.performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(30, 128)
            )
        }
    }
    
    /**
     * Success feedback (correct answer)
     */
    fun successFeedback(view: View? = null) {
        view?.performHapticFeedback(
            HapticFeedbackConstants.CONFIRM,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 50, 50, 50)
            val amplitudes = intArrayOf(0, 100, 0, 150)
            vibrator?.vibrate(
                VibrationEffect.createWaveform(pattern, amplitudes, -1)
            )
        }
    }
    
    /**
     * Error feedback (wrong answer)
     */
    fun errorFeedback(view: View? = null) {
        view?.performHapticFeedback(
            HapticFeedbackConstants.REJECT,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 100, 100, 100)
            val amplitudes = intArrayOf(0, 200, 0, 200)
            vibrator?.vibrate(
                VibrationEffect.createWaveform(pattern, amplitudes, -1)
            )
        }
    }
    
    /**
     * Heavy feedback for important events (quiz completed)
     */
    fun heavyFeedback(view: View? = null) {
        view?.performHapticFeedback(
            HapticFeedbackConstants.LONG_PRESS,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: HapticFeedbackHelper? = null
        
        fun getInstance(context: Context): HapticFeedbackHelper {
            return INSTANCE ?: synchronized(this) {
                val instance = HapticFeedbackHelper(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

