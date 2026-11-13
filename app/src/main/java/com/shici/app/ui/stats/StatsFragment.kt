package com.shici.app.ui.stats

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.shici.app.App
import com.shici.app.R
import com.shici.app.databinding.FragmentStatsBinding
import kotlinx.coroutines.launch

class StatsFragment : Fragment(R.layout.fragment_stats) {

    private var binding: FragmentStatsBinding? = null
    private val repo get() = (requireActivity().application as App).repo

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentStatsBinding.bind(view)
        binding = b
        b.chart.setData(listOf("一", "二", "三", "四", "五", "六", "日"), List(7) { 0 })
    }

    override fun onResume() {
        super.onResume()
        val b = binding ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val s = repo.stats()
            b.tvStreak.text = s.streak.toString()
            b.tvAccuracy.text = "${s.accuracy}%"
            b.tvTotal.text = s.totalWords.toString()
            b.tvLearning.text = s.learning.toString()
            b.tvFragile.text = s.fragile.toString()
            b.tvMastered.text = s.mastered.toString()
            b.tvTotalReviews.text = "累计复习 ${s.totalReviews} 次 · 数据只保存在本机"

            b.chart.setData(
                s.week.map { "${it.date.dayOfMonth}" },
                s.week.map { it.count },
            )
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
