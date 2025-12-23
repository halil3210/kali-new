package alie.info.newmultichoice.ui.kalitools

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import alie.info.newmultichoice.data.KaliTool
import alie.info.newmultichoice.databinding.ItemKaliToolBinding

class KaliToolsAdapter(
    private val onToolClick: (KaliTool) -> Unit
) : ListAdapter<KaliTool, KaliToolsAdapter.KaliToolViewHolder>(KaliToolDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KaliToolViewHolder {
        val binding = ItemKaliToolBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return KaliToolViewHolder(binding, onToolClick)
    }

    override fun onBindViewHolder(holder: KaliToolViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class KaliToolViewHolder(
        private val binding: ItemKaliToolBinding,
        private val onToolClick: (KaliTool) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tool: KaliTool) {
            binding.toolName.text = tool.name
            binding.toolDescription.text = tool.description
            binding.categoryChip.text = tool.category
            
            binding.root.setOnClickListener {
                onToolClick(tool)
            }
        }
    }

    private class KaliToolDiffCallback : DiffUtil.ItemCallback<KaliTool>() {
        override fun areItemsTheSame(oldItem: KaliTool, newItem: KaliTool): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: KaliTool, newItem: KaliTool): Boolean {
            return oldItem == newItem
        }
    }
}

