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
import com.example.qr_kodlu_yoklama_sistemi.location.LocationPolicy
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QRScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScannerBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var isScanning = true
    private var pendingLessonId: String? = null
    private var pendingTeacherLat: Double? = null
    private var pendingTeacherLon: Double? = null
    // Artırılmış tolerans demo sırasında konum sapmalarını azaltmak için
    private val maxAllowedDistanceMeters = 2000f

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
        val cameraPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (cameraPermissionGranted) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            val cameraGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (cameraGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Kamera izni olmadan QR taranamaz", Toast.LENGTH_LONG).show()
                finish()
            }
        } else if (requestCode == 101) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            val lessonId = pendingLessonId
            val teacherLat = pendingTeacherLat
            val teacherLon = pendingTeacherLon

            pendingLessonId = null
            pendingTeacherLat = null
            pendingTeacherLon = null

            if (granted && lessonId != null) {
                verifyLocationAndSave(lessonId, teacherLat, teacherLon)
            } else if (LocationPolicy.isEmulatorDevice() && lessonId != null) {
                saveAttendance(lessonId)
            } else {
                Toast.makeText(this, "Konum izni verilmedi", Toast.LENGTH_SHORT).show()
                isScanning = true
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
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
            BarcodeScanning.getClient().process(image).addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    if (barcode.valueType == Barcode.TYPE_TEXT) validateQRWithLocation(barcode.rawValue ?: "")
                }
            }.addOnCompleteListener { imageProxy.close() }
        } else { imageProxy.close() }
    }

    private fun validateQRWithLocation(qrToken: String) {
        isScanning = false
        db.collection("Lessons")
            .whereEqualTo("activeToken", qrToken)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                if (doc != null) {
                    val lessonId = doc.id
                    val tLat = doc.getDouble("latitude")
                    val tLon = doc.getDouble("longitude")
                    val lessonLocation = if (tLat != null && tLon != null) {
                        LocationPolicy.createLocation("lesson", tLat, tLon)
                    } else {
                        null
                    }
                    binding.tvLessonCoords.text = "Ders: ${LocationPolicy.describe(lessonLocation)}"
                    verifyLocationAndSave(lessonId, tLat, tLon)
                } else {
                    Toast.makeText(this, "Geçersiz QR Kod!", Toast.LENGTH_SHORT).show()
                    isScanning = true
                }
            }
            .addOnFailureListener { e ->
                Log.e("QRScanner", "Failed to validate QR token", e)
                Toast.makeText(this, "QR doğrulanamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                isScanning = true
            }
    }

    @SuppressLint("MissingPermission")
    private fun verifyLocationAndSave(lessonId: String, tLat: Double?, tLon: Double?) {
        val locationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!locationGranted) {
            pendingLessonId = lessonId
            pendingTeacherLat = tLat
            pendingTeacherLon = tLon
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
            isScanning = true
            Toast.makeText(this, "Konum izni gerekiyor", Toast.LENGTH_SHORT).show()
            return
        }

        if (tLat == null || tLon == null) {
            saveAttendance(lessonId)
            return
        }

        val cancellationTokenSource = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { sLoc: Location? ->
                val rawLocation = sLoc ?: run {
                    // Yedek olarak son bilinen konumu dene
                    fusedLocationClient.lastLocation.result
                }
                val locationToUse = LocationPolicy.resolveForDevice(rawLocation)

                if (locationToUse != null) {
                    // Show student coords
                    val sLatText = String.format(Locale.getDefault(), "%.6f", locationToUse.latitude)
                    val sLonText = String.format(Locale.getDefault(), "%.6f", locationToUse.longitude)
                    binding.tvStudentCoords.text = "Öğrenci: $sLatText, $sLonText"

                    val results = FloatArray(1)
                    Location.distanceBetween(tLat ?: locationToUse.latitude, tLon ?: locationToUse.longitude, locationToUse.latitude, locationToUse.longitude, results)
                    binding.tvDistance.text = "Mesafe: ${results[0].toInt()} m"
                    if (results[0] <= maxAllowedDistanceMeters) {
                        saveAttendance(lessonId)
                    } else {
                        Toast.makeText(this, "Sınıfta Değilsiniz! (Mesafe: ${results[0].toInt()} m)", Toast.LENGTH_LONG).show()
                        isScanning = true
                    }
                } else {
                    Toast.makeText(this, "Konum alınamadı!", Toast.LENGTH_SHORT).show()
                    isScanning = true
                }
            }
            .addOnFailureListener {
                fusedLocationClient.lastLocation.addOnSuccessListener { fallbackLoc: Location? ->
                    if (fallbackLoc != null) {
                        val results = FloatArray(1)
                        Location.distanceBetween(tLat, tLon, fallbackLoc.latitude, fallbackLoc.longitude, results)
                        if (results[0] <= maxAllowedDistanceMeters) saveAttendance(lessonId)
                        else {
                            Toast.makeText(this, "Sınıfta Değilsiniz! (Mesafe: ${results[0].toInt()} m)", Toast.LENGTH_LONG).show()
                            isScanning = true
                        }
                    } else {
                        Toast.makeText(this, "Konum alınamadı!", Toast.LENGTH_SHORT).show()
                        isScanning = true
                    }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) it.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") it.vibrate(100)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
