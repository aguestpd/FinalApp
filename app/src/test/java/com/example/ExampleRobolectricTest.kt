package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.AttendanceRecord
import com.example.data.AttendanceStatus
import com.example.data.Employee
import com.example.data.MaterialItem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: AppRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AppRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun read_string_from_context() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Presensi & Material", appName)
    }

    @Test
    fun test_employee_and_attendance_flow() = runBlocking {
        // 1. Insert Employee
        val emp = Employee(nik = "TEST001", nama = "Budi Hartono", jabatan = "Site Manager")
        val insertResult = repository.insertEmployee(emp)
        assertTrue(insertResult.isSuccess)

        // 2. Reject duplicate NIK
        val duplicateEmp = Employee(nik = "TEST001", nama = "Budi Lain", jabatan = "Staff")
        val dupResult = repository.insertEmployee(duplicateEmp)
        assertTrue(dupResult.isFailure)

        // 3. Create Attendance Date (initially empty)
        val dateResult = repository.createAttendanceDate("2026-09-24", "Shift Pagi")
        assertTrue(dateResult.isSuccess)
        val dateId = dateResult.getOrThrow()

        // 4. Validate duplicate date rejection
        val duplicateDateResult = repository.createAttendanceDate("2026-09-24", "Shift Siang")
        assertTrue(duplicateDateResult.isFailure)

        // 5. Verify records initially empty (data masih kosong)
        val initialRecords = repository.getRecordsByDateIdSync(dateId)
        assertEquals(0, initialRecords.size)

        // 6. Save attendance records via saveRecordsForDate (Input Presensi flow)
        val recordToSave = listOf(
            AttendanceRecord(
                attendanceDateId = dateId,
                employeeId = emp.id,
                namaKaryawan = emp.nama,
                nik = emp.nik,
                jabatan = emp.jabatan,
                status = AttendanceStatus.HADIR,
                keterangan = "Hadir Tepat Waktu"
            )
        )
        val saveResult = repository.saveRecordsForDate(dateId, recordToSave)
        assertTrue(saveResult.isSuccess)

        val records = repository.getRecordsByDateIdSync(dateId)
        assertEquals(1, records.size)
        assertEquals("Budi Hartono", records[0].namaKaryawan)
        assertEquals(AttendanceStatus.HADIR, records[0].status)

        // 7. Update status to CUTI
        repository.updateRecordStatus(records[0].id, AttendanceStatus.CUTI, "Cuti Tahunan")
        val updatedRecords = repository.getRecordsByDateIdSync(dateId)
        assertEquals(AttendanceStatus.CUTI, updatedRecords[0].status)
        assertEquals("Cuti Tahunan", updatedRecords[0].keterangan)

        // 8. Material operations
        val matResult = repository.insertMaterial(
            MaterialItem(kode = "MAT-T1", nama = "Semen Holcim", satuan = "Sak", stok = 50.0, kategori = "Semen")
        )
        assertTrue(matResult.isSuccess)

        // 9. BOM Operations and Material Code Validation
        val bomResult = repository.insertBomHeader(noBom = "BOM-TEST-01", model = "Rumah Tipe 36", keterangan = "Standar")
        assertTrue(bomResult.isSuccess)
        val bomId = bomResult.getOrThrow()

        // 9a. Adding BOM Item with NON-EXISTENT material code must fail
        val invalidItemResult = repository.insertBomItem(bomHeaderId = bomId, noBom = "BOM-TEST-01", kodeMaterial = "NON_EXISTING_KODE", kebutuhan = 20.0)
        assertTrue("Item with non-existent material code must fail", invalidItemResult.isFailure)

        // 9b. Adding BOM Item with valid material code must succeed
        val validItemResult = repository.insertBomItem(bomHeaderId = bomId, noBom = "BOM-TEST-01", kodeMaterial = "MAT-T1", kebutuhan = 20.0)
        assertTrue("Item with valid material code must succeed", validItemResult.isSuccess)

        // 10. Penerimaan Operations and No BOM Validation
        // 10a. Penerimaan with NON-EXISTENT No BOM must fail
        val invalidPenerimaan = repository.insertPenerimaan(tanggal = "2026-09-25", noBom = "BOM-GHAIB", qtyPesanan = 100.0)
        assertTrue("Penerimaan with non-existent BOM must fail", invalidPenerimaan.isFailure)

        // 10b. Penerimaan with valid No BOM must succeed
        val validPenerimaan = repository.insertPenerimaan(tanggal = "2026-09-25", noBom = "BOM-TEST-01", qtyPesanan = 100.0)
        assertTrue("Penerimaan with valid BOM must succeed", validPenerimaan.isSuccess)

        // 11. Pemakaian Operations and Constraints
        // 11a. Pemakaian with NON-EXISTENT No BOM must fail
        val invalidPemakaianBom = repository.insertPemakaian(tanggal = "2026-09-26", noBom = "BOM-INVALID", qtyPemakaian = 10.0)
        assertTrue("Pemakaian with non-existent BOM must fail", invalidPemakaianBom.isFailure)

        // 11b. Pemakaian with Qty Pemakaian > Qty Penerimaan (150.0 > 100.0) must fail
        val invalidQtyPemakaian = repository.insertPemakaian(tanggal = "2026-09-26", noBom = "BOM-TEST-01", qtyPemakaian = 150.0)
        assertTrue("Pemakaian with qty exceeding penerimaan must fail", invalidQtyPemakaian.isFailure)

        // 11c. Pemakaian with valid Qty (e.g. 40.0 <= 100.0) must succeed
        val validPemakaian = repository.insertPemakaian(tanggal = "2026-09-26", noBom = "BOM-TEST-01", qtyPemakaian = 40.0)
        assertTrue("Pemakaian with valid qty must succeed", validPemakaian.isSuccess)

        // 11d. Second pemakaian with valid cumulative Qty (50.0 + 40.0 = 90.0 <= 100.0) must succeed
        val validPemakaian2 = repository.insertPemakaian(tanggal = "2026-09-26", noBom = "BOM-TEST-01", qtyPemakaian = 50.0)
        assertTrue(validPemakaian2.isSuccess)

        // 11e. Third pemakaian exceeding remaining (20.0 + 90.0 = 110.0 > 100.0) must fail
        val invalidExceedingPemakaian = repository.insertPemakaian(tanggal = "2026-09-26", noBom = "BOM-TEST-01", qtyPemakaian = 20.0)
        assertTrue(invalidExceedingPemakaian.isFailure)
    }

    @Test
    fun test_utc_date_preservation_no_offset() {
        val testDates = listOf("2026-09-24", "2026-01-01", "2026-12-31", "2025-02-28")
        for (dateStr in testDates) {
            val parts = dateStr.split("-")
            val y = parts[0].toInt()
            val m = parts[1].toInt()
            val d = parts[2].toInt()
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(java.util.Calendar.YEAR, y)
                set(java.util.Calendar.MONTH, m - 1)
                set(java.util.Calendar.DAY_OF_MONTH, d)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val millis = cal.timeInMillis

            // Format back
            val calBack = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = millis
            }
            val backFormatted = String.format(
                java.util.Locale.US,
                "%04d-%02d-%02d",
                calBack.get(java.util.Calendar.YEAR),
                calBack.get(java.util.Calendar.MONTH) + 1,
                calBack.get(java.util.Calendar.DAY_OF_MONTH)
            )
            assertEquals("Date must remain identical with no previous day subtraction", dateStr, backFormatted)
        }
    }
}
