package com.example.qr_kodlu_yoklama_sistemi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lessons")
data class Lesson(
    @PrimaryKey(autoGenerate = true) val lessonId: Int = 0,
    val lessonName: String,
    val lessonCode: String
)