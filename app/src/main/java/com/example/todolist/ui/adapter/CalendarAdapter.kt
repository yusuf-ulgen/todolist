package com.example.todolist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ItemDayBinding
import java.time.DayOfWeek

class CalendarAdapter(
    private val days: List<DayOfWeek>,
    private val onClick: (DayOfWeek) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    private var selectedPos = RecyclerView.NO_POSITION

    inner class DayViewHolder(val binding: ItemDayBinding)
        : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val old = selectedPos
                selectedPos = adapterPosition
                notifyItemChanged(old)
                notifyItemChanged(selectedPos)
                onClick(days[selectedPos])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        DayViewHolder(ItemDayBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ))

    override fun getItemCount() = days.size

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val dow = days[position]

        val labels = listOf("Pzt","Sal","Çar","Per","Cum","Cmt","Paz")
        holder.binding.dayText.text = labels[dow.ordinal]

        // Seçilene özel arka plan
        if (position == selectedPos) {
            holder.binding.root.setBackgroundResource(R.drawable.bg_day_selected)
        } else {
            holder.binding.root.setBackgroundResource(0)
        }
    }

    fun getPositionFor(day: DayOfWeek): Int {
        return days.indexOf(day)
    }

    fun selectDay(dow: DayOfWeek) {
        val pos = getPositionFor(dow).takeIf { it >= 0 } ?: return
        val old = selectedPos
        selectedPos = pos
        notifyItemChanged(old)
        notifyItemChanged(selectedPos)
        onClick(dow)
    }
}