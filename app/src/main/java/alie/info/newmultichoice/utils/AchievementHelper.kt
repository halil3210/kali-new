package alie.info.newmultichoice.utils

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Toast
import alie.info.newmultichoice.R
import alie.info.newmultichoice.data.Achievement
import alie.info.newmultichoice.databinding.ToastAchievementBinding
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.util.concurrent.TimeUnit

object AchievementHelper {
    
    fun showAchievementUnlocked(context: Context, achievement: Achievement) {
        val inflater = LayoutInflater.from(context)
        val binding = ToastAchievementBinding.inflate(inflater)
        
        binding.achievementIcon.text = achievement.icon
        binding.achievementTitle.text = "Achievement Unlocked!"
        binding.achievementName.text = achievement.title
        
        Toast(context).apply {
            duration = Toast.LENGTH_LONG
            view = binding.root
            setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 100)
            show()
        }
    }
    
    fun showConfetti(konfettiView: KonfettiView) {
        val party = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
            position = Position.Relative(0.5, 0.3)
        )
        
        konfettiView.start(party)
    }
}

