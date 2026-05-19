package com.example.qr_kodlu_yoklama_sistemi.ui.student

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qr_kodlu_yoklama_sistemi.databinding.ActivityStudentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class StudentHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentHomeBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: StudentAttendanceAdapter
    private val attendanceList = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadStudentName()

        setupRecyclerView()
        fetchStudentAttendance()

        // ID Düzeltildi: cardScanQR
        binding.cardScanQR.setOnClickListener {
            startActivity(Intent(this, QRScannerActivity::class.java))
        }
    }

    private fun loadStudentName() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("Users").document(uid).get()
            .addOnSuccessListener { doc ->
                val displayName = listOf(
                    doc.getString("fullName"),
                    doc.getString("studentName"),
                    doc.getString("name"),
                    auth.currentUser?.displayName,
                    auth.currentUser?.email?.substringBefore("@")
                ).firstOrNull { !it.isNullOrBlank() } ?: "Öğrenci"
                binding.tvWelcomeStudent.text = "Hoş Geldin,\n$displayName"
            }
            .addOnFailureListener {
                val fallbackName = auth.currentUser?.displayName
                    ?: auth.currentUser?.email?.substringBefore("@")
                    ?: "Öğrenci"
                binding.tvWelcomeStudent.text = "Hoş Geldin,\n$fallbackName"
            }
    }

    private fun setupRecyclerView() {
        adapter = StudentAttendanceAdapter(attendanceList)
        binding.rvStudentAttendance.layoutManager = LinearLayoutManager(this)
        binding.rvStudentAttendance.adapter = adapter
    }

    private fun fetchStudentAttendance() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("Attendances")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, _ ->
                attendanceList.clear()
                snapshots?.let {
                    for (doc in it) { attendanceList.add(doc.data) }
                    adapter.updateList(attendanceList)
                    updateChart(attendanceList.size)
                }
            }
    }

    private fun updateChart(attendanceCount: Int) {
        val totalLessons = 20
        val absentCount = if (totalLessons > attendanceCount) totalLessons - attendanceCount else 0
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(attendanceCount.toFloat(), "Katılım"))
        entries.add(PieEntry(absentCount.toFloat(), "Devamsızlık"))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(Color.parseColor("#1A73E8"), Color.parseColor("#D93025"))
        dataSet.valueTextColor = Color.WHITE
        
        binding.pieChart.data = PieData(dataSet)
        binding.pieChart.centerText = "Yoklama"
        binding.pieChart.description.isEnabled = false
        binding.pieChart.animateY(1000)
        binding.pieChart.invalidate()
    }
}
