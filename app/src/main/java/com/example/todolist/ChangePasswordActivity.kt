package com.example.todolist

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.todolist.databinding.ActivityChangePasswordBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()

        // Şifreyi değiştirme butonuna tıklama işlemi
        binding.savePasswordButton.setOnClickListener {
            val oldPassword = binding.oldPassword.text.toString().trim()
            val newPassword = binding.newPassword.text.toString().trim()
            val confirmPassword = binding.confirmPassword.text.toString().trim()

            if (newPassword == confirmPassword) {
                val user = mAuth.currentUser

                if (user != null && oldPassword.isNotEmpty() && newPassword.isNotEmpty()) {
                    // Eski şifreyi doğrulama
                    val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)

                    user.reauthenticate(credential).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Eski şifre doğrulandı, şifreyi değiştirme işlemi
                            user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    // Şifre başarılı şekilde değiştirildi
                                    Toast.makeText(this, "Şifre başarıyla değiştirildi", Toast.LENGTH_SHORT).show()

                                    // Kullanıcıyı giriş ekranına yönlendir
                                    val intent = Intent(this, Giris::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()  // Bu aktiviteyi kapatıyoruz
                                } else {
                                    // Şifre değiştirilemediyse
                                    Toast.makeText(this, "Şifre değiştirilemedi: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            // Eski şifre doğru değil
                            Toast.makeText(this, "Eski şifre yanlış", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                // Şifreler uyuşmuyor
                Toast.makeText(this, "Yeni şifreler uyuşmuyor", Toast.LENGTH_SHORT).show()
            }
        }
    }
}