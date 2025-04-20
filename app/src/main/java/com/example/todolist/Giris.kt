package com.example.todolist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.AuthResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.common.api.ApiException
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging

class Giris : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1   // Google Sign-in için request code

    // Değişkenler için findViewById kullanacağız
    private lateinit var kaydetId: Button
    private lateinit var girisyapId: Button
    private lateinit var signInButton: Button
    private lateinit var mailId: EditText
    private lateinit var sifreId: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_giris)

        kaydetId = findViewById(R.id.kaydet_id)
        girisyapId = findViewById(R.id.girisyap_id)
        signInButton = findViewById(R.id.signInButton)
        mailId = findViewById(R.id.mail_id)
        sifreId = findViewById(R.id.sifre_id)

        mAuth = FirebaseAuth.getInstance()

        // Google Sign-In için ayarları yapalım
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Bu web client ID'sini Firebase Console'dan alacaksınız.
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Giriş Yap butonuna tıklama işlemi
        girisyapId.setOnClickListener {
            val email = mailId.text.toString()
            val password = sifreId.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
            }
        }

        // Kaydol butonuna tıklama işlemi
        kaydetId.setOnClickListener {
            val email = mailId.text.toString()
            val password = sifreId.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                registerUser(email, password)
            } else {
                Toast.makeText(this, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
            }
        }

        // Google ile giriş butonuna tıklama işlemi
        signInButton.setOnClickListener {
            signIn() // Google ile giriş yapma fonksiyonunu çağırıyoruz
        }
    }

    // Google ile giriş yapmak için çağrılacak fonksiyon
    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google giriş hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Google ile giriş yaptıktan sonra Firebase ile kimlik doğrulama
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, OnCompleteListener<AuthResult> { task ->
                if (task.isSuccessful) {
                    // Giriş başarılı, kullanıcıyı MainActivity'ye yönlendiriyoruz
                    val user = mAuth.currentUser
                    Toast.makeText(this, "Hoş geldiniz, ${user?.displayName}", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Giriş yapıldığında bu sayfayı kapatıyoruz
                } else {
                    // Giriş başarısız oldu
                    Toast.makeText(this, "Giriş başarısız", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Firebase ile e-posta ve şifre ile kullanıcı kaydetme işlemi
    private fun registerUser(email: String, password: String) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Kayıt başarılı
                    val user = mAuth.currentUser
                    Toast.makeText(this, "Kayıt başarılı, hoş geldiniz ${user?.email}", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Kayıt işlemi sonrası bu sayfayı kapatıyoruz
                } else {
                    // Kayıt başarısız oldu
                    Toast.makeText(this, "Kayıt sırasında hata: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Firebase ile giriş işlemi
    private fun loginUser(email: String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Giriş başarılı
                    val user = mAuth.currentUser
                    Toast.makeText(this, "Hoş geldiniz ${user?.email}", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Giriş yapıldığında bu sayfayı kapatıyoruz
                } else {
                    // Giriş başarısız oldu
                    Toast.makeText(this, "Giriş sırasında hata: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }


    // Bildirim izinlerini sormak için AlertDialog gösterme
    private fun showNotificationDialog() {
        val options = arrayOf("Bildirim Gönderilsin", "Sadece Önemliler Gönderilsin", "Gönderilmesin")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Bildirim Seçenekleri")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Tüm bildirimler gönderilsin
                        requestNotificationPermission(NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    }
                    1 -> {
                        // Önemli bildirimler gönderilsin
                        requestNotificationPermission(NotificationManagerCompat.IMPORTANCE_HIGH)
                    }
                    2 -> {
                        // Bildirim gönderilmesin
                        // Burada bildirim izni istemeyebilirsiniz, çünkü bildirimler devre dışı
                        Toast.makeText(this, "Bildirimler devre dışı", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    // Kullanıcı izni almak için NotificationManager kullanarak işlemi başlat
    private fun requestNotificationPermission(priority: Int) {
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            // Bildirimler zaten açık, buraya ek bir işlem gerek yok
            Toast.makeText(this, "Bildirimler aktif.", Toast.LENGTH_SHORT).show()
        } else {
            // Bildirim izni almak için Firebase Cloud Messaging (FCM) ile ayar yapılabilir
            FirebaseMessaging.getInstance().subscribeToTopic("all_notifications")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Bildirim tercihini kaydediyoruz (SharedPreferences kullanabilirsiniz)
                        saveNotificationPreference(priority)
                    }
                }
        }
    }

    // Bildirim tercihini kaydetmek için SharedPreferences kullanma
    private fun saveNotificationPreference(priority: Int) {
        val sharedPreferences = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("notification_priority", priority)
        editor.apply()
    }
}
