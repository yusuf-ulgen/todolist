package com.example.todolist

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.todolist.data.Todolist
import com.example.todolist.databinding.ActivityNewListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewListBinding
    val currentListId = intent.getLongExtra("currentListId", 0L) // default olarak 0 alıyoruz


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar ayarları
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish() // Geri tuşuna basınca activity'yi kapat
        }

        // "Yeni Liste" butonuna basıldığında listeyi kaydet ve ListelerimActivity'e dön
        binding.createButton.setOnClickListener {
            val listName = binding.listNameEditText.text.toString().trim()
            if (listName.isNotEmpty()) {
                saveList(listName)
            } else {
                Toast.makeText(this, "Liste adı boş olamaz!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveList(listName: String) {
        // Yeni bir liste oluştur
        val newList = Todolist(name = listName) // Burada boş bir liste oluşturuyoruz

        GlobalScope.launch(Dispatchers.IO) {
            // Listeyi veritabanına ekle
            AppDatabase.getDatabase(applicationContext).todolistDao().insert(newList)

            // Liste kaydedildikten sonra ListelerimActivity'ye dön
            withContext(Dispatchers.Main) {
                val intent = Intent(this@NewListActivity, ListelerimActivity::class.java)
                startActivity(intent) // Listelerim sayfasına geçiş
                finish() // Yeni liste sayfasını kapat
            }
        }
    }
}
