package com.example.qr_kodlu_yoklama_sistemi.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.qr_kodlu_yoklama_sistemi.R
import com.example.qr_kodlu_yoklama_sistemi.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    }

    private fun setupUniversitySpinner() {
        val universities = arrayOf("Üniversite Seçiniz", "İzmir Ekonomi Üniversitesi", "Ege Üniversitesi", "Dokuz Eylül Üniversitesi", "İstanbul Teknik Üniversitesi")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, universities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerUniversity.adapter = adapter
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.registerForm.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun performRegistration() {
        val university = binding.spinnerUniversity.selectedItem.toString()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val passwordConfirm = binding.etPasswordConfirm.text.toString().trim()
        val role = if (binding.rbTeacher.isChecked) "Teacher" else "Student"
        val studentNumber = binding.etStudentNumber.text.toString().trim()

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
            .addOnSuccessListener { result ->
                val user = result.user
                user?.sendEmailVerification()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        saveUserToFirestore(user.uid, email, university, role, studentNumber)
                    } else {
                        setLoading(false)
                        Toast.makeText(this, "Doğrulama e-postası gönderilemedi", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Kayıt Hatası: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveUserToFirestore(uid: String, email: String, university: String, role: String, studentNumber: String) {
        val userMap = mutableMapOf<String, Any>(
            "uid" to uid,
            "email" to email,
            "university" to university,
            "role" to role,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        if (role == "Student") userMap["studentNumber"] = studentNumber

        db.collection("Users").document(uid).set(userMap)
            .addOnSuccessListener {
                setLoading(false)
                Toast.makeText(this, "Kayıt Başarılı! Lütfen e-postanızı onaylayın.", Toast.LENGTH_LONG).show()
                auth.signOut()
                finish()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Veritabanı Hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
