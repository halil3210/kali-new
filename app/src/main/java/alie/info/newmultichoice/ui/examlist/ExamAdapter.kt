package alie.info.newmultichoice.ui.examlist

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import alie.info.newmultichoice.data.Exam
import alie.info.newmultichoice.data.QuizRepository
import alie.info.newmultichoice.databinding.ItemExamBinding
import kotlinx.coroutines.launch

class ExamAdapter(
    private val onItemClick: (Exam) -> Unit,
    private val repository: QuizRepository
) : ListAdapter<Exam, ExamAdapter.ExamViewHolder>(ExamDiffCallback()) {

    private var lifecycleOwner: LifecycleOwner? = null
    
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        lifecycleOwner = recyclerView.context as? LifecycleOwner
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExamViewHolder {
        val binding = ItemExamBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExamViewHolder(binding, repository, lifecycleOwner)
    }

    override fun onBindViewHolder(holder: ExamViewHolder, position: Int) {
        val exam = getItem(position)
        holder.bind(exam)
        holder.itemView.setOnClickListener { onItemClick(exam) }
    }

    class ExamViewHolder(
        private val binding: ItemExamBinding,
        private val repository: QuizRepository,
        private val lifecycleOwner: LifecycleOwner?
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(exam: Exam) {
            binding.examNumber.text = exam.examNumber.toString()
            binding.examTitle.text = exam.title
            binding.examDescription.text = exam.description
            binding.difficultyChip.text = exam.difficulty
            
            // Check if exam is unlocked
            lifecycleOwner?.lifecycleScope?.launch {
                val isUnlocked = repository.isExamUnlocked(exam.examNumber)
                updateUIForLockState(exam, isUnlocked)
            }
        }
        
        private fun updateUIForLockState(exam: Exam, isUnlocked: Boolean) {
            if (!isUnlocked) {
                // Locked state
                binding.examTitle.alpha = 0.5f
                binding.examDescription.alpha = 0.5f
                binding.examNumber.alpha = 0.5f
                binding.difficultyChip.alpha = 0.5f
                
                // Show progress for Exam 1
                if (exam.examNumber == 1) {
                    lifecycleOwner?.lifecycleScope?.launch {
                        val correctAnswers = repository.getTotalCorrectAnswers()
                        val progress = (correctAnswers.toFloat() / 50f * 100f).toInt().coerceAtMost(100)
                        binding.examStatus.text = "🔒 $correctAnswers/50 (${progress}%)"
                        binding.examStatus.setTextColor(Color.parseColor("#FF9800"))
                    }
                } else {
                    binding.examStatus.text = "🔒 Locked"
                    binding.examStatus.setTextColor(Color.parseColor("#FF9800"))
                }
            } else {
                // Unlocked state
                binding.examTitle.alpha = 1.0f
                binding.examDescription.alpha = 1.0f
                binding.examNumber.alpha = 1.0f
                binding.difficultyChip.alpha = 1.0f
                binding.examStatus.text = "Not Taken"
                binding.examStatus.setTextColor(Color.parseColor("#FFC107"))
            }
            
            // Set difficulty chip colors
            when (exam.difficulty) {
                "EASY" -> {
                    binding.difficultyChip.setChipBackgroundColorResource(android.R.color.holo_green_dark)
                    binding.difficultyChip.chipStrokeColor = android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#9CCC65")
                    )
                }
                "MEDIUM" -> {
                    binding.difficultyChip.setChipBackgroundColorResource(android.R.color.holo_orange_dark)
                    binding.difficultyChip.chipStrokeColor = android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#FFB74D")
                    )
                }
                "HARD" -> {
                    binding.difficultyChip.setChipBackgroundColorResource(android.R.color.holo_red_dark)
                    binding.difficultyChip.chipStrokeColor = android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#EF5350")
                    )
                }
            }
            
            // Status - for now always "Not Taken"
            binding.examStatus.text = "Not Taken"
        }
    }

    class ExamDiffCallback : DiffUtil.ItemCallback<Exam>() {
        override fun areItemsTheSame(oldItem: Exam, newItem: Exam): Boolean {
            return oldItem.examNumber == newItem.examNumber
        }

        override fun areContentsTheSame(oldItem: Exam, newItem: Exam): Boolean {
            return oldItem == newItem
        }
    }
}

