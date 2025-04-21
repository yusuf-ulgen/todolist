package com.example.todolist

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.todolist.databinding.ActivityStatisticsBinding

class StatisticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStatisticsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Veritabanından son 30 günün kaydını alarak burada göstereceksiniz
        // Örneğin:
        // val statistics = statisticsDao.getLast30DaysData()
        // binding.textView.text = statistics.toString()
    }
}
