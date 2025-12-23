package alie.info.newmultichoice.ui.sessions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import alie.info.newmultichoice.auth.AuthManager
import alie.info.newmultichoice.data.QuizDatabase
import alie.info.newmultichoice.data.QuizSession
import kotlinx.coroutines.launch

class SessionsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = QuizDatabase.getInstance(application)
    private val sessionDao = database.quizSessionDao()
    private val authManager = AuthManager.getInstance(application)
    
    private val _sessions = MutableLiveData<List<QuizSession>>()
    val sessions: LiveData<List<QuizSession>> = _sessions
    
    init {
        loadSessions()
    }
    
    private fun loadSessions() {
        viewModelScope.launch {
            try {
                val userId = authManager.getUserId() ?: ""
                val allSessions = sessionDao.getAllSessions(userId)
                _sessions.postValue(allSessions)
            } catch (e: Exception) {
                e.printStackTrace()
                _sessions.postValue(emptyList())
            }
        }
    }
    
    fun refreshSessions() {
        loadSessions()
    }
    
    fun deleteSession(session: QuizSession) {
        viewModelScope.launch {
            try {
                sessionDao.deleteSession(session)
                loadSessions()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
