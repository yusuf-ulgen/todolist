package com.example.todolist

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todolist.databinding.ActivityStatisticsBinding

class StatisticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var viewModel: TaskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
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

        // Observe stats
        viewModel.last30DaysStats.observe(this) { stats ->
            binding.statsRecycler.layoutManager = LinearLayoutManager(this@StatisticsActivity)
            binding.statsRecycler.adapter = DailyStatAdapter(stats) { stat ->
                startActivity(
                    Intent(this@StatisticsActivity, DayDetailActivity::class.java)
                        .putExtra("date", stat.date)
                )
            }
        }

        viewModel.loadLast30DaysStats()
    }
}