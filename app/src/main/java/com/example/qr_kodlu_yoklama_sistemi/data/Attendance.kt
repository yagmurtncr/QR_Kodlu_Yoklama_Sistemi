package com.example.qr_kodlu_yoklama_sistemi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val attendanceId: Int = 0,
    val lessonId: Int,
    val studentId: Int,
    val date: String,
    val isPresent: Boolean
)