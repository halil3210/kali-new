package alie.info.newmultichoice.ui.faq

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import alie.info.newmultichoice.R
import alie.info.newmultichoice.databinding.ItemFaqBinding

class FaqAdapter : ListAdapter<FaqItem, FaqAdapter.FaqViewHolder>(FaqDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
        val binding = ItemFaqBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FaqViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FaqViewHolder(private val binding: ItemFaqBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(faqItem: FaqItem) {
            binding.questionText.text = faqItem.question
            binding.answerText.text = faqItem.answer

            // Set visibility based on expanded state
            binding.answerText.visibility = if (faqItem.isExpanded) View.VISIBLE else View.GONE
            binding.expandIcon.setImageResource(
                if (faqItem.isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )

            // Toggle expansion on click
            binding.root.setOnClickListener {
                faqItem.isExpanded = !faqItem.isExpanded
                notifyItemChanged(bindingAdapterPosition)
            }
        }
    }

    class FaqDiffCallback : DiffUtil.ItemCallback<FaqItem>() {
        override fun areItemsTheSame(oldItem: FaqItem, newItem: FaqItem): Boolean {
            return oldItem.question == newItem.question
        }

        override fun areContentsTheSame(oldItem: FaqItem, newItem: FaqItem): Boolean {
            return oldItem == newItem
        }
    }
}

