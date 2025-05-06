package com.example.todolist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.todolist.data.Todolist
import com.example.todolist.data.TodolistDao
import com.example.todolist.databinding.ActivityListelerimBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListelerimActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListelerimBinding
    private lateinit var listDao: TodolistDao

    override fun onCreate(savedInstanceState: Bundle?) {
        // Tema’yı uygula
        ThemeHelper.applyTheme(ThemeHelper.loadTheme(this))
        super.onCreate(savedInstanceState)
        binding = ActivityListelerimBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

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
    }

    private fun loadLists() {
        lifecycleScope.launch {
            val lists = withContext(Dispatchers.IO) {
                listDao.getAllLists()
            }
            // eskileri temizleyelim
            binding.buttonContainer.removeAllViews()
            // her bir listeye bir buton ekle
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
            // tıklayınca o listeyi aç (MainActivity)
            setOnClickListener {
                Intent(this@ListelerimActivity, MainActivity::class.java).also { intent ->
                    intent.putExtra("listId", todoList.id)
                    intent.putExtra("listName", todoList.name)
                    startActivity(intent)
                }
            }
        }
        binding.buttonContainer.addView(btn)
    }
}
