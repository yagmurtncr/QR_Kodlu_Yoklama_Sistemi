package com.example.qr_kodlu_yoklama_sistemi.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface AttendanceDao {
    // Öğrenci İşlemleri
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student)

    @Query("SELECT * FROM students")
    fun getAllStudents(): LiveData<List<Student>>

    @Delete
    suspend fun deleteStudent(student: Student)

    // Ders İşlemleri
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: Lesson)

    @Query("SELECT * FROM lessons")
    fun getAllLessons(): LiveData<List<Lesson>>

    @Delete
    suspend fun deleteLesson(lesson: Lesson)

    // Yoklama İşlemleri
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun takeAttendance(attendance: Attendance)

    @Query("SELECT * FROM attendance WHERE lessonId = :lId")
    fun getLessonAttendance(lId: String): LiveData<List<Attendance>>
    
    @Query("SELECT * FROM attendance ORDER BY timestamp DESC")
    fun getAllAttendance(): LiveData<List<Attendance>>

    @Query("DELETE FROM attendance")
    suspend fun clearAllAttendance()
}
