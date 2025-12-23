package alie.info.newmultichoice.utils

import android.content.Context
import android.provider.Settings
import java.util.UUID

/**
 * Utility for generating and managing unique device identifiers
 */
object DeviceUtils {
    
    private const val PREFS_NAME = "device_prefs"
    private const val KEY_DEVICE_ID = "device_id"
    
    /**
     * Get or create a unique device ID
     * Uses Android ID as base, with fallback to generated UUID
     */
    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Check if we already have a stored ID
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        
        if (deviceId == null) {
            // Try to get Android ID
            deviceId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (e: Exception) {
                // Fallback to UUID if Android ID fails
                UUID.randomUUID().toString()
            }
            
            // Store for future use
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
            android.util.Log.d("DeviceUtils", "Generated new device ID: $deviceId")
        }
        
        return deviceId
    }
    
    /**
     * Clear stored device ID (useful for testing)
     */
    fun clearDeviceId(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_DEVICE_ID).apply()
        android.util.Log.d("DeviceUtils", "Device ID cleared")
    }
}

