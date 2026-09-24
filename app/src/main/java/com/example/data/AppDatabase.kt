package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        Employee::class,
        AttendanceDate::class,
        AttendanceRecord::class,
        MaterialItem::class,
        BomHeader::class,
        BomItem::class,
        PenerimaanItem::class,
        PenerimaanDetailItem::class,
        PemakaianItem::class,
        PemakaianDetailItem::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun employeeDao(): EmployeeDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun materialDao(): MaterialDao
    abstract fun bomDao(): BomDao
    abstract fun penerimaanDao(): PenerimaanDao
    abstract fun pemakaianDao(): PemakaianDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "presensi_material_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            val employeeDao = database.employeeDao()
            val attendanceDao = database.attendanceDao()
            val materialDao = database.materialDao()
            val bomDao = database.bomDao()
            val penerimaanDao = database.penerimaanDao()
            val pemakaianDao = database.pemakaianDao()

            val employees = listOf(
                Employee(nik = "EMP001", nama = "Budi Santoso", jabatan = "Project Manager", nomorHp = "081234567890"),
                Employee(nik = "EMP002", nama = "Siti Rahmawati", jabatan = "Site Engineer", nomorHp = "081298765432"),
                Employee(nik = "EMP003", nama = "Ahmad Hidayat", jabatan = "Mandor Utama", nomorHp = "081345678901"),
                Employee(nik = "EMP004", nama = "Doni Kurniawan", jabatan = "Tukang Besi & Las", nomorHp = "081456789012"),
                Employee(nik = "EMP005", nama = "Eko Prasetyo", jabatan = "Tukang Batu & Plester", nomorHp = "081567890123"),
                Employee(nik = "EMP006", nama = "Fajar Pratama", jabatan = "Operator Alat Berat", nomorHp = "081678901234"),
                Employee(nik = "EMP007", nama = "Gita Permata", jabatan = "Admin Logistik", nomorHp = "081789012345"),
                Employee(nik = "EMP008", nama = "Hendra Wijaya", jabatan = "Quality Control", nomorHp = "081890123456")
            )

            val insertedEmployees = mutableListOf<Employee>()
            for (emp in employees) {
                val id = employeeDao.insertEmployee(emp)
                insertedEmployees.add(emp.copy(id = id))
            }

            // Populate today's and yesterday's attendance
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            val todayStr = dateFormat.format(calendar.time)

            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = dateFormat.format(calendar.time)

            // Today's attendance
            val todayDateId = attendanceDao.insertDate(
                AttendanceDate(tanggal = todayStr, catatan = "Presensi Harian Kerja Shift Pagi")
            )
            val todayRecords = listOf(
                AttendanceRecord(attendanceDateId = todayDateId, employeeId = insertedEmployees[0].id, namaKaryawan = insertedEmployees[0].nama, nik = insertedEmployees[0].nik, jabatan = insertedEmployees[0].jabatan, status = AttendanceStatus.HADIR, keterangan = "Tepat Waktu"),
                AttendanceRecord(attendanceDateId = todayDateId, employeeId = insertedEmployees[1].id, namaKaryawan = insertedEmployees[1].nama, nik = insertedEmployees[1].nik, jabatan = insertedEmployees[1].jabatan, status = AttendanceStatus.HADIR, keterangan = "Tepat Waktu"),
                AttendanceRecord(attendanceDateId = todayDateId, employeeId = insertedEmployees[2].id, namaKaryawan = insertedEmployees[2].nama, nik = insertedEmployees[2].nik, jabatan = insertedEmployees[2].jabatan, status = AttendanceStatus.HADIR, keterangan = "Tepat Waktu"),
                AttendanceRecord(attendanceDateId = todayDateId, employeeId = insertedEmployees[3].id, namaKaryawan = insertedEmployees[3].nama, nik = insertedEmployees[3].nik, jabatan = insertedEmployees[3].jabatan, status = AttendanceStatus.IZIN, keterangan = "Urusan Keluarga"),
                AttendanceRecord(attendanceDateId = todayDateId, employeeId = insertedEmployees[4].id, namaKaryawan = insertedEmployees[4].nama, nik = insertedEmployees[4].nik, jabatan = insertedEmployees[4].jabatan, status = AttendanceStatus.SAKIT, keterangan = "Surat Dokter Flu & Demam"),
                AttendanceRecord(attendanceDateId = todayDateId, employeeId = insertedEmployees[5].id, namaKaryawan = insertedEmployees[5].nama, nik = insertedEmployees[5].nik, jabatan = insertedEmployees[5].jabatan, status = AttendanceStatus.HADIR, keterangan = "Tepat Waktu"),
                AttendanceRecord(attendanceDateId = todayDateId, employeeId = insertedEmployees[6].id, namaKaryawan = insertedEmployees[6].nama, nik = insertedEmployees[6].nik, jabatan = insertedEmployees[6].jabatan, status = AttendanceStatus.HADIR, keterangan = "Tepat Waktu"),
                AttendanceRecord(attendanceDateId = todayDateId, employeeId = insertedEmployees[7].id, namaKaryawan = insertedEmployees[7].nama, nik = insertedEmployees[7].nik, jabatan = insertedEmployees[7].jabatan, status = AttendanceStatus.SETENGAH_HARI, keterangan = "Izin Pulang Siang")
            )
            attendanceDao.insertRecords(todayRecords)

            // Yesterday's attendance
            val yesterdayDateId = attendanceDao.insertDate(
                AttendanceDate(tanggal = yesterdayStr, catatan = "Presensi Harian Kerja")
            )
            val yesterdayRecords = listOf(
                AttendanceRecord(attendanceDateId = yesterdayDateId, employeeId = insertedEmployees[0].id, namaKaryawan = insertedEmployees[0].nama, nik = insertedEmployees[0].nik, jabatan = insertedEmployees[0].jabatan, status = AttendanceStatus.HADIR),
                AttendanceRecord(attendanceDateId = yesterdayDateId, employeeId = insertedEmployees[1].id, namaKaryawan = insertedEmployees[1].nama, nik = insertedEmployees[1].nik, jabatan = insertedEmployees[1].jabatan, status = AttendanceStatus.HADIR),
                AttendanceRecord(attendanceDateId = yesterdayDateId, employeeId = insertedEmployees[2].id, namaKaryawan = insertedEmployees[2].nama, nik = insertedEmployees[2].nik, jabatan = insertedEmployees[2].jabatan, status = AttendanceStatus.HADIR),
                AttendanceRecord(attendanceDateId = yesterdayDateId, employeeId = insertedEmployees[3].id, namaKaryawan = insertedEmployees[3].nama, nik = insertedEmployees[3].nik, jabatan = insertedEmployees[3].jabatan, status = AttendanceStatus.HADIR),
                AttendanceRecord(attendanceDateId = yesterdayDateId, employeeId = insertedEmployees[4].id, namaKaryawan = insertedEmployees[4].nama, nik = insertedEmployees[4].nik, jabatan = insertedEmployees[4].jabatan, status = AttendanceStatus.ALPA, keterangan = "Tanpa Pemberitahuan"),
                AttendanceRecord(attendanceDateId = yesterdayDateId, employeeId = insertedEmployees[5].id, namaKaryawan = insertedEmployees[5].nama, nik = insertedEmployees[5].nik, jabatan = insertedEmployees[5].jabatan, status = AttendanceStatus.HADIR),
                AttendanceRecord(attendanceDateId = yesterdayDateId, employeeId = insertedEmployees[6].id, namaKaryawan = insertedEmployees[6].nama, nik = insertedEmployees[6].nik, jabatan = insertedEmployees[6].jabatan, status = AttendanceStatus.IZIN, keterangan = "Pemeriksaan Kesehatan"),
                AttendanceRecord(attendanceDateId = yesterdayDateId, employeeId = insertedEmployees[7].id, namaKaryawan = insertedEmployees[7].nama, nik = insertedEmployees[7].nik, jabatan = insertedEmployees[7].jabatan, status = AttendanceStatus.HADIR)
            )
            attendanceDao.insertRecords(yesterdayRecords)

            // Materials
            val materials = listOf(
                MaterialItem(kode = "MAT-001", nama = "Semen Portland Gresik 50kg", satuan = "Sak", stok = 150.0, kategori = "Semen & Perekat", lokasi = "Gudang A - Rak 1"),
                MaterialItem(kode = "MAT-002", nama = "Besi Beton Ulir D16 (12 Meter)", satuan = "Batang", stok = 320.0, kategori = "Besi & Baja", lokasi = "Area Terbuka Blok B"),
                MaterialItem(kode = "MAT-003", nama = "Pasir Cor Muntilan Bersih", satuan = "M3", stok = 28.5, kategori = "Pasir & Agregat", lokasi = "Dump Zone 1"),
                MaterialItem(kode = "MAT-004", nama = "Bata Ringan / Hebel 10cm", satuan = "M3", stok = 45.0, kategori = "Dinding & Partisi", lokasi = "Gudang A - Blok 3"),
                MaterialItem(kode = "MAT-005", nama = "Cat Weathercoat Putih 20L", satuan = "Pail", stok = 12.0, kategori = "Finishing & Cat", lokasi = "Ruang Cat Lt.1"),
                MaterialItem(kode = "MAT-006", nama = "Pipa PVC Wavin AW 3 inch", satuan = "Batang", stok = 65.0, kategori = "Plumbing & Pipa", lokasi = "Gudang Pipa Blok C")
            )
            for (mat in materials) {
                materialDao.insertMaterial(mat)
            }

            // Seed Master BOM & BOM Items
            val bom1Id = bomDao.insertBomHeader(
                BomHeader(noBom = "BOM-001", model = "Pondasi Rumah Tipe 36", keterangan = "Pondasi batu kali & sloof beton")
            )
            bomDao.insertBomItem(
                BomItem(bomHeaderId = bom1Id, noBom = "BOM-001", kodeMaterial = "MAT-001", namaMaterial = "Semen Portland Gresik 50kg", satuan = "Sak", kebutuhan = 50.0)
            )
            bomDao.insertBomItem(
                BomItem(bomHeaderId = bom1Id, noBom = "BOM-001", kodeMaterial = "MAT-002", namaMaterial = "Besi Beton Ulir D16 (12 Meter)", satuan = "Batang", kebutuhan = 40.0)
            )
            bomDao.insertBomItem(
                BomItem(bomHeaderId = bom1Id, noBom = "BOM-001", kodeMaterial = "MAT-003", namaMaterial = "Pasir Cor Muntilan Bersih", satuan = "M3", kebutuhan = 8.0)
            )

            val bom2Id = bomDao.insertBomHeader(
                BomHeader(noBom = "BOM-002", model = "Struktur Kolom Tipe 45", keterangan = "Kolom praktis dan balok lantai")
            )
            bomDao.insertBomItem(
                BomItem(bomHeaderId = bom2Id, noBom = "BOM-002", kodeMaterial = "MAT-002", namaMaterial = "Besi Beton Ulir D16 (12 Meter)", satuan = "Batang", kebutuhan = 60.0)
            )
            bomDao.insertBomItem(
                BomItem(bomHeaderId = bom2Id, noBom = "BOM-002", kodeMaterial = "MAT-001", namaMaterial = "Semen Portland Gresik 50kg", satuan = "Sak", kebutuhan = 35.0)
            )

            // Seed Penerimaan
            penerimaanDao.insertPenerimaan(
                PenerimaanItem(tanggal = "2026-09-20", noBom = "BOM-001", qtyPesanan = 100.0, keterangan = "Batch 1 Pembangunan")
            )
            penerimaanDao.insertPenerimaan(
                PenerimaanItem(tanggal = "2026-09-22", noBom = "BOM-002", qtyPesanan = 50.0, keterangan = "Batch 1 Struktur")
            )

            // Seed Pemakaian (Note: qtyPemakaian <= total penerimaan)
            pemakaianDao.insertPemakaian(
                PemakaianItem(tanggal = "2026-09-23", noBom = "BOM-001", qtyPemakaian = 40.0, keterangan = "Pengecoran blok timur")
            )
            pemakaianDao.insertPemakaian(
                PemakaianItem(tanggal = "2026-09-24", noBom = "BOM-002", qtyPemakaian = 25.0, keterangan = "Pemasangan kolom lantai 1")
            )
        }
    }
}
