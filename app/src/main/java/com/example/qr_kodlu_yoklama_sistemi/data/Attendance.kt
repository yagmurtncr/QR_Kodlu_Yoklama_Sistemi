package com.example.qr_kodlu_yoklama_sistemi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lessonId: String,
    val userId: String,
    val timestamp: Long,
    val status: String
)
