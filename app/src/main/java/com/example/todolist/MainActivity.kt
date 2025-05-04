package com.example.todolist

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.EditText
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.todolist.NotificationPreferenceRepository
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ActivityMainBinding
import com.example.todolist.databinding.ItemTaskBinding
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
import android.graphics.Color
import android.graphics.drawable.ClipDrawable.HORIZONTAL
import android.view.View
import com.google.android.material.tabs.TabLayout
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TaskAdapter
    private lateinit var db: AppDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var resetTimeDao: ResetTimeDao
    private lateinit var requestNotifPermission: ActivityResultLauncher<String>
    private lateinit var notifPrefRepo: NotificationPreferenceRepository
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var weeklyAdapter: TaskAdapter
    private var currentSelectedDow: DayOfWeek = LocalDate.now().dayOfWeek
    private var tasks: List<Task> = mutableListOf()
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
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = ""
            subtitle = "Bugünün görevleri 0/0"
            setDisplayShowTitleEnabled(true)
        }

        binding.tabLayout.apply {
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    when (tab.position) {
                        0 -> { showDailyView() }
                        1 -> { showWeeklyView() }
                    }
                }
                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })
        }

        createNotificationChannel()

        // örnek: açık gri / koyu mavi
        val unselectedColor = ContextCompat.getColor(this, R.color.gray)
        val selectedColor   = ContextCompat.getColor(this, R.color.black)
        binding.tabLayout.setTabTextColors(unselectedColor, selectedColor)

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

        db = AppDatabase.getDatabase(applicationContext)
        taskDao = db.taskDao()
        resetTimeDao = db.resetTimeDao()

        adapter = TaskAdapter(
            mutableListOf(),
            ::addTask,
            taskDao,
            { updateTaskStats() },
            onTimeClick = { task, b ->

                // önce arka planda preference’ı al
                GlobalScope.launch(Dispatchers.IO) {
                    val kind = notifPrefRepo.loadKind()

                    withContext(Dispatchers.Main) {
                        fun proceed() {
                            // Android13+ izin kontrolü
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                requestNotifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                            // sonra time picker’ı göster
                            showTimePickerAndSave(task, b)
                        }

                        if (kind < 0) {
                            // daha önce hiç seçilmedi, diyalogu göster
                            showNotificationChoice { selectedKind ->
                                // 1) seçimi kaydet
                                GlobalScope.launch(Dispatchers.IO) {
                                    notifPrefRepo.saveKind(selectedKind)
                                }
                                // 2) devam et
                                proceed()
                            }
                        } else {
                            // zaten seçim var, direkt devam et
                            proceed()
                        }
                    }
                }

            }
        )
        binding.includeDaily.todoRecyclerView.adapter = adapter


        weeklyAdapter = TaskAdapter(
            mutableListOf(),
            { task -> addTaskForWeekly(task, currentSelectedDow) },
            taskDao,
            onStatsChanged = { updateWeeklyStats() },
            onTimeClick   = { task, b -> showTimePickerAndSave(task, b) }
        )
        binding.includeWeekly.weeklyTasksRecyclerView.adapter = weeklyAdapter


        loadTasks()

        val recyclerView = binding.includeDaily.todoRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // CHANGE animasyonlarını kapat—boşluk kalma riskini iyice ortadan kaldırır
        (recyclerView.itemAnimator as? androidx.recyclerview.widget.SimpleItemAnimator)
            ?.supportsChangeAnimations = false

        // "1") Günlük için tamamen ayrı touchCallback:
        val dailyTouchCallback = object : ItemTouchHelper.SimpleCallback(
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
                recyclerView.post {
                    adapter.notifyItemRangeChanged(0, adapter.itemCount)
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                // silinen görevi al
                val deletedTask = tasks[pos]
                // UI’dan kaldır
                adapter.deleteItem(pos)
                // veritabanından sil
                GlobalScope.launch(Dispatchers.IO) { taskDao.deleteTask(deletedTask) }

                // 3 saniyelik undo Snackbar
                Snackbar.make(binding.root, "Görev silindi", Snackbar.LENGTH_LONG /*≈3s*/)
                    .setAction("Geri Al") {
                        // veritabanına geri ekle
                        GlobalScope.launch(Dispatchers.IO) {
                            taskDao.insertTask(deletedTask)
                        }
                        // UI’a geri ekle
                        adapter.restoreItem(deletedTask, pos)
                    }
                    .show()
            }

            override fun isLongPressDragEnabled() = true
        }

        // günlük RV’ye tak
        ItemTouchHelper(dailyTouchCallback)
            .attachToRecyclerView(binding.includeDaily.todoRecyclerView)


        // 2) Haftalık için tamamen ayrı touchCallback:
        val weeklyTouchCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                weeklyAdapter.notifyItemRangeChanged(0, weeklyAdapter.itemCount)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                // silinen görevi al yine weeklyAdapter’dan
                val deletedTask = weeklyAdapter.getTasks()[pos]

                // UI’dan kaldır ve DB’den sil
                weeklyAdapter.deleteItem(pos)
                GlobalScope.launch(Dispatchers.IO) {
                    taskDao.deleteTask(deletedTask)
                }

                // sayaç güncelle
                updateWeeklyStats()

                // undo Snackbar
                Snackbar.make(binding.root, "Görev silindi", Snackbar.LENGTH_LONG)
                    .setAction("Geri Al") {
                        GlobalScope.launch(Dispatchers.IO) {
                            taskDao.insertTask(deletedTask)
                        }
                        weeklyAdapter.restoreItem(deletedTask, pos)
                        updateWeeklyStats()
                    }
                    .show()
            }

            override fun isLongPressDragEnabled() = true
        }

        ItemTouchHelper(weeklyTouchCallback)
            .attachToRecyclerView(binding.includeWeekly.weeklyTasksRecyclerView)

        adapter.onItemDelete = { position ->
            val deletedTask = tasks[position]
            adapter.deleteItem(position)
            GlobalScope.launch(Dispatchers.IO) { taskDao.deleteTask(deletedTask) }

            Snackbar.make(binding.root, "Görev silindi", Snackbar.LENGTH_LONG)
                .setAction("Geri Al") {
                    GlobalScope.launch(Dispatchers.IO) { taskDao.insertTask(deletedTask) }
                    adapter.restoreItem(deletedTask, position)
                }
                .show()
        }

        weeklyAdapter.onItemDelete = { pos ->
            // sadece weeklyAdapter listesinden al
            val deletedTask = weeklyAdapter.getTasks()[pos]

            // UI’dan kaldır
            weeklyAdapter.deleteItem(pos)
            // veritabanından sil
            GlobalScope.launch(Dispatchers.IO) {
                taskDao.deleteTask(deletedTask)
            }

            // hemen sayaç güncellensin
            updateWeeklyStats()

            // 3 saniyelik undo Snackbar
            Snackbar.make(binding.root, "Görev silindi", Snackbar.LENGTH_LONG)
                .setAction("Geri Al") {
                    // veritabanına geri koy
                    GlobalScope.launch(Dispatchers.IO) {
                        taskDao.insertTask(deletedTask)
                    }
                    // UI’a geri ekle
                    weeklyAdapter.restoreItem(deletedTask, pos)
                    // yine sayaç güncellensin
                    updateWeeklyStats()
                }
                .show()
        }

        binding.fab.setOnClickListener {
            // Haftalık moddaysak includeWeekly görünür olmalı
            if (findViewById<View>(R.id.includeWeekly).visibility == View.VISIBLE) {
                // Haftalık mod
                val newTask = Task(
                    userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                    content = "",
                    time = ""
                )
                addTaskForWeekly(newTask, currentSelectedDow)
            } else {
                // Günlük mod
                val newTask = Task(
                    userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                    content = "",
                    time = ""
                )
                addTask(newTask)
            }
        }


        val db = AppDatabase.getDatabase(this)
        notifPrefRepo = NotificationPreferenceRepository(db.notificationPrefDao())

        // 1.c) İzin launcher’ını register et
        requestNotifPermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                Snackbar.make(binding.root,
                    "Bildirim izni reddedildi",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showDailyView() {
        // ViewBinding işe yaramıyorsa direkt:
        findViewById<View>(R.id.includeDaily).visibility = View.VISIBLE
        findViewById<View>(R.id.includeWeekly).visibility = View.GONE

        // Günlük listeyi yeniden yükle ve istatistiği güncelle
        loadTasks()
        updateTaskStats()
    }

    private fun showWeeklyView() {
        findViewById<View>(R.id.includeDaily).visibility = View.GONE
        findViewById<View>(R.id.includeWeekly).visibility = View.VISIBLE

        setupWeeklyCalendar()

        // Gün olarak bugünü atıyoruz
        currentSelectedDow = LocalDate.now().dayOfWeek

        binding.includeWeekly.weeklyCalendarRecyclerView.post {
            val pos = calendarAdapter.getPositionFor(currentSelectedDow)
            if (pos >= 0) {
                calendarAdapter.selectDay(currentSelectedDow)
                binding.includeWeekly.weeklyCalendarRecyclerView.scrollToPosition(pos)
                // ve seçilen günün görevlerini yükle
                loadWeeklyTasksForDay(currentSelectedDow)
            }
        }
    }


    private fun setupWeeklyCalendar() {
        // 1) Haftanın günleri
        val days = listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )

        // 2) Adapter’i oluştur ve sakla
        calendarAdapter = CalendarAdapter(days) { selectedDow ->
            currentSelectedDow = selectedDow
            loadWeeklyTasksForDay(selectedDow)
        }

        // 3) RecyclerView’e ata
        binding.includeWeekly.weeklyCalendarRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = calendarAdapter
        }

        // 4) Haftalık görevler listesi (dikey)
        binding.includeWeekly.weeklyTasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = weeklyAdapter
        }

        // 5) Başlangıçta boş istatistik
        binding.includeWeekly.weeklyStatTextView.text = "Haftalık: 0/0"
    }

    private fun loadWeeklyTasksForDay(dow: DayOfWeek) {
        currentSelectedDow = dow
        GlobalScope.launch(Dispatchers.IO) {
            val weekly = taskDao.getTasksByWeekday(FirebaseAuth.getInstance().currentUser!!.uid, dow.name)
            withContext(Dispatchers.Main) {
                weeklyAdapter.setTasks(weekly)
                updateWeeklyStats()
            }
        }
        updateWeeklyStats()
    }

    private fun addTaskForWeekly(task: Task, dow: DayOfWeek) {
        task.weekday = dow.name
        GlobalScope.launch(Dispatchers.IO) {
            taskDao.insertTask(task)
            withContext(Dispatchers.Main) {
                loadWeeklyTasksForDay(dow)
            }
        }
    }

    private fun showNotificationChoice(onChosen: (kind: Int) -> Unit) {
        val options = arrayOf(
            "Her görev için bildirim gönder",
            "Sadece pinlilere gönder",
            "Hiçbirine gönderme"
        )
        AlertDialog.Builder(this)
            .setTitle("Bildirim tercihinizi seçin")
            .setSingleChoiceItems(options, /*defaultIndex=*/ -1) { dialog, which ->
                onChosen(which)
                dialog.dismiss()
            }
            .setNegativeButton("İptal", null)
            .show()
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

    private fun loadTasks(scrollToEnd: Boolean = false) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        GlobalScope.launch(Dispatchers.IO) {
            val all      = taskDao.getTasksByUserId(uid)
            val daily    = all.filter { it.weekday.isNullOrEmpty() }
            withContext(Dispatchers.Main) {
                tasks = daily
                adapter.setTasks(tasks)
                if (scrollToEnd && tasks.isNotEmpty()) {
                    binding.includeDaily.todoRecyclerView
                        .scrollToPosition(tasks.size - 1)
                }
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
                loadTasks(scrollToEnd = true)
            }
        }
    }

    private fun fireDailyConfettiIfNeeded() {
        val total = adapter.getTasks().size
        val done  = adapter.getTasks().count { it.isChecked }
        if (total > 0 && done == total) {
            binding.includeDaily.konfettiView.apply {
                // Önce üst katmana taşı
                bringToFront()
                // Başlat
                visibility = View.VISIBLE
                start(createParty())
                postDelayed({ visibility = View.GONE }, 5_000)
            }
        }
    }

    private fun fireWeeklyConfettiIfNeeded() {
        val total = weeklyAdapter.getTasks().size
        val done  = weeklyAdapter.getTasks().count { it.isChecked }
        if (total > 0 && done == total) {
            binding.includeWeekly.weeklyKonfettiView.apply {
                // Önce üst katmana taşı
                bringToFront()
                // Başlat
                visibility = View.VISIBLE
                start(createParty())
                postDelayed({ visibility = View.GONE }, 5_000)
            }
        }
    }

    private fun createParty(): Party {
        return Party(
            colors = listOf(Color.YELLOW, Color.MAGENTA, Color.GREEN),
            shapes = listOf(Shape.Circle, Shape.Square),
            size = listOf(Size(12)),
            position = Position.Relative(0.5, 0.5),
            emitter = Emitter(duration = 1, TimeUnit.SECONDS).max(200)
        )
    }

    private fun updateTaskStats() {
        val total = adapter.getTasks().size
        val done  = adapter.getTasks().count { it.isChecked }

        supportActionBar?.subtitle = "Bugünün görevleri $done/$total"

        fireDailyConfettiIfNeeded()
    }

    private fun updateWeeklyStats() {
        val total = weeklyAdapter.getTasks().size
        val done  = weeklyAdapter.getTasks().count { it.isChecked }
        binding.includeWeekly.weeklyStatTextView.text =
            "${currentSelectedDow.getDisplayName(TextStyle.SHORT, Locale.getDefault())}: $done/$total"

        fireWeeklyConfettiIfNeeded()
    }

    fun scheduleDailyResetAlarm(context: Context, resetHour: Int, resetMinute: Int) {
        val intent = Intent(context, ResetReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, resetHour)
            set(Calendar.MINUTE, resetMinute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            pi
        )
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

    private fun cancelTaskNotification(task: Task) {
        val requestCode = ((task.id shl 3) + (task.weekday?.hashCode() ?: 0)).toInt()
        val pi = PendingIntent.getBroadcast(
            this,
            requestCode,
            Intent(this, NotificationReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi?.let {
            (getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(it)
            it.cancel()
        }
    }



    private fun scheduleTaskNotification(task: Task, hour: Int, minute: Int) {
        cancelTaskNotification(task)

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (!task.weekday.isNullOrEmpty()) {
                // Haftalık
                val targetDow = DayOfWeek.valueOf(task.weekday!!).value % 7 + 1
                set(Calendar.DAY_OF_WEEK, targetDow)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, 1)
            } else {
                // Günlük
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("taskId", task.id.toInt())
            putExtra("taskContent", task.content)
        }

        val requestCode = ((task.id shl 3) + (task.weekday?.hashCode() ?: 0)).toInt()
        val pi = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        (getSystemService(Context.ALARM_SERVICE) as AlarmManager).setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            pi
        )
    }


    private fun showTimePickerAndSave(task: Task, b: ItemTaskBinding) {
        val calNow = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val timeText = "%02d:%02d".format(hourOfDay, minute)
                b.timeTextView.text = timeText
                task.time = timeText

                GlobalScope.launch(Dispatchers.IO) {
                    taskDao.updateTask(task)
                }

                cancelTaskNotification(task)
                scheduleTaskNotification(task, hourOfDay, minute)

            },
            calNow.get(Calendar.HOUR_OF_DAY),
            calNow.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "task_channel"
            val channelName = "Görev Hatırlatıcı"
            val channelDesc = "Görev zamanları için hatırlatıcı bildirimleri"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDesc
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }

            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}