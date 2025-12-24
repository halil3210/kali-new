package alie.info.newmultichoice.ui.sessions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import alie.info.newmultichoice.data.QuizSession
import alie.info.newmultichoice.databinding.ItemSessionBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class SessionsAdapter(
    private val onDeleteClick: (QuizSession) -> Unit
) : ListAdapter<QuizSession, SessionsAdapter.SessionViewHolder>(
    AsyncDifferConfig.Builder(SessionDiffCallback())
        .setBackgroundThreadExecutor(Executors.newSingleThreadExecutor())
        .build()
) {

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

        fun bind(session: QuizSession) {
            binding.sessionDateText.text = DATE_FORMAT.format(Date(session.timestamp))
            binding.sessionScoreText.text = "${session.correctAnswers} / ${session.totalQuestions}"
            binding.correctText.text = "✅ ${session.correctAnswers}"
            binding.incorrectText.text = "❌ ${session.wrongAnswers}"
            binding.accuracyText.text = String.format("%.1f%%", session.percentage)

            // Delete button click listener - only set once, not on every bind
            binding.deleteButton.setOnClickListener {
                onDeleteClick(session)
            }
        }
    }

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
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

