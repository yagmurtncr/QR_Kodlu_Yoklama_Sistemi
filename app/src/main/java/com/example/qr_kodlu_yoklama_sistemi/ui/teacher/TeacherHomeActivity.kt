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
    private var teacherUniversity: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        fetchTeacherProfile()
        fetchLessons()

        binding.cardStartAttendance.setOnClickListener { showAddLessonDialog() }
        binding.fabAddLesson.setOnClickListener { showAddLessonDialog() }
    }

    private fun fetchTeacherProfile() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("Users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val name = doc.getString("fullName") ?: "Hocam"
                teacherUniversity = doc.getString("university") ?: ""
                binding.tvTeacherWelcome.text = getString(R.string.welcome_teacher, name)
            }
        }
    }

    private fun setupRecyclerView() {
        lessonAdapter = LessonAdapter(lessonList) { lessonId, lessonName ->
            val intent = Intent(this, QRGeneratorActivity::class.java)
            intent.putExtra("LESSON_ID", lessonId)
            intent.putExtra("LESSON_NAME", lessonName)
            startActivity(intent)
        }
        binding.rvTeacherLessons.layoutManager = LinearLayoutManager(this)
        binding.rvTeacherLessons.adapter = lessonAdapter
    }

    private fun fetchLessons() {
        val teacherId = auth.currentUser?.uid ?: return
        db.collection("Lessons").whereEqualTo("teacherId", teacherId)
            .addSnapshotListener { snapshots, _ ->
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

        if (lessonList.isEmpty()) return

        // Son 5 dersin katılım verisini Firestore'dan asenkron alıp grafiğe yansıtıyoruz
        lessonList.take(5).forEachIndexed { index, lesson ->
            val lessonId = lesson["lessonId"].toString()
            val lessonName = lesson["lessonName"].toString()
            
            db.collection("Attendances").whereEqualTo("lessonId", lessonId).get()
                .addOnSuccessListener { attendances ->
                    entries.add(BarEntry(index.toFloat(), attendances.size().toFloat()))
                    labels.add(if (lessonName.length > 5) lessonName.take(5) else lessonName)
                    
                    if (index == lessonList.take(5).size - 1 || index == entries.size -1) {
                        refreshChart(entries, labels)
                    }
                }
        }
    }

    private fun refreshChart(entries: List<BarEntry>, labels: List<String>) {
        val dataSet = BarDataSet(entries, "Öğrenci Sayısı")
        dataSet.colors = listOf(Color.parseColor("#1A73E8"), Color.parseColor("#6C63FF"))
        dataSet.valueTextColor = Color.GRAY
        
        binding.barChart.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                setDrawGridLines(false)
                textColor = Color.GRAY
            }
            axisLeft.textColor = Color.GRAY
            axisRight.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    private fun showAddLessonDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Yeni Ders")
        val input = EditText(this)
        input.hint = "Ders Adı"
        builder.setView(input)
        builder.setPositiveButton("Ekle") { _, _ ->
            val name = input.text.toString().trim()
            if (name.isNotEmpty()) startInstantAttendance(name)
        }
        builder.show()
    }

    private fun startInstantAttendance(lessonName: String) {
        val teacherId = auth.currentUser?.uid ?: return
        val lessonData = hashMapOf(
            "lessonName" to lessonName,
            "teacherId" to teacherId,
            "university" to teacherUniversity,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        db.collection("Lessons").add(lessonData).addOnSuccessListener { doc ->
            val intent = Intent(this, QRGeneratorActivity::class.java)
            intent.putExtra("LESSON_ID", doc.id)
            intent.putExtra("LESSON_NAME", lessonName)
            startActivity(intent)
        }
    }
}
