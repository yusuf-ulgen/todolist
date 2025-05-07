package com.example.todolist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.data.TaskHistory
import com.example.todolist.databinding.ItemTaskHistoryBinding

class TaskHistoryAdapter(
    private val items: List<TaskHistory>
): RecyclerView.Adapter<TaskHistoryAdapter.VH>() {

    inner class VH(val b: ItemTaskHistoryBinding): RecyclerView.ViewHolder(b.root) {
        fun bind(h: TaskHistory) {
            b.doneCheck.isChecked = h.isChecked
            b.contentText.text    = h.content
            b.timeText.text       = h.time
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemTaskHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, pos: Int) = holder.bind(items[pos])
}