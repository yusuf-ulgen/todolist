package com.example.todolist

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ItemTaskBinding

class TaskAdapter(
        private val addTaskCallback: (Task) -> Unit,
        private val taskDao: TaskDao,
        var onStatsChanged: () -> Unit,
        private val onTimeClick: (task: Task, binding: ItemTaskBinding) -> Unit,
        private val onTaskUpdate: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    inner class TaskViewHolder(val binding: ItemTaskBinding) :
            RecyclerView.ViewHolder(binding.root)
    var onItemDelete: ((position: Int) -> Unit)? = null
    
    // Sürükleme (drag) sırasında görsel sıralamayı takip etmek için gölge liste
    private var shadowList: MutableList<Task> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
            holder: TaskViewHolder,
            position: Int,
            payloads: MutableList<Any>
    ) {
        if (payloads.contains("UPDATE_RANK")) {
            holder.binding.taskNumber.text = (position + 1).toString()
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
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
        val initColorRes =
                if (task.isChecked) R.color.task_text_checked else R.color.task_text_default

        b.taskEditText.setTextColor(ContextCompat.getColor(ctx, initColorRes))
        b.taskEditText.paintFlags =
                if (task.isChecked) b.taskEditText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                else b.taskEditText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

        b.taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
            task.isChecked = isChecked
            onTaskUpdate(task)
            onStatsChanged()

            // Metin rengini güncelle
            val colorRes = if (isChecked) R.color.task_text_checked else R.color.task_text_default

            b.taskEditText.setTextColor(ContextCompat.getColor(ctx, colorRes))

            // Üstünü çiz / çizgiyi kaldır
            if (isChecked) {
                b.taskEditText.paintFlags = b.taskEditText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                b.taskEditText.paintFlags =
                        b.taskEditText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
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

        // 5) Öncelik Göstergesi
        val priorityColor =
                when (task.priority) {
                    1 -> R.color.priority_low
                    2 -> R.color.priority_medium
                    3 -> R.color.priority_high
                    else -> android.R.color.transparent
                }
        b.priorityIndicator.setBackgroundColor(ContextCompat.getColor(ctx, priorityColor))
        b.priorityIndicator.visibility = if (task.priority > 0) View.VISIBLE else View.GONE

        // 6) Görev numarası ve checkbox görünürlüğü
        b.taskNumber.text = (position + 1).toString()
        val hasContent = task.content.isNotBlank()
        b.taskNumber.visibility = if (hasContent) View.VISIBLE else View.GONE
        b.taskCheckBox.visibility = if (hasContent) View.VISIBLE else View.GONE

        // 6) Düzenleme modu
        b.taskEditText.isEnabled = task.content.isBlank()

        // 7) Saat seçici
        b.timeTextView.setOnClickListener { onTimeClick(task, b) }

        // 8) Görevi kaydet (IME action) + klavyeyi gizle + focus temizleme
        b.taskEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val txt = b.taskEditText.text.toString().trim()
                if (txt.isEmpty()) {
                    b.taskEditText.error = "Görev adı boş olamaz."
                    b.taskEditText.requestFocus()
                } else {
                    task.content = txt
                    onTaskUpdate(task)
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
            val priorityMenu = popup.menu.addSubMenu("Öncelik")
            priorityMenu.add(0, 0, 0, "Yok")
            priorityMenu.add(0, 1, 1, "Düşük")
            priorityMenu.add(0, 2, 2, "Orta")
            priorityMenu.add(0, 3, 3, "Yüksek")

            val pinTitle = if (task.isPinned) "Sabitlemeyi Kaldır" else "Başa Sabitle"
            popup.menu.add(pinTitle)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Sil" ->
                            AlertDialog.Builder(ctx)
                                    .setTitle("Görevi Sil")
                                    .setMessage("Bu görevi silmek istediğinize emin misiniz?")
                                    .setPositiveButton("Evet") { _, _ ->
                                        onItemDelete?.invoke(position)
                                        onStatsChanged()
                                    }
                                    .setNegativeButton("Hayır", null)
                                    .show()
                    "Düzenle" -> {
                        b.taskEditText.visibility = View.VISIBLE
                        b.taskEditText.isEnabled = true
                        b.taskEditText.requestFocus()
                        b.taskEditText.setSelection(b.taskEditText.text.length)
                        b.taskMarqueeText.visibility = View.GONE
                    }
                    "Yok" -> {
                        task.priority = 0
                        onTaskUpdate(task)
                        notifyItemChanged(position)
                    }
                    "Düşük" -> {
                        task.priority = 1
                        onTaskUpdate(task)
                        notifyItemChanged(position)
                    }
                    "Orta" -> {
                        task.priority = 2
                        onTaskUpdate(task)
                        notifyItemChanged(position)
                    }
                    "Yüksek" -> {
                        task.priority = 3
                        onTaskUpdate(task)
                        notifyItemChanged(position)
                    }
                    pinTitle -> {
                        if (task.isPinned) {
                            task.isPinned = false
                            onTaskUpdate(task)
                            notifyItemChanged(position)
                        } else {
                            val pinnedCount = currentList.count { it.isPinned }
                            if (pinnedCount >= 10) {
                                b.taskEditText.error = "En fazla 10 görev sabitlenebilir!"
                                b.taskEditText.requestFocus()
                            } else {
                                task.isPinned = true
                                onTaskUpdate(task)
                                notifyItemChanged(position)
                            }
                        }
                    }
                }
                true
            }
            popup.show()
        }
    }

    override fun getItemCount() = currentList.size

    fun moveItem(from: Int, to: Int) {
        if (from == to) return
        if (from in shadowList.indices && to in shadowList.indices) {
            java.util.Collections.swap(shadowList, from, to)
            notifyItemMoved(from, to)
            
            // Sıra numaralarını anlık güncelle (flicker yapmadan)
            // Sadece yer değiştiren iki öğeyi değil, aradaki her şeyi güncellemek en sağlamı
            val start = Math.min(from, to)
            val count = Math.abs(from - to) + 1
            notifyItemRangeChanged(start, count, "UPDATE_RANK")
        }
    }

    fun deleteItem(position: Int, onDeleted: (() -> Unit)? = null) {
        val newList = currentList.toMutableList()
        if (position in newList.indices) {
            newList.removeAt(position)
            // notifyItemRemoved(position) // ItemTouchHelper already does this visually
            submitList(newList) {
                onDeleted?.invoke()
            }
        }
    }

    fun restoreItem(task: Task, position: Int, onRestored: (() -> Unit)? = null) {
        val newList = currentList.toMutableList()
        if (position <= newList.size) {
            newList.add(position, task)
            submitList(newList) {
                onRestored?.invoke()
            }
        }
    }

    fun setTasks(newTasks: List<Task>, commitCallback: (() -> Unit)? = null) {
        shadowList = newTasks.toMutableList()
        submitList(newTasks, commitCallback)
    }

    fun getTasks(): List<Task> = shadowList
}

class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
    override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem == newItem
    }
}
