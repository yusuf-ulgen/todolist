package com.example.todolist

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
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
    private var userDefaultList: Todolist? = null
    private var isMoving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(ThemeHelper.loadTheme(this))
        super.onCreate(savedInstanceState)
        binding = ActivityListelerimBinding.inflate(layoutInflater)
        setContentView(binding.root)
        com.example.todolist.WindowInsetsHelper.applyTopBottomInsets(binding.root)
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

        // Firebase Auth State Listener
        val mAuth = FirebaseAuth.getInstance()
        if (mAuth.currentUser == null) {
            startActivity(Intent(this, Giris::class.java))
            finish()
            return
        }

        mAuth.addAuthStateListener { auth ->
            if (auth.currentUser == null && !isFinishing) {
                // Anlık kopmaları önlemek için 2 saniye bekleyip hala null ise çıkış yap
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (FirebaseAuth.getInstance().currentUser == null && !isFinishing) {
                        android.util.Log.d("ListelerimAuth", "User is still null, redirecting to Giris...")
                        val intent = Intent(this, Giris::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }, 2000)
            }
        }

        // RecyclerView + Adapter
        adapter = ListelerimAdapter(
            lists,
            onClick = { todo ->
                Intent(this, MainActivity::class.java).apply {
                    putExtra("LIST_ID", todo.id)
                    putExtra("LIST_NAME", todo.name)
                    startActivity(this)
                }
            },
            onLongClick = { todo ->
                if (todo.name != "GÜNLÜK/HAFTALIK") confirmAndDelete(todo)
            },
            onRenameRequest = { todo ->
                if (todo.name != "GÜNLÜK/HAFTALIK") showRenameDialog(todo)
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ListelerimActivity)
            adapter = this@ListelerimActivity.adapter
        }

        // Default liste butonu
        binding.buttonTodolist.setOnClickListener {
            userDefaultList?.let { defaultList ->
                Intent(this, MainActivity::class.java).also {
                    it.putExtra("LIST_ID", defaultList.id)
                    it.putExtra("LIST_NAME", defaultList.name)
                    startActivity(it)
                }
            } ?: run {
                // Eğer liste henüz yüklenmemişse tekrar yükle
                viewModel.loadAllLists()
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
                // Default listeyi taşıma (İsim kontrolü ile)
                if (lists[from].name == "GÜNLÜK/HAFTALIK" || lists[to].name == "GÜNLÜK/HAFTALIK") {
                    return false
                }
                
                isMoving = true
                Collections.swap(lists, from, to)
                adapter.notifyItemMoved(from, to)
                return true
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                
                val rawList = lists.toList()
                val updated = rawList.map { it.copy() }
                
                val originalAnimator = recyclerView.itemAnimator
                recyclerView.itemAnimator = null
                
                updated.forEachIndexed { idx, todoList ->
                    todoList.sortOrder = idx
                }
                
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

        // Real-time sync başlat
        viewModel.startSync()

        // Yeni liste oluşturma
        binding.fab.setOnClickListener {
            startActivity(Intent(this, NewListActivity::class.java))
        }

        // Observe lists
        viewModel.lists.observe(this) { updatedLists ->
            if (isMoving) return@observe
            
            // Varsayılan listeyi ayır
            userDefaultList = updatedLists.find { it.name == "GÜNLÜK/HAFTALIK" }
            
            lists.clear()
            // Varsayılan liste hariç diğerlerini ekle
            lists.addAll(updatedLists.filter { it.name != "GÜNLÜK/HAFTALIK" })
            adapter.notifyDataSetChanged()

            // Liste yüklendikten sonra tutorial'ı göster
            showWelcomeTutorial()
        }

        // Sürüm notlarını ve Güncellemeyi kontrol et
        com.example.todolist.utils.UpdateManager.checkUpdates(this)
        com.example.todolist.utils.ReleaseNotes.checkAndShow(this)
    }

    private fun showWelcomeTutorial() {
        val manager = com.example.todolist.util.TutorialManager(this)
        val steps = listOf(
            com.example.todolist.util.TutorialStep(
                null,
                "Hoş Geldiniz!",
                "ToDoList ile görevlerinizi organize etmeye hazırsınız. Hadi kısa bir tur atalım."
            ),
            com.example.todolist.util.TutorialStep(
                null,
                "Web Senkronizasyonu",
                "Görevlerinize artık her yerden ulaşabilirsiniz! Web sitemiz üzerinden mobil uygulamanızla tam senkronize çalışabilirsiniz.",
                "Web'e Git",
                "https://todolist.yusufulgen.com"
            ),
            com.example.todolist.util.TutorialStep(
                R.id.buttonTodolist,
                "Günlük & Haftalık",
                "Ana görev listenize buradan ulaşabilirsiniz. Günlük ve haftalık tekrarlayan işleriniz burada yer alır."
            ),
            com.example.todolist.util.TutorialStep(
                R.id.fab,
                "Yeni Liste",
                "Özel projeleriniz veya alışveriş listeleriniz için yeni listeler oluşturabilirsiniz."
            ),
            com.example.todolist.util.TutorialStep(
                null,
                "Menü",
                "Sağ üstteki menüden ayarlara gidebilir, geri bildirim gönderebilir veya çıkış yapabilirsiniz."
            )
        )
        manager.showTutorial("listelerim_page", steps)
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
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_delete_list, null)
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setView(view)
            .setCancelable(true)
            .create()
        
        dialog.show()
        
        // Diyalog gösterildikten sonra arka plan ve boyut ayarlarını yapıyoruz
        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        view.findViewById<TextView>(R.id.dialogMessage).text = "“${todo.name}” listesini silmek istediğinize emin misiniz? Bu işlem geri alınamaz."
        
        view.findViewById<View>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        view.findViewById<View>(R.id.btnDelete).setOnClickListener {
            viewModel.deleteList(todo)
            dialog.dismiss()
        }
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
                com.example.todolist.util.StylishAlert.show(this, "Geri bildiriminiz gönderildi!", false)
            }
            .addOnFailureListener { e ->
                com.example.todolist.util.StylishAlert.show(this, "Gönderilemedi: ${e.message}")
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showRenameDialog(todo: Todolist) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_rename_list, null)
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setView(view)
            .create()
        
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val editText = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.renameEditText)
        editText.setText(todo.name)
        editText.setSelection(todo.name.length)

        view.findViewById<View>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        view.findViewById<View>(R.id.btnSave).setOnClickListener {
            val newName = editText.text.toString().trim()
            if (newName.isNotEmpty()) {
                todo.name = newName
                viewModel.updateLists(todo)
                dialog.dismiss()
            } else {
                editText.error = "İsim boş olamaz"
            }
        }

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        editText.requestFocus()
    }
}