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

        // 1) İçerik ve Saat
        b.taskEditText.setText(task.content)
        b.timeTextView.text = if (task.time.isNotBlank() && task.time != "Saat") task.time else "Saat"

        // 2) isChecked durumu — önce eski listener'ı temizle
        b.taskCheckBox.setOnCheckedChangeListener(null)
        b.taskCheckBox.isChecked = task.isChecked
        b.taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
            task.isChecked = isChecked
            GlobalScope.launch(Dispatchers.IO) {
                taskDao.updateTask(task)
            }
            onStatsChanged()
        }

        // 3) isPinned durumu
        b.pinIcon.visibility = if (task.isPinned) View.VISIBLE else View.GONE

        // 4) Dinamik padding
        val ctx = holder.itemView.context
        val base = ctx.resources.getDimensionPixelSize(R.dimen.padding_default)
        val extra = ctx.resources.getDimensionPixelSize(R.dimen.padding_extra)
        b.taskEditText.setPadding(
            if (task.isPinned) base + extra else base,
            b.taskEditText.paddingTop,
            b.taskEditText.paddingEnd,
            b.taskEditText.paddingBottom
        )

        // 5) Görev numarası ve checkbox görünürlüğü
        b.taskNumber.text = (position + 1).toString()
        b.taskNumber.visibility = if (task.content.isNotBlank()) View.VISIBLE else View.GONE
        b.taskCheckBox.visibility = if (task.content.isNotBlank()) View.VISIBLE else View.GONE

        // 6) Düzenleme modu
        b.taskEditText.isEnabled = task.content.isBlank()

        // 7) Saat seçici
        b.timeTextView.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                ctx,
                { _, h, m ->
                    val timeText = String.format("%02d:%02d", h, m)
                    b.timeTextView.text = timeText
                    task.time = timeText
                    GlobalScope.launch(Dispatchers.IO) {
                        taskDao.updateTask(task)
                    }
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }

        // 8) Görevi kaydet (IME action)
        b.taskEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val txt = b.taskEditText.text.toString().trim()
                if (txt.isEmpty()) {
                    b.taskEditText.error = "Görev adı boş olamaz."
                    b.taskEditText.requestFocus()
                    return@setOnEditorActionListener true
                }
                task.content = txt
                GlobalScope.launch(Dispatchers.IO) {
                    taskDao.updateTask(task)
                }
                notifyItemChanged(position)
                onStatsChanged()
                b.taskEditText.isEnabled = false
                b.taskNumber.visibility = View.VISIBLE
                b.taskCheckBox.visibility = View.VISIBLE
                true
            } else false
        }

        // 9) Satır menüsü (Sil, Düzenle, Başa Sabitle)
        holder.itemView.setOnClickListener {
            val popup = PopupMenu(ctx, it)
            popup.menu.add("Düzenle")
            popup.menu.add("Sil")
            popup.menu.add("Başa Sabitle")
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Sil" -> {
                        AlertDialog.Builder(ctx)
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
                            b.taskEditText.error = "En fazla 5 görev sabitlenebilir!"
                            b.taskEditText.requestFocus()
                        } else {
                            task.isPinned = true
                            GlobalScope.launch(Dispatchers.IO) {
                                taskDao.updateTask(task)
                            }
                            tasks.removeAt(position)
                            val idx = tasks.indexOfLast { it.isPinned }
                            tasks.add(if (idx == -1) 0 else idx + 1, task)
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
    fun getTasks(): List<Task> = tasks
}