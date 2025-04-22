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
        val b = holder.binding

        // İçerikleri ata
        b.taskEditText.setText(task.content)
        b.timeTextView.text = task.time

        // Saat ayarı
        if (task.time.isNotBlank() && task.time != "Saat") {
            b.timeTextView.text = task.time
        } else {
            b.timeTextView.text = "Saat"
        }

        // Pin ikonunu göster/gizle
        b.pinIcon.visibility = if (task.isPinned) View.VISIBLE else View.GONE

        // --- BURADAN BAŞLIYOR: Dinamik padding ayarı ---
        val ctx = holder.itemView.context
        val basePadding = ctx.resources.getDimensionPixelSize(R.dimen.padding_default) // 8dp
        if (task.isPinned) {
            val extra = ctx.resources.getDimensionPixelSize(R.dimen.padding_extra)   // 24dp
            b.taskEditText.setPadding(
                basePadding + extra,
                b.taskEditText.paddingTop,
                b.taskEditText.paddingEnd,
                b.taskEditText.paddingBottom
            )
        } else {
            b.taskEditText.setPadding(
                basePadding,
                b.taskEditText.paddingTop,
                b.taskEditText.paddingEnd,
                b.taskEditText.paddingBottom
            )
        }
        // --- DİNAMİK PADDING BİTTİ ---

        // Görev numarası
        b.taskNumber.text = (position + 1).toString()
        b.taskNumber.visibility = if (task.content.isNotBlank()) View.VISIBLE else View.GONE

        // EditText etkinlik
        b.taskEditText.isEnabled = task.content.isBlank()
        b.taskCheckBox.visibility = if (task.content.isNotBlank()) View.VISIBLE else View.GONE

        // Checkbox durum yönetimi
        b.taskCheckBox.isChecked = task.isChecked
        b.taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
            task.isChecked = isChecked
            GlobalScope.launch(Dispatchers.IO) {
                taskDao.updateTask(task)
            }
            onStatsChanged()
        }

        // Saat tıklama
        b.timeTextView.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            TimePickerDialog(
                holder.itemView.context,
                { _, selectedHour, selectedMinute ->
                    val timeText = String.format("%02d:%02d", selectedHour, selectedMinute)
                    b.timeTextView.text = timeText
                    task.time = timeText
                },
                hour, minute, true
            ).show()
        }

        // EditText IME action
        b.taskEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val context = holder.itemView.context
                val taskText = b.taskEditText.text.toString().trim()
                if (taskText.isEmpty()) {
                    Toast.makeText(context, "Görev adı boş olamaz.", Toast.LENGTH_SHORT).show()
                    return@setOnEditorActionListener true
                }
                task.content = taskText
                task.time = b.timeTextView.text.toString()
                GlobalScope.launch(Dispatchers.IO) {
                    taskDao.updateTask(task)
                    withContext(Dispatchers.Main) {
                        notifyItemChanged(position)
                        onStatsChanged()
                        b.taskEditText.isEnabled = false
                        b.taskCheckBox.visibility = View.VISIBLE
                        b.taskNumber.visibility = View.VISIBLE
                    }
                }
                true
            } else false
        }

        // Checkbox tek-seçim mantığı
        b.taskCheckBox.setOnClickListener {
            val isChecked = b.taskCheckBox.isChecked
            if (isChecked) {
                tasks.filter { it != task }.forEach {
                    it.isChecked = false
                    GlobalScope.launch(Dispatchers.IO) {
                        taskDao.updateTask(it)
                    }
                }
            }
            task.isChecked = isChecked
            GlobalScope.launch(Dispatchers.IO) {
                taskDao.updateTask(task)
                withContext(Dispatchers.Main) {
                    notifyItemChanged(position)
                    onStatsChanged()
                }
            }
            if (task.time.isBlank() || task.time == "Saat") {
                b.timeTextView.text = "Saat"
            } else {
                b.timeTextView.text = task.time
            }
        }

        // Satır menüsü
        holder.itemView.setOnClickListener {
            val popup = PopupMenu(holder.itemView.context, it)
            popup.menu.add("Düzenle")
            popup.menu.add("Sil")
            popup.menu.add("Başa Sabitle")

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Sil" -> {
                        AlertDialog.Builder(holder.itemView.context)
                            .setTitle("Görevi sil")
                            .setMessage("Bu görevi silmek istediğinize emin misiniz?")
                            .setPositiveButton("Evet") { _, _ ->
                                onItemDelete?.invoke(position)
                                onStatsChanged()
                            }
                            .setNegativeButton("Hayır", null)
                            .show()
                    }
                    "Düzenle" -> {
                        b.taskEditText.isEnabled = true
                        b.taskEditText.requestFocus()
                        b.taskEditText.setSelection(b.taskEditText.text.length)
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
        GlobalScope.launch(Dispatchers.IO) {
            taskDao.deleteTask(taskToDelete)
            withContext(Dispatchers.Main) {
                tasks.removeAt(position)
                notifyItemRemoved(position)
                onStatsChanged()
            }
        }
    }
}