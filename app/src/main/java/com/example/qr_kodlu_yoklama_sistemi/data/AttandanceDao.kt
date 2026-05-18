package com.example.qr_kodlu_yoklama_sistemi.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student)

    @Query("SELECT * FROM students")
    fun getAllStudents(): LiveData<List<Student>>

    @Insert
    suspend fun takeAttendance(attendance: Attendance)

    @Query("SELECT * FROM attendance WHERE lessonId = :lId")
    fun getLessonAttendance(lId: Int): LiveData<List<Attendance>>
}