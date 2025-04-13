package com.example.todolist

import android.app.AlertDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Build
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import android.widget.TimePicker
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ItemTaskBinding
import java.util.*

class TaskAdapter(
    private val tasks: MutableList<Task>
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root)

    var onItemDelete: ((position: Int) -> Unit)? = null
    var onStatsChanged: (() -> Unit)? = null

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

        holder.binding.taskCheckBox.isChecked = task.isChecked

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
                holder.binding.taskEditText.clearFocus()
                holder.binding.taskEditText.isEnabled = false
                holder.binding.taskCheckBox.visibility = View.VISIBLE
                holder.binding.taskNumber.visibility = View.VISIBLE

                // ✅ Alarm kurma
                if (timeText != "Saat") {
                    val timeParts = timeText.split(":")
                    val hour = timeParts[0].toInt()
                    val minute = timeParts[1].toInt()
                    setAlarm(holder.itemView.context, task, hour, minute, position)
                }
                onStatsChanged?.invoke()
                true
            } else false
        }

        holder.binding.taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
            task.isChecked = isChecked
            holder.binding.taskEditText.paintFlags = if (isChecked) {
                holder.binding.taskEditText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                holder.binding.taskEditText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            onStatsChanged?.invoke()
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
                                onStatsChanged?.invoke()
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

    private fun setAlarm(context: Context, task: Task, hour: Int, minute: Int, id: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

        // Android 12 ve sonrası için izin kontrolü
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(context, "Alarm izni verilmemiş. Ayarlardan izin vermelisiniz.", Toast.LENGTH_LONG).show()

                // Kullanıcıyı ayarlara yönlendirme
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
                return // ❗ HATA ALMAMAK İÇİN MUTLAKA FONKSİYONDAN ÇIK
            }
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", task.content)
            putExtra("id", id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
        }

        alarmManager.setExact(
            android.app.AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }



    override fun getItemCount(): Int = tasks.size

    fun moveItem(from: Int, to: Int) {
        val task = tasks.removeAt(from)
        tasks.add(to, task)
        notifyItemMoved(from, to)
    }

    fun deleteItem(position: Int) {
        tasks.removeAt(position)
        notifyItemRemoved(position)
    }
}
