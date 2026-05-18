package com.example.qr_kodlu_yoklama_sistemi.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.qr_kodlu_yoklama_sistemi.MainActivity
import com.example.qr_kodlu_yoklama_sistemi.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityRegisterBinding.inflate(layoutInflater)
            setContentView(binding.root)

            binding.rgRole.setOnCheckedChangeListener { _, checkedId ->
                if (checkedId == binding.rbStudent.id) {
                    binding.etStudentNumber.visibility = View.VISIBLE
                } else {
                    binding.etStudentNumber.visibility = View.GONE
                    binding.etStudentNumber.text?.clear()
                }
            }

            binding.btnRegister.setOnClickListener {
                performRegistration()
            }
        } catch (e: Exception) {
            Log.e("RegisterActivity", "onCreate error: ${e.message}")
            Toast.makeText(this, "Başlatma hatası oluştu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isFinishing) return
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.registerForm.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.btnRegister.isEnabled = !isLoading
    }

    private fun performRegistration() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val studentNumber = binding.etStudentNumber.text.toString().trim()
        val role = if (binding.rbTeacher.isChecked) "Teacher" else "Student"

        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Geçerli bir e-posta adresi giriniz!", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Şifre en az 6 karakter olmalıdır!", Toast.LENGTH_SHORT).show()
            return
        }

        if (role == "Student" && studentNumber.length != 9) {
            Toast.makeText(this, "Öğrenci numarası tam 9 haneli olmalıdır!", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (isFinishing) return@addOnCompleteListener
                
                if (task.isSuccessful) {
                    saveUserToFirestore(fullName, email, phone, role, studentNumber)
                } else {
                    setLoading(false)
                    val exception = task.exception
                    val message = when (exception) {
                        is FirebaseAuthUserCollisionException -> "Bu e-posta zaten kullanımda!"
                        else -> "Kayıt hatası: ${exception?.localizedMessage}"
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveUserToFirestore(fullName: String, email: String, phone: String, role: String, studentNumber: String) {
        val userId = auth.currentUser?.uid ?: run {
            setLoading(false)
            return
        }

        val userMap = mutableMapOf<String, Any>(
            "uid" to userId,
            "fullName" to fullName,
            "email" to email,
            "phone" to phone,
            "role" to role,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        if (role == "Student") {
            userMap["studentNumber"] = studentNumber
        }

        db.collection("Users").document(userId).set(userMap)
            .addOnSuccessListener {
                if (isFinishing) return@addOnSuccessListener
                setLoading(false)
                Toast.makeText(this, "Kayıt Başarılı!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                if (isFinishing) return@addOnFailureListener
                setLoading(false)
                Toast.makeText(this, "Veriler kaydedilemedi: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
