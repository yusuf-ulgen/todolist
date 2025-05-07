package com.example.todolist

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.data.Todolist
import com.example.todolist.data.TodolistDao
import com.example.todolist.databinding.ActivityListelerimBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListelerimActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListelerimBinding
    private lateinit var listDao: TodolistDao
    private lateinit var recyclerView: RecyclerView
    private var lists: List<Todolist> = emptyList()  // Listeleri tutacak bir değişken

    companion object {
        const val DEFAULT_LIST_ID   = 1L
        private const val DEFAULT_LIST_NAME = "GÜNLÜK/HAFTALIK"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Tema’yı uygula
        ThemeHelper.applyTheme(ThemeHelper.loadTheme(this))
        super.onCreate(savedInstanceState)
        binding = ActivityListelerimBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // DAO al
        listDao = AppDatabase.getDatabase(this).todolistDao()

        // Sabit "To-Do List" butonu: default liste
        binding.buttonTodolist.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // Veritabanından listeleri yükle ve butonlarını ekle
        loadLists()

        // FAB'a basınca NewListActivity (yeni liste oluşturma ekranı) açılsın
        binding.fab.setOnClickListener {
            startActivity(Intent(this, NewListActivity::class.java))
        }

        recyclerView.adapter = ListelerimAdapter(lists) { position ->
            if (lists[position].id == 1L) return@ListelerimAdapter
            showDeleteConfirmationDialog(position)
        }
        loadLists()
    }

    override fun onResume() {
        super.onResume()
        loadLists()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, Settings::class.java))
                true
            }
            R.id.action_feedback -> {
                showFeedbackDialog()   // Aynı metodu kullanabilirsiniz
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
    }

    private fun loadLists() {
        lifecycleScope.launch {
            // 1) IO thread’inde önce tabloyu kontrol edelim
            withContext(Dispatchers.IO) {
                val all = listDao.getAllLists()
                if (all.isEmpty()) {
                    // boşsa default listeyi ekle
                    listDao.insertList(Todolist(name = DEFAULT_LIST_NAME))
                }
            }

            // 2) sonra tekrar çek ve ekle
            lists = withContext(Dispatchers.IO) {
                listDao.getAllLists()
            }

            // 3) UI’ı güncelle
            binding.buttonContainer.removeAllViews()
            lists.forEach { todoList ->
                addListButton(todoList)
            }
        }
    }

    private fun onListItemClick(listId: Long) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("listId", listId) // Yeni listeyi MainActivity'ye gönder
        startActivity(intent)
    }

    private fun addListButton(todoList: Todolist) {
        val marginPx = (16 * resources.displayMetrics.density).toInt()
        val btn = Button(this).apply {
            text = todoList.name
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = marginPx }

            // Kısa tık: listeyi aç
            setOnClickListener {
                Intent(this@ListelerimActivity, MainActivity::class.java).also { intent ->
                    intent.putExtra("listId", todoList.id)
                    intent.putExtra("listName", todoList.name)
                    startActivity(intent)
                }
            }

            // Sadece DEFAULT_LIST_ID olmayanlara uzun basma ekle
            if (todoList.id != DEFAULT_LIST_ID) {
                setOnLongClickListener {
                    AlertDialog.Builder(this@ListelerimActivity)
                        .setTitle("Silme Onayı")
                        .setMessage("“${todoList.name}” listesini silmek istediğinize emin misiniz?")
                        .setPositiveButton("Evet") { _, _ ->
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    listDao.delete(todoList)
                                }
                                loadLists()
                            }
                        }
                        .setNegativeButton("Hayır", null)
                        .show()
                    true
                }
            }
        }
        binding.buttonContainer.addView(btn)
    }

    fun showDeleteConfirmationDialog(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Silme Onayı")
            .setMessage("Bu listeyi silmek istediğinizden emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                deleteList(position)  // Position parametresi ile listeyi sil
            }
            .setNegativeButton("Hayır", null)
            .show()
    }


    fun deleteList(position: Int) {
        val listToDelete = lists[position]  // Bu listedeki öğeyi alıyoruz
        if (listToDelete.id == 1L) {
            Toast.makeText(this, "Varsayılan liste silinemez", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                listDao.delete(listToDelete) // Veritabanından silme işlemi
            }
            // Listeden silinen öğeyi UI'dan kaldır
            loadLists()  // Listeyi tekrar yükleyelim
        }
    }

    private fun showFeedbackDialog() {
        val dialogView = layoutInflater.inflate(R.layout.feedback_dialog, null)
        val titleEditText = dialogView.findViewById<EditText>(R.id.feedbackTitleEditText)
        val messageEditText = dialogView.findViewById<EditText>(R.id.feedbackMessageEditText)

        android.app.AlertDialog.Builder(this)
            .setTitle("Problem Başlığı")
            .setView(dialogView)
            .setPositiveButton("İleri") { _, _ ->
                val title = titleEditText.text.toString().trim()
                val message = messageEditText.text.toString().trim()

                // Hataları EditText üzerinde göster
                var valid = true
                if (title.isEmpty()) {
                    titleEditText.error = "Lütfen bir başlık girin!"
                    titleEditText.requestFocus()
                    valid = false
                }
                if (message.isEmpty()) {
                    messageEditText.error = "Lütfen probleminizi girin!"
                    if (valid) messageEditText.requestFocus()
                    valid = false
                }
                if (!valid) return@setPositiveButton

                showSubmitFeedbackDialog(title, message)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showSubmitFeedbackDialog(title: String, message: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Geri Bildirim")
            .setMessage("Başlık: $title\nProblem: $message\n\nGöndermek istiyor musunuz?")
            .setPositiveButton("Gönder") { _, _ ->
                // Firestore referansı
                val db = Firebase.firestore
                // Belge verisi
                val data = mapOf(
                    "title"     to title,
                    "message"   to message,
                    "timestamp" to System.currentTimeMillis(),
                    "userId"    to FirebaseAuth.getInstance().currentUser?.uid,
                    "userEmail" to FirebaseAuth.getInstance().currentUser?.email
                )
                // yaz
                db.collection("feedbacks")
                    .add(data)
                    .addOnSuccessListener {
                        Snackbar.make(binding.root, "Geri bildiriminiz gönderildi!", Snackbar.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Snackbar.make(binding.root, "Gönderilemedi: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}