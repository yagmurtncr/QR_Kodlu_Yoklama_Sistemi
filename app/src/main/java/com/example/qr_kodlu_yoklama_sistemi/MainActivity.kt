package com.example.qr_kodlu_yoklama_sistemi

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.qr_kodlu_yoklama_sistemi.ui.admin.AdminHomeActivity
import com.example.qr_kodlu_yoklama_sistemi.ui.auth.AuthSelectionActivity
import com.example.qr_kodlu_yoklama_sistemi.ui.student.StudentHomeActivity
import com.example.qr_kodlu_yoklama_sistemi.ui.teacher.TeacherHomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null) {
            startActivity(Intent(this, AuthSelectionActivity::class.java))
            finish()
        } else {
            // Kullanıcı zaten giriş yapmışsa rolüne bak
            val db = FirebaseFirestore.getInstance()
            db.collection("Users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val role = doc.getString("role")
                        val intent = when (role) {
                            "Admin" -> Intent(this, AdminHomeActivity::class.java)
                            "Teacher" -> Intent(this, TeacherHomeActivity::class.java)
                            else -> Intent(this, StudentHomeActivity::class.java)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        auth.signOut()
                        startActivity(Intent(this, AuthSelectionActivity::class.java))
                        finish()
                    }
                }
                .addOnFailureListener {
                    startActivity(Intent(this, AuthSelectionActivity::class.java))
                    finish()
                }
        }
    }
}
