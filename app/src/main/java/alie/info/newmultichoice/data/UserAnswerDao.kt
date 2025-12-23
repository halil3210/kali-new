package alie.info.newmultichoice.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Data Access Object for user answers
 */
@Dao
interface UserAnswerDao {
    
    @Insert
    suspend fun insertAnswer(answer: UserAnswer)
    
    @Query("SELECT * FROM user_answers WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getAnswersForSession(sessionId: Long): List<UserAnswer>
    
    @Query("DELETE FROM user_answers WHERE sessionId = :sessionId")
    suspend fun deleteAnswersForSession(sessionId: Long)
    
    @Query("DELETE FROM user_answers")
    suspend fun deleteAllAnswers()
    
    @Query("SELECT * FROM user_answers ORDER BY timestamp DESC")
    suspend fun getAllAnswers(): List<UserAnswer>
    
    @Query("SELECT * FROM user_answers WHERE isCorrect = 0 ORDER BY timestamp DESC")
    suspend fun getIncorrectAnswers(): List<UserAnswer>
}

