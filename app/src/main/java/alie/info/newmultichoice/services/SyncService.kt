package alie.info.newmultichoice.services

import android.content.Context
import androidx.fragment.app.Fragment
import alie.info.newmultichoice.sync.SyncManager
import alie.info.newmultichoice.ui.overlays.OverlayManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for syncing data with server and showing overlays
 */
class SyncService(private val context: Context) {

    private val syncManager = SyncManager(context)
    
    companion object {
        @Volatile
        private var INSTANCE: SyncService? = null
        
        fun getInstance(context: Context): SyncService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncService(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    /**
     * Upload data to server and show success overlay
     * Calls: POST /api/sync/upload
     */
    suspend fun uploadAndShowOverlay(fragment: Fragment): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val result = syncManager.uploadToServer()
                
                when (result) {
                    is alie.info.newmultichoice.sync.SyncResult.Success -> {
                        withContext(Dispatchers.Main) {
                            OverlayManager.showSyncSuccessOverlay(
                                fragment = fragment,
                                statsUpdated = true, // From result
                                sessionsUploaded = 0 // Parse from result message
                            )
                        }
                        android.util.Log.d("SyncService", "✅ Upload successful: ${result.message}")
                        true
                    }
                    is alie.info.newmultichoice.sync.SyncResult.Failure -> {
                        android.util.Log.e("SyncService", "❌ Upload failed: ${result.error}")
                        false
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SyncService", "❌ Upload error", e)
                false
            }
        }
    }
    
    /**
     * Download data from server and show overlay
     * Calls: GET /api/sync/download/:deviceId
     */
    suspend fun downloadAndShowOverlay(fragment: Fragment): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val result = syncManager.downloadFromServer()
                
                when (result) {
                    is alie.info.newmultichoice.sync.SyncResult.Success -> {
                        withContext(Dispatchers.Main) {
                            OverlayManager.showDownloadCompleteOverlay(
                                fragment = fragment,
                                sessionsDownloaded = 0, // Parse from result
                                statsDownloaded = true
                            )
                        }
                        android.util.Log.d("SyncService", "✅ Download successful: ${result.message}")
                        true
                    }
                    is alie.info.newmultichoice.sync.SyncResult.Failure -> {
                        android.util.Log.e("SyncService", "❌ Download failed: ${result.error}")
                        false
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SyncService", "❌ Download error", e)
                false
            }
        }
    }
    
    /**
     * Silent sync without overlay (for background sync)
     */
    suspend fun silentSync(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val uploadResult = syncManager.uploadToServer()
                val downloadResult = syncManager.downloadFromServer()
                
                uploadResult is alie.info.newmultichoice.sync.SyncResult.Success &&
                downloadResult is alie.info.newmultichoice.sync.SyncResult.Success
            } catch (e: Exception) {
                android.util.Log.e("SyncService", "❌ Silent sync error", e)
                false
            }
        }
    }
}

