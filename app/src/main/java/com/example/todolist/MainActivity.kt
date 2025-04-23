package com.example.todolist

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
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

        val touchCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to   = target.adapterPosition

                // Adapter’dan güncel listeyi al
                val list = adapter.getTasks()
                // “Pinli” görev sayısı
                val pinnedCount = list.count { it.isPinned }

                // Kaydırdığımız satır pinli mi?
                val fromPinned = list[from].isPinned
                // Taşımak istediğimiz hedef pozisyon pinli mi?
                val toPinned   = list[to].isPinned

                // Eğer her ikisi de pinliler bölgesindeyse veya her ikisi de pinsiz bölgedeyse, taşı
                if ((from < pinnedCount && to < pinnedCount) ||
                    (from >= pinnedCount && to >= pinnedCount)) {
                    adapter.moveItem(from, to)
                    return true
                }

                // Bölge atlamaya izin verme
                return false
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                // Drag&Drop bittiğinde tüm numaraları yenile
                adapter.notifyItemRangeChanged(0, adapter.itemCount)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                adapter.deleteItem(pos)   // sadece sil
            }

            override fun isLongPressDragEnabled() = true
        }
        ItemTouchHelper(touchCallback).attachToRecyclerView(recyclerView)

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

    override fun onResume() {
        super.onResume()
        loadTasks()
        checkResetTime()
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
                        Snackbar.make(
                            binding.root,
                            "Bu saatte zaten bir görev var!",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    taskDao.insertTask(task)
                    loadTasks()
                }
            } else {
                taskDao.insertTask(task)
                loadTasks()
            }
        }
    }

    private fun updateTaskStats() {
        // MainActivity.tasks yerine adapter.getTasks() kullanıyoruz
        val current = adapter.getTasks()
        val total = current.size
        val done  = current.count { it.isChecked }
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

        AlertDialog.Builder(this)
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
        AlertDialog.Builder(this)
            .setTitle("Geri Bildirim")
            .setMessage("Başlık: $title\nProblem: $message\n\nGöndermek istiyor musunuz?")
            .setPositiveButton("Gönder") { _, _ ->
                Snackbar.make(
                    binding.root,
                    "Geri bildiriminiz gönderildi!",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("İptal", null)
            .show()
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