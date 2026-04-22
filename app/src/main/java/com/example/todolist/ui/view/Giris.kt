@file:Suppress("DEPRECATION")
package com.example.todolist

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.util.Log
import com.example.todolist.databinding.ActivityGirisBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
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
        com.example.todolist.WindowInsetsHelper.applyTopBottomInsets(binding.root)

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
        Log.d("GirisLog", "signIn() başlatıldı")
        googleSignInClient.signOut().addOnCompleteListener {
            Log.d("GirisLog", "Eski oturum kapatıldı, intent başlatılıyor")
            startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("GirisLog", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("GirisLog", "Google hesabı alındı: ${account.email}")
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.e("GirisLog", "Google Sign-In hatası (ApiException): StatusCode=${e.statusCode}, Mesaj=${e.message}")
                val friendlyMsg = getGoogleFriendlyMessage(e)
                showFieldError(binding.mailId, friendlyMsg)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        Log.d("GirisLog", "Firebase auth başlatılıyor. Token: ${idToken.take(10)}...")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("GirisLog", "Firebase auth başarılı!")
                    navigateToMain()
                } else {
                    Log.e("GirisLog", "Firebase auth hatası!", task.exception)
                    showFieldError(binding.mailId, getFriendlyMessage(task.exception))
                }
            }
    }

    private fun registerUser(email: String, password: String) {
        val finalEmail = if (!email.contains("@")) "$email@gmail.com" else email
        mAuth.createUserWithEmailAndPassword(finalEmail, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToMain()
                } else {
                    showFieldError(binding.mailId, getFriendlyMessage(task.exception))
                }
            }
    }

    private fun loginUser(email: String, password: String) {
        val finalEmail = if (!email.contains("@")) "$email@gmail.com" else email
        mAuth.signInWithEmailAndPassword(finalEmail, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToMain()
                } else {
                    showFieldError(binding.mailId, getFriendlyMessage(task.exception))
                }
            }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, ListelerimActivity::class.java))
        finish()
    }

    private fun getFriendlyMessage(exception: Exception?): String {
        if (exception == null) return "Bilinmeyen bir hata oluştu."

        if (exception is FirebaseAuthException) {
            return when (exception.errorCode) {
                "ERROR_INVALID_EMAIL" -> "Geçersiz e-posta adresi."
                "ERROR_USER_NOT_FOUND", "user-not-found" -> "Bu e-posta adresiyle kayıtlı bir hesap bulunamadı."
                "ERROR_WRONG_PASSWORD", "wrong-password" -> "E-posta veya şifre hatalı."
                "ERROR_EMAIL_ALREADY_IN_USE" -> "Bu e-posta adresi zaten başka bir hesap tarafından kullanılıyor."
                "ERROR_WEAK_PASSWORD" -> "Şifreniz çok zayıf. En az 6 karakter belirleyin."
                "ERROR_USER_DISABLED" -> "Hesabınız dondurulmuş. Lütfen destekle iletişime geçin."
                "ERROR_TOO_MANY_REQUESTS" -> "Çok fazla başarısız deneme yaptınız. Lütfen bir süre sonra tekrar deneyin."
                "ERROR_NETWORK_REQUEST_FAILED" -> "İnternet bağlantınızı kontrol edin."
                "ERROR_INVALID_CREDENTIAL" -> "Hatalı şifre veya e-posta girdiniz."
                "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "Bu e-posta adresi ile zaten farklı bir yöntemle (örn. Google) kayıt olunmuş."
                else -> "Bir hata oluştu: ${exception.localizedMessage}"
            }
        }

        val msg = exception.message ?: ""
        val lowerMsg = msg.lowercase()
        return when {
            lowerMsg.contains("invalid-email") || lowerMsg.contains("badly formatted") -> "Lütfen geçerli bir e-posta adresi girin."
            lowerMsg.contains("user-not-found") || lowerMsg.contains("no user record") || lowerMsg.contains("user_not_found") -> "Bu e-posta adresiyle kayıtlı bir kullanıcı bulunamadı."
            lowerMsg.contains("wrong-password") || lowerMsg.contains("invalid-credential") || lowerMsg.contains("invalid credential") || lowerMsg.contains("invalid_credential") -> "Hatalı şifre veya e-posta girdiniz."
            lowerMsg.contains("email-already-in-use") || lowerMsg.contains("already in use") -> "Bu e-posta adresi zaten kullanımda."
            lowerMsg.contains("weak-password") -> "Şifreniz çok zayıf. En az 6 karakter belirleyin."
            lowerMsg.contains("network-request-failed") -> "İnternet bağlantınızı kontrol edin."
            else -> "İşlem başarısız oldu: ${exception.localizedMessage ?: "Lütfen bilgilerinizi kontrol edip tekrar deneyiniz."}"
        }
    }

    private fun getGoogleFriendlyMessage(e: ApiException): String {
        return when (e.statusCode) {
            10 -> "Google Giriş Hatası (10): Uygulama yapılandırması hatalı. Lütfen SHA-1 sertifikasını ve Web Client ID'yi kontrol edin."
            7 -> "Ağ hatası: İnternet bağlantınız yok veya Google sunucularına erişilemiyor."
            12501 -> "Giriş işlemi iptal edildi."
            12500 -> "Google Play Hizmetleri hatası. Lütfen hizmetlerin güncel olduğundan emin olun."
            12502 -> "Giriş işlemi zaten devam ediyor."
            else -> "Google ile giriş yapılamadı (Hata Kodu: ${e.statusCode})"
        }
    }
}