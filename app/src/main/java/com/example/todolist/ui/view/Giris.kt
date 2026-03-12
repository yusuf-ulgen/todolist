@file:Suppress("DEPRECATION")
package com.example.todolist

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.todolist.databinding.ActivityGirisBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@Suppress("DEPRECATION")
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
        googleSignInClient.signOut().addOnCompleteListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
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
}