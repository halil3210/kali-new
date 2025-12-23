package alie.info.newmultichoice.ui.faq

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import alie.info.newmultichoice.databinding.FragmentFaqBinding

class FaqFragment : Fragment() {

    private var _binding: FragmentFaqBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: FaqAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFaqBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadFaqItems()
        startShineAnimation()
    }

    private fun setupRecyclerView() {
        adapter = FaqAdapter()
        binding.faqRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.faqRecyclerView.adapter = adapter
    }

    private fun loadFaqItems() {
        val faqItems = listOf(
            FaqItem(
                question = "❓ How do I unlock Marathon mode?",
                answer = "Answer 50 questions correctly in the regular quiz mode to unlock Marathon and Certification Exams."
            ),
            FaqItem(
                question = "🏃 What is Marathon mode?",
                answer = "Marathon mode challenges you to answer all 375 questions within 30 minutes. It's an extreme time-pressure challenge!"
            ),
            FaqItem(
                question = "📝 How do Certification Exams work?",
                answer = "Each exam has 80 questions and a 90-minute time limit. You need to make fewer than 10 errors to pass. Exam 1 requires 50 correct answers in regular quiz. Each subsequent exam unlocks after passing the previous one."
            ),
            FaqItem(
                question = "🎯 How do I unlock achievements?",
                answer = "Achievements unlock automatically as you complete specific tasks:\n\n• First Steps - Complete your first quiz\n• Perfect Score - Score 100% in a quiz\n• Speed Demon - Complete a quiz in under 5 minutes\n• Dedicated - Maintain a 10-day streak\n• Master - Achieve 90%+ average score"
            ),
            FaqItem(
                question = "📊 What do the statistics show?",
                answer = "The Statistics page shows your overall performance including total questions answered, correct answers, average score, current streak, and longest streak."
            ),
            FaqItem(
                question = "🔄 Can I practice wrong answers?",
                answer = "Yes! Use the 'Practice Wrong Answers' option in the menu to focus on questions you've answered incorrectly in the past."
            ),
            FaqItem(
                question = "🗑️ How do I delete a quiz session?",
                answer = "Go to Quiz Sessions in the menu, find the session you want to delete, and tap the delete button on the right side of the session card."
            ),
            FaqItem(
                question = "📤 Can I share my results?",
                answer = "Yes! After completing a quiz, Marathon, or Exam, tap the Share button on the results screen to share your score via WhatsApp, Twitter, Instagram, or other apps."
            ),
            FaqItem(
                question = "🌐 Are questions available in multiple languages?",
                answer = "Yes! The app supports both English (EN) and German (DE). Use the language toggle button in the quiz screen to switch between languages."
            ),
            FaqItem(
                question = "🛠️ What is Kali Linux Tools?",
                answer = "Kali Linux Tools is a comprehensive guide featuring popular penetration testing tools, their usage, commands, and security notes. It's a great resource for learning cybersecurity tools."
            ),
            FaqItem(
                question = "❌ What happens if I quit a quiz or exam?",
                answer = "Your progress will be saved, but the session will be marked as incomplete. In exams, quitting will result in a failure and you'll need to restart the exam."
            ),
            FaqItem(
                question = "⏱️ Can I pause a quiz or exam?",
                answer = "Regular quiz mode has no time limit, so you can take breaks. However, Marathon and Certification Exams have strict time limits and cannot be paused."
            ),
            FaqItem(
                question = "🔔 How do notifications work?",
                answer = "The app sends daily reminders to maintain your streak and achieve your daily goals. You can manage notification permissions in your device settings."
            ),
            FaqItem(
                question = "💾 Is my progress saved automatically?",
                answer = "Yes! All your quiz sessions, statistics, achievements, and progress are automatically saved to your device's database."
            ),
            FaqItem(
                question = "🔄 Can I reset my progress?",
                answer = "Currently, the app doesn't have a built-in reset feature. If you need to reset, you'll need to clear the app data from your device settings (this will delete all progress)."
            )
        )
        adapter.submitList(faqItems)
    }

    private fun startShineAnimation() {
        val animator = ObjectAnimator.ofFloat(binding.shineOverlay, "translationX", -200f, binding.root.width.toFloat() + 200f)
        animator.duration = 3000
        animator.repeatCount = ObjectAnimator.INFINITE
        animator.repeatMode = ObjectAnimator.RESTART
        animator.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

