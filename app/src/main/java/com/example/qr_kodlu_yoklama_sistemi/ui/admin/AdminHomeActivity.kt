package com.example.qr_kodlu_yoklama_sistemi.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.qr_kodlu_yoklama_sistemi.databinding.ActivityAdminHomeBinding
import com.google.firebase.auth.FirebaseAuth

class AdminHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminHomeBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardManageLessons.setOnClickListener {
            // Ders atama ekranına git (Henüz oluşturmadık)
            startActivity(Intent(this, AssignLessonActivity::class.java))
        }

        binding.cardManageUsers.setOnClickListener {
            Toast.makeText(this, "Kullanıcı listesi yakında eklenecek", Toast.LENGTH_SHORT).show()
        }
    }
}
