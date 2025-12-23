package alie.info.newmultichoice.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import alie.info.newmultichoice.R
import alie.info.newmultichoice.databinding.FragmentStatisticsBinding

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: StatisticsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[StatisticsViewModel::class.java]
        
        setupUI()
        observeViewModel()
        
        return binding.root
    }
    
    private fun setupUI() {
        binding.reviewWrongAnswersButton.setOnClickListener {
            // Navigate to quiz in practice mode (only wrong answers)
            val action = StatisticsFragmentDirections.actionNavStatisticsToQuizFragment(practiceMode = true)
            findNavController().navigate(action)
        }
    }
    
    private fun observeViewModel() {
        viewModel.totalQuestions.observe(viewLifecycleOwner) { total ->
            binding.totalQuestionsText.text = total.toString()
        }
        
        viewModel.correctAnswers.observe(viewLifecycleOwner) { correct ->
            binding.correctAnswersText.text = correct.toString()
        }
        
        viewModel.incorrectAnswers.observe(viewLifecycleOwner) { incorrect ->
            binding.incorrectAnswersText.text = incorrect.toString()
            
            // Show/hide button and empty state
            if (incorrect > 0) {
                binding.reviewWrongAnswersButton.visibility = View.VISIBLE
                binding.reviewWrongAnswersButton.text = getString(R.string.review_wrong_answers) + " ($incorrect)"
                binding.emptyStateText.visibility = View.GONE
            } else {
                binding.reviewWrongAnswersButton.visibility = View.GONE
                binding.emptyStateText.visibility = View.VISIBLE
            }
        }
        
        viewModel.accuracy.observe(viewLifecycleOwner) { accuracy ->
            binding.accuracyText.text = String.format("%.1f%%", accuracy)
            binding.accuracyProgress.progress = accuracy.toInt()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

