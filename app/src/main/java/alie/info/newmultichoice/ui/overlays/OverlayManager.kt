package alie.info.newmultichoice.ui.overlays

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import alie.info.newmultichoice.R
import alie.info.newmultichoice.databinding.*

/**
 * Central manager for all API endpoint overlays
 * Shows beautiful dialogs when specific conditions are met
 */
object OverlayManager {

    /**
     * Show Marathon Unlocked Overlay
     * Triggered when: User reaches 50 correct answers
     * API: GET /api/stats/unlock-status/:deviceId
     */
    fun showMarathonUnlockedOverlay(
        fragment: Fragment,
        correctAnswers: Int,
        accuracy: Float,
        onStartMarathon: () -> Unit
    ) {
        if (!fragment.isAdded || fragment.context == null) return

        val binding = DialogMarathonUnlockedBinding.inflate(LayoutInflater.from(fragment.requireContext()))
        
        binding.progressValue.text = correctAnswers.toString()
        binding.accuracyValue.text = "${accuracy.toInt()}%"
        
        val dialog = MaterialAlertDialogBuilder(fragment.requireContext(), R.style.TransparentDialog)
            .setView(binding.root)
            .setCancelable(false)
            .create()

        binding.continueButton.setOnClickListener {
            dialog.dismiss()
            onStartMarathon()
        }

        dialog.show()
        
        // Trigger confetti animation
        triggerConfetti(fragment.requireContext())
    }

    /**
     * Show Exams Unlocked Overlay
     * Triggered when: User reaches 50 correct answers (same condition as Marathon)
     * API: GET /api/stats/unlock-status/:deviceId
     */
    fun showExamsUnlockedOverlay(
        fragment: Fragment,
        correctAnswers: Int,
        onViewExams: () -> Unit
    ) {
        if (!fragment.isAdded || fragment.context == null) return

        val binding = DialogMarathonUnlockedBinding.inflate(LayoutInflater.from(fragment.requireContext()))
        
        // Customize for Exams
        binding.unlockDescription.text = 
            "You answered $correctAnswers questions correctly!\n\n" +
            "Prepare now for the certification exams. 8 different exams with 80 questions each are waiting for you!"
        
        binding.progressValue.text = correctAnswers.toString()
        
        val dialog = MaterialAlertDialogBuilder(fragment.requireContext(), R.style.TransparentDialog)
            .setView(binding.root)
            .setCancelable(false)
            .create()

        binding.continueButton.text = "View Exams 📝"
        binding.continueButton.setOnClickListener {
            dialog.dismiss()
            onViewExams()
        }

        dialog.show()
        triggerConfetti(fragment.requireContext())
    }

    /**
     * Show Sync Success Overlay
     * Triggered when: POST /api/sync/upload successful
     */
    fun showSyncSuccessOverlay(
        fragment: Fragment,
        statsUpdated: Boolean,
        sessionsUploaded: Int
    ) {
        if (!fragment.isAdded || fragment.context == null) return

        val binding = DialogSyncSuccessBinding.inflate(LayoutInflater.from(fragment.requireContext()))
        
        binding.statsUpdated.text = if (statsUpdated) "Yes" else "No"
        binding.sessionsCount.text = sessionsUploaded.toString()
        binding.lastSyncTime.text = "Just now"
        
        val dialog = MaterialAlertDialogBuilder(fragment.requireContext(), R.style.TransparentDialog)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        binding.okButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Show Achievement Unlocked Overlay
     * Triggered when: Achievement conditions are met
     * Examples: Perfect Score, Speed Demon, Dedicated, Master
     */
    fun showAchievementUnlockedOverlay(
        fragment: Fragment,
        achievementName: String,
        achievementDescription: String,
        achievementIcon: String,
        xpReward: Int,
        onShare: (() -> Unit)? = null
    ) {
        if (!fragment.isAdded || fragment.context == null) return

        val binding = DialogAchievementUnlockedBinding.inflate(LayoutInflater.from(fragment.requireContext()))
        
        binding.achievementIcon.text = achievementIcon
        binding.achievementName.text = achievementName
        binding.achievementDescription.text = achievementDescription
        binding.achievementReward.text = "+$xpReward XP"
        
        val dialog = MaterialAlertDialogBuilder(fragment.requireContext(), R.style.TransparentDialog)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        binding.shareButton.setOnClickListener {
            dialog.dismiss()
            onShare?.invoke()
        }

        binding.closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        triggerConfetti(fragment.requireContext())
    }

    /**
     * Show Exam Next Level Unlocked Overlay
     * Triggered when: POST /api/stats/unlock-exam successful
     * User passed an exam and next exam is unlocked
     */
    fun showExamNextUnlockedOverlay(
        fragment: Fragment,
        passedExamNumber: Int,
        nextExamNumber: Int,
        score: Float,
        errors: Int,
        maxErrors: Int,
        onStartNextExam: () -> Unit,
        onLater: () -> Unit
    ) {
        if (!fragment.isAdded || fragment.context == null) return

        val binding = DialogExamNextUnlockedBinding.inflate(LayoutInflater.from(fragment.requireContext()))
        
        binding.examTitle.text = "Exam $nextExamNumber\nFreigeschaltet!"
        binding.examUnlockDescription.text = 
            "Congratulations! You passed Exam $passedExamNumber.\n\n" +
            "The next challenge awaits you!"
        
        binding.passedExam.text = "Exam $passedExamNumber"
        binding.examScore.text = "${score.toInt()}%"
        binding.examErrors.text = "$errors / $maxErrors"
        binding.startNextExamButton.text = "Exam $nextExamNumber starten"
        
        val dialog = MaterialAlertDialogBuilder(fragment.requireContext(), R.style.TransparentDialog)
            .setView(binding.root)
            .setCancelable(false)
            .create()

        binding.startNextExamButton.setOnClickListener {
            dialog.dismiss()
            onStartNextExam()
        }

        binding.laterButton.setOnClickListener {
            dialog.dismiss()
            onLater()
        }

        dialog.show()
        triggerConfetti(fragment.requireContext())
    }

    /**
     * Show Stats Updated Overlay
     * Triggered when: POST /api/stats/update successful
     * Shows updated user stats
     */
    fun showStatsUpdatedOverlay(
        fragment: Fragment,
        totalQuizzes: Int,
        correctAnswers: Int,
        accuracy: Float,
        currentStreak: Int
    ) {
        if (!fragment.isAdded || fragment.context == null) return

        MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle("📊 Stats Updated")
            .setMessage(
                "Your statistics have been saved to the server!\n\n" +
                "• Total Quizzes: $totalQuizzes\n" +
                "• Correct Answers: $correctAnswers\n" +
                "• Accuracy: ${accuracy.toInt()}%\n" +
                "• Current Streak: $currentStreak 🔥"
            )
            .setPositiveButton("Great!") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Show Download Complete Overlay
     * Triggered when: GET /api/sync/download/:deviceId successful
     * Shows downloaded data
     */
    fun showDownloadCompleteOverlay(
        fragment: Fragment,
        sessionsDownloaded: Int,
        statsDownloaded: Boolean
    ) {
        if (!fragment.isAdded || fragment.context == null) return

        MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle("⬇️ Download Complete")
            .setMessage(
                "Data loaded from server successfully!\n\n" +
                "• Stats: ${if (statsDownloaded) "✓" else "✗"}\n" +
                "• Sessions: $sessionsDownloaded\n\n" +
                "Your local data has been updated."
            )
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Trigger confetti animation
     */
    private fun triggerConfetti(context: Context) {
        // TODO: Implement confetti animation library if desired
        android.util.Log.d("OverlayManager", "🎉 Confetti triggered!")
    }

    /**
     * Achievement definitions
     */
    object Achievements {
        data class Achievement(
            val id: String,
            val name: String,
            val description: String,
            val icon: String,
            val xpReward: Int
        )

        val FIRST_STEPS = Achievement(
            "first_steps",
            "First Steps",
            "Complete your first quiz!",
            "🎯",
            10
        )

        val PERFECT_SCORE = Achievement(
            "perfect_score",
            "Perfect Score",
            "Answer all questions correctly in a quiz!",
            "💯",
            50
        )

        val SPEED_DEMON = Achievement(
            "speed_demon",
            "Speed Demon",
            "Complete a quiz in under 5 minutes!",
            "⚡",
            30
        )

        val DEDICATED = Achievement(
            "dedicated",
            "Dedicated",
            "Achieve a 10-day streak!",
            "🔥",
            100
        )

        val MASTER = Achievement(
            "master",
            "Master",
            "Achieve an overall accuracy of 90%!",
            "🏆",
            200
        )

        fun getAchievement(id: String): Achievement? {
            return when (id) {
                "first_steps" -> FIRST_STEPS
                "perfect_score" -> PERFECT_SCORE
                "speed_demon" -> SPEED_DEMON
                "dedicated" -> DEDICATED
                "master" -> MASTER
                else -> null
            }
        }
    }
}

