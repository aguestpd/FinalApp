package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(
    tableName = "karyawan",
    indices = [Index(value = ["nik"], unique = true)]
)
data class Employee(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nik: String,
    val nama: String,
    val jabatan: String,
    val nomorHp: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "presensi_tanggal",
    indices = [Index(value = ["tanggal"], unique = true)]
)
data class AttendanceDate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tanggal: String, // Format: YYYY-MM-DD
    val catatan: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class AttendanceStatus(val label: String, val code: String) {
    HADIR("Hadir", "H"),
    IZIN("Izin", "I"),
    ALPA("Alpa", "A"),
    SAKIT("Sakit", "S"),
    CUTI("Cuti", "C"),
    SETENGAH_HARI("Setengah Hari", "SH");

    companion object {
        fun fromString(value: String?): AttendanceStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) }
                ?: HADIR
        }
    }
}

@Entity(
    tableName = "presensi_record",
    foreignKeys = [
        ForeignKey(
            entity = AttendanceDate::class,
            parentColumns = ["id"],
            childColumns = ["attendanceDateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["attendanceDateId"]),
        Index(value = ["employeeId"]),
        Index(value = ["attendanceDateId", "employeeId"], unique = true)
    ]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val attendanceDateId: Long,
    val employeeId: Long,
    val namaKaryawan: String,
    val nik: String,
    val jabatan: String,
    val status: AttendanceStatus = AttendanceStatus.HADIR,
    val keterangan: String = ""
)

@Entity(
    tableName = "material",
    indices = [Index(value = ["kode"], unique = true)]
)
data class MaterialItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val kode: String,
    val nama: String,
    val satuan: String, // Sak, Kg, Batang, Meter, Pcs, Lembar, Dus, Liter
    val stok: Double,
    val kategori: String, // Semen, Besi, Pasir & Batu, Cat & Finishing, Kayu, Listrik, Sanitasi
    val lokasi: String = "Gudang Utama",
    val keterangan: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bom_header",
    indices = [Index(value = ["noBom"], unique = true)]
)
data class BomHeader(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noBom: String,
    val model: String,
    val keterangan: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bom_item",
    foreignKeys = [
        ForeignKey(
            entity = BomHeader::class,
            parentColumns = ["id"],
            childColumns = ["bomHeaderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bomHeaderId"]),
        Index(value = ["kodeMaterial"]),
        Index(value = ["bomHeaderId", "kodeMaterial"], unique = true)
    ]
)
data class BomItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bomHeaderId: Long,
    val noBom: String,
    val kodeMaterial: String,
    val namaMaterial: String,
    val satuan: String,
    val kebutuhan: Double,
    val keterangan: String = ""
)

@Entity(
    tableName = "penerimaan",
    indices = [Index(value = ["noBom"]), Index(value = ["tanggal"])]
)
data class PenerimaanItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tanggal: String, // YYYY-MM-DD
    val noBom: String,
    val qtyPesanan: Double,
    val keterangan: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "penerimaan_detail",
    foreignKeys = [
        ForeignKey(
            entity = PenerimaanItem::class,
            parentColumns = ["id"],
            childColumns = ["penerimaanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["penerimaanId"]),
        Index(value = ["kodeMaterial"]),
        Index(value = ["penerimaanId", "kodeMaterial"], unique = true)
    ]
)
data class PenerimaanDetailItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val penerimaanId: Long,
    val noBom: String,
    val kodeMaterial: String,
    val namaMaterial: String,
    val satuan: String,
    val kebutuhan: Double,
    val qty: Double,
    val keterangan: String = ""
)

@Entity(
    tableName = "pemakaian",
    indices = [Index(value = ["noBom"]), Index(value = ["tanggal"])]
)
data class PemakaianItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tanggal: String, // YYYY-MM-DD
    val noBom: String,
    val qtyPemakaian: Double,
    val keterangan: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "pemakaian_detail",
    foreignKeys = [
        ForeignKey(
            entity = PemakaianItem::class,
            parentColumns = ["id"],
            childColumns = ["pemakaianId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["pemakaianId"]),
        Index(value = ["kodeMaterial"]),
        Index(value = ["pemakaianId", "kodeMaterial"], unique = true)
    ]
)
data class PemakaianDetailItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pemakaianId: Long,
    val noBom: String,
    val kodeMaterial: String,
    val namaMaterial: String,
    val satuan: String,
    val kebutuhan: Double,
    val qty: Double,
    val keterangan: String = ""
)

data class AttendanceDateSummary(
    val id: Long,
    val tanggal: String,
    val catatan: String,
    val createdAt: Long,
    val totalKaryawan: Int,
    val jumlahHadir: Int,
    val jumlahIzin: Int,
    val jumlahAlpa: Int,
    val jumlahSakit: Int,
    val jumlahCuti: Int = 0,
    val jumlahSetengahHari: Int
)

data class EmployeeAttendanceInput(
    val employeeId: Long,
    val nik: String,
    val nama: String,
    val jabatan: String,
    val status: AttendanceStatus = AttendanceStatus.HADIR,
    val keterangan: String = "",
    val recordId: Long? = null
)

class Converters {
    @TypeConverter
    fun fromAttendanceStatus(status: AttendanceStatus?): String {
        return status?.name ?: AttendanceStatus.HADIR.name
    }

    @TypeConverter
    fun toAttendanceStatus(value: String?): AttendanceStatus {
        return AttendanceStatus.fromString(value)
    }
}
