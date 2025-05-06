package com.example.todolist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
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

        // Mevcut listeleri yükle:
        loadLists()

        // “+” ikonuna basınca ad soran dialog
        binding.fab.setOnClickListener {
            showAddListDialog()
        }
    }

    private fun loadLists() {
        lifecycleScope.launch {
            val lists = withContext(Dispatchers.IO) { listDao.getAllLists() }
            binding.buttonContainer.removeAllViews()
            lists.forEach { todoList ->
                addListButton(todoList)
            }
        }
    }

    private fun showAddListDialog() {
        val input = EditText(this).apply {
            hint = "Liste adı girin"
        }
        AlertDialog.Builder(this)
            .setTitle("Yeni Liste Oluştur")
            .setView(input)
            .setPositiveButton("Oluştur") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    lifecycleScope.launch {
                        val newId = withContext(Dispatchers.IO) {
                            listDao.insertList(Todolist(name = name))
                        }
                        val newList = Todolist(id = newId, name = name)
                        addListButton(newList)
                    }
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun addListButton(todoList: Todolist) {
        val marginPx = (16 * resources.displayMetrics.density).toInt()
        val btn = Button(this).apply {
            text = todoList.name
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = marginPx }
            // Tıklandığında MainActivity’ye listId ile geç
            setOnClickListener {
                val intent = Intent(this@ListelerimActivity, MainActivity::class.java).apply {
                    putExtra("listId", todoList.id)
                    putExtra("listName", todoList.name)
                    }
                startActivity(intent)
            }
        }
        binding.buttonContainer.addView(btn)
    }
}
