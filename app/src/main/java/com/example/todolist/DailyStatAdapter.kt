package com.example.todolist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ItemDailyStatBinding
import java.text.SimpleDateFormat
import java.util.Locale

class DailyStatAdapter(
    private val items: List<DailyStat>,
    private val onClick: (DailyStat)->Unit
): RecyclerView.Adapter<DailyStatAdapter.VH>() {

    inner class VH(val binding: ItemDailyStatBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(stat: DailyStat){
            binding.dateText.text = SimpleDateFormat("d MMMM", Locale("tr"))
                .format(SimpleDateFormat("yyyy-MM-dd").parse(stat.date)!!)
            binding.countText.text = "${stat.completed}/${stat.total}"
            binding.root.setOnClickListener{ onClick(stat) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemDailyStatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, pos: Int) = holder.bind(items[pos])
}