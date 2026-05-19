package com.example.qr_kodlu_yoklama_sistemi.ui.auth

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.qr_kodlu_yoklama_sistemi.R
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

            setupUniversitySpinner()

            binding.rgRole.setOnCheckedChangeListener { _, checkedId ->
                binding.etStudentNumber.visibility = if (checkedId == R.id.rbStudent) View.VISIBLE else View.GONE
            }

            binding.btnRegister.setOnClickListener {
                performRegistration()
            }
            
            binding.tvBackToLogin.setOnClickListener {
                finish()
            }
        } catch (e: Exception) {
            Log.e("RegisterActivity", "Binding error: ${e.message}")
        }
    }

    private fun setupUniversitySpinner() {
        val universities = arrayOf("Üniversite Seçiniz", "İzmir Ekonomi Üniversitesi", "Ege Üniversitesi", "Dokuz Eylül Üniversitesi", "İstanbul Teknik Üniversitesi")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, universities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerUniversity.adapter = adapter
    }

    private fun setLoading(isLoading: Boolean) {
        if (isFinishing) return
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.registerForm.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.btnRegister.isEnabled = !isLoading
    }

    private fun performRegistration() {
        val fullName = binding.etFullName.text.toString().trim()
        val university = binding.spinnerUniversity.selectedItem.toString()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val passwordConfirm = binding.etPasswordConfirm.text.toString().trim()
        val studentNumber = binding.etStudentNumber.text.toString().trim()
        val role = if (binding.rbTeacher.isChecked) "Teacher" else "Student"

        if (fullName.isEmpty()) {
            binding.etFullName.error = "Ad Soyad gereklidir"
            return
        }

        if (university == "Üniversite Seçiniz") {
            Toast.makeText(this, "Lütfen bir üniversite seçiniz", Toast.LENGTH_SHORT).show()
            return
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Geçerli bir e-posta giriniz"
            return
        }

        if (password.length < 6) {
            binding.etPassword.error = "Şifre en az 6 karakter olmalıdır"
            return
        }

        if (password != passwordConfirm) {
            binding.etPasswordConfirm.error = "Şifreler uyuşmuyor"
            return
        }

        if (role == "Student" && studentNumber.length != 9) {
            binding.etStudentNumber.error = "Öğrenci numarası 9 haneli olmalıdır"
            return
        }

        setLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (isFinishing) return@addOnCompleteListener
                
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                        if (verifyTask.isSuccessful) {
                            saveUserToFirestore(user.uid, fullName, email, university, role, studentNumber)
                        } else {
                            // Eğer onay maili gönderilemezse oluşturulan auth hesabını silerek rollback yap
                            user.delete().addOnCompleteListener {
                                setLoading(false)
                                Toast.makeText(this, "Onay maili gönderilemedi. Hesap silindi.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
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

    private fun saveUserToFirestore(uid: String, fullName: String, email: String, university: String, role: String, studentNumber: String) {
        val userMap = mutableMapOf<String, Any>(
            "uid" to uid,
            "fullName" to fullName,
            "email" to email,
            "university" to university,
            "role" to role,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        if (role == "Student") userMap["studentNumber"] = studentNumber

        db.collection("Users").document(uid).set(userMap)
            .addOnSuccessListener {
                if (isFinishing) return@addOnSuccessListener
                setLoading(false)
                Toast.makeText(this, "Kayıt Başarılı! Lütfen mailinizi onaylayın.", Toast.LENGTH_LONG).show()
                auth.signOut()
                finish()
            }
            .addOnFailureListener { e ->
                if (isFinishing) return@addOnFailureListener
                // Firestore yazılamadıysa oluşturulan auth hesabını sil (rollback)
                val current = auth.currentUser
                current?.delete()?.addOnCompleteListener {
                    setLoading(false)
                    Toast.makeText(this, "Veritabanı hatası: ${e.message}. Hesap silindi.", Toast.LENGTH_LONG).show()
                } ?: run {
                    setLoading(false)
                    Toast.makeText(this, "Veritabanı hatası: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}
