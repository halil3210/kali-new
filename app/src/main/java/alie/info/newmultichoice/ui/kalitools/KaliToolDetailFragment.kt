package alie.info.newmultichoice.ui.kalitools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import alie.info.newmultichoice.data.QuizDatabase
import alie.info.newmultichoice.databinding.FragmentKaliToolDetailBinding
import kotlinx.coroutines.launch

class KaliToolDetailFragment : Fragment() {

    private var _binding: FragmentKaliToolDetailBinding? = null
    private val binding get() = _binding!!
    
    private val args: KaliToolDetailFragmentArgs by navArgs()
    private lateinit var database: QuizDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKaliToolDetailBinding.inflate(inflater, container, false)
        database = QuizDatabase.getInstance(requireContext())
        
        loadToolDetails()
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

    private fun loadToolDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            val tool = database.kaliToolDao().getToolById(args.toolId)
            
            tool?.let {
                // Check if binding is still valid
                val currentBinding = _binding ?: return@launch
                
                currentBinding.toolNameHeader.text = it.name
                currentBinding.mainFunctionText.text = it.mainFunction
                currentBinding.importanceText.text = it.importance
                currentBinding.runCommandText.text = it.runCommand
                currentBinding.guiPathText.text = it.guiPath
                currentBinding.usageText.text = it.usage
                currentBinding.noteText.text = it.note
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

