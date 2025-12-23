package alie.info.newmultichoice.ui.examlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import alie.info.newmultichoice.R
import alie.info.newmultichoice.data.Exam
import alie.info.newmultichoice.data.ExamSets
import alie.info.newmultichoice.data.QuizRepository
import alie.info.newmultichoice.databinding.FragmentExamListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ExamListFragment : Fragment() {

    private var _binding: FragmentExamListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ExamAdapter
    private lateinit var repository: QuizRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExamListBinding.inflate(inflater, container, false)
        repository = QuizRepository.getInstance(requireContext())
        
        setupRecyclerView()
        loadExams()
        startShineAnimation()
        
        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = ExamAdapter(
            onItemClick = { exam ->
                // Check if exam is unlocked
                viewLifecycleOwner.lifecycleScope.launch {
                    val isUnlocked = repository.isExamUnlocked(exam.examNumber)
                    if (isUnlocked) {
                        // Navigate to ExamQuizFragment
                        val action = ExamListFragmentDirections.actionNavExamListToExamQuizFragment(
                            examNumber = exam.examNumber,
                            examTitle = exam.title,
                            examDifficulty = exam.difficulty
                        )
                        findNavController().navigate(action)
                    } else {
                        // Show locked dialog
                        showLockedDialog(exam.examNumber)
                    }
                }
            },
            repository = repository
        )
        binding.examsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.examsRecyclerView.adapter = adapter
    }

    private fun loadExams() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Simulate loading
            delay(300)
            
            val exams = ExamSets.getExamSets()
            
            val currentBinding = _binding ?: return@launch
            adapter.submitList(exams)
        }
    }

    private fun startShineAnimation() {
        val shineAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.shine_animation)
        binding.shineOverlay.startAnimation(shineAnimation)
    }
    
    private fun showLockedDialog(examNumber: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val correctAnswers = repository.getTotalCorrectAnswers()
            
            val message = if (examNumber == 1) {
                // Special message for Exam 1
                val remaining = 50 - correctAnswers
                if (remaining > 0) {
                    "🎯 To unlock Exam 1, you need to answer $remaining more questions correctly in the Quiz!\n\n" +
                    "Current progress: $correctAnswers / 50 ✅"
                } else {
                    "Exam 1 is now unlocked! 🎉"
                }
            } else {
                // For other exams
                "Pass Exam ${examNumber - 1} first to unlock this exam!\n\n" +
                "You must complete the exams in order."
            }
            
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("🔒 Exam Locked")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

