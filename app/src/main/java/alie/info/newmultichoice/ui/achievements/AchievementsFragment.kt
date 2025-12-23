package alie.info.newmultichoice.ui.achievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import alie.info.newmultichoice.data.Achievements
import alie.info.newmultichoice.data.QuizRepository
import alie.info.newmultichoice.databinding.FragmentAchievementsBinding
import kotlinx.coroutines.launch

class AchievementsFragment : Fragment() {

    private var _binding: FragmentAchievementsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: QuizRepository
    private lateinit var adapter: AchievementsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAchievementsBinding.inflate(inflater, container, false)
        repository = QuizRepository.getInstance(requireContext())
        
        setupRecyclerView()
        loadAchievements()
        
        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = AchievementsAdapter()
        binding.achievementsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.achievementsRecyclerView.adapter = adapter
    }

    private fun loadAchievements() {
        viewLifecycleOwner.lifecycleScope.launch {
            val allAchievements = Achievements.getAll()
            repository.getUserStats().collect { stats ->
                // Check if binding is still valid
                if (_binding == null) return@collect
                
                val unlocked = if (stats != null) {
                    stats.unlockedAchievements.split(",").filter { id -> id.isNotEmpty() }.toSet()
                } else {
                    emptySet()
                }
                
                val achievements = allAchievements.map { achievement ->
                    achievement.copy(isUnlocked = unlocked.contains(achievement.id))
                }
                adapter.submitList(achievements)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

