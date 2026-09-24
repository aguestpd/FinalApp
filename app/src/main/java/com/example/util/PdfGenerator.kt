package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.AttendanceDate
import com.example.data.AttendanceRecord
import com.example.data.AttendanceStatus
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    fun generateAttendancePdf(
        context: Context,
        attendanceDate: AttendanceDate,
        records: List<AttendanceRecord>
    ): File? {
        val document = PdfDocument()
        val pageWidth = 595 // A4 standard width (points)
        val pageHeight = 842 // A4 standard height (points)
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // Margin
        val margin = 36f
        var currentY = 40f

        // --- HEADER BANNER / KOP LAPORAN ---
        paint.color = Color.parseColor("#1E3A8A") // Deep Blue
        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 4f, paint)
        currentY += 24f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f
        paint.color = Color.parseColor("#0F172A")
        canvas.drawText("LAPORAN REKAPITULASI PRESENSI HARIAN", margin, currentY, paint)
        currentY += 16f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        paint.color = Color.parseColor("#475569")
        canvas.drawText("Sistem Manajemen Karyawan, Presensi & Inventaris Material", margin, currentY, paint)
        currentY += 20f

        // Format Date Indonesian
        val displayDate = try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateObj = parser.parse(attendanceDate.tanggal)
            val formatter = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
            formatter.format(dateObj ?: Date())
        } catch (e: Exception) {
            attendanceDate.tanggal
        }

        // Info box
        val infoBoxTop = currentY
        val infoBoxBottom = currentY + 54f
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(RectF(margin, infoBoxTop, pageWidth - margin, infoBoxBottom), 6f, 6f, paint)

        paint.color = Color.parseColor("#334155")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Tanggal Presensi:", margin + 12f, currentY + 20f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(displayDate, margin + 115f, currentY + 20f, paint)

        if (attendanceDate.catatan.isNotBlank()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Keterangan:", margin + 12f, currentY + 40f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(attendanceDate.catatan, margin + 115f, currentY + 40f, paint)
        }
        currentY = infoBoxBottom + 16f

        // --- SUMMARY STATS CHIPS IN PDF ---
        val hadirCount = records.count { it.status == AttendanceStatus.HADIR }
        val alpaCount = records.count { it.status == AttendanceStatus.ALPA }
        val izinCount = records.count { it.status == AttendanceStatus.IZIN }
        val sakitCount = records.count { it.status == AttendanceStatus.SAKIT }
        val cutiCount = records.count { it.status == AttendanceStatus.CUTI }
        val shCount = records.count { it.status == AttendanceStatus.SETENGAH_HARI }
        val totalCount = records.size

        val stats = listOf(
            Triple("Total: $totalCount", "#0F172A", "#E2E8F0"),
            Triple("Hadir: $hadirCount", "#15803D", "#DCFCE7"),
            Triple("Izin: $izinCount", "#B45309", "#FEF3C7"),
            Triple("Sakit: $sakitCount", "#C2410C", "#FFEDD5"),
            Triple("Cuti: $cutiCount", "#7E22CE", "#F3E8FF"),
            Triple("Alpa: $alpaCount", "#B91C1C", "#FEE2E2"),
            Triple("Setengah Hari: $shCount", "#1D4ED8", "#DBEAFE")
        )

        var statX = margin
        val statY = currentY
        val statHeight = 22f
        for (item in stats) {
            val text = item.first
            val textWidth = paint.measureText(text) + 20f
            paint.color = Color.parseColor(item.third)
            canvas.drawRoundRect(RectF(statX, statY, statX + textWidth, statY + statHeight), 4f, 4f, paint)

            paint.color = Color.parseColor(item.second)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9f
            canvas.drawText(text, statX + 10f, statY + 14f, paint)
            statX += textWidth + 6f
        }
        currentY += statHeight + 20f

        // --- TABLE HEADER ---
        val colNo = margin
        val colNik = margin + 30f
        val colNama = margin + 100f
        val colJabatan = margin + 240f
        val colStatus = margin + 360f
        val colKet = margin + 440f
        val tableRight = pageWidth - margin
        val rowHeight = 22f

        paint.color = Color.parseColor("#1E3A8A")
        canvas.drawRect(margin, currentY, tableRight, currentY + rowHeight, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9.5f
        val headerTextY = currentY + 14.5f
        canvas.drawText("No", colNo + 6f, headerTextY, paint)
        canvas.drawText("NIK", colNik + 6f, headerTextY, paint)
        canvas.drawText("Nama Karyawan", colNama + 6f, headerTextY, paint)
        canvas.drawText("Jabatan", colJabatan + 6f, headerTextY, paint)
        canvas.drawText("Status", colStatus + 6f, headerTextY, paint)
        canvas.drawText("Keterangan", colKet + 6f, headerTextY, paint)

        currentY += rowHeight

        // --- TABLE ROWS ---
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        records.forEachIndexed { index, record ->
            val rowY = currentY
            // Alternating row background
            if (index % 2 == 1) {
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(margin, rowY, tableRight, rowY + rowHeight, paint)
            }

            // Bottom border line
            paint.color = Color.parseColor("#E2E8F0")
            paint.strokeWidth = 0.5f
            canvas.drawLine(margin, rowY + rowHeight, tableRight, rowY + rowHeight, paint)

            val textY = rowY + 14.5f
            paint.color = Color.parseColor("#334155")
            canvas.drawText("${index + 1}", colNo + 6f, textY, paint)
            canvas.drawText(record.nik, colNik + 6f, textY, paint)
            canvas.drawText(record.namaKaryawan.take(22), colNama + 6f, textY, paint)
            canvas.drawText(record.jabatan.take(18), colJabatan + 6f, textY, paint)

            // Status Badge
            val statusColor = when (record.status) {
                AttendanceStatus.HADIR -> Color.parseColor("#16A34A")
                AttendanceStatus.ALPA -> Color.parseColor("#DC2626")
                AttendanceStatus.IZIN -> Color.parseColor("#D97706")
                AttendanceStatus.SAKIT -> Color.parseColor("#EA580C")
                AttendanceStatus.CUTI -> Color.parseColor("#7E22CE")
                AttendanceStatus.SETENGAH_HARI -> Color.parseColor("#2563EB")
            }
            paint.color = statusColor
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(record.status.label, colStatus + 6f, textY, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#64748B")
            val ketText = record.keterangan.ifBlank { "-" }.take(18)
            canvas.drawText(ketText, colKet + 6f, textY, paint)

            currentY += rowHeight
        }

        // --- SIGNATURE SECTION ---
        currentY = (currentY + 30f).coerceAtLeast(pageHeight - 120f)
        val sigLeftX = margin + 40f
        val sigRightX = pageWidth - margin - 160f

        paint.color = Color.parseColor("#475569")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Dibuat oleh,", sigLeftX, currentY, paint)
        canvas.drawText("Mengetahui / Menyetujui,", sigRightX, currentY, paint)

        currentY += 45f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#0F172A")
        canvas.drawText("( Admin Presensi )", sigLeftX, currentY, paint)
        canvas.drawText("( Pimpinan / Site Manager )", sigRightX, currentY, paint)

        // Footer timestamp
        val genTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.color = Color.parseColor("#94A3B8")
        canvas.drawText("Dicetak secara otomatis pada $genTime oleh Aplikasi Presensi & Material", margin, pageHeight - 20f, paint)

        document.finishPage(page)

        // Write to cache file
        return try {
            val pdfDir = File(context.cacheDir, "pdf").apply { mkdirs() }
            val fileName = "Presensi_${attendanceDate.tanggal}.pdf"
            val file = File(pdfDir, fileName)
            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            document.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            null
        }
    }

    fun shareOrOpenPdf(context: Context, pdfFile: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Presensi ${pdfFile.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan atau Simpan Laporan PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuka file PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun printPdf(context: Context, pdfFile: File, jobName: String = "Laporan Presensi") {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val printAdapter = PdfPrintAdapter(pdfFile)
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager.print(jobName, printAdapter, printAttributes)
            } else {
                shareOrOpenPdf(context, pdfFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            shareOrOpenPdf(context, pdfFile)
        }
    }
}
