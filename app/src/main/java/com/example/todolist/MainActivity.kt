package com.example.todolist

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ActivityMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TaskAdapter
    private lateinit var db: AppDatabase
    private lateinit var taskDao: TaskDao
    private var tasks: List<Task> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeHelper.applyTheme(ThemeHelper.loadTheme(this)) // ⬅ önce bu
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // Room database setup
        db = AppDatabase.getDatabase(applicationContext)
        taskDao = db.taskDao()
        adapter = TaskAdapter(mutableListOf(), ::addTask, taskDao) { updateTaskStats() }

        val recyclerView = binding.contentMain.todoRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Drag & drop
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

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled(): Boolean = true
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)

        adapter.onItemDelete = { position ->
            adapter.deleteItem(position) // Görevi sil
            updateTaskStats() // Günlük görev sayısını güncelle
        }

        adapter.onStatsChanged = {
            updateTaskStats()
        }

        binding.fab.setOnClickListener {
            Log.d("MainActivity", "FAB clicked")
            val newTask = Task(content = "", time = "Saat")
            addTask(newTask)
        }

        // Veritabanındaki görevleri yükle
        loadTasks()
    }

    private fun loadTasks() {
        GlobalScope.launch(Dispatchers.IO) {
            val taskList = taskDao.getAllTasks() // Veritabanından görevleri al
            withContext(Dispatchers.Main) {
                tasks = taskList // Burada tasks'ı güncelliyoruz
                adapter.setTasks(tasks) // Veriyi adaptöre aktar
                updateTaskStats() // Günlük görev sayısını güncelle
            }
        }
    }

    private fun addTask(task: Task) {
        GlobalScope.launch(Dispatchers.IO) {
            taskDao.insertTask(task) // Veritabanına yeni görev ekle
            loadTasks() // Veritabanını yeniden yükle
        }
    }

    private fun updateTaskStats() {
        val total = tasks.size // Toplam görev sayısı
        val done = tasks.count { it.isChecked } // Tamamlanan görev sayısı
        binding.taskStatsTextView.text = "Bugünün görevleri $done/$total"
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
