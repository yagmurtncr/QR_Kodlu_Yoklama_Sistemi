package com.example.qr_kodlu_yoklama_sistemi.ui.student

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.qr_kodlu_yoklama_sistemi.databinding.ActivityQrScannerBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QRScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScannerBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var isScanning = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkPermissionsAndStartCamera()
        binding.btnCloseScanner.setOnClickListener { finish() }
    }

    private fun checkPermissionsAndStartCamera() {
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, permissions, 100)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy -> processImageProxy(imageProxy) }
                }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
            } catch (exc: Exception) { Log.e("QRScanner", "Camera Error", exc) }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && isScanning) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            BarcodeScanning.getClient().process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        if (barcode.valueType == Barcode.TYPE_TEXT) {
                            validateQRWithLocation(barcode.rawValue ?: "")
                        }
                    }
                }
                .addOnCompleteListener { imageProxy.close() }
        } else { imageProxy.close() }
    }

    private fun validateQRWithLocation(qrToken: String) {
        isScanning = false
        val lessonId = intent.getStringExtra("LESSON_ID") ?: "test_lesson"

        db.collection("Lessons").document(lessonId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                if (doc.getString("activeToken") == qrToken) {
                    verifyLocationAndSave(lessonId, doc.getDouble("latitude"), doc.getDouble("longitude"))
                } else {
                    Toast.makeText(this, "Geçersiz QR Kod!", Toast.LENGTH_SHORT).show()
                    isScanning = true
                }
            }
        }.addOnFailureListener { isScanning = true }
    }

    @SuppressLint("MissingPermission")
    private fun verifyLocationAndSave(lessonId: String, tLat: Double?, tLon: Double?) {
        if (tLat == null || tLon == null) {
            saveAttendance(lessonId)
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { sLoc: Location? ->
            if (sLoc != null) {
                val results = FloatArray(1)
                Location.distanceBetween(tLat, tLon, sLoc.latitude, sLoc.longitude, results)
                if (results[0] <= 100) saveAttendance(lessonId)
                else {
                    Toast.makeText(this, "Sınıf dışında yoklama alınamaz! (${results[0].toInt()}m)", Toast.LENGTH_LONG).show()
                    isScanning = true
                }
            } else {
                Toast.makeText(this, "Konum alınamadı!", Toast.LENGTH_SHORT).show()
                isScanning = true
            }
        }
    }

    private fun saveAttendance(lessonId: String) {
        provideHapticFeedback()
        val attendanceData = hashMapOf(
            "userId" to auth.currentUser?.uid,
            "lessonId" to lessonId,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "status" to "Var"
        )
        db.collection("Attendances").add(attendanceData).addOnSuccessListener {
            Toast.makeText(this, "Yoklama Onaylandı! ✅", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun provideHapticFeedback() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") it.vibrate(100)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
