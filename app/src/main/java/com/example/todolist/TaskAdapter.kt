package com.example.todolist

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ItemTaskBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class TaskAdapter(
    private var tasks: MutableList<Task>,
    private val addTaskCallback: (Task) -> Unit,
    private val taskDao: TaskDao,
    var onStatsChanged: () -> Unit // `updateTaskStats` fonksiyonunu buraya callback olarak alıyoruz
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    var onItemDelete: ((position: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.binding.taskEditText.setText(task.content)
        holder.binding.timeTextView.text = task.time

        // Eğer saat belirtildiyse, TextView'i buna göre güncelle
        if (task.time.isNotBlank() && task.time != "Saat") {
            holder.binding.timeTextView.text = task.time
        } else {
            holder.binding.timeTextView.text = "Saat"
        }

        holder.binding.pinIcon.visibility =
            if (task.isPinned) View.VISIBLE else View.GONE

        holder.binding.taskNumber.text = (position + 1).toString()
        holder.binding.taskNumber.visibility =
            if (task.content.isNotBlank()) View.VISIBLE else View.GONE

        holder.binding.taskEditText.isEnabled = task.content.isBlank()
        holder.binding.taskCheckBox.visibility =
            if (task.content.isNotBlank()) View.VISIBLE else View.GONE

        holder.binding.taskCheckBox.isChecked = task.isChecked
        holder.binding.taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
            task.isChecked = isChecked
            GlobalScope.launch(Dispatchers.IO) {
                taskDao.updateTask(task)
            }
            onStatsChanged() // Stats'ı güncelle
        }

        holder.binding.timeTextView.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(
                holder.itemView.context,
                { _, selectedHour, selectedMinute ->
                    val timeText = String.format("%02d:%02d", selectedHour, selectedMinute)
                    holder.binding.timeTextView.text = timeText
                    task.time = timeText
                },
                hour, minute, true
            ).show()
        }

        holder.binding.taskEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val context = holder.itemView.context
                val taskText = holder.binding.taskEditText.text.toString().trim()

                if (taskText.isEmpty()) {
                    Toast.makeText(context, "Görev adı boş olamaz.", Toast.LENGTH_SHORT).show()
                    return@setOnEditorActionListener true
                }

                task.content = taskText
                task.time = holder.binding.timeTextView.text.toString() // Saat de güncelleniyor

                // Veritabanına güncelleme
                GlobalScope.launch(Dispatchers.IO) {
                    taskDao.updateTask(task)
                    withContext(Dispatchers.Main) {
                        notifyItemChanged(position) // RecyclerView'ı güncelle
                        onStatsChanged() // Günlük görev sayısını güncelle
                    }
                }
                true
            } else false
        }

        // Checkbox tıklanırsa, task'ın isChecked durumunu güncelle
        holder.binding.taskCheckBox.setOnClickListener {
            // Checkbox'ı tıklayınca kontrol et
            val isChecked = holder.binding.taskCheckBox.isChecked

            // Eğer checkbox işaretlenmişse
            if (isChecked) {
                // Diğer checkbox'ları `false` yap
                tasks.filter { it != task }.forEach {
                    it.isChecked = false
                    GlobalScope.launch(Dispatchers.IO) {
                        taskDao.updateTask(it) // Diğer görevlerin checkbox durumunu güncelle
                    }
                }
            }

            // Şimdi seçilen görev için `isChecked` değerini güncelle
            task.isChecked = isChecked

            // Veritabanını güncelle
            GlobalScope.launch(Dispatchers.IO) {
                taskDao.updateTask(task) // Güncellenmiş görevi veritabanına kaydet

                withContext(Dispatchers.Main) {
                    // RecyclerView'i güncelle
                    notifyItemChanged(position)
                    onStatsChanged() // Günlük görev sayısını güncelle
                }
            }

            // Saatin gösterilmesi
            if (task.time.isBlank() || task.time == "Saat") {
                holder.binding.timeTextView.text = "Saat"
            } else {
                holder.binding.timeTextView.text = task.time
            }
        }

        holder.itemView.setOnClickListener {
            val popup = PopupMenu(holder.itemView.context, it)
            popup.menu.add("Düzenle")
            popup.menu.add("Sil")
            popup.menu.add("Başa Sabitle")

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Sil" -> {
                        val context = holder.itemView.context
                        AlertDialog.Builder(context)
                            .setTitle("Görevi sil")
                            .setMessage("Bu görevi silmek istediğinize emin misiniz?")
                            .setPositiveButton("Evet") { _, _ ->
                                onItemDelete?.invoke(position)
                                onStatsChanged() // Günlük görev sayısını güncelle
                            }
                            .setNegativeButton("Hayır", null)
                            .show()
                    }

                    "Düzenle" -> {
                        holder.binding.taskEditText.isEnabled = true
                        holder.binding.taskEditText.requestFocus()
                        holder.binding.taskEditText.setSelection(holder.binding.taskEditText.text.length)
                    }

                    "Başa Sabitle" -> {
                        val pinnedCount = tasks.count { it.isPinned }
                        if (pinnedCount >= 5) {
                            Toast.makeText(it.context, "En fazla 5 görev sabitlenebilir!", Toast.LENGTH_SHORT).show()
                        } else {
                            task.isPinned = true
                            tasks.removeAt(position)
                            val pinIndex = tasks.indexOfLast { it.isPinned }
                            tasks.add(if (pinIndex == -1) 0 else pinIndex + 1, task)
                            notifyDataSetChanged()
                        }
                    }
                }
                true
            }
            popup.show()
        }
    }

    fun setTasks(newTasks: List<Task>) {
        tasks = newTasks.toMutableList()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = tasks.size

    fun moveItem(from: Int, to: Int) {
        val task = tasks.removeAt(from)
        tasks.add(to, task)
        notifyItemMoved(from, to)
    }

    fun deleteItem(position: Int) {
        val taskToDelete = tasks[position]
        // Veritabanında silme işlemi yapılmalı
        GlobalScope.launch(Dispatchers.IO) {
            taskDao.deleteTask(taskToDelete) // Veritabanından sil
            withContext(Dispatchers.Main) {
                tasks.removeAt(position)
                notifyItemRemoved(position)
                onStatsChanged() // Günlük görev sayısını güncelle
            }
        }
    }
}