package alie.info.newmultichoice.api

import alie.info.newmultichoice.utils.Logger
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.math.pow

class NetworkRetryInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var lastException: IOException? = null

        // Retry mit exponential backoff
        for (attempt in 0 until MAX_RETRIES) {
            try {
                Logger.d("NetworkRetry", "Attempt ${attempt + 1}/$MAX_RETRIES for ${request.url}")

                response = chain.proceed(request)

                // Erfolgreiche Response
                if (response.isSuccessful) {
                    return response
                }

                // Server-Fehler (5xx) - retry
                if (response.code in 500..599) {
                    Logger.w("NetworkRetry", "Server error ${response.code}, retrying...")
                    response.close()
                    if (attempt < MAX_RETRIES - 1) {
                        Thread.sleep(calculateDelay(attempt))
                        continue
                    }
                }

                // Client-Fehler (4xx) - nicht retry
                return response

            } catch (e: IOException) {
                lastException = e
                Logger.w("NetworkRetry", "Network error on attempt ${attempt + 1}: ${e.message}")

                // Retry nur bei bestimmten Netzwerkfehlern
                if (shouldRetry(e) && attempt < MAX_RETRIES - 1) {
                    Thread.sleep(calculateDelay(attempt))
                    continue
                }

                // Fallback-Logik für verschiedene URLs
                if (attempt == MAX_RETRIES - 1) {
                    return handleFallback(chain, request, e)
                }
            }
        }

        // Alle Versuche fehlgeschlagen
        throw lastException ?: IOException("All retry attempts failed")
    }

    private fun shouldRetry(exception: IOException): Boolean {
        return when (exception) {
            is SocketTimeoutException -> true
            is ConnectException -> true
            is UnknownHostException -> true
            else -> exception.message?.contains("timeout", ignoreCase = true) == true
        }
    }

    private fun calculateDelay(attempt: Int): Long {
        // Exponential backoff: 1s, 2s, 4s, 8s...
        return (BASE_DELAY * 2.0.pow(attempt.toDouble())).toLong().coerceAtMost(MAX_DELAY)
    }

    private fun handleFallback(chain: Interceptor.Chain, request: okhttp3.Request, exception: IOException): Response {
        Logger.w("NetworkRetry", "All retries failed, trying fallback URLs...")

        val originalUrl = RetrofitClient.getCurrentBaseUrl()

        // Versuche lokale Entwicklung-URLs zuerst, dann Produktions-URLs
        val fallbackUrls = listOf(
            // Lokale Entwicklung (HTTP)
            "http://192.168.44.1:3000/",     // WiFi Access Point
            "http://192.168.178.27:3000/",   // Heimnetzwerk
            "http://localhost:3000/",        // Lokaler Entwicklungs-Server

            // Produktion (HTTPS zuerst, dann HTTP als letzter Fallback)
            "https://klcp.alie.info/",        // HTTPS subdomain (primary)
            "https://188.245.153.241/",       // HTTPS IP fallback
            "http://188.245.153.241:3000/"    // HTTP direct fallback
        ).filter { it != originalUrl }

        for (fallbackUrl in fallbackUrls) {
            try {
                Logger.d("NetworkRetry", "Trying fallback URL: $fallbackUrl")

                // Temporär URL ändern
                RetrofitClient.useCustomUrl(fallbackUrl)

                val newRequest = request.newBuilder()
                    .url(request.url.toString().replace(originalUrl, fallbackUrl))
                    .build()

                val response = chain.proceed(newRequest)
                if (response.isSuccessful) {
                    Logger.i("NetworkRetry", "Fallback successful with $fallbackUrl")
                    return response
                }
                response.close()

            } catch (e: Exception) {
                Logger.w("NetworkRetry", "Fallback $fallbackUrl also failed: ${e.message}")
            }
        }

        // URL zurücksetzen
        RetrofitClient.resetToDefault()

        throw exception
    }

    companion object {
        private const val MAX_RETRIES = 3
        private const val BASE_DELAY = 1000L // 1 second
        private const val MAX_DELAY = 8000L // 8 seconds
    }
}
