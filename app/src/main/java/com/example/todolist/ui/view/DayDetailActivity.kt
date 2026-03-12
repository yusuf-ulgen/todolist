package com.example.todolist

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todolist.databinding.ActivityDayDetailBinding
import java.text.SimpleDateFormat
import java.util.Locale

class DayDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDayDetailBinding
    private lateinit var viewModel: TaskViewModel

    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDayDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(applicationContext)
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

        // Intent’ten tarih al
        val date = intent.getStringExtra("date") ?: return

        // Başlıkta göster (örn. "3 Mayıs")
        binding.dateHeader.text = SimpleDateFormat("d MMMM yyyy", Locale("tr"))
            .format(SimpleDateFormat("yyyy-MM-dd").parse(date)!!)

        // Observe history
        viewModel.history.observe(this) { history ->
            binding.historyRecycler.layoutManager = LinearLayoutManager(this@DayDetailActivity)
            binding.historyRecycler.adapter = TaskHistoryAdapter(history)
        }

        viewModel.loadHistoryByDate(date)
    }
}