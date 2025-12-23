package alie.info.newmultichoice.utils

import android.content.Context
import android.widget.Toast
import alie.info.newmultichoice.api.RetrofitClient
import alie.info.newmultichoice.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Helper class for debugging server connections
 */
object ServerDebugHelper {
    
    /**
     * Test server connection and show result
     */
    fun testServerConnection(context: Context, scope: CoroutineScope) {
        scope.launch {
            try {
                showToast(context, "Testing server connection...")
                
                val deviceId = DeviceUtils.getDeviceId(context)
                android.util.Log.d("ServerDebug", "Device ID: $deviceId")
                
                // Test health endpoint
                val apiService = RetrofitClient.getApiService()
                val healthResponse = withContext(Dispatchers.IO) {
                    apiService.healthCheck()
                }
                
                if (healthResponse.isSuccessful) {
                    val health = healthResponse.body()
                    android.util.Log.d("ServerDebug", "✅ Health check: ${health?.status} (uptime: ${health?.uptime}s)")
                    
                    // Test unlock status endpoint
                    val unlockResponse = withContext(Dispatchers.IO) {
                        apiService.getUnlockStatus(deviceId)
                    }
                    
                    if (unlockResponse.isSuccessful) {
                        val unlock = unlockResponse.body()
                        android.util.Log.d("ServerDebug", "✅ Unlock status: ${unlock?.data}")
                        
                        showToast(
                            context, 
                            "✅ Server OK!\n" +
                            "Progress: ${unlock?.data?.currentProgress ?: 0}/50\n" +
                            "Marathon: ${if (unlock?.data?.marathonUnlocked == true) "✅" else "🔒"}\n" +
                            "Server: ${RetrofitClient.getCurrentBaseUrl()}"
                        )
                    } else {
                        showToast(context, "❌ Unlock endpoint failed: ${unlockResponse.code()}")
                    }
                } else {
                    showToast(context, "❌ Health check failed: ${healthResponse.code()}")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("ServerDebug", "❌ Connection error", e)
                showToast(context, "❌ Error: ${e.message}")
            }
        }
    }
    
    /**
     * Test uploading data to server
     */
    fun testUploadToServer(context: Context, scope: CoroutineScope) {
        scope.launch {
            try {
                showToast(context, "Uploading data to server...")
                
                val syncManager = SyncManager(context)
                val result = syncManager.uploadToServer()
                
                when (result) {
                    is alie.info.newmultichoice.sync.SyncResult.Success -> {
                        showToast(context, "✅ Upload successful!\n${result.message}")
                    }
                    is alie.info.newmultichoice.sync.SyncResult.Failure -> {
                        showToast(context, "❌ Upload failed:\n${result.error}")
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("ServerDebug", "❌ Upload error", e)
                showToast(context, "❌ Upload error: ${e.message}")
            }
        }
    }
    
    private suspend fun showToast(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}

