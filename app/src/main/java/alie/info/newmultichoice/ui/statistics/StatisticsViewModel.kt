package alie.info.newmultichoice.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import alie.info.newmultichoice.data.QuizDatabase
import alie.info.newmultichoice.data.UserAnswer
import kotlinx.coroutines.launch

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = QuizDatabase.getInstance(application)
    private val userAnswerDao = database.userAnswerDao()
    
    private val _totalQuestions = MutableLiveData<Int>(0)
    val totalQuestions: LiveData<Int> = _totalQuestions
    
    private val _correctAnswers = MutableLiveData<Int>(0)
    val correctAnswers: LiveData<Int> = _correctAnswers
    
    private val _incorrectAnswers = MutableLiveData<Int>(0)
    val incorrectAnswers: LiveData<Int> = _incorrectAnswers
    
    private val _accuracy = MutableLiveData<Double>(0.0)
    val accuracy: LiveData<Double> = _accuracy
    
    private val _wrongAnswers = MutableLiveData<List<UserAnswer>>(emptyList())
    val wrongAnswers: LiveData<List<UserAnswer>> = _wrongAnswers
    
    init {
        loadStatistics()
    }
    
    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                // Get all user answers
                val allAnswers = userAnswerDao.getAllAnswers()
                
                // Calculate statistics
                val total = allAnswers.size
                val correct = allAnswers.count { it.isCorrect }
                val incorrect = allAnswers.count { !it.isCorrect }
                val accuracyValue = if (total > 0) (correct.toDouble() / total.toDouble()) * 100 else 0.0
                
                // Get wrong answers
                val wrong = allAnswers.filter { !it.isCorrect }
                
                // Update LiveData
                _totalQuestions.postValue(total)
                _correctAnswers.postValue(correct)
                _incorrectAnswers.postValue(incorrect)
                _accuracy.postValue(accuracyValue)
                _wrongAnswers.postValue(wrong)
                
            } catch (e: Exception) {
                e.printStackTrace()
                // Set default values on error
                _totalQuestions.postValue(0)
                _correctAnswers.postValue(0)
                _incorrectAnswers.postValue(0)
                _accuracy.postValue(0.0)
                _wrongAnswers.postValue(emptyList())
            }
        }
    }
    
    fun refreshStatistics() {
        loadStatistics()
    }
}

