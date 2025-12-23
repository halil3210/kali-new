package alie.info.newmultichoice.sync

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Background Worker for automatic data synchronization
 * Runs periodically when device has internet connection
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val syncManager = SyncManager(applicationContext)
            
            // Check if server is reachable
            if (!syncManager.isServerReachable()) {
                android.util.Log.d("SyncWorker", "⚠️ Server not reachable, skipping sync")
                return Result.retry()
            }
            
            // Perform smart sync
            when (val result = syncManager.smartSync()) {
                is SyncResult.Success -> {
                    android.util.Log.d("SyncWorker", "✅ Sync completed: ${result.message}")
                    Result.success()
                }
                is SyncResult.Failure -> {
                    android.util.Log.e("SyncWorker", "❌ Sync failed: ${result.error}")
                    Result.retry()
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "❌ Worker error", e)
            Result.failure()
        }
    }
    
    companion object {
        private const val WORK_NAME = "klcp_sync_work"
        
        /**
         * Schedule periodic sync (every 6 hours when connected to network)
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            
            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                6, TimeUnit.HOURS,
                30, TimeUnit.MINUTES // Flex interval
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.MINUTES
                )
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            
            android.util.Log.d("SyncWorker", "📅 Periodic sync scheduled")
        }
        
        /**
         * Trigger immediate one-time sync
         */
        fun syncNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                "klcp_sync_now",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
            
            android.util.Log.d("SyncWorker", "🔄 Immediate sync triggered")
        }
        
        /**
         * Cancel all scheduled syncs
         */
        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            android.util.Log.d("SyncWorker", "🛑 Sync cancelled")
        }
    }
}

