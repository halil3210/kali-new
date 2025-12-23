package alie.info.newmultichoice.ui.sessions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import alie.info.newmultichoice.data.QuizSession
import alie.info.newmultichoice.databinding.ItemSessionBinding
import java.text.SimpleDateFormat
import java.util.*

class SessionsAdapter(
    private val onDeleteClick: (QuizSession) -> Unit
) : ListAdapter<QuizSession, SessionsAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SessionViewHolder(binding, onDeleteClick)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SessionViewHolder(
        private val binding: ItemSessionBinding,
        private val onDeleteClick: (QuizSession) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        fun bind(session: QuizSession) {
            binding.sessionDateText.text = dateFormat.format(Date(session.timestamp))
            binding.sessionScoreText.text = "${session.correctAnswers} / ${session.totalQuestions}"
            binding.correctText.text = "✅ ${session.correctAnswers}"
            binding.incorrectText.text = "❌ ${session.wrongAnswers}"
            binding.accuracyText.text = String.format("%.1f%%", session.percentage)
            
            // Delete button click listener
            binding.deleteButton.setOnClickListener {
                onDeleteClick(session)
            }
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<QuizSession>() {
        override fun areItemsTheSame(oldItem: QuizSession, newItem: QuizSession): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: QuizSession, newItem: QuizSession): Boolean {
            return oldItem == newItem
        }
    }
}

