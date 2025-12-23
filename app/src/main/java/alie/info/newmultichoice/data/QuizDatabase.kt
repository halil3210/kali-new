package alie.info.newmultichoice.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for the KLCP Quiz app
 */
@Database(
    entities = [Question::class, QuizSession::class, UserAnswer::class, UserStats::class, KaliTool::class],
    version = 6, // Added userId to QuizSession for multi-user support
    exportSchema = false
)
abstract class QuizDatabase : RoomDatabase() {
    
    abstract fun questionDao(): QuestionDao
    abstract fun quizSessionDao(): QuizSessionDao
    abstract fun userAnswerDao(): UserAnswerDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun kaliToolDao(): KaliToolDao
    
    companion object {
        @Volatile
        private var INSTANCE: QuizDatabase? = null
        
        fun getInstance(context: Context): QuizDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuizDatabase::class.java,
                    "klcp_quiz_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        fun getDatabase(context: Context): QuizDatabase {
            return getInstance(context)
        }
    }
}

