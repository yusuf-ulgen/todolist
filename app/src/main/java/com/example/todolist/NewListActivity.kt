package com.example.todolist

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.todolist.data.Todolist
import com.example.todolist.databinding.ActivityNewListBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("NAME_SHADOWING")
class NewListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(ThemeHelper.loadTheme(this))
        super.onCreate(savedInstanceState)
        binding = ActivityNewListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent ile gelen değerleri burada alıyoruz
        val currentListId = intent.getLongExtra("listId", 1L)
        val listName      = intent.getStringExtra("listName") ?: "Yeni Liste"

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = listName

        // FAB’e tıklandığında bu listeye ait işlemler (örneğin yeni göreve geçiş) buraya
        binding.fab.setOnClickListener {
            // Örnek: MainActivity’ye geri dönerken listId’yi iletmek isterseniz:
            val i = Intent(this, MainActivity::class.java)
            i.putExtra("listId", currentListId)
            startActivity(i)
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

    @OptIn(DelicateCoroutinesApi::class)
    private fun saveList(listName: String) {
        // Yeni bir liste oluştur
        val newList = Todolist(name = listName) // Burada boş bir liste oluşturuyoruz

        GlobalScope.launch(Dispatchers.IO) {
            // Listeyi veritabanına ekle
            AppDatabase.getDatabase(applicationContext).todolistDao().insert(newList)

            // Liste kaydedildikten sonra ListelerimActivity'ye dön
            withContext(Dispatchers.Main) {
                val intent = Intent(this@NewListActivity, ListelerimActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
            }
        }
    }
}