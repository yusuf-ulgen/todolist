package com.example.todolist

import android.content.Intent
import android.os.Bundle
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
            // 1) Alanlardaki önceki hataları temizle
            binding.oldPassword.error = null
            binding.newPassword.error = null
            binding.confirmPassword.error = null

            val oldPassword     = binding.oldPassword.text.toString().trim()
            val newPassword     = binding.newPassword.text.toString().trim()
            val confirmPassword = binding.confirmPassword.text.toString().trim()

            // 2) Basit validasyon
            var valid = true
            if (oldPassword.isEmpty()) {
                binding.oldPassword.error = "Eski şifreyi giriniz"
                binding.oldPassword.requestFocus()
                valid = false
            }
            if (newPassword.isEmpty()) {
                binding.newPassword.error = "Yeni şifreyi giriniz"
                if (valid) binding.newPassword.requestFocus()
                valid = false
            }
            if (confirmPassword.isEmpty()) {
                binding.confirmPassword.error = "Onay şifresini giriniz"
                if (valid) binding.confirmPassword.requestFocus()
                valid = false
            }
            if (valid && newPassword != confirmPassword) {
                binding.confirmPassword.error = "Yeni şifreler uyuşmuyor"
                binding.confirmPassword.requestFocus()
                valid = false
            }
            if (!valid) return@setOnClickListener

            // 3) Kullanıcıyı yeniden doğrula
            val user = mAuth.currentUser ?: return@setOnClickListener
            val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
            user.reauthenticate(credential).addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    // 4) Şifre güncelle
                    user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            // Başarılıysa giriş ekranına dön
                            val intent = Intent(this, Giris::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            binding.newPassword.error = "Şifre değiştirilemedi: ${updateTask.exception?.message}"
                            binding.newPassword.requestFocus()
                        }
                    }
                } else {
                    binding.oldPassword.error = "Eski şifre yanlış"
                    binding.oldPassword.requestFocus()
                }
            }
        }
    }
}