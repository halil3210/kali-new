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
    
    // Production Server URL with Let's Encrypt HTTPS
    private const val PRIMARY_BASE_URL = "https://klcp.alie.info/"
    private const val SECONDARY_BASE_URL = "https://klcp.alie.info/"
    
    private var currentBaseUrl = PRIMARY_BASE_URL
    
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
                // If primary fails, try secondary
                if (currentBaseUrl == PRIMARY_BASE_URL) {
                    android.util.Log.w("RetrofitClient", "Primary server failed, trying secondary...")
                    currentBaseUrl = SECONDARY_BASE_URL
                    getApiService() // Recreate with new URL
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
        currentBaseUrl = SECONDARY_BASE_URL
        retrofit = null // Force recreate
    }
    
    /**
     * Switch to primary server URL
     */
    fun usePrimaryServer() {
        currentBaseUrl = PRIMARY_BASE_URL
        retrofit = null // Force recreate
    }
    
    /**
     * Get current base URL
     */
    fun getCurrentBaseUrl(): String = currentBaseUrl
}

