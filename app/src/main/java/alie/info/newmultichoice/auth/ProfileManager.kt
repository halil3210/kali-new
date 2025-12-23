package alie.info.newmultichoice.auth

import android.content.Context
import android.content.SharedPreferences

class ProfileManager private constructor(context: Context) {

    private val prefs: SharedPreferences = 
        context.getSharedPreferences("klcp_profile", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var INSTANCE: ProfileManager? = null
        
        private const val PREF_NAME = "profile_name"
        private const val PREF_AGE = "profile_age"

        fun getInstance(context: Context): ProfileManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProfileManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    fun getName(): String? {
        return prefs.getString(PREF_NAME, null)
    }

    fun setName(name: String) {
        prefs.edit().putString(PREF_NAME, name).apply()
    }

    fun getAge(): Int {
        return prefs.getInt(PREF_AGE, 0)
    }

    fun setAge(age: Int) {
        prefs.edit().putInt(PREF_AGE, age).apply()
    }

    fun hasProfile(): Boolean {
        return !getName().isNullOrBlank()
    }

    fun clearProfile() {
        prefs.edit().clear().apply()
    }
}

