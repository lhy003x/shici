package com.shici.app.ui.words

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shici.app.R
import com.shici.app.data.Word
import com.shici.app.databinding.ItemWordBinding

class WordAdapter : RecyclerView.Adapter<WordAdapter.Holder>() {

    private val items = mutableListOf<Word>()

    fun submit(list: List<Word>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemWordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    class Holder(private val binding: ItemWordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(word: Word) {
            binding.tvSpelling.text = word.spelling
            binding.tvMeaning.text = buildString {
                if (word.phonetic.isNotBlank()) append(word.phonetic).append("  ")
                append(word.meaning)
            }
            binding.tvStatus.text = when {
                word.box == 0 -> "未学"
                word.box >= 5 -> "已掌握"
                else -> "L${word.box}"
            }
            binding.tvStatus.setTextColor(
                binding.root.context.getColor(
                    when {
                        word.box >= 5 -> R.color.green_dark
                        word.box == 0 -> R.color.ink2
                        else -> R.color.amber
                    }
                )
            )
        }
    }
}
