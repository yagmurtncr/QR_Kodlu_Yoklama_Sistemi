package com.example.qr_kodlu_yoklama_sistemi.ui.student

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.qr_kodlu_yoklama_sistemi.databinding.ItemAttendanceBinding
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class StudentAttendanceAdapter(private var list: List<Map<String, Any>>) :
    RecyclerView.Adapter<StudentAttendanceAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAttendanceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttendanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        // Öğrenci ekranında "Ders ID" veya "Ders Adı" göstermek daha mantıklı
        holder.binding.tvStudentId.text = "Ders ID: ${item["lessonId"]}"
        holder.binding.tvStatus.text = "Durum: ${item["status"]}"

        val timestamp = item["timestamp"] as? Timestamp
        val date = timestamp?.toDate()
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        holder.binding.tvTime.text = "Tarih: ${if (date != null) sdf.format(date) else "---"}"
    }

    override fun getItemCount(): Int = list.size

    fun updateList(newList: List<Map<String, Any>>) {
        list = newList
        notifyDataSetChanged()
    }
}
