package com.example.qr_kodlu_yoklama_sistemi.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.qr_kodlu_yoklama_sistemi.R
import com.example.qr_kodlu_yoklama_sistemi.databinding.ActivityLoginBinding
import com.example.qr_kodlu_yoklama_sistemi.ui.admin.AdminHomeActivity
import com.example.qr_kodlu_yoklama_sistemi.ui.teacher.TeacherHomeActivity
import com.example.qr_kodlu_yoklama_sistemi.ui.student.StudentHomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
            overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out)
        }

        binding.tvBackToSelection.setOnClickListener {
            finish()
        }
    }

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Geçerli bir e-posta giriniz"
            return
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Şifre boş bırakılamaz"
            return
        }

        // Firebase Auth Giriş
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    if (user.isEmailVerified) {
                        // Email onaylıysa rolünü kontrol et ve yönlendir
                        checkUserRoleAndRedirect(user.uid)
                    } else {
                        Toast.makeText(this, "Lütfen e-postanızı onaylayın!", Toast.LENGTH_LONG).show()
                        auth.signOut()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Giriş Başarısız: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun checkUserRoleAndRedirect(uid: String) {
        db.collection("Users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role")
                    val intent = when (role) {
                        "Admin" -> Intent(this, AdminHomeActivity::class.java)
                        "Teacher" -> Intent(this, TeacherHomeActivity::class.java)
                        else -> Intent(this, StudentHomeActivity::class.java)
                    }
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                } else {
                    // Eğer Users koleksiyonunda doküman yoksa, otomatik olarak basit bir kullanıcı dokümanı oluşturmayı dene
                    Log.i("LoginActivity", "User document not found for uid=$uid — creating default user doc")
                    val current = auth.currentUser
                    val email = current?.email ?: ""
                    val fullName = when {
                        !current?.displayName.isNullOrBlank() -> current?.displayName ?: ""
                        email.contains("@") -> email.substringBefore("@")
                        else -> "Öğrenci"
                    }
                    val userMap = hashMapOf<String, Any>(
                        "uid" to uid,
                        "email" to email,
                        "fullName" to fullName,
                        "name" to fullName,
                        "studentName" to fullName,
                        "role" to "Student"
                    )
                    db.collection("Users").document(uid).set(userMap)
                        .addOnSuccessListener {
                            // Oluşturduktan sonra normal yönlendirmeyi tekrar dene
                            val intent = Intent(this, StudentHomeActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e("LoginActivity", "Failed to create user document for uid=$uid: ${e.message}")
                            Toast.makeText(this, "Kullanıcı verisi oluşturulamadı: ${e.message}", Toast.LENGTH_LONG).show()
                            auth.signOut()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("LoginActivity", "Failed to read user document for uid=$uid: ${e.message}")
                Toast.makeText(this, "Kullanıcı bilgisi okunamadı: ${e.message}", Toast.LENGTH_LONG).show()
                auth.signOut()
            }
    }
}
