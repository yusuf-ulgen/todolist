package com.example.todolist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ItemListBinding

class ListelerimAdapter(
    private val lists: MutableList<Todolist>,
    private val onClick: (Todolist) -> Unit,
    private val onLongClick: (Todolist) -> Unit,
    private val onRenameRequest: (Todolist) -> Unit
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
                val popup = android.widget.PopupMenu(context, this)
                popup.menuInflater.inflate(R.menu.menu_list_item, popup.menu)
                popup.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.rename_list -> {
                            onRenameRequest(todo)
                            true
                        }
                        R.id.delete_list -> {
                            onLongClick(todo)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
                true
            }

        }
    }

    override fun getItemCount(): Int = lists.size

    class ListViewHolder(val binding: ItemListBinding) : RecyclerView.ViewHolder(binding.root)
}