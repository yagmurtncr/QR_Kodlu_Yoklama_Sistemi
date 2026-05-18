package com.example.qr_kodlu_yoklama_sistemi.ui.teacher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qr_kodlu_yoklama_sistemi.R
import com.example.qr_kodlu_yoklama_sistemi.databinding.ActivityTeacherHomeBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TeacherHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTeacherHomeBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var lessonAdapter: LessonAdapter
    private val lessonList = mutableListOf<Map<String, Any>>()
    private var teacherUniversity: String = "Üniversite Bilgisi Yok"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        fetchTeacherProfile()
        fetchLessons()

        binding.cardStartAttendance.setOnClickListener {
            showAddLessonDialog()
        }

        binding.fabAddLesson.setOnClickListener {
            showAddLessonDialog()
        }
    }

    private fun fetchTeacherProfile() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("Users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val name = doc.getString("fullName") ?: "Hocam"
                teacherUniversity = doc.getString("university") ?: "Bilinmeyen Üniversite"
                binding.tvTeacherWelcome.text = "Merhaba, $name"
            }
        }
    }

    private fun setupRecyclerView() {
        lessonAdapter = LessonAdapter(lessonList) { lessonId, lessonName ->
            val intent = Intent(this, QRGeneratorActivity::class.java)
            intent.putExtra("LESSON_ID", lessonId)
            intent.putExtra("LESSON_NAME", lessonName)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out)
        }
        binding.rvTeacherLessons.layoutManager = LinearLayoutManager(this)
        binding.rvTeacherLessons.adapter = lessonAdapter
    }

    private fun fetchLessons() {
        val teacherId = auth.currentUser?.uid ?: return
        db.collection("Lessons")
            .whereEqualTo("teacherId", teacherId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                lessonList.clear()
                snapshots?.let {
                    for (doc in it) {
                        val data = doc.data.toMutableMap()
                        data["lessonId"] = doc.id
                        lessonList.add(data)
                    }
                    lessonAdapter.updateList(lessonList)
                    binding.tvTotalLessons.text = lessonList.size.toString()
                    updateBarChart()
                }
            }
    }

    private fun updateBarChart() {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        // Mock data for last 5 lessons performance
        val mockData = listOf(65f, 88f, 72f, 95f, 80f)
        val mockLabels = listOf("Pzt", "Sal", "Çar", "Per", "Cum")

        for (i in mockData.indices) {
            entries.add(BarEntry(i.toFloat(), mockData[i]))
            labels.add(mockLabels[i])
        }

        val dataSet = BarDataSet(entries, "Katılım Oranı (%)")
        dataSet.colors = listOf(Color.parseColor("#1A73E8"), Color.parseColor("#6C63FF"))
        dataSet.valueTextColor = Color.GRAY
        dataSet.valueTextSize = 10f

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f

        binding.barChart.apply {
            data = barData
            description.isEnabled = false
            legend.isEnabled = false
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                setDrawGridLines(false)
                granularity = 1f
                textColor = Color.GRAY
            }
            
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                textColor = Color.GRAY
            }
            axisRight.isEnabled = false
            
            animateY(1000)
            invalidate()
        }
    }

    private fun showAddLessonDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Yeni Ders Başlat")
        val input = EditText(this)
        input.hint = "Ders Adı"
        builder.setView(input)

        builder.setPositiveButton("Başlat") { _, _ ->
            val name = input.text.toString().trim()
            if (name.isNotEmpty()) startInstantAttendance(name)
        }
        builder.setNegativeButton("İptal", null)
        builder.show()
    }

    private fun startInstantAttendance(lessonName: String) {
        val teacherId = auth.currentUser?.uid ?: return
        val lessonData = hashMapOf(
            "lessonName" to lessonName,
            "teacherId" to teacherId,
            "university" to teacherUniversity,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "activeToken" to "",
            "webDisplayActive" to false
        )
        db.collection("Lessons").add(lessonData).addOnSuccessListener { doc ->
            val intent = Intent(this, QRGeneratorActivity::class.java)
            intent.putExtra("LESSON_ID", doc.id)
            intent.putExtra("LESSON_NAME", lessonName)
            startActivity(intent)
        }
    }
}
