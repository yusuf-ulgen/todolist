@file:Suppress("DEPRECATION")
package com.example.todolist

import android.annotation.SuppressLint
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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ActivityMainBinding
import com.example.todolist.databinding.ItemTaskBinding
import androidx.lifecycle.ViewModelProvider
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
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.DelicateCoroutinesApi
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    @SuppressLint("NewApi")
    private var currentSelectedDow: DayOfWeek = LocalDate.now().dayOfWeek
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TaskAdapter
    private lateinit var db: AppDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var resetTimeDao: ResetTimeDao
    private lateinit var requestNotifPermission: ActivityResultLauncher<String>
    private lateinit var notifPrefRepo: NotificationPreferenceRepository
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var weeklyAdapter: TaskAdapter
    private lateinit var viewModel: TaskViewModel
    private var tasks: List<Task> = mutableListOf()
    private var searchQuery: String = ""
    private var isMoving = false
    private val moveResetHandler = Handler(Looper.getMainLooper())
    private var currentListId: Long = 1L
    private var listName: String = "GÜNLÜK/HAFTALIK"
    private var shouldScrollToBottomDaily = false
    private var shouldScrollToBottomWeekly = false
    private var dailyTasksList: List<Task> = mutableListOf()
    private var weeklyTasksList: List<Task> = mutableListOf()

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        )

        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentListId = intent?.getLongExtra("listId", 1L) ?: 1L
        listName      = intent?.getStringExtra("listName") ?: "GÜNLÜK/HAFTALIK"

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = listName
            subtitle = if (currentListId == 1L) "" else ""
            setDisplayShowTitleEnabled(true)
        }

        if (currentListId != 1L) {
            // özel listelerde recyclerview üstündeki TextView’i gizle
            binding.toolbarStatText.visibility = View.GONE
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
        val repository = TaskRepository(
            db.taskDao(),
            db.todolistDao(),
            db.dailyStatDao(),
            db.taskHistoryDao(),
            db.notificationPrefDao(),
            db.resetTimeDao()
        )
        val factory = TaskViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TaskViewModel::class.java]

        taskDao = db.taskDao()
        resetTimeDao = db.resetTimeDao()

        adapter = TaskAdapter(
            { task -> viewModel.addTask(task.apply { listId = currentListId }) },
            taskDao,
            { updateTaskStats() },
            onTimeClick = { task, b ->
                // ... (existing logic for notification choice)
                lifecycleScope.launch(Dispatchers.IO) {
                    val kind = notifPrefRepo.loadKind()
                    withContext(Dispatchers.Main) {
                        fun proceed() {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                requestNotifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                            showTimePickerAndSave(task, b)
                        }
                        if (kind < 0) {
                            showNotificationChoice { selectedKind ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    notifPrefRepo.saveKind(selectedKind)
                                }
                                proceed()
                            }
                        } else {
                            proceed()
                        }
                    }
                }
            },
            onTaskUpdate = { task -> viewModel.updateTask(task) }
        )
        binding.includeDaily.todoRecyclerView.adapter = adapter


        weeklyAdapter = TaskAdapter(
            { task -> viewModel.addTask(task.apply { 
                weekday = currentSelectedDow.name
                listId = currentListId 
            }) },
            taskDao,
            onStatsChanged = { updateWeeklyStats() },
            onTimeClick = { task, b -> showTimePickerAndSave(task, b) },
            onTaskUpdate = { task -> viewModel.updateTask(task) }
        )
        binding.includeWeekly.weeklyTasksRecyclerView.adapter = weeklyAdapter

        // Observe daily tasks
        viewModel.tasks.observe(this) { updatedTasks ->
            dailyTasksList = updatedTasks
            filterAndDisplayTasks()
        }

        // Observe weekly tasks
        viewModel.weeklyTasks.observe(this) { updatedTasks ->
            weeklyTasksList = updatedTasks
            filterAndDisplayTasks()
        }

        loadTasks()

        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText.orEmpty()
                filterAndDisplayTasks()
                return true
            }
        })

        intent?.let {
            currentListId = it.getLongExtra("listId", 1L) // Burada default değeri 1L olarak veriyoruz
            listName = it.getStringExtra("listName") ?: "GÜNLÜK/HAFTALIK"
        }

        // 2) Toolbar başlığı olarak liste adını koy
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = listName
            subtitle = ""  // veya günlük görev sayısını altyazı olarak istiyorsan burayı ayarla
        }

        // 3) Eğer özel bir liste (id != 1) ise tabları ve haftalığı kapat:
        if (currentListId != 1L) {
            binding.tabLayout.visibility        = View.GONE
            binding.includeWeekly.root.visibility = View.GONE
            binding.includeDaily.root.visibility  = View.VISIBLE
        } else {
            binding.tabLayout.visibility        = View.VISIBLE
            binding.includeWeekly.root.visibility = View.GONE
            binding.includeDaily.root.visibility  = View.VISIBLE

            // Tam bir listener nesnesi oluştur:
            binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    when (tab.position) {
                        0 -> showDailyView()
                        1 -> showWeeklyView()
                    }
                }
                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })
        }

        // listener kapandıktan sonra buraya devam et:
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
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                if (isMoving) return false
                isMoving = true

                val from = vh.adapterPosition
                val to   = target.adapterPosition
                val list = adapter.getTasks()
                val pinnedCount = list.count { it.isPinned }

                if ((from < pinnedCount && to < pinnedCount) ||
                    (from >= pinnedCount && to >= pinnedCount)
                    ) {
                    // 1) UI’da taşı
                    adapter.moveItem(from, to)
                    // 2) Yeni sortOrder’ları ayarla
                    adapter.getTasks().forEachIndexed { idx, task ->
                        task.sortOrder = idx
                        viewModel.updateTask(task)
                        }
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
                val deletedTask = adapter.getTasks()[pos]
                adapter.deleteItem(pos)
                viewModel.deleteTask(deletedTask)

                // 3 saniyelik undo Snackbar
                Snackbar.make(binding.root, "Görev silindi", Snackbar.LENGTH_LONG /*≈3s*/)
                    .setAction("Geri Al") {
                        viewModel.addTask(deletedTask)
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
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = vh.adapterPosition
                val to   = target.adapterPosition

                if (isMoving) return false
                isMoving = true
                val list = adapter.getTasks()
                val pinnedCount = list.count { it.isPinned }
                if ((from < pinnedCount && to < pinnedCount) ||
                    (from >= pinnedCount && to >= pinnedCount)
                    ) {
                    // 1) UI’da taşı
                    weeklyAdapter.moveItem(from, to)
                    // 2) Her öğeye yeni sortOrder ata
                    weeklyAdapter.getTasks().forEachIndexed { idx, task ->
                        task.sortOrder = idx
                        viewModel.updateTask(task)
                        }
                    scheduleMoveReset()
                    return true
                    }
                scheduleMoveReset()
                return false
                }

            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                // taşıma sonrası trigger için
                weeklyAdapter.notifyItemRangeChanged(0, weeklyAdapter.itemCount)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                // silinen görevi al yine weeklyAdapter’dan
                val deletedTask = weeklyAdapter.getTasks()[pos]

                // UI’dan kaldır ve DB’den sil
                weeklyAdapter.deleteItem(pos)
                viewModel.deleteTask(deletedTask)

                // sayaç güncelle
                updateWeeklyStats()

                // undo Snackbar
                Snackbar.make(binding.root, "Görev silindi", Snackbar.LENGTH_LONG)
                    .setAction("Geri Al") {
                        viewModel.addTask(deletedTask)
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
            val deletedTask = adapter.getTasks()[position]
            adapter.deleteItem(position)
            viewModel.deleteTask(deletedTask)

            Snackbar.make(binding.root, "Görev silindi", Snackbar.LENGTH_LONG)
                .setAction("Geri Al") {
                    viewModel.addTask(deletedTask)
                    adapter.restoreItem(deletedTask, position)
                }
                .show()
        }

        weeklyAdapter.onItemDelete = { pos ->
            val deletedTask = weeklyAdapter.getTasks()[pos]
            weeklyAdapter.deleteItem(pos)
            viewModel.deleteTask(deletedTask)
            updateWeeklyStats()

            Snackbar.make(binding.root, "Görev silindi", Snackbar.LENGTH_LONG)
                .setAction("Geri Al") {
                    viewModel.addTask(deletedTask)
                    weeklyAdapter.restoreItem(deletedTask, pos)
                    updateWeeklyStats()
                }
                .show()
        }

        binding.fab.setOnClickListener {
            // Haftalık moddaysak includeWeekly görünür olmalı
            val newTask = Task(
                userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                content = "",
                time = "",
                listId = currentListId // Burada listId'yi geçiriyoruz
            )

            if (findViewById<View>(R.id.includeWeekly).visibility == View.VISIBLE) {
                // Haftalık moddaysa, yeni görev ekle
                addTaskForWeekly(newTask, currentSelectedDow, currentListId) // Burada da listId'yi geçiriyoruz
            } else {
                // Günlük moddaysa, yeni görev ekle
                addTask(newTask, currentListId) // Burada da listId'yi geçiriyoruz
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
    @Deprecated("This method has been deprecated in favor of using the.", ReplaceWith("finish()"))
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        finish()
    }


    private fun showDailyView() {
        // ViewBinding işe yaramıyorsa direkt:
        findViewById<View>(R.id.includeDaily).visibility = View.VISIBLE
        findViewById<View>(R.id.includeWeekly).visibility = View.GONE

        // Günlük listeyi yeniden yükle ve istatistiği güncelle
        loadTasks(scrollToEnd = false)
        updateTaskStats()
    }

    @SuppressLint("NewApi")
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
                loadWeeklyTasksForDay(currentSelectedDow, scrollToEnd = false)
            }
        }
    }


    @SuppressLint("NewApi", "SetTextI18n")
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

    @OptIn(DelicateCoroutinesApi::class)
    private fun loadWeeklyTasksForDay(dow: DayOfWeek, scrollToEnd: Boolean = false) {
        currentSelectedDow = dow
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModel.loadWeeklyTasksForDay(uid, dow.name)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun addTaskForWeekly(task: Task, selectedDayOfWeek: DayOfWeek, listId: Long) {
        shouldScrollToBottomWeekly = true
        task.weekday = selectedDayOfWeek.name
        task.listId = listId // listId'yi ekliyoruz

        lifecycleScope.launch(Dispatchers.IO) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            // Mevcut haftalık görevler o güne ait
            val existing = taskDao.getTasksByWeekday(
                uid,
                selectedDayOfWeek.name
                        )
            task.sortOrder = existing.size
            task.weekday = selectedDayOfWeek.name
            task.listId = listId

            taskDao.insertTask(task)
            withContext(Dispatchers.Main) {
                loadWeeklyTasksForDay(selectedDayOfWeek, scrollToEnd = true)
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
            .setSingleChoiceItems(options, -1) { dialog, which ->
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            loadWeeklyTasksForDay(currentSelectedDow)
        }
        checkAndPerformReset()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun loadTasks(scrollToEnd: Boolean = false) {
        viewModel.loadTasksByListId(currentListId)
    }

    private fun filterAndDisplayTasks() {
        if (binding.includeWeekly.root.visibility == View.VISIBLE) {
            val filtered = if (searchQuery.isBlank()) {
                weeklyTasksList
            } else {
                weeklyTasksList.filter { it.content.contains(searchQuery, ignoreCase = true) }
            }
            val (pinned, rest) = filtered.partition { it.isPinned }
            val sortedList = pinned + rest

            weeklyAdapter.setTasks(sortedList) {
                if (shouldScrollToBottomWeekly) {
                    binding.includeWeekly.weeklyTasksRecyclerView.scrollToPosition(weeklyAdapter.itemCount - 1)
                    shouldScrollToBottomWeekly = false
                }
                updateWeeklyStats()
            }
        } else {
            val filtered = if (searchQuery.isBlank()) {
                dailyTasksList
            } else {
                dailyTasksList.filter { it.content.contains(searchQuery, ignoreCase = true) }
            }
            val (pinned, rest) = filtered.partition { it.isPinned }
            val sortedList = pinned + rest

            adapter.setTasks(sortedList) {
                if (shouldScrollToBottomDaily) {
                    binding.includeDaily.todoRecyclerView.scrollToPosition(adapter.itemCount - 1)
                    shouldScrollToBottomDaily = false
                }
                updateTaskStats()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun addTask(task: Task, listId: Long) {
        shouldScrollToBottomDaily = true
        val currentUser = FirebaseAuth.getInstance().currentUser

        task.listId = listId // Assigning the listId

        lifecycleScope.launch(Dispatchers.IO) {
            // Mevcut günlük görevler
            val existing: List<Task> = taskDao.getTasksByListId(listId)
            // En sona at
            task.sortOrder = existing.size
            task.weekday = ""  // günlük olduğundan weekday boş

            // Checking if there's a conflict with the time for the task
            if (task.time.isNotBlank() && task.time != "Saat") {
                val conflict = taskDao.getTaskByTimeAndUserId(task.time, currentUser?.uid ?: "")
                if (conflict != null) {
                    withContext(Dispatchers.Main) {
                        Snackbar.make(binding.root, "Bu saatte zaten bir görev var!", Snackbar.LENGTH_SHORT).show()
                    }
                    return@launch
                }
            }

            taskDao.insertTask(task) // Inserting the task into the database
            withContext(Dispatchers.Main) {
                loadTasks(scrollToEnd = true) // Reloading the tasks and scrolling to the end
            }
        }
    }

    private fun fireDailyConfettiIfNeeded() {
        if (adapter.itemCount == 0) return
        val total = adapter.itemCount
        val done  = adapter.getTasks().count { it.isChecked }
        if (done == total) {
            binding.includeDaily.konfettiView.apply {
                visibility = View.VISIBLE
                bringToFront()
                start(createParty())
                postDelayed({ visibility = View.GONE }, 5_000)
            }
        }
    }

    private fun fireWeeklyConfettiIfNeeded() {
        if (weeklyAdapter.itemCount == 0) return
        val total = weeklyAdapter.itemCount
        val done  = weeklyAdapter.getTasks().count { it.isChecked }
        if (done == total) {
            binding.includeWeekly.weeklyKonfettiView.apply {
                visibility = View.VISIBLE
                bringToFront()
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

    @SuppressLint("NewApi", "SetTextI18n")
    private fun updateWeeklyStats() {
        val total = weeklyAdapter.getTasks().size
        val done  = weeklyAdapter.getTasks().count { it.isChecked }

        supportActionBar?.subtitle =
            "Haftalık (${currentSelectedDow.getDisplayName(TextStyle.SHORT, Locale.getDefault())}): $done/$total"

        binding.includeWeekly.weeklyStatTextView.text =
            "${currentSelectedDow.getDisplayName(TextStyle.SHORT, Locale.getDefault())}: $done/$total"

        fireWeeklyConfettiIfNeeded()
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
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // kendi client id’niz
            .requestEmail()
            .build()
        val googleClient = GoogleSignIn.getClient(this, gso)
        googleClient.signOut().addOnCompleteListener {
            // 2) Sonra Firebase’den çıkış
            FirebaseAuth.getInstance().signOut()
            // 3) Giriş ekranına dön
            startActivity(Intent(this, Giris::class.java))
            finish()
        }
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

    @SuppressLint("NewApi")
    private fun checkAndPerformReset() {
        lifecycleScope.launch(Dispatchers.IO) {
            val resetTime = resetTimeDao.getResetTime() ?: return@launch

            val now = Calendar.getInstance()
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            val lastResetDay = prefs.getString("last_reset_day", "")

            // Reset zamanını belirle (Bugün için ayarlanan saat)
            val resetCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, resetTime.resetHour)
                set(Calendar.MINUTE, resetTime.resetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Eğer şu an reset saatinden önceyse, henüz reset vakti gelmemiştir.
            if (now.before(resetCal)) return@launch

            // Bugünün anahtarı (Reset yapıldıktan sonra kaydedilecek)
            val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
            if (lastResetDay == todayKey) return@launch

            // 1) Tüm görevleri çek (SADECE listId = 1 ve mevcut kullanıcıya ait olanları istatistik için kullanacağız)
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val allTasks = taskDao.getAllTasks().filter { it.listId == 1L && it.userId == currentUid }

            // 2) İstatistik hesaplama: Günlük Görevler + Bugünün Haftalık Görevleri
            val todayDowName = LocalDate.now().dayOfWeek.name
            
            val dailyTasks = allTasks.filter { it.weekday.isNullOrBlank() }
            val weeklyTodayTasks = allTasks.filter { it.weekday == todayDowName }
            
            val combinedTasks = dailyTasks + weeklyTodayTasks
            val completedCount = combinedTasks.count { it.isChecked }
            val totalCount = combinedTasks.size

            // İstatistiği kaydet
            db.dailyStatDao().upsert(DailyStat(todayKey, completedCount, totalCount))

            // 3) Geçmişe kaydet (Sadece günlük olanlar mı? Kullanıcı "o günün istatistiklerini" dediği için kombineyi de kaydedebiliriz ama şimdilik mevcut yapıyı koruyalım)
            val history = combinedTasks.map {
                TaskHistory(date = todayKey, content = it.content, time = it.time, isChecked = it.isChecked)
            }
            db.taskHistoryDao().insertAll(history)

            // 4) Günlük görevlerin checkbox'larını sıfırla
            dailyTasks.forEach {
                it.isChecked = false
                taskDao.updateTask(it)
            }

            // 5) Haftalık görevleri sıfırla — SADECE PAZARTESİ İSE (Hafta başı)
            // Kullanıcı "pazartesi gecesi" dediğinde genellikle pazartesiden salıya geçiş veya pazartesi başını kasteder.
            // "salı yaptığım görevlere çarşamba girip baktığımda checkboxları kaybolmamalı" dediği için
            // haftalık görevler haftada sadece bir kez sıfırlanmalı.
            if (LocalDate.now().dayOfWeek == DayOfWeek.MONDAY) {
                val allWeeklyTasks = allTasks.filter { !it.weekday.isNullOrBlank() }
                allWeeklyTasks.forEach {
                    it.isChecked = false
                    taskDao.updateTask(it)
                }
            } else {
                // Diğer günlerde sadece bugünün haftalık görevlerini de sıfırlama seçeneği olabilir ama 
                // kullanıcı "haftalık görevler pazartesi gecesinde sıfırlanmalı" dediği için diğer günler ellemiyoruz.
                // Eğer her günü kendi içinde sıfırlamak isteseydik buraya weeklyTodayTasks eklerdik.
                // Ancak kullanıcı net bir şekilde "haftalık görevler pazartesi gecesi sıfırlanmalı" dedi.
            }

            // 6) Tercihi kaydet ve UI tazele
            prefs.edit().putString("last_reset_day", todayKey).apply()
            withContext(Dispatchers.Main) {
                loadTasks()
                if (binding.includeWeekly.root.visibility == View.VISIBLE) {
                    loadWeeklyTasksForDay(LocalDate.now().dayOfWeek)
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

    @SuppressLint("NewApi")
    private fun scheduleTaskNotification(task: Task, hour: Int, minute: Int) {
        cancelTaskNotification(task)
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
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
            putExtra("listId", task.listId)
        }

        val pi = PendingIntent.getBroadcast(
            this, task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Android M+ için exact & idle’da da çalışır
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        } catch (se: SecurityException) {
            // izin yoksa; en yakın inexact zamanda çalıştır
            am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }


    private fun showTimePickerAndSave(task: Task, b: ItemTaskBinding) {
        val calNow = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val timeText = "%02d:%02d".format(hourOfDay, minute)
                b.timeTextView.text = timeText
                task.time = timeText

                viewModel.updateTask(task)

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