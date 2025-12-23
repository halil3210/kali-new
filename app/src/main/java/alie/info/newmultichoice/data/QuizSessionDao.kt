package alie.info.newmultichoice.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/**
 * Data Access Object for quiz sessions
 */
@Dao
interface QuizSessionDao {
    
    @Insert
    suspend fun insertSession(session: QuizSession): Long
    
    @Update
    suspend fun updateSession(session: QuizSession)
    
    @Query("SELECT * FROM quiz_sessions WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllSessions(userId: String): List<QuizSession>
    
    @Query("SELECT * FROM quiz_sessions WHERE userId = :userId ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentSessions(userId: String): List<QuizSession>
    
    @Delete
    suspend fun deleteSession(session: QuizSession)
    
    @Query("DELETE FROM quiz_sessions WHERE userId = :userId")
    suspend fun deleteAllSessions(userId: String)
}

