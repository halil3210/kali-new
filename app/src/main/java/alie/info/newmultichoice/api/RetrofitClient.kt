package alie.info.newmultichoice.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Retrofit Client Singleton
 * 
 * Server Priority:
 * 1. WiFi Access Point: http://192.168.44.1:3000 (for local clients)
 * 2. Heimnetzwerk: http://192.168.178.27:3000 (fallback)
 */
object RetrofitClient {

    // Production Server URLs
    private const val PRIMARY_DOMAIN = "https://klcp.alie.info/"
    private const val SECONDARY_DOMAIN = "https://klcp.alie.info/"
    private const val FALLBACK_IP_HTTP = "http://188.245.153.241/"  // HTTP fallback to IP

    private var currentBaseUrl = PRIMARY_DOMAIN
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Enable logging in debug builds
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val request = chain.request()
            try {
                chain.proceed(request)
            } catch (e: Exception) {
                // Implement cascading fallback: Domain HTTPS -> Domain HTTPS -> IP HTTP
                when (currentBaseUrl) {
                    PRIMARY_DOMAIN -> {
                        alie.info.newmultichoice.utils.Logger.w("RetrofitClient", "Primary domain failed, trying secondary domain...")
                        currentBaseUrl = SECONDARY_DOMAIN
                        getApiService() // Recreate with new URL
                    }
                    SECONDARY_DOMAIN -> {
                        alie.info.newmultichoice.utils.Logger.w("RetrofitClient", "Secondary domain failed, falling back to IP (HTTP)...")
                        currentBaseUrl = FALLBACK_IP_HTTP
                        getApiService() // Recreate with HTTP fallback
                    }
                    else -> {
                        alie.info.newmultichoice.utils.Logger.e("RetrofitClient", "All server endpoints failed")
                        throw e
                    }
                }
                throw e
            }
        }
        .build()
    
    private var retrofit: Retrofit? = null
    
    private fun getRetrofit(): Retrofit {
        if (retrofit == null || retrofit?.baseUrl().toString() != currentBaseUrl) {
            retrofit = Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }
    
    fun getApiService(): ApiService {
        return getRetrofit().create(ApiService::class.java)
    }
    
    /**
     * Get AuthApiService instance
     */
    fun getAuthApiService(): alie.info.newmultichoice.auth.AuthApiService {
        return getRetrofit().create(alie.info.newmultichoice.auth.AuthApiService::class.java)
    }
    
    /**
     * Switch to secondary server URL
     */
    fun useSecondaryServer() {
        currentBaseUrl = SECONDARY_DOMAIN
        retrofit = null // Force recreate
    }

    /**
     * Switch to primary server URL
     */
    fun usePrimaryServer() {
        currentBaseUrl = PRIMARY_DOMAIN
        retrofit = null // Force recreate
    }

    /**
     * Force fallback to IP address (HTTP)
     */
    fun useIpFallback() {
        currentBaseUrl = FALLBACK_IP_HTTP
        retrofit = null // Force recreate
    }
    
    /**
     * Get current base URL
     */
    fun getCurrentBaseUrl(): String = currentBaseUrl
}

