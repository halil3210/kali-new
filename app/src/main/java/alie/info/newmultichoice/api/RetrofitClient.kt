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

    // Development Server URLs (HTTP for local testing)
    private const val LOCAL_DEVELOPMENT = "http://localhost:3000/"  // Local development server

    // Production Server URLs
    private const val PRIMARY_DOMAIN = "https://klcp.alie.info/"  // HTTPS subdomain with Let's Encrypt
    private const val SECONDARY_DOMAIN = "https://188.245.153.241/"  // IP fallback (HTTPS)
    private const val FALLBACK_IP_HTTP = "http://188.245.153.241:3000/"  // Direct IP HTTP fallback

    private var currentBaseUrl = PRIMARY_DOMAIN  // Start with HTTPS subdomain
    
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
        .apply {
            // Allow self-signed certificates for development
            val trustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(trustManager), null)

            sslSocketFactory(sslContext.socketFactory, trustManager)
            hostnameVerifier { _, _ -> true }
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

    /**
     * Temporarily use a custom URL for fallback
     */
    fun useCustomUrl(url: String) {
        currentBaseUrl = url
        retrofit = null // Force recreation
    }

    /**
     * Reset to default URL (starts with local development)
     */
    fun resetToDefault() {
        currentBaseUrl = LOCAL_DEVELOPMENT
        retrofit = null
    }
}

