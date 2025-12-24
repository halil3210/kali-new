package alie.info.newmultichoice.utils

import android.util.Log

/**
 * Safe logging utility that only logs in debug builds
 */
object Logger {

    private const val TAG = "KLCP_QUIZ"

    // Check if we're in debug mode - will be replaced by BuildConfig.DEBUG in production
    private val IS_DEBUG: Boolean by lazy {
        try {
            // Try to access BuildConfig.DEBUG - if it fails, assume production
            val buildConfigClass = Class.forName("alie.info.newmultichoice.BuildConfig")
            val debugField = buildConfigClass.getField("DEBUG")
            debugField.get(null) as? Boolean ?: false
        } catch (e: Exception) {
            // BuildConfig not available or error - assume production
            false
        }
    }

    fun d(message: String) {
        if (IS_DEBUG) {
            Log.d(TAG, message)
        }
    }

    fun d(tag: String, message: String) {
        if (IS_DEBUG) {
            Log.d(tag, message)
        }
    }

    fun e(message: String) {
        if (IS_DEBUG) {
            Log.e(TAG, message)
        }
    }

    fun e(tag: String, message: String) {
        if (IS_DEBUG) {
            Log.e(TAG, message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable) {
        if (IS_DEBUG) {
            Log.e(TAG, message, throwable)
        }
    }

    fun i(message: String) {
        if (IS_DEBUG) {
            Log.i(TAG, message)
        }
    }

    fun i(tag: String, message: String) {
        if (IS_DEBUG) {
            Log.i(TAG, message)
        }
    }

    fun w(message: String) {
        if (IS_DEBUG) {
            Log.w(TAG, message)
        }
    }

    fun w(tag: String, message: String) {
        if (IS_DEBUG) {
            Log.w(TAG, message)
        }
    }

    fun v(message: String) {
        if (IS_DEBUG) {
            Log.v(TAG, message)
        }
    }

    fun v(tag: String, message: String) {
        if (IS_DEBUG) {
            Log.v(TAG, message)
        }
    }
}
