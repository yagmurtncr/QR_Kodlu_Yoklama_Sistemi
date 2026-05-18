package com.example.qr_kodlu_yoklama_sistemi.ui.student

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
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

        val userEmail = auth.currentUser?.email
        binding.tvWelcomeStudent.text = "Hoş Geldin,\n$userEmail"

        setupRecyclerView()
        fetchStudentAttendance()

        binding.btnScanQR.setOnClickListener {
            startActivity(Intent(this, QRScannerActivity::class.java))
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
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                attendanceList.clear()
                snapshots?.let {
                    for (doc in it) {
                        attendanceList.add(doc.data)
                    }
                    adapter.updateList(attendanceList)
                    updateChart(attendanceList.size)
                }
            }
    }

    private fun updateChart(attendanceCount: Int) {
        // Test verisi: Toplam 20 ders olduğunu varsayalım
        val totalLessons = 20
        val absentCount = totalLessons - attendanceCount

        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(attendanceCount.toFloat(), "Katılım"))
        entries.add(PieEntry(absentCount.toFloat(), "Devamsızlık"))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(Color.parseColor("#4CAF50"), Color.parseColor("#F44336"))
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 14f

        val data = PieData(dataSet)
        binding.pieChart.data = data
        binding.pieChart.centerText = "Yoklama Durumu"
        binding.pieChart.description.isEnabled = false
        binding.pieChart.animateY(1000)
        binding.pieChart.invalidate()
    }
}
