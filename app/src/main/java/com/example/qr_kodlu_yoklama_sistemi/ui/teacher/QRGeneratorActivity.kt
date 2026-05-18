package com.example.qr_kodlu_yoklama_sistemi.ui.teacher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.qr_kodlu_yoklama_sistemi.R
import com.example.qr_kodlu_yoklama_sistemi.databinding.ActivityQrGeneratorBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.text.SimpleDateFormat
import java.util.*

class QRGeneratorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrGeneratorBinding
    private val db = FirebaseFirestore.getInstance()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private var lessonId: String? = null
    private var lessonName: String? = null
    private var timeLeft = 20

    private val qrRefreshRunnable = object : Runnable {
        override fun run() {
            if (timeLeft <= 0) {
                generateAndUpdateQR()
                timeLeft = 20
            }
            binding.pbTimer.progress = (timeLeft * 5)
            timeLeft--
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrGeneratorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        lessonId = intent.getStringExtra("LESSON_ID") ?: "test_lesson"
        lessonName = intent.getStringExtra("LESSON_NAME") ?: "Genel Ders"

        binding.tvLessonName.text = lessonName
        binding.tvDateTime.text = SimpleDateFormat("dd MMMM yyyy | HH:mm", Locale("tr")).format(Date())

        saveTeacherLocation()
        handler.post(qrRefreshRunnable)

        binding.btnOpenWebQR.setOnClickListener {
            db.collection("Lessons").document(lessonId!!).update("webDisplayActive", true)
            Toast.makeText(this, "Web QR Aktif!", Toast.LENGTH_SHORT).show()
        }

        binding.btnViewList.setOnClickListener {
            val intent = Intent(this, AttendanceListActivity::class.java)
            intent.putExtra("LESSON_ID", lessonId)
            intent.putExtra("LESSON_NAME", lessonName)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    @SuppressLint("MissingPermission")
    private fun saveTeacherLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    db.collection("Lessons").document(lessonId!!).update(
                        "latitude", it.latitude,
                        "longitude", it.longitude
                    )
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }

    private fun generateAndUpdateQR() {
        val newToken = UUID.randomUUID().toString()
        try {
            val bitMatrix = MultiFormatWriter().encode(newToken, BarcodeFormat.QR_CODE, 512, 512)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap: Bitmap = barcodeEncoder.createBitmap(bitMatrix)
            binding.ivQrCode.setImageBitmap(bitmap)

            lessonId?.let { id ->
                db.collection("Lessons").document(id).update("activeToken", newToken)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(qrRefreshRunnable)
    }
}
