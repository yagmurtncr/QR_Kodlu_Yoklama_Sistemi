package com.example.qr_kodlu_yoklama_sistemi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentName: String,
    val studentNumber: String,
    val studentMail: String
)