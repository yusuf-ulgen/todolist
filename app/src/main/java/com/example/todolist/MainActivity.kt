package com.example.todolist

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TaskAdapter
    private lateinit var db: AppDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var resetTimeDao: ResetTimeDao
    private var tasks: List<Task> = mutableListOf()

    // Bu flag, drag/swipe tamamlanmadan yenisini engelliyor
    private var isMoving = false
    private val moveResetHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        )

        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)  // Önce binding ile setContentView'i yapın

        // Hide action bar
        supportActionBar?.hide()

        val mAuth = FirebaseAuth.getInstance()

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
        adapter = TaskAdapter(
            mutableListOf(),    // 1. param: başlangıç listesi
            ::addTask,          // 2. param: addTask callback
            taskDao,            // 3. param: TaskDao
            { updateTaskStats() } // 4. param: onStatsChanged callback
        )

        checkAndPerformReset()
        loadTasks()

        val recyclerView = binding.contentMain.todoRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val touchCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (isMoving) return false
                isMoving = true

                val from = vh.adapterPosition
                val to = target.adapterPosition
                val list = adapter.getTasks()
                val pinnedCount = list.count { it.isPinned }

                if ((from < pinnedCount && to < pinnedCount) ||
                    (from >= pinnedCount && to >= pinnedCount)
                ) {
                    adapter.moveItem(from, to)
                    scheduleMoveReset()
                    return true
                }
                scheduleMoveReset()
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
            GlobalScope.launch(Dispatchers.IO) {
                taskDao.deleteTask(tasks[position])
                withContext(Dispatchers.Main) {
                    adapter.deleteItem(position)
                    updateTaskStats()
                }
            }
        }

        adapter.onStatsChanged = {
            updateTaskStats()
        }

        binding.fab.setOnClickListener {
            val newTask = Task(
                userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",  // Kullanıcı ID
                content = "",
                time = ""
            )
            addTask(newTask)
        }
    }

    private fun scheduleMoveReset() {
        moveResetHandler.removeCallbacksAndMessages(null)
        moveResetHandler.postDelayed({ isMoving = false }, 150)
    }

    override fun onResume() {
        super.onResume()
        loadTasks()
        checkAndPerformReset()
    }

    private val mAuth = FirebaseAuth.getInstance()

    private fun loadTasks() {
        val currentUser = mAuth.currentUser ?: return
        GlobalScope.launch(Dispatchers.IO) {
            val taskList = taskDao.getTasksByUserId(currentUser.uid)
            withContext(Dispatchers.Main) {
                tasks = taskList
                adapter.setTasks(tasks)
                updateTaskStats()
            }
        }
    }

    private fun addTask(task: Task) {
        val currentUser = mAuth.currentUser ?: return
        task.userId = currentUser.uid
        GlobalScope.launch(Dispatchers.IO) {
            val existing = taskDao.getTasksByUserId(currentUser.uid)
            task.sortOrder = existing.size

            if (task.time.isNotBlank() && task.time != "Saat") {
                val conflict = taskDao.getTaskByTimeAndUserId(task.time, currentUser.uid)
                if (conflict != null) {
                    withContext(Dispatchers.Main) {
                        Snackbar.make(
                            binding.root,
                            "Bu saatte zaten bir görev var!",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }
            }

            taskDao.insertTask(task)
            withContext(Dispatchers.Main) {
                loadTasks()
            }
        }
    }

    private fun updateTaskStats() {
        val total = adapter.getTasks().size
        val done  = adapter.getTasks().count { it.isChecked }
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

    //  Reset kontrol + kayıt + sıfırlama
    private fun checkAndPerformReset() {
        GlobalScope.launch(Dispatchers.IO) {
            val resetTime = resetTimeDao.getResetTime() ?: return@launch

            val now = Calendar.getInstance()
            val h = now.get(Calendar.HOUR_OF_DAY)
            val m = now.get(Calendar.MINUTE)
            val nowSecond = now.get(Calendar.SECOND)

            // Bugünün anahtarı
            val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(now.time)

            // Daha önce bu gün resetlendi mi?
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            if (prefs.getString("last_reset_day", "") == todayKey) return@launch

            // Toplam dakikaya çevir ve karşılaştır
            val nowTotal   = h * 60 + m
            val resetTotal = resetTime.resetHour * 60 + resetTime.resetMinute

            if (nowTotal > resetTotal || (nowTotal == resetTotal && nowSecond >= 0)) {
                // 1) İstatistik kaydet
                val allTasks  = taskDao.getAllTasks()
                val completed = allTasks.count { it.isChecked }
                val total     = allTasks.size
                db.dailyStatDao().upsert(DailyStat(todayKey, completed, total))

                // 2) History kaydet
                val history = allTasks.map {
                    TaskHistory(
                        date      = todayKey,
                        content   = it.content,
                        time      = it.time,
                        isChecked = it.isChecked
                    )
                }
                db.taskHistoryDao().insertAll(history)

                // 3) Tümünü sıfırla
                allTasks.forEach {
                    it.isChecked = false
                    taskDao.updateTask(it)
                }

                // 4) Bugün sıfırlandı kaydet
                prefs.edit().putString("last_reset_day", todayKey).apply()

                // 5) UI thread’de listeyi yenile
                withContext(Dispatchers.Main) {
                    loadTasks()
                }
            }
        }
    }

    private fun scheduleDailyResetAlarm(resetHour: Int, resetMinute: Int) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        // 13+ için izin kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Kullanıcıyı Ayarlar > Alarmlar kısmına yönlendirebilirsiniz
                Toast.makeText(this, "Exact alarm izni yok", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val intent  = Intent(this, ResetReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, resetHour)
            set(Calendar.MINUTE, resetMinute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            pending
        )
    }
}