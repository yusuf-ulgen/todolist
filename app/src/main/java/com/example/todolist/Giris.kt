package com.example.todolist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.todolist.databinding.ActivityGirisBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging

class Giris : AppCompatActivity() {

    private lateinit var binding: ActivityGirisBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGirisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1) FirebaseAuth'ı başlat
        mAuth = FirebaseAuth.getInstance()

        // 2) Eğer zaten login olmuşsa ListelerimActivity'ye git
        if (mAuth.currentUser != null) {
            startActivity(Intent(this, ListelerimActivity::class.java))
            finish()
            return
        }

        // Google Sign-In ayarları
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Buton click listener'ları
        binding.girisyapId.setOnClickListener { onLoginClicked() }
        binding.kaydetId.setOnClickListener { onRegisterClicked() }
        binding.signInButton.setOnClickListener { signIn() }
    }

    private fun onLoginClicked() {
        val email = binding.mailId.text.toString().trim()
        val password = binding.sifreId.text.toString().trim()
        clearFieldErrors()

        var valid = true
        if (email.isEmpty()) {
            showFieldError(binding.mailId, "Bu alan boş bırakılamaz")
            valid = false
        }
        if (password.isEmpty()) {
            showFieldError(binding.sifreId, "Bu alan boş bırakılamaz")
            valid = false
        }
        if (!valid) return

        loginUser(email, password)
    }

    private fun onRegisterClicked() {
        val email = binding.mailId.text.toString().trim()
        val password = binding.sifreId.text.toString().trim()
        clearFieldErrors()

        var valid = true
        if (email.isEmpty()) {
            showFieldError(binding.mailId, "Bu alan boş bırakılamaz")
            valid = false
        }
        if (password.isEmpty()) {
            showFieldError(binding.sifreId, "Bu alan boş bırakılamaz")
            valid = false
        }
        if (!valid) return

        registerUser(email, password)
    }

    private fun clearFieldErrors() {
        binding.mailId.error = null
        binding.mailId.setCompoundDrawables(null, null, null, null)
        binding.sifreId.error = null
        binding.sifreId.setCompoundDrawables(null, null, null, null)
    }

    private fun showFieldError(field: android.widget.EditText, message: String) {
        val icon = ContextCompat.getDrawable(this, android.R.drawable.ic_dialog_alert)
        icon?.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
        field.error = message
        field.setCompoundDrawables(icon, null, null, null)
        field.requestFocus()
    }

    private fun signIn() {
        startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                showFieldError(binding.mailId, "Google giriş hatası: ${e.message}")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToMain()
                } else {
                    showFieldError(binding.mailId, "Giriş başarısız: ${task.exception?.message}")
                }
            }
    }

    private fun registerUser(email: String, password: String) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToMain()
                } else {
                    showFieldError(binding.mailId, "Kayıt hatası: ${task.exception?.message}")
                }
            }
    }

    private fun loginUser(email: String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToMain()
                } else {
                    showFieldError(binding.mailId, "Giriş hatası: ${task.exception?.message}")
                }
            }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, ListelerimActivity::class.java))
        finish()
    }

    private fun showNotificationDialog() {
        val root = binding.root
        val options = arrayOf("Bildirim Gönderilsin", "Sadece Önemliler Gönderilsin", "Gönderilmesin")
        AlertDialog.Builder(this)
            .setTitle("Bildirim Seçenekleri")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> requestNotificationPermission(NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    1 -> requestNotificationPermission(NotificationManagerCompat.IMPORTANCE_HIGH)
                    2 -> Snackbar.make(root, "Bildirimler devre dışı", Snackbar.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun requestNotificationPermission(priority: Int) {
        val root = binding.root
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            Snackbar.make(root, "Bildirimler zaten aktif.", Snackbar.LENGTH_SHORT).show()
        } else {
            FirebaseMessaging.getInstance().subscribeToTopic("all_notifications")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        saveNotificationPreference(priority)
                        Snackbar.make(root, "Bildirim tercihiniz kaydedildi.", Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(root, "Bildirim aboneliği başarısız.", Snackbar.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun saveNotificationPreference(priority: Int) {
        val prefs = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        prefs.edit().putInt("notification_priority", priority).apply()
    }
}