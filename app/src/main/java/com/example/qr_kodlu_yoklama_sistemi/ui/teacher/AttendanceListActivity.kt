package com.example.qr_kodlu_yoklama_sistemi.ui.teacher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qr_kodlu_yoklama_sistemi.databinding.ActivityAttendanceListBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AttendanceListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAttendanceListBinding
    private val db = FirebaseFirestore.getInstance()
    private val attendanceList = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: AttendanceAdapter
    private var lessonId: String? = null
    private var lessonName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lessonId = intent.getStringExtra("LESSON_ID")
        lessonName = intent.getStringExtra("LESSON_NAME")
        binding.tvTitle.text = "$lessonName - Yoklama"

        setupRecyclerView()
        fetchAttendanceData()

        binding.btnExportPdf.setOnClickListener {
            if (checkPermission()) exportToPdf() else requestPermission()
        }

        binding.btnExportExcel.setOnClickListener {
            if (checkPermission()) exportToExcel() else requestPermission()
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = AttendanceAdapter(attendanceList)
        binding.rvAttendance.layoutManager = LinearLayoutManager(this)
        binding.rvAttendance.adapter = adapter
    }

    private fun fetchAttendanceData() {
        lessonId?.let { id ->
            db.collection("Attendances")
                .whereEqualTo("lessonId", id)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) return@addSnapshotListener
                    attendanceList.clear()
                    snapshots?.let {
                        for (doc in it) { attendanceList.add(doc.data) }
                        adapter.updateList(attendanceList)
                    }
                }
        }
    }

    private fun exportToPdf() {
        try {
            val path = getExternalFilesDir(null)?.absolutePath
            val file = File(path, "${lessonName?.replace(" ", "_")}_Rapor.pdf")
            val writer = PdfWriter(FileOutputStream(file))
            val pdf = PdfDocument(writer)
            val document = Document(pdf)

            document.add(Paragraph("$lessonName Yoklama Raporu").setBold().setFontSize(18f))
            document.add(Paragraph("Tarih: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}"))
            
            val table = Table(floatArrayOf(1f, 2f))
            table.addCell("Öğrenci ID")
            table.addCell("Kayıt Zamanı")

            for (item in attendanceList) {
                table.addCell(item["userId"]?.toString() ?: "---")
                val ts = item["timestamp"] as? com.google.firebase.Timestamp
                table.addCell(ts?.toDate()?.toString() ?: "---")
            }
            document.add(table)
            document.close()
            Toast.makeText(this, "PDF İndirildi: ${file.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) { Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun exportToExcel() {
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Yoklama Listesi")
            
            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue("Öğrenci ID")
            headerRow.createCell(1).setCellValue("Durum")
            headerRow.createCell(2).setCellValue("Tarih/Saat")

            var rowNum = 1
            for (item in attendanceList) {
                val row = sheet.createRow(rowNum++)
                row.createCell(0).setCellValue(item["userId"]?.toString() ?: "")
                row.createCell(1).setCellValue(item["status"]?.toString() ?: "Var")
                val ts = item["timestamp"] as? com.google.firebase.Timestamp
                row.createCell(2).setCellValue(ts?.toDate()?.toString() ?: "")
            }

            val path = getExternalFilesDir(null)?.absolutePath
            val file = File(path, "${lessonName?.replace(" ", "_")}_Liste.xlsx")
            val out = FileOutputStream(file)
            workbook.write(out)
            out.close()
            workbook.close()
            Toast.makeText(this, "Excel İndirildi: ${file.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) { Toast.makeText(this, "Excel Hatası: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun checkPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    private fun requestPermission() = ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
}
