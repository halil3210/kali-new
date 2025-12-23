package alie.info.newmultichoice.ui.kalitools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import alie.info.newmultichoice.data.KaliTool
import alie.info.newmultichoice.data.KaliTools
import alie.info.newmultichoice.data.QuizDatabase
import alie.info.newmultichoice.databinding.FragmentKaliToolsBinding
import kotlinx.coroutines.launch

class KaliToolsFragment : Fragment() {

    private var _binding: FragmentKaliToolsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: KaliToolsAdapter
    private lateinit var database: QuizDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKaliToolsBinding.inflate(inflater, container, false)
        database = QuizDatabase.getInstance(requireContext())
        
        setupRecyclerView()
        loadTools()
        startShineAnimation()
        
        return binding.root
    }
    
    private fun startShineAnimation() {
        val shineAnimation = android.view.animation.AnimationUtils.loadAnimation(
            requireContext(),
            alie.info.newmultichoice.R.anim.shine_animation
        )
        binding.shineOverlay.startAnimation(shineAnimation)
    }

    private fun setupRecyclerView() {
        adapter = KaliToolsAdapter { tool ->
            navigateToToolDetail(tool)
        }
        
        binding.toolsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@KaliToolsFragment.adapter
        }
    }

    private fun loadTools() {
        binding.loadingIndicator.visibility = View.VISIBLE
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Initialize tools in database if empty
            val toolDao = database.kaliToolDao()
            val existingTools = toolDao.getAllTools()
            
            existingTools.collect { tools ->
                // Check if binding is still valid
                val currentBinding = _binding ?: return@collect
                
                if (tools.isEmpty()) {
                    // First time - populate database
                    toolDao.insertTools(KaliTools.getAllTools())
                } else {
                    // Display tools
                    currentBinding.loadingIndicator.visibility = View.GONE
                    if (tools.isEmpty()) {
                        currentBinding.emptyStateText.visibility = View.VISIBLE
                        currentBinding.toolsRecyclerView.visibility = View.GONE
                    } else {
                        currentBinding.emptyStateText.visibility = View.GONE
                        currentBinding.toolsRecyclerView.visibility = View.VISIBLE
                        adapter.submitList(tools)
                    }
                }
            }
        }
    }

    private fun navigateToToolDetail(tool: KaliTool) {
        val action = KaliToolsFragmentDirections.actionNavKaliToolsToKaliToolDetail(tool.id)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

