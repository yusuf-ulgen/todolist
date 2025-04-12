import android.app.AlertDialog
import android.app.TimePickerDialog
import android.graphics.Paint
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import android.widget.TimePicker
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ItemTaskBinding
import java.util.*

class TaskAdapter(
    private val tasks: MutableList<String>
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root)

    var onItemMove: ((from: Int, to: Int) -> Unit)? = null
    var onItemDelete: ((position: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.binding.taskEditText.setText(task)
        holder.binding.taskCheckBox.visibility = View.GONE

        // Saat seçimi
        holder.binding.timeTextView.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(
                holder.itemView.context,
                { _: TimePicker, selectedHour: Int, selectedMinute: Int ->
                    val timeText = String.format("%02d:%02d", selectedHour, selectedMinute)
                    holder.binding.timeTextView.text = timeText
                },
                hour, minute, true
            ).show()
        }

        // Enter ile görev tamamla
        holder.binding.taskEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val context = holder.itemView.context
                val taskText = holder.binding.taskEditText.text.toString().trim()
                val timeText = holder.binding.timeTextView.text.toString()

                if (taskText.isEmpty()) {
                    Toast.makeText(context, "Görev adı boş olamaz", Toast.LENGTH_SHORT).show()
                    return@setOnEditorActionListener true
                }

                if (timeText == "Saat") {
                    Toast.makeText(context, "Lütfen bir saat seçin", Toast.LENGTH_SHORT).show()
                    return@setOnEditorActionListener true
                }

                tasks[position] = taskText
                holder.binding.taskCheckBox.visibility = View.VISIBLE
                holder.binding.taskEditText.clearFocus()
                true
            } else false
        }

        holder.binding.taskEditText.isEnabled = true // Her açıldığında varsayılan olarak yazılabilir

        holder.binding.taskEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val context = holder.itemView.context
                val taskText = holder.binding.taskEditText.text.toString().trim()
                val timeText = holder.binding.timeTextView.text.toString()

                if (taskText.isEmpty()) {
                    Toast.makeText(context, "Görev adı boş olamaz", Toast.LENGTH_SHORT).show()
                    return@setOnEditorActionListener true
                }

                if (timeText == "Saat") {
                    Toast.makeText(context, "Lütfen bir saat seçin", Toast.LENGTH_SHORT).show()
                    return@setOnEditorActionListener true
                }

                tasks[position] = taskText
                holder.binding.taskCheckBox.visibility = View.VISIBLE
                holder.binding.taskEditText.clearFocus()

                // DÜZENLEMEYİ KAPAT
                holder.binding.taskEditText.isEnabled = false
                true
            } else false
        }


        // Çizgi efekti
        holder.binding.taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
            holder.binding.taskEditText.paintFlags = if (isChecked) {
                holder.binding.taskEditText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                holder.binding.taskEditText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }

        // Uzun basınca silme ve düzenleme menüsü
        holder.itemView.setOnLongClickListener {
            val popup = PopupMenu(holder.itemView.context, it)
            popup.menu.add("Düzenle")
            popup.menu.add("Sil")

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Sil" -> {
                        val context = holder.itemView.context
                        val dialog = AlertDialog.Builder(context)
                            .setTitle("Görevi sil")
                            .setMessage("Bu görevi silmek istediğinize emin misiniz?")
                            .setPositiveButton("Evet") { _, _ ->
                                onItemDelete?.invoke(position)
                            }
                            .setNegativeButton("Hayır", null)
                            .create()
                        dialog.show()

                    }
                    "Düzenle" -> {
                        holder.binding.taskEditText.isEnabled = true
                        holder.binding.taskEditText.requestFocus()
                        holder.binding.taskEditText.setSelection(holder.binding.taskEditText.text.length)
                    }
                }
                true
            }

            popup.show()
            true
        }
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
