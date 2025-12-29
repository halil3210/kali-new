package alie.info.newmultichoice.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class OfflineManager(private val context: Context) {

    private val _isOnline = MutableLiveData<Boolean>()
    val isOnline: LiveData<Boolean> = _isOnline

    private val _connectionType = MutableLiveData<ConnectionType>()
    val connectionType: LiveData<ConnectionType> = _connectionType

    init {
        checkConnection()
    }

    fun checkConnection(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        val isConnected = capabilities != null && (
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        )

        val connectionType = when {
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> ConnectionType.WIFI
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> ConnectionType.MOBILE
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> ConnectionType.ETHERNET
            else -> ConnectionType.NONE
        }

        _isOnline.postValue(isConnected)
        _connectionType.postValue(connectionType)

        return isConnected
    }

    fun isOnline(): Boolean = _isOnline.value ?: false

    fun getConnectionType(): ConnectionType = _connectionType.value ?: ConnectionType.NONE

    /**
     * Should we attempt network requests?
     * WIFI: Always
     * MOBILE: Only for essential requests
     * NONE: Never
     */
    fun shouldAttemptNetwork(isEssential: Boolean = false): Boolean {
        return when (getConnectionType()) {
            ConnectionType.WIFI -> true
            ConnectionType.MOBILE -> isEssential // Nur essentielle Requests über Mobile
            ConnectionType.ETHERNET -> true
            ConnectionType.NONE -> false
        }
    }

    enum class ConnectionType {
        WIFI, MOBILE, ETHERNET, NONE
    }
}
