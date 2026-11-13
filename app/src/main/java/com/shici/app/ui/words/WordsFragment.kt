package com.shici.app.ui.words

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.shici.app.App
import com.shici.app.R
import com.shici.app.data.BookWithCount
import com.shici.app.databinding.FragmentWordsBinding
import kotlinx.coroutines.launch

class WordsFragment : Fragment(R.layout.fragment_words) {

    private var binding: FragmentWordsBinding? = null
    private val repo get() = (requireActivity().application as App).repo
    private val adapter = WordAdapter()

    private var books: List<BookWithCount> = emptyList()
    private var selectedBook = -1L
    private var filter = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentWordsBinding.bind(view)
        binding = b

        b.rvWords.layoutManager = LinearLayoutManager(requireContext())
        b.rvWords.adapter = adapter

        b.etSearch.doAfterTextChanged { reload() }

        b.filterGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            filter = when (checkedId) {
                R.id.btnFilterHard -> 1
                R.id.btnFilterMastered -> 2
                else -> 0
            }
            reload()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repo.seedIfNeeded()
            books = repo.books()
            selectedBook = books.firstOrNull()?.id ?: -1L
            buildChips()
            reload()
        }
    }

    private fun buildChips() {
        val b = binding ?: return
        b.bookChips.removeAllViews()
        books.forEach { book ->
            val chip = TextView(requireContext()).apply {
                text = "${book.name} · ${book.wordCount}"
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(dp(13), dp(8), dp(13), dp(8))
                setOnClickListener {
                    selectedBook = book.id
                    filter = 0
                    binding?.filterGroup?.check(R.id.btnFilterAll)
                    buildChips()
                    reload()
                }
            }
            val active = book.id == selectedBook
            chip.background = ContextCompat.getDrawable(
                requireContext(),
                if (active) R.drawable.bg_pill_green else R.drawable.bg_pill,
            )
            chip.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (active) R.color.green_dark else R.color.ink2,
                )
            )
            if (active) chip.setTypeface(null, Typeface.BOLD)

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            lp.marginEnd = dp(8)
            b.bookChips.addView(chip, lp)
        }
        b.bookScroll.visibility = if (books.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun reload() {
        val b = binding ?: return
        val keyword = b.etSearch.text?.toString()?.trim().orEmpty()
        viewLifecycleOwner.lifecycleScope.launch {
            val list = when {
                keyword.isNotEmpty() -> repo.search(keyword)
                filter == 1 -> repo.hardWords()
                filter == 2 -> repo.masteredWords()
                selectedBook > 0 -> repo.wordsOfBook(selectedBook, 999)
                else -> emptyList()
            }
            val target = binding ?: return@launch
            adapter.submit(list)
            target.tvCount.text = "共 ${list.size} 个词"
            target.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
