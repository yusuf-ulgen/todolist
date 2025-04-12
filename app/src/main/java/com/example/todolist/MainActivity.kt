package com.example.todolist

import TaskAdapter
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val taskList = mutableListOf<String>()
    private lateinit var adapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        adapter = TaskAdapter(taskList)
        val recyclerView = binding.contentMain.todoRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Sürükleyerek taşıma
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                adapter.moveItem(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // swipe özelliği devre dışı
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

// Silme fonksiyonunu adapter'a bağla
        adapter.onItemDelete = { position ->
            adapter.deleteItem(position)
        }



        binding.fab.setOnClickListener {
            taskList.add("") // Alta ekle
            adapter.notifyItemInserted(taskList.lastIndex)
            binding.contentMain.todoRecyclerView.scrollToPosition(taskList.lastIndex)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, Settings::class.java)
                startActivity(intent)
                true
            }
            R.id.action_feedback -> {
                showFeedbackDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showFeedbackDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Feedback")
            .setMessage("Buraya geri bildirim formu gelecek.")
            .setPositiveButton("Kapat") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()
        dialog.show()
    }
}
