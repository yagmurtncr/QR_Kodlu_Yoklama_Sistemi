package com.example.qr_kodlu_yoklama_sistemi.ui.admin

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.qr_kodlu_yoklama_sistemi.databinding.ActivityAssignLessonBinding
import com.google.firebase.firestore.FirebaseFirestore

class AssignLessonActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAssignLessonBinding
    private val db = FirebaseFirestore.getInstance()
    private val teachersList = mutableListOf<Pair<String, String>>() // Name to UID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssignLessonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadTeachers()

        binding.btnSaveAssignment.setOnClickListener {
            saveAssignment()
        }
    }

    private fun loadTeachers() {
        db.collection("Users")
            .whereEqualTo("role", "Teacher")
            .get()
            .addOnSuccessListener { documents ->
                val names = mutableListOf<String>()
                for (doc in documents) {
                    val name = doc.getString("fullName") ?: "İsimsiz Hoca"
                    val uid = doc.id
                    teachersList.add(Pair(name, uid))
                    names.add(name)
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerTeachers.adapter = adapter
            }
    }

    private fun saveAssignment() {
        val lessonName = binding.etLessonName.text.toString().trim()
        val minAttendance = binding.etMinAttendance.text.toString().toIntOrNull() ?: 70
        val studentIdsRaw = binding.etStudentIds.text.toString().trim()
        val teacherIndex = binding.spinnerTeachers.selectedItemPosition

        if (lessonName.isEmpty() || teacherIndex == -1) {
            Toast.makeText(this, "Lütfen ders adını ve hocayı seçin!", Toast.LENGTH_SHORT).show()
            return
        }

        val teacherUid = teachersList[teacherIndex].second
        val studentIds = studentIdsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        val lessonData = hashMapOf(
            "lessonName" to lessonName,
            "teacherId" to teacherUid,
            "minAttendancePercentage" to minAttendance,
            "assignedStudents" to studentIds,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("Lessons").add(lessonData)
            .addOnSuccessListener {
                Toast.makeText(this, "Ders Ataması Başarıyla Yapıldı!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Hata: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
