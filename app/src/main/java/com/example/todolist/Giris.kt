package com.example.todolist

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

class Giris : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1   // Google Sign-in için request code

    // Değişkenler için findViewById kullanacağız
    private lateinit var signInButton: Button
    private lateinit var kaydetId: Button
    private lateinit var mailId: EditText
    private lateinit var sifreId: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_giris)

        // findViewById kullanarak UI elemanlarını tanımlayalım
        signInButton = findViewById(R.id.signInButton)
        kaydetId = findViewById(R.id.kaydet_id)
        mailId = findViewById(R.id.mail_id)
        sifreId = findViewById(R.id.sifre_id)

        mAuth = FirebaseAuth.getInstance()

        // Google Sign-In için ayarları yapalım
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))  // Bu web client ID'sini Firebase Console'dan alacaksınız.
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Google ile giriş butonuna tıklama işlemi
        signInButton.setOnClickListener {
            signIn() // Google ile giriş yapma fonksiyonunu çağırıyoruz
        }

        // Kaydol butonuna tıklama işlemi
        kaydetId.setOnClickListener {
            val email = mailId.text.toString()
            val password = sifreId.text.toString()

            // Eğer kullanıcı zaten giriş yapmışsa
            val user = mAuth.currentUser
            if (email.isNotEmpty() && password.isNotEmpty()) {
                mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Kayıt başarılı, MainActivity'ye yönlendir
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish() // Giris Activity'sini kapat
                        } else {
                            // Kayıt başarısız oldu
                            Toast.makeText(this, "Kayıt sırasında hata: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Google ile giriş yapmak için çağrılacak fonksiyon
    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    // OnActivityResult, kullanıcı Google ile giriş yaptıktan sonra çalışacak
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)  // Google hesabı ile Firebase'e giriş
            } catch (e: ApiException) {
                // Google login error handling
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
                    // Giriş başarılı, kullanıcıyı anasayfaya yönlendiriyoruz
                    val user = mAuth.currentUser
                    Toast.makeText(this, "Hoş geldiniz, ${user?.displayName}", Toast.LENGTH_SHORT).show()
                    // Ana aktiviteye yönlendirme
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Giriş yapıldığında bu aktiviteyi kapatıyoruz
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
                    Toast.makeText(
                        this,
                        "Kayıt başarılı, hoş geldiniz ${user?.email}",
                        Toast.LENGTH_SHORT
                    ).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Kayıt işlemi sonrası bu sayfayı kapatıyoruz
                } else {
                    // Kayıt başarısız oldu
                    Toast.makeText(
                        this,
                        "Kayıt sırasında hata: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}
