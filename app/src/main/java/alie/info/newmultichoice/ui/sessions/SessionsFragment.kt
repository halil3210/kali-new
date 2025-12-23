package alie.info.newmultichoice.ui.sessions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import alie.info.newmultichoice.R
import alie.info.newmultichoice.databinding.FragmentSessionsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SessionsFragment : Fragment() {

    private var _binding: FragmentSessionsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: SessionsViewModel
    private lateinit var adapter: SessionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[SessionsViewModel::class.java]
        
        setupRecyclerView()
        observeViewModel()
        
        return binding.root
    }
    
    private fun setupRecyclerView() {
        adapter = SessionsAdapter { session ->
            showDeleteConfirmationDialog(session)
        }
        binding.sessionsRecyclerView.adapter = adapter
        binding.sessionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }
    
    private fun showDeleteConfirmationDialog(session: alie.info.newmultichoice.data.QuizSession) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Session")
            .setMessage("Are you sure you want to delete this quiz session?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteSession(session)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun observeViewModel() {
        viewModel.sessions.observe(viewLifecycleOwner) { sessions ->
            if (sessions.isEmpty()) {
                binding.emptyStateText.visibility = View.VISIBLE
                binding.sessionsRecyclerView.visibility = View.GONE
            } else {
                binding.emptyStateText.visibility = View.GONE
                binding.sessionsRecyclerView.visibility = View.VISIBLE
                adapter.submitList(sessions)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

