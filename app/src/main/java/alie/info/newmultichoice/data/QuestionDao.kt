package alie.info.newmultichoice.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object for questions
 */
@Dao
interface QuestionDao {
    
    @Query("SELECT * FROM questions ORDER BY id ASC")
    suspend fun getAllQuestions(): List<Question>
    
    @Query("SELECT * FROM questions WHERE id = :questionId")
    suspend fun getQuestionById(questionId: Int): Question?
    
    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getQuestionCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<Question>)
    
    @Query("DELETE FROM questions")
    suspend fun deleteAllQuestions()
}

