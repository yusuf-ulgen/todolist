package com.example.todolist

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.*
import com.example.todolist.databinding.ActivityDayDetailBinding
import java.text.SimpleDateFormat
import java.util.Locale

class DayDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDayDetailBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDayDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getDatabase(this)

        // Intent’ten tarih al
        val date = intent.getStringExtra("date") ?: return

        // Başlıkta göster (örn. "3 Mayıs")
        binding.dateHeader.text = SimpleDateFormat("d MMMM yyyy", Locale("tr"))
            .format(SimpleDateFormat("yyyy-MM-dd").parse(date)!!)

        // Verileri çek ve listele
        GlobalScope.launch(Dispatchers.IO) {
            val history = db.taskHistoryDao().getByDate(date)
            withContext(Dispatchers.Main) {
                binding.historyRecycler.layoutManager = LinearLayoutManager(this@DayDetailActivity)
                binding.historyRecycler.adapter   = TaskHistoryAdapter(history)
            }
        }
    }
}