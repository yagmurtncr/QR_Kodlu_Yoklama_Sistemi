package com.example.qr_kodlu_yoklama_sistemi.ui.teacher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.qr_kodlu_yoklama_sistemi.R
import com.example.qr_kodlu_yoklama_sistemi.databinding.ActivityQrGeneratorBinding
import com.example.qr_kodlu_yoklama_sistemi.location.LocationPolicy
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.firestore.SetOptions
import java.util.Locale
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.text.SimpleDateFormat
import java.util.*

class QRGeneratorActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1001
    }

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
                .addOnSuccessListener {
                    val webUrl = buildWebQrUrl(lessonId!!)
                    Toast.makeText(this, "Web QR aktif edildi. URL: $webUrl", Toast.LENGTH_LONG).show()
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Web QR aktif edilemedi: ${e.message}", Toast.LENGTH_LONG).show()
                }
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
        if (LocationPolicy.isLocationPermissionGranted(this)) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                val resolvedLocation = LocationPolicy.resolveForDevice(location)
                if (resolvedLocation != null) {
                    persistTeacherLocation(resolvedLocation)
                } else {
                    requestTeacherCurrentLocation()
                }
            }.addOnFailureListener {
                requestTeacherCurrentLocation()
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestTeacherCurrentLocation() {
        val cts = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { curLoc: Location? ->
                val resolvedLocation = LocationPolicy.resolveForDevice(curLoc)
                if (resolvedLocation != null) {
                    persistTeacherLocation(resolvedLocation)
                } else {
                    if (LocationPolicy.isEmulatorDevice()) {
                        persistTeacherLocation(LocationPolicy.defaultDemoLocation())
                    } else {
                        binding.tvTeacherCoords.text = "Konum: alınamadı"
                    }
                }
            }
            .addOnFailureListener {
                if (LocationPolicy.isEmulatorDevice()) {
                    persistTeacherLocation(LocationPolicy.defaultDemoLocation())
                } else {
                    binding.tvTeacherCoords.text = "Konum: alınamadı"
                }
            }
    }

    private fun persistTeacherLocation(location: Location) {
        val lat = location.latitude
        val lon = location.longitude
        binding.tvTeacherCoords.text = "Konum: ${LocationPolicy.describe(location)}"
        db.collection("Lessons").document(lessonId!!)
            .set(
                mapOf(
                    "latitude" to lat,
                    "longitude" to lon
                ),
                SetOptions.merge()
            )
            .addOnFailureListener {
                Toast.makeText(this, "Öğretmen konumu kaydedilemedi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                saveTeacherLocation()
            } else if (LocationPolicy.isEmulatorDevice()) {
                persistTeacherLocation(LocationPolicy.defaultDemoLocation())
            } else {
                binding.tvTeacherCoords.text = "Konum: izin verilmedi"
                Toast.makeText(this, "Konum izni olmadan öğretmen konumu kaydedilemez", Toast.LENGTH_SHORT).show()
            }
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

    private fun buildWebQrUrl(lessonId: String): String {
        return "https://qr-kodlu-yoklama-takip-sistemi.web.app/?lessonId=$lessonId"
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(qrRefreshRunnable)
    }
}
