package com.example.todolist

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.data.Task
import com.example.todolist.data.TaskDao
import com.example.todolist.databinding.ItemTaskBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TaskAdapter(
    private var tasks: MutableList<Task>,
    private val addTaskCallback: (Task) -> Unit,
    private val taskDao: TaskDao,
    var onStatsChanged: () -> Unit,
    private val onTimeClick: (task: Task, binding: ItemTaskBinding) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)
    var onItemDelete: ((position: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        val b = holder.binding
        val ctx = holder.itemView.context

        val isEditing = task.content.isBlank()
        b.taskEditText.visibility = if (isEditing) View.VISIBLE else View.GONE
        b.taskMarqueeText.visibility = if (isEditing) View.GONE else View.VISIBLE
        b.taskMarqueeText.text = task.content
        b.taskMarqueeText.isSelected = true


        // 1) İçerik ve Saat
        b.taskEditText.setText(task.content)
        b.timeTextView.text = task.time.takeIf { it.isNotBlank() && it != "Saat" } ?: "Saat"

        // 2) isChecked durumu, renk ve üstünü çizme
        b.taskCheckBox.setOnCheckedChangeListener(null)
        b.taskCheckBox.isChecked = task.isChecked

        // Hem metin rengini hem de strike‐through’u başlangıçta ayarla
        val initColorRes = if (task.isChecked)
            R.color.task_text_checked
        else
            R.color.task_text_default

        b.taskEditText.setTextColor(ContextCompat.getColor(ctx, initColorRes))
        b.taskEditText.paintFlags = if (task.isChecked)
            b.taskEditText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else
            b.taskEditText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

        b.taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
            task.isChecked = isChecked
            GlobalScope.launch(Dispatchers.IO) {
                taskDao.updateTask(task)
            }
            onStatsChanged()

            // Metin rengini güncelle
            val colorRes = if (isChecked)
                R.color.task_text_checked
            else
                R.color.task_text_default

            b.taskEditText.setTextColor(ContextCompat.getColor(ctx, colorRes))

            // Üstünü çiz / çizgiyi kaldır
            if (isChecked) {
                b.taskEditText.paintFlags = b.taskEditText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                b.taskEditText.paintFlags = b.taskEditText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }


        // 3) isPinned durumu
        b.pinIcon.visibility = if (task.isPinned) View.VISIBLE else View.GONE

        // 4) Dinamik padding
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
        val hasContent = task.content.isNotBlank()
        b.taskNumber.visibility = if (hasContent) View.VISIBLE else View.GONE
        b.taskCheckBox.visibility = if (hasContent) View.VISIBLE else View.GONE

        // 6) Düzenleme modu
        b.taskEditText.isEnabled = task.content.isBlank()

        // 7) Saat seçici
        b.timeTextView.setOnClickListener {
            onTimeClick(task, b)
        }

        // 8) Görevi kaydet (IME action) + klavyeyi gizle + focus temizleme
        b.taskEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val txt = b.taskEditText.text.toString().trim()
                if (txt.isEmpty()) {
                    b.taskEditText.error = "Görev adı boş olamaz."
                    b.taskEditText.requestFocus()
                } else {
                    task.content = txt
                    GlobalScope.launch(Dispatchers.IO) { taskDao.updateTask(task) }
                    b.taskEditText.isEnabled = false
                    b.taskEditText.clearFocus()
                    b.taskEditText.visibility = View.GONE
                    b.taskMarqueeText.text = task.content
                    b.taskMarqueeText.visibility = View.VISIBLE
                    b.taskMarqueeText.isSelected = true
                    b.taskNumber.visibility = View.VISIBLE
                    b.taskCheckBox.visibility = View.VISIBLE
                    (ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(v.windowToken, 0)
                    notifyItemChanged(position)
                    onStatsChanged()
                }
                true
            } else false
        }

        // 9) Odaklanınca scroll ve marquee ayarı
        b.taskEditText.isSelected = true
        b.taskEditText.setOnFocusChangeListener { _, hasFocus ->
            b.taskEditText.isCursorVisible = hasFocus
            b.taskEditText.isSelected = hasFocus
            if (hasFocus) {
                (holder.itemView.parent as? RecyclerView)?.post {
                    (holder.itemView.parent as RecyclerView).smoothScrollToPosition(position)
                }
            }
        }

        // 10) Satır menüsü (Sil, Düzenle, Başa Sabitle)
        holder.itemView.setOnClickListener { it ->
            val popup = PopupMenu(ctx, it)
            popup.menu.add("Düzenle")
            popup.menu.add("Sil")
            val pinTitle = if (task.isPinned) "Sabitlemeyi Kaldır" else "Başa Sabitle"
            popup.menu.add(pinTitle)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Sil" -> AlertDialog.Builder(ctx)
                        .setTitle("Görevi Sil")
                        .setMessage("Bu görevi silmek istediğinize emin misiniz?")
                        .setPositiveButton("Evet") { _, _ -> onItemDelete?.invoke(position); onStatsChanged() }
                        .setNegativeButton("Hayır", null).show()
                    "Düzenle" -> {
                        b.taskEditText.visibility = View.VISIBLE
                        b.taskEditText.isEnabled = true
                        b.taskEditText.requestFocus()
                        b.taskEditText.setSelection(b.taskEditText.text.length)
                        b.taskMarqueeText.visibility = View.GONE
                    }
                    pinTitle -> {
                        if (task.isPinned) {
                            task.isPinned = false
                            GlobalScope.launch(Dispatchers.IO) { taskDao.updateTask(task) }
                            tasks.removeAt(position)
                            val insertPos = tasks.indexOfLast { it.isPinned } + 1
                            tasks.add(insertPos, task)
                        } else {
                            val pinnedCount = tasks.count { it.isPinned }
                            if (pinnedCount >= 10) {
                                b.taskEditText.error = "En fazla 10 görev sabitlenebilir!"
                                b.taskEditText.requestFocus()
                            } else {
                                task.isPinned = true
                                GlobalScope.launch(Dispatchers.IO) { taskDao.updateTask(task) }
                                tasks.removeAt(position)
                                val insertPos = tasks.indexOfLast { it.isPinned }
                                tasks.add(if (insertPos == -1) 0 else insertPos + 1, task)
                            }
                        }
                        notifyDataSetChanged()
                    }
                }
                true
            }
            popup.show()
        }
    }

    override fun getItemCount() = tasks.size

    fun moveItem(from: Int, to: Int) {
        // 1) Öğeyi listeden al
        val item = tasks.removeAt(from)
        // 2) Yeni pozisyona koy
        tasks.add(to, item)
        // 3) RecyclerView’a bildir
        notifyItemMoved(from, to)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun deleteItem(position: Int) {
        // 1) Hemen UI’dan çıkar
        val removed = tasks.removeAt(position)
        notifyItemRemoved(position)
        // İndeksler değiştiği için kalanları da güncelle
        notifyItemRangeChanged(position, tasks.size - position)
        // İstatistikleri yenile
        onStatsChanged()

        // 2) Arka planda DB’den sil
        GlobalScope.launch(Dispatchers.IO) {
            taskDao.deleteTask(removed)
        }
    }

    // Pozisyona göre silinen görevi geri eklemek için
    fun restoreItem(task: Task, position: Int) {
        tasks.add(position, task)
        notifyItemInserted(position)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setTasks(newTasks: List<Task>) {
        tasks = newTasks.toMutableList()
        notifyDataSetChanged()
    }

    fun getTasks() = tasks
}