package com.example.todolist

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ItemTaskBinding

class TaskAdapter(private val tasks: MutableList<String>) :
    RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.binding.taskEditText.setText(tasks[position])
        holder.binding.taskEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                tasks[position] = holder.binding.taskEditText.text.toString()
            }
        }

        // Enter tuşuna basıldığında yazıyı kaydet ve klavyeyi kapat
        holder.binding.taskEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                tasks[position] = holder.binding.taskEditText.text.toString()
                holder.binding.taskEditText.clearFocus()
                true
            } else {
                false
            }
        }

        // Saat göstergesi sabit örnek olarak 12:00 - burada ileri geliştirme yapılabilir
        holder.binding.timeTextView.text = "12:00"
    }

    override fun getItemCount(): Int = tasks.size
}

