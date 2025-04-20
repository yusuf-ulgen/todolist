package com.example.todolist

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import android.widget.TimePicker
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
    private val addTaskCallback: (Task) -> Unit, // Callback fonksiyonu
    private val taskDao: TaskDao, // Burada doğru türde TaskDao geçiyoruz
    var onStatsChanged: () -> Unit // `updateTaskStats` fonksiyonunu buraya callback olarak alıyoruz
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root)

    var onItemDelete: ((position: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.binding.taskEditText.setText(task.content)
        holder.binding.timeTextView.text = task.time

        holder.binding.pinIcon.visibility =
            if (task.isPinned) View.VISIBLE else View.GONE

        holder.binding.taskNumber.text = (position + 1).toString()
        holder.binding.taskNumber.visibility =
            if (task.content.isNotBlank()) View.VISIBLE else View.GONE

        holder.binding.taskEditText.isEnabled = task.content.isBlank()
        holder.binding.taskCheckBox.visibility =
            if (task.content.isNotBlank()) View.VISIBLE else View.GONE

        // Her checkbox'ı bağımsız olarak işaretleyin
        holder.binding.taskCheckBox.isChecked = task.isChecked

        // Sadece bu görev için checkbox durumu değişsin
        holder.binding.taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
            task.isChecked = isChecked // Yalnızca bu görev için geçerli
            GlobalScope.launch(Dispatchers.IO) {
                taskDao.updateTask(task) // Veritabanında güncelleme yapılır
            }
            onStatsChanged?.invoke() // Stats'ı güncelle
        }

        holder.binding.timeTextView.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(
                holder.itemView.context,
                { _: TimePicker, selectedHour: Int, selectedMinute: Int ->
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
                val timeText = holder.binding.timeTextView.text.toString()

                if (taskText.isEmpty()) {
                    Toast.makeText(context, "Görev adı boş olamaz.", Toast.LENGTH_SHORT).show()
                    return@setOnEditorActionListener true
                }

                if (timeText == "Saat") {
                    Toast.makeText(context, "Lütfen bir saat seçin.", Toast.LENGTH_SHORT).show()
                    return@setOnEditorActionListener true
                }

                task.content = taskText
                task.time = timeText // Güncellenen zamanı veritabanına kaydetmek için
                holder.binding.taskEditText.clearFocus()
                holder.binding.taskEditText.isEnabled = false
                holder.binding.taskCheckBox.visibility = View.VISIBLE
                holder.binding.taskNumber.visibility = View.VISIBLE

                // Veritabanına güncelleme
                GlobalScope.launch(Dispatchers.IO) {
                    taskDao.updateTask(task) // Güncellenmiş görevi veritabanına kaydet
                    withContext(Dispatchers.Main) {
                        notifyItemChanged(position) // RecyclerView'ı güncelle
                        onStatsChanged() // Günlük görev sayısını güncelle
                    }
                }

                true
            } else false
        }

        holder.binding.taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
            task.isChecked = isChecked // Veritabanına kaydedilmesi gereken durum
            // Veritabanında güncelleme
            GlobalScope.launch(Dispatchers.IO) {
                taskDao.updateTask(task) // Güncellenmiş görevi veritabanına kaydet
                withContext(Dispatchers.Main) {
                    notifyItemChanged(position) // RecyclerView'ı güncelle
                    onStatsChanged?.invoke() // Günlük görev sayısını güncelle
                }
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
        tasks = newTasks.toMutableList() // tasks listesini güncelle
        notifyDataSetChanged() // RecyclerView'ı güncelle
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
                tasks.removeAt(position) // Listeyi güncelle
                // Listeyi yeniden sıralayalım (sıralama işlemi burada yapılır)
                tasks.sortBy { it.time } // Örneğin, zamanı göre sıralama
                notifyDataSetChanged() // RecyclerView'ı güncelle
            }
        }
    }
}
