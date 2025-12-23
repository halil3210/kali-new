package alie.info.newmultichoice.data

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String, // emoji
    val isUnlocked: Boolean = false
)

object Achievements {
    fun getAll(): List<Achievement> = listOf(
        Achievement(
            id = "first_steps",
            title = "First Steps",
            description = "Complete your first quiz",
            icon = "🎯"
        ),
        Achievement(
            id = "perfect_score",
            title = "Perfect Score",
            description = "Get 100% in a quiz",
            icon = "💯"
        ),
        Achievement(
            id = "speed_demon",
            title = "Speed Demon",
            description = "Complete a quiz in under 5 minutes",
            icon = "⚡"
        ),
        Achievement(
            id = "dedicated",
            title = "Dedicated",
            description = "Maintain a 10-day streak",
            icon = "🔥"
        ),
        Achievement(
            id = "master",
            title = "Master",
            description = "Achieve 90%+ overall accuracy",
            icon = "👑"
        )
    )
}
