package com.example.todolist

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ActivityListelerimBinding
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

class ListelerimActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListelerimBinding
    private lateinit var adapter: ListelerimAdapter
    private lateinit var viewModel: TaskViewModel
    private val lists = mutableListOf<Todolist>()
    private var isMoving = false

    companion object {
        const val DEFAULT_LIST_ID = 1L
        private const val DEFAULT_LIST_NAME = "GÜNLÜK/HAFTALIK"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(ThemeHelper.loadTheme(this))
        super.onCreate(savedInstanceState)
        binding = ActivityListelerimBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = TaskRepository(
            db.taskDao(),
            db.todolistDao(),
            db.dailyStatDao(),
            db.taskHistoryDao(),
            db.notificationPrefDao(),
            db.resetTimeDao()
        )
        val factory = TaskViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TaskViewModel::class.java]

        // RecyclerView + Adapter
        adapter = ListelerimAdapter(
            lists,
            onClick = { todo ->
                Intent(this, MainActivity::class.java).apply {
                    putExtra("listId", todo.id)
                    putExtra("listName", todo.name)
                    startActivity(this)
                }
            },
            onLongClick = { todo ->
                if (todo.id != DEFAULT_LIST_ID) confirmAndDelete(todo)
            },
            onRenameRequest = { todo ->
                if (todo.id != DEFAULT_LIST_ID) showRenameDialog(todo)
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ListelerimActivity)
            adapter = this@ListelerimActivity.adapter
        }

        // Default liste butonu
        binding.buttonTodolist.setOnClickListener {
            Intent(this, MainActivity::class.java).also {
                it.putExtra("listId", DEFAULT_LIST_ID)
                it.putExtra("listName", DEFAULT_LIST_NAME)
                startActivity(it)
            }
        }

        // Drag & drop
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = vh.adapterPosition
                val to = target.adapterPosition
                // Default listeyi taşıma
                if (lists[from].id == DEFAULT_LIST_ID || lists[to].id == DEFAULT_LIST_ID) {
                    return false
                }
                
                isMoving = true
                Collections.swap(lists, from, to)
                adapter.notifyItemMoved(from, to)
                return true
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                
                // 1) Listeyi al ve kopyala (Deep Copy)
                val rawList = lists.toList()
                val updated = rawList.map { it.copy() }
                
                // 2) Animasyon kilidi
                val originalAnimator = recyclerView.itemAnimator
                recyclerView.itemAnimator = null
                
                updated.forEachIndexed { idx, todoList ->
                    todoList.sortOrder = idx
                }
                
                // 3) Senkron UI Güncelleme (Gecikmesiz)
                // ListelerimAdapter basit bir RecyclerView.Adapter, notifyDataSetChanged yeterli
                // Ancak elimizdeki 'lists' bir MutableList. Onu güncelleyip notify ediyoruz.
                lists.clear()
                lists.addAll(updated)
                adapter.notifyDataSetChanged()
                
                viewModel.updateLists(*updated.toTypedArray()) {
                    isMoving = false
                    viewModel.loadAllLists() 
                    recyclerView.post { recyclerView.itemAnimator = originalAnimator }
                }
            }
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) = Unit
        }).attachToRecyclerView(binding.recyclerView)

        // Yeni liste oluşturma
        binding.fab.setOnClickListener {
            startActivity(Intent(this, NewListActivity::class.java))
        }

        // Observe lists
        viewModel.lists.observe(this) { updatedLists ->
            if (isMoving) return@observe
            lists.clear()
            lists.addAll(updatedLists.filter { it.id != DEFAULT_LIST_ID })
            adapter.notifyDataSetChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        loadLists()
    }

    override fun onCreateOptionsMenu(menu: Menu) = menuInflater.inflate(R.menu.menu_main, menu).let { true }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> {
            startActivity(Intent(this, Settings::class.java))
            true
        }
        R.id.action_feedback -> {
            showFeedbackDialog()
            true
        }
        R.id.action_logout -> {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, Giris::class.java))
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadLists() {
        viewModel.loadAllLists()
    }

    private fun confirmAndDelete(todo: Todolist) {
        AlertDialog.Builder(this)
            .setTitle("Silme Onayı")
            .setMessage("“${todo.name}” listesini silmek istediğinize emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                viewModel.deleteList(todo)
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun showFeedbackDialog() {
        val dialogView = layoutInflater.inflate(R.layout.feedback_dialog, null)
        val titleEditText = dialogView.findViewById<EditText>(R.id.feedbackTitleEditText)
        val messageEditText = dialogView.findViewById<EditText>(R.id.feedbackMessageEditText)
        val nextButton = dialogView.findViewById<android.view.View>(R.id.feedbackNextButton)
        val cancelButton = dialogView.findViewById<android.view.View>(R.id.feedbackCancelButton)

        val dialog = android.app.Dialog(this)
        dialog.setContentView(dialogView)
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        nextButton.setOnClickListener {
            val title = titleEditText.text.toString().trim()
            val message = messageEditText.text.toString().trim()

            if (title.isEmpty()) {
                titleEditText.error = "Başlık boş olamaz"
                return@setOnClickListener
            }
            if (message.isEmpty()) {
                messageEditText.error = "Mesaj boş olamaz"
                return@setOnClickListener
            }
            dialog.dismiss()
            submitFeedback(title, message)
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun submitFeedback(title: String, message: String) {
        val db = Firebase.firestore
        val data = mapOf(
            "title" to title,
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "userId" to FirebaseAuth.getInstance().currentUser?.uid,
            "userEmail" to FirebaseAuth.getInstance().currentUser?.email
        )
        db.collection("feedbacks")
            .add(data)
            .addOnSuccessListener {
                Snackbar.make(binding.root, "Geri bildiriminiz gönderildi!", Snackbar.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Snackbar.make(binding.root, "Gönderilemedi: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showRenameDialog(todo: Todolist) {
        val input = EditText(this)
        input.setText(todo.name)

        AlertDialog.Builder(this)
            .setTitle("Listeyi Yeniden Adlandır")
            .setView(input)
            .setPositiveButton("Kaydet") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    todo.name = newName
                    viewModel.updateLists(todo)
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}