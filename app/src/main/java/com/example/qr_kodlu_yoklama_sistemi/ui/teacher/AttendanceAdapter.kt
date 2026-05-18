package com.example.qr_kodlu_yoklama_sistemi.ui.teacher

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.qr_kodlu_yoklama_sistemi.R
import com.example.qr_kodlu_yoklama_sistemi.databinding.ItemAttendanceBinding
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class AttendanceAdapter(private var attendanceList: List<Map<String, Any>>) :
    RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    class AttendanceViewHolder(val binding: ItemAttendanceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val binding = ItemAttendanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AttendanceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val item = attendanceList[position]
        val context = holder.itemView.context

        // UI IDs updated to match professional layout
        holder.binding.tvStudentName.text = "Öğrenci: ${item["userId"]}" // In a real app, you'd fetch the name
        holder.binding.tvStudentId.text = "ID: ${item["userId"]}"
        
        val status = item["status"]?.toString() ?: "GELMEDİ"
        holder.binding.tvStatusBadge.text = status.uppercase()

        // Dynamic Badge Styling
        if (status.equals("Var", ignoreCase = true)) {
            holder.binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.success_green))
            holder.binding.tvStatusBadge.backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.holo_green_light)?.withAlpha(30)
        } else {
            holder.binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.error_red))
            holder.binding.tvStatusBadge.backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.holo_red_light)?.withAlpha(30)
        }

        val timestamp = item["timestamp"] as? Timestamp
        val date = timestamp?.toDate()
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        holder.binding.tvTime.text = if (date != null) sdf.format(date) else "--:--:--"
    }

    override fun getItemCount(): Int = attendanceList.size

    fun updateList(newList: List<Map<String, Any>>) {
        attendanceList = newList
        notifyDataSetChanged()
    }
}
