package com.example.todolist

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TaskAdapter
    private lateinit var db: AppDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var resetTimeDao: ResetTimeDao
    private var tasks: List<Task> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)  // Önce binding ile setContentView'i yapın

        // Hide action bar
        supportActionBar?.hide()

        var mAuth = FirebaseAuth.getInstance()

        // Firebase kullanıcı oturumu kontrolü
        val user = mAuth.currentUser
        if (user == null) {
            val intent = Intent(this, Giris::class.java)
            startActivity(intent)
            finish()
        }

        // ThemeHelper ve diğer işlemler
        ThemeHelper.applyTheme(ThemeHelper.loadTheme(this))

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        db = AppDatabase.getDatabase(applicationContext)
        taskDao = db.taskDao()
        resetTimeDao = db.resetTimeDao()
        adapter = TaskAdapter(mutableListOf(), ::addTask, taskDao) { updateTaskStats() }

        loadTasks()
        checkResetTime()

        val recyclerView = binding.contentMain.todoRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                adapter.moveItem(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                adapter.deleteItem(position) // Öğeyi sil
            }

            override fun isLongPressDragEnabled(): Boolean = true
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // Diğer kodlar burada
        adapter.onItemDelete = { position ->
            adapter.deleteItem(position) // Görevi sil
            updateTaskStats() // Günlük görev sayısını güncelle
        }

        adapter.onStatsChanged = {
            updateTaskStats()
        }

        binding.fab.setOnClickListener {
            val newTask = Task(content = "", time = "")  // Zorunlu saat girmeyi kaldırdık
            addTask(newTask)
        }
    }

    private fun loadTasks() {
        GlobalScope.launch(Dispatchers.IO) {
            val taskList = taskDao.getAllTasks() // Veritabanından görevleri al
            withContext(Dispatchers.Main) {
                tasks = taskList // Burada tasks'ı güncelliyoruz
                adapter.setTasks(tasks) // Veriyi adaptöre aktar
                updateTaskStats() // Görevlerin güncel sayısını göster
            }
        }
    }

    private fun addTask(task: Task) {
        GlobalScope.launch(Dispatchers.IO) {
            if (task.time.isNotBlank() && task.time != "Saat") {
                val existingTask = taskDao.getTaskByTime(task.time)
                if (existingTask != null) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Bu saatte zaten bir görev var!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    taskDao.insertTask(task)
                    loadTasks() // Veritabanını yeniden yükle
                }
            } else {
                taskDao.insertTask(task)
                loadTasks() // Veritabanını yeniden yükle
            }
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
            R.id.action_logout -> {
                logOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logOut() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, Giris::class.java)
        startActivity(intent)
        finish()
    }

    private fun showFeedbackDialog() {
        val dialogView = layoutInflater.inflate(R.layout.feedback_dialog, null)
        val titleEditText = dialogView.findViewById<EditText>(R.id.feedbackTitleEditText)
        val messageEditText = dialogView.findViewById<EditText>(R.id.feedbackMessageEditText)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Problem Başlığı")
            .setView(dialogView)
            .setPositiveButton("İleri") { _, _ ->
                val title = titleEditText.text.toString().trim()
                val message = messageEditText.text.toString().trim()

                if (title.isEmpty()) {
                    Toast.makeText(this, "Lütfen bir başlık girin!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (message.isEmpty()) {
                    Toast.makeText(this, "Lütfen probleminizi girin!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                showSubmitFeedbackDialog(title, message)
            }
            .setNegativeButton("İptal") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun showSubmitFeedbackDialog(title: String, message: String) {
        val submitDialog = AlertDialog.Builder(this)
            .setTitle("Geri Bildirim")
            .setMessage("Başlık: $title\nProblem: $message\n\nGöndermek istiyor musunuz?")
            .setPositiveButton("Gönder") { _, _ ->
                Toast.makeText(this, "Geri bildiriminiz gönderildi!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("İptal") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        submitDialog.show()
    }

    private fun resetTasksAtSpecificTime() {
        GlobalScope.launch(Dispatchers.IO) {
            val resetTime = resetTimeDao.getResetTime() // Reset saati veritabanından al
            val currentTime = Calendar.getInstance()
            val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
            val currentMinute = currentTime.get(Calendar.MINUTE)

            if (currentHour == resetTime.resetHour && currentMinute == resetTime.resetMinute) {
                val tasks = taskDao.getAllTasks()
                tasks.forEach { task ->
                    task.isChecked = false
                    taskDao.updateTask(task) // Veritabanını güncelle
                }
            }
        }
    }

    private fun resetTasks() {
        GlobalScope.launch(Dispatchers.IO) {
            val tasks = taskDao.getAllTasks()
            tasks.forEach { task ->
                task.isChecked = false
                taskDao.updateTask(task) // Veritabanında güncelleniyor
            }
        }
    }

    private fun checkResetTime() {
        GlobalScope.launch(Dispatchers.IO) {
            val resetTime = resetTimeDao.getResetTime() // Reset saati veritabanından al

            if (resetTime != null) {
                val currentTime = Calendar.getInstance()
                val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
                val currentMinute = currentTime.get(Calendar.MINUTE)

                if (currentHour == resetTime.resetHour && currentMinute == resetTime.resetMinute) {
                    resetTasks() // Eğer saat geldiyse görevleri sıfırla
                }
            } else {
                Log.w("MainActivity", "Reset time not found in the database.")
            }
        }
    }
}