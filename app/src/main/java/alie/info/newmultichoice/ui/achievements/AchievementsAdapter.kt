package alie.info.newmultichoice.ui.achievements

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import alie.info.newmultichoice.data.Achievement
import alie.info.newmultichoice.databinding.ItemAchievementBinding

class AchievementsAdapter : ListAdapter<Achievement, AchievementsAdapter.ViewHolder>(AchievementDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAchievementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemAchievementBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(achievement: Achievement) {
            binding.achievementIcon.text = achievement.icon
            binding.achievementTitle.text = achievement.title
            binding.achievementDescription.text = achievement.description
            
            if (achievement.isUnlocked) {
                binding.lockIcon.visibility = View.GONE
                binding.achievementIcon.alpha = 1.0f
            } else {
                binding.lockIcon.visibility = View.VISIBLE
                binding.achievementIcon.alpha = 0.3f
            }
        }
    }

    class AchievementDiffCallback : DiffUtil.ItemCallback<Achievement>() {
        override fun areItemsTheSame(oldItem: Achievement, newItem: Achievement): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Achievement, newItem: Achievement): Boolean {
            return oldItem == newItem
        }
    }
}

