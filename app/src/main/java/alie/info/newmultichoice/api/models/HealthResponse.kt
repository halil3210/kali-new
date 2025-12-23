package alie.info.newmultichoice.api.models

import com.google.gson.annotations.SerializedName

/**
 * Response for health check endpoint
 */
data class HealthResponse(
    @SerializedName("status")
    val status: String,
    
    @SerializedName("timestamp")
    val timestamp: String,
    
    @SerializedName("uptime")
    val uptime: Double,
    
    @SerializedName("memory")
    val memory: MemoryUsage?
)

data class MemoryUsage(
    @SerializedName("rss")
    val rss: Long,
    
    @SerializedName("heapTotal")
    val heapTotal: Long,
    
    @SerializedName("heapUsed")
    val heapUsed: Long,
    
    @SerializedName("external")
    val external: Long,
    
    @SerializedName("arrayBuffers")
    val arrayBuffers: Long
)

