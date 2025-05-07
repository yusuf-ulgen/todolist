package com.example.todolist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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
}
