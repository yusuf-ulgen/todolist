package com.example.todolist

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todolist.databinding.ActivityStatisticsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatisticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getDatabase(this)

        // 30 günlük veri çek
        GlobalScope.launch(Dispatchers.IO) {
            val stats = db.dailyStatDao().getLast30Days()
            withContext(Dispatchers.Main) {
                binding.statsRecycler.layoutManager = LinearLayoutManager(this@StatisticsActivity)
                binding.statsRecycler.adapter = DailyStatAdapter(stats){ stat ->
                    // Tıklayınca detay sayfası, stat.date’i geç
                    startActivity(
                        Intent(this@StatisticsActivity, DayDetailActivity::class.java)
                        .putExtra("date", stat.date))
                }
            }
        }
    }
}