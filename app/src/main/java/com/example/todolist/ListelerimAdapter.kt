package com.example.todolist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.data.Todolist
import com.example.todolist.databinding.ItemListBinding

class ListelerimAdapter(private val lists: List<Todolist>, private val onLongClick: (Int) -> Unit) :
    RecyclerView.Adapter<ListelerimAdapter.ListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val binding = ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val currentItem = lists[position]
        holder.bind(currentItem)
        holder.itemView.setOnLongClickListener {
            onLongClick(position) // Long click olayını tetikler
            true
        }
    }

    override fun getItemCount(): Int = lists.size

    class ListViewHolder(private val binding: ItemListBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(todoList: Todolist) {
            binding.listName.text = todoList.name
        }
    }
}