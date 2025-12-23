package alie.info.newmultichoice.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ExamDifficulty {
    EASY,
    MEDIUM,
    HARD
}

@Entity(tableName = "exams")
data class Exam(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val examNumber: Int, // 1-8
    val title: String,
    val description: String,
    val difficulty: String, // "EASY", "MEDIUM", "HARD"
    val questionIds: String, // Comma-separated list of 80 question IDs
    val timeLimit: Int = 90 * 60 * 1000, // 90 minutes in milliseconds
    val passingScore: Int = 70, // Max 10 wrong answers out of 80 = 70 correct minimum
    val isCompleted: Boolean = false,
    val lastScore: Int = 0,
    val lastAttemptDate: Long = 0
)

object ExamSets {
    fun getExamSets(): List<Exam> {
        return listOf(
            // EASY Level (1-3)
            Exam(
                examNumber = 1,
                title = "Exam 1: Basic Commands",
                description = "Simple Linux commands and file operations",
                difficulty = "EASY",
                questionIds = "" // Will be populated with 80 easier question IDs
            ),
            Exam(
                examNumber = 2,
                title = "Exam 2: File System Basics",
                description = "Navigate and manage the Linux file system",
                difficulty = "EASY",
                questionIds = ""
            ),
            Exam(
                examNumber = 3,
                title = "Exam 3: User Management",
                description = "Create and manage users and groups",
                difficulty = "EASY",
                questionIds = ""
            ),
            
            // MEDIUM Level (4-6)
            Exam(
                examNumber = 4,
                title = "Exam 4: System Administration",
                description = "Processes, services, and system monitoring",
                difficulty = "MEDIUM",
                questionIds = ""
            ),
            Exam(
                examNumber = 5,
                title = "Exam 5: Networking",
                description = "Network configuration and troubleshooting",
                difficulty = "MEDIUM",
                questionIds = ""
            ),
            Exam(
                examNumber = 6,
                title = "Exam 6: Shell Scripting",
                description = "Bash scripting and automation",
                difficulty = "MEDIUM",
                questionIds = ""
            ),
            
            // HARD Level (7-8)
            Exam(
                examNumber = 7,
                title = "Exam 7: Advanced Topics",
                description = "Security, SELinux, and advanced configurations",
                difficulty = "HARD",
                questionIds = ""
            ),
            Exam(
                examNumber = 8,
                title = "Exam 8: Final Certification",
                description = "Comprehensive exam covering all topics",
                difficulty = "HARD",
                questionIds = ""
            )
        )
    }
}

