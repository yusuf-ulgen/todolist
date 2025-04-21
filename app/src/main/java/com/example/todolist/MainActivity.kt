package com.example.todolist

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
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
        supportActionBar?.hide()
        var mAuth = FirebaseAuth.getInstance()

        // Firebase kullanıcı oturumu kontrolü
        val user = mAuth.currentUser
        if (user == null) {
            // Eğer kullanıcı giriş yapmamışsa Giris ekranına yönlendir
            val intent = Intent(this, Giris::class.java)
            startActivity(intent)
            finish() // MainActivity'yi kapat
        } else {
            // Kullanıcı giriş yaptıysa, MainActivity'yi aç
            setContentView(R.layout.activity_main)
        }

        ThemeHelper.applyTheme(ThemeHelper.loadTheme(this)) // ⬅ önce bu
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // Room database setup
        db = AppDatabase.getDatabase(applicationContext)
        taskDao = db.taskDao()
        resetTimeDao = db.resetTimeDao()
        adapter = TaskAdapter(mutableListOf(), ::addTask, taskDao) { updateTaskStats() }

        // Veritabanındaki görevleri yükle
        loadTasks()

        // Resetleme saati kontrolü
        checkResetTime()  // Resetleme saati kontrolünü yapıyoruz

        val recyclerView = binding.contentMain.todoRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Drag & drop
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
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

        adapter.onItemDelete = { position ->
            adapter.deleteItem(position) // Görevi sil
            updateTaskStats() // Günlük görev sayısını güncelle
        }

        adapter.onStatsChanged = {
            updateTaskStats()
        }

        binding.fab.setOnClickListener {
            Log.d("MainActivity", "FAB clicked")
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
            // Eğer saatsiz görev değilse (yani zaman belirtilmişse)
            if (task.time.isNotBlank() && task.time != "Saat") {
                // Veritabanında aynı saatte görev olup olmadığını kontrol et
                val existingTask = taskDao.getTaskByTime(task.time)

                // Eğer o saatte başka bir görev varsa
                if (existingTask != null) {
                    // Kullanıcıya uyarı mesajı göster
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Bu saatte zaten bir görev var!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Eğer o saatte görev yoksa, yeni görevi ekle
                    taskDao.insertTask(task)
                    loadTasks() // Veritabanını yeniden yükle
                }
            } else {
                // Eğer saatsiz görevse, zaman kontrolü yapmadan ekle
                taskDao.insertTask(task)
                loadTasks() // Veritabanını yeniden yükle
            }
        }
    }

    // Günlük görev sayısını güncelleyen fonksiyon
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
        FirebaseAuth.getInstance().signOut()  // Firebase ile çıkış yap
        val intent = Intent(this, Giris::class.java) // Giris Activity'sine yönlendir
        startActivity(intent)
        finish() // MainActivity'yi kapat
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

    private fun resetTasksAtSpecificTime() {
        GlobalScope.launch(Dispatchers.IO) {
            val resetTime = resetTimeDao.getResetTime() // Reset saati veritabanından al
            val currentTime = Calendar.getInstance()
            val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
            val currentMinute = currentTime.get(Calendar.MINUTE)

            // Saat geldi mi kontrol et
            if (currentHour == resetTime.resetHour && currentMinute == resetTime.resetMinute) {
                // Saat geldi, tüm görevlerin isChecked durumunu sıfırla
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

            // Veritabanı boşsa, işlemi güvenli bir şekilde kontrol et
            if (resetTime != null) {
                val currentTime = Calendar.getInstance()
                val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
                val currentMinute = currentTime.get(Calendar.MINUTE)

                // Saat geldi mi kontrol et
                if (currentHour == resetTime.resetHour && currentMinute == resetTime.resetMinute) {
                    resetTasks() // Eğer saat geldiyse görevleri sıfırla
                }
            } else {
                // Eğer resetTime null dönerse, kullanıcıya uyarı verebiliriz veya varsayılan bir işlem yapabiliriz
                Log.w("MainActivity", "Reset time not found in the database.")
            }
        }
    }

}
