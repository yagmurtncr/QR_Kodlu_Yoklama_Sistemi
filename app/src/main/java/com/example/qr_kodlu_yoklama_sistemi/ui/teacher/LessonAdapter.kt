package com.example.qr_kodlu_yoklama_sistemi.ui.teacher

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.qr_kodlu_yoklama_sistemi.databinding.ItemLessonBinding

class LessonAdapter(
    private var lessonList: List<Map<String, Any>>,
    private val onStartClick: (String, String) -> Unit
) : RecyclerView.Adapter<LessonAdapter.LessonViewHolder>() {

    class LessonViewHolder(val binding: ItemLessonBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val binding = ItemLessonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LessonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = lessonList[position]
        val lessonId = lesson["lessonId"]?.toString() ?: ""
        val lessonName = lesson["lessonName"]?.toString() ?: "Adsız Ders"
        val university = lesson["university"]?.toString() ?: "Üniversite Belirtilmedi"

        holder.binding.tvLessonName.text = lessonName
        holder.binding.tvUniversityName.text = university

        holder.binding.btnStartLesson.setOnClickListener {
            onStartClick(lessonId, lessonName)
        }
    }

    override fun getItemCount(): Int = lessonList.size

    fun updateList(newList: List<Map<String, Any>>) {
        lessonList = newList
        notifyDataSetChanged()
    }
}
