package com.example.qr_kodlu_yoklama_sistemi.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Student::class, Lesson::class, Attendance::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
}