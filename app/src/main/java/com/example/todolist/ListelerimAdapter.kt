package com.example.todolist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.data.Todolist
import com.example.todolist.databinding.ItemListBinding

class ListelerimAdapter(
    private val lists: MutableList<Todolist>,
    private val onClick: (Todolist) -> Unit,
    private val onLongClick: (Todolist) -> Unit
) : RecyclerView.Adapter<ListelerimAdapter.ListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val binding = ItemListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val todo = lists[position]
        holder.binding.listItemButton.apply {
            text = todo.name
            setOnClickListener { onClick(todo) }
            setOnLongClickListener {
                onLongClick(todo)
                true
            }
        }
    }

    override fun getItemCount(): Int = lists.size

    class ListViewHolder(val binding: ItemListBinding) : RecyclerView.ViewHolder(binding.root)
}