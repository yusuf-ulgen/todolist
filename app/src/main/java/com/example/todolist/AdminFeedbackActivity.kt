package com.example.todolist

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todolist.databinding.ActivityAdminFeedbackBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AdminFeedbackActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminFeedbackBinding
    private val db = Firebase.firestore
    private val adminUid = "NvKPJHa85rfVgFId0r46FcKGq5u1"  // konsoldan kopyaladığın UID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1) Kimlik kontrolü
        val seenUid  = FirebaseAuth.getInstance().currentUser?.uid
        val adminUid = "NvKPJHa85rfVgFId0r46FcKGq5u1"

        if (seenUid != adminUid) {
            Toast.makeText(this, "Yetkiniz yok.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2) RecyclerView ayarları
        binding.recycler.layoutManager = LinearLayoutManager(this)
        val adapter = AdminFeedbackAdapter(emptyList())
        binding.recycler.adapter = adapter

        // 3) Firestore’dan feedbackleri çek
        db.collection("feedbacks")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snaps, err ->
                if (err != null) {
                    Toast.makeText(this, "Hata: ${err.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                val list = snaps
                    ?.documents
                    ?.map { d ->
                        FeedbackItem(
                            id        = d.id,
                            title     = d.getString("title") ?: "",
                            message   = d.getString("message") ?: "",
                            timestamp = d.getLong("timestamp") ?: 0L,
                            userEmail = d.getString("userEmail") ?: ""
                        )
                    }
                    ?: emptyList()
                adapter.submitList(list)
            }
    }
}