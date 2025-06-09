package com.example.todolist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ItemAdminFeedbackBinding
import java.text.SimpleDateFormat
import java.util.*

data class FeedbackItem(val id: String, val title: String, val message: String, val timestamp: Long, val userEmail: String?)

class AdminFeedbackAdapter(
    private var items: List<FeedbackItem>
) : RecyclerView.Adapter<AdminFeedbackAdapter.VH>() {

    inner class VH(val b: ItemAdminFeedbackBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemAdminFeedbackBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val fb = items[position]
        holder.b.tvTitle.text   = fb.title
        holder.b.tvMessage.text = fb.message
        holder.b.tvDate.text    = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(fb.timestamp))
    }

    fun submitList(new: List<FeedbackItem>) {
        items = new
        notifyDataSetChanged()
    }
}