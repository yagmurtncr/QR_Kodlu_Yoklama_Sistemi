package com.example.qr_kodlu_yoklama_sistemi.ui.student

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.qr_kodlu_yoklama_sistemi.databinding.ItemAttendanceBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class StudentAttendanceAdapter(
    private var attendanceList: List<Map<String, Any>>
) : RecyclerView.Adapter<StudentAttendanceAdapter.AttendanceViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val lessonNameCache = mutableMapOf<String, String>()

    class AttendanceViewHolder(val binding: ItemAttendanceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val binding = ItemAttendanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AttendanceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val attendance = attendanceList[position]

        val lessonName = attendance["lessonName"]?.toString()
        val lessonId = attendance["lessonId"]?.toString()

        when {
            !lessonName.isNullOrBlank() -> holder.binding.tvAttendanceLessonName.text = lessonName
            !lessonId.isNullOrBlank() -> {
                holder.binding.tvAttendanceLessonName.text = lessonNameCache[lessonId] ?: "Ders Yükleniyor"
                if (!lessonNameCache.containsKey(lessonId)) {
                    db.collection("Lessons").document(lessonId).get().addOnSuccessListener { doc ->
                        lessonNameCache[lessonId] = doc.getString("lessonName") ?: "Ders Adı Yok"
                        notifyItemChanged(position)
                    }
                }
            }
            else -> holder.binding.tvAttendanceLessonName.text = "Ders Adı Yok"
        }
        
        val timestamp = attendance["timestamp"] as? com.google.firebase.Timestamp
        val dateString = timestamp?.let {
            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(it.toDate())
        } ?: "Tarih Bilgisi Yok"
        
        holder.binding.tvAttendanceDate.text = dateString
        holder.binding.tvAttendanceStatus.text = "Katıldı"
    }

    override fun getItemCount(): Int = attendanceList.size

    fun updateList(newList: List<Map<String, Any>>) {
        attendanceList = newList
        lessonNameCache.clear()
        notifyDataSetChanged()
    }
}
