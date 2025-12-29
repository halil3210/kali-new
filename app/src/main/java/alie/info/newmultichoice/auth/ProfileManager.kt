package alie.info.newmultichoice.auth

import android.content.Context
import android.content.SharedPreferences

class ProfileManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var instance: ProfileManager? = null

        fun getInstance(context: Context): ProfileManager {
            return instance ?: synchronized(this) {
                instance ?: ProfileManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun getName(): String? = prefs.getString("name", null)
    fun getEmail(): String? = prefs.getString("email", null)
    fun getAge(): Int = prefs.getInt("age", 0)

    fun setName(name: String) {
        prefs.edit().putString("name", name).apply()
    }

    fun setEmail(email: String) {
        prefs.edit().putString("email", email).apply()
    }

    fun setAge(age: Int) {
        prefs.edit().putInt("age", age).apply()
    }

    fun clearProfile() {
        prefs.edit().clear().apply()
    }
}
