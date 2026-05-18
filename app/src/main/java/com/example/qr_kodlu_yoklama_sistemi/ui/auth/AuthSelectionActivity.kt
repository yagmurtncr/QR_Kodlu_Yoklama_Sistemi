package com.example.qr_kodlu_yoklama_sistemi.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.qr_kodlu_yoklama_sistemi.databinding.ActivityAuthSelectionBinding

class AuthSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Giriş Yap butonuna tıklandığında
        binding.btnGoToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // Kayıt Ol butonuna tıklandığında
        binding.btnGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}