package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM karyawan ORDER BY nama ASC")
    fun getAllEmployees(): Flow<List<Employee>>

    @Query("SELECT * FROM karyawan ORDER BY nama ASC")
    suspend fun getAllEmployeesList(): List<Employee>

    @Query("SELECT * FROM karyawan WHERE id = :id")
    suspend fun getEmployeeById(id: Long): Employee?

    @Query("SELECT * FROM karyawan WHERE nik = :nik LIMIT 1")
    suspend fun getEmployeeByNik(nik: String): Employee?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEmployee(employee: Employee): Long

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Delete
    suspend fun deleteEmployee(employee: Employee)

    @Query("DELETE FROM karyawan WHERE id = :id")
    suspend fun deleteEmployeeById(id: Long)

    @Query("SELECT COUNT(*) FROM karyawan")
    suspend fun getCount(): Int
}

@Dao
interface AttendanceDao {
    @Query("""
        SELECT 
            d.id AS id,
            d.tanggal AS tanggal,
            d.catatan AS catatan,
            d.createdAt AS createdAt,
            COUNT(r.id) AS totalKaryawan,
            COALESCE(SUM(CASE WHEN r.status = 'HADIR' THEN 1 ELSE 0 END), 0) AS jumlahHadir,
            COALESCE(SUM(CASE WHEN r.status = 'IZIN' THEN 1 ELSE 0 END), 0) AS jumlahIzin,
            COALESCE(SUM(CASE WHEN r.status = 'ALPA' THEN 1 ELSE 0 END), 0) AS jumlahAlpa,
            COALESCE(SUM(CASE WHEN r.status = 'SAKIT' THEN 1 ELSE 0 END), 0) AS jumlahSakit,
            COALESCE(SUM(CASE WHEN r.status = 'CUTI' THEN 1 ELSE 0 END), 0) AS jumlahCuti,
            COALESCE(SUM(CASE WHEN r.status = 'SETENGAH_HARI' THEN 1 ELSE 0 END), 0) AS jumlahSetengahHari
        FROM presensi_tanggal d
        LEFT JOIN presensi_record r ON d.id = r.attendanceDateId
        GROUP BY d.id
        ORDER BY d.tanggal DESC
    """)
    fun getAllSummaries(): Flow<List<AttendanceDateSummary>>

    @Query("""
        SELECT 
            d.id AS id,
            d.tanggal AS tanggal,
            d.catatan AS catatan,
            d.createdAt AS createdAt,
            COUNT(r.id) AS totalKaryawan,
            COALESCE(SUM(CASE WHEN r.status = 'HADIR' THEN 1 ELSE 0 END), 0) AS jumlahHadir,
            COALESCE(SUM(CASE WHEN r.status = 'IZIN' THEN 1 ELSE 0 END), 0) AS jumlahIzin,
            COALESCE(SUM(CASE WHEN r.status = 'ALPA' THEN 1 ELSE 0 END), 0) AS jumlahAlpa,
            COALESCE(SUM(CASE WHEN r.status = 'SAKIT' THEN 1 ELSE 0 END), 0) AS jumlahSakit,
            COALESCE(SUM(CASE WHEN r.status = 'CUTI' THEN 1 ELSE 0 END), 0) AS jumlahCuti,
            COALESCE(SUM(CASE WHEN r.status = 'SETENGAH_HARI' THEN 1 ELSE 0 END), 0) AS jumlahSetengahHari
        FROM presensi_tanggal d
        LEFT JOIN presensi_record r ON d.id = r.attendanceDateId
        WHERE d.tanggal >= :startDate AND d.tanggal <= :endDate
        GROUP BY d.id
        ORDER BY d.tanggal DESC
    """)
    fun getSummariesByDateRange(startDate: String, endDate: String): Flow<List<AttendanceDateSummary>>

    @Query("SELECT * FROM presensi_tanggal WHERE id = :id LIMIT 1")
    suspend fun getDateById(id: Long): AttendanceDate?

    @Query("SELECT * FROM presensi_tanggal WHERE tanggal = :tanggal LIMIT 1")
    suspend fun getDateByTanggal(tanggal: String): AttendanceDate?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDate(date: AttendanceDate): Long

    @Query("DELETE FROM presensi_tanggal WHERE id = :id")
    suspend fun deleteDateById(id: Long)

    @Query("SELECT * FROM presensi_record WHERE attendanceDateId = :dateId ORDER BY namaKaryawan ASC")
    fun getRecordsByDateId(dateId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM presensi_record WHERE attendanceDateId = :dateId ORDER BY namaKaryawan ASC")
    suspend fun getRecordsByDateIdSync(dateId: Long): List<AttendanceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecord>)

    @Query("DELETE FROM presensi_record WHERE attendanceDateId = :dateId")
    suspend fun deleteRecordsByDateId(dateId: Long)

    @Query("UPDATE presensi_record SET status = :status, keterangan = :keterangan WHERE id = :recordId")
    suspend fun updateRecordStatus(recordId: Long, status: AttendanceStatus, keterangan: String)

    @Update
    suspend fun updateRecord(record: AttendanceRecord)

    @Query("DELETE FROM presensi_record WHERE employeeId = :employeeId")
    suspend fun deleteRecordsByEmployeeId(employeeId: Long)
}

@Dao
interface MaterialDao {
    @Query("SELECT * FROM material ORDER BY nama ASC")
    fun getAllMaterials(): Flow<List<MaterialItem>>

    @Query("SELECT * FROM material WHERE id = :id LIMIT 1")
    suspend fun getMaterialById(id: Long): MaterialItem?

    @Query("SELECT * FROM material WHERE kode = :kode LIMIT 1")
    suspend fun getMaterialByKode(kode: String): MaterialItem?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMaterial(material: MaterialItem): Long

    @Update
    suspend fun updateMaterial(material: MaterialItem)

    @Query("DELETE FROM material WHERE id = :id")
    suspend fun deleteMaterialById(id: Long)

    @Query("UPDATE material SET stok = :newStock, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStock(id: Long, newStock: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM material")
    suspend fun getCount(): Int
}

@Dao
interface BomDao {
    @Query("SELECT * FROM bom_header ORDER BY noBom ASC")
    fun getAllBomHeaders(): Flow<List<BomHeader>>

    @Query("SELECT * FROM bom_header WHERE id = :id LIMIT 1")
    suspend fun getBomById(id: Long): BomHeader?

    @Query("SELECT * FROM bom_header WHERE noBom = :noBom LIMIT 1")
    suspend fun getBomByNoBom(noBom: String): BomHeader?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBomHeader(header: BomHeader): Long

    @Update
    suspend fun updateBomHeader(header: BomHeader)

    @Query("DELETE FROM bom_header WHERE id = :id")
    suspend fun deleteBomHeaderById(id: Long)

    @Query("SELECT * FROM bom_item WHERE bomHeaderId = :bomHeaderId ORDER BY namaMaterial ASC")
    fun getItemsByBomHeaderId(bomHeaderId: Long): Flow<List<BomItem>>

    @Query("SELECT * FROM bom_item WHERE bomHeaderId = :bomHeaderId ORDER BY namaMaterial ASC")
    suspend fun getItemsByBomHeaderIdSync(bomHeaderId: Long): List<BomItem>

    @Query("SELECT * FROM bom_item WHERE id = :id LIMIT 1")
    suspend fun getBomItemById(id: Long): BomItem?

    @Query("SELECT * FROM bom_item WHERE bomHeaderId = :bomHeaderId AND kodeMaterial = :kodeMaterial LIMIT 1")
    suspend fun getBomItemByKode(bomHeaderId: Long, kodeMaterial: String): BomItem?

    @Query("SELECT * FROM bom_item WHERE noBom = :noBom ORDER BY namaMaterial ASC")
    suspend fun getBomItemsByNoBomList(noBom: String): List<BomItem>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBomItem(item: BomItem): Long

    @Update
    suspend fun updateBomItem(item: BomItem)

    @Query("DELETE FROM bom_item WHERE id = :id")
    suspend fun deleteBomItemById(id: Long)
}

@Dao
interface PenerimaanDao {
    @Query("SELECT * FROM penerimaan ORDER BY tanggal DESC, id DESC")
    fun getAllPenerimaan(): Flow<List<PenerimaanItem>>

    @Query("SELECT * FROM penerimaan WHERE id = :id LIMIT 1")
    suspend fun getPenerimaanById(id: Long): PenerimaanItem?

    @Query("SELECT * FROM penerimaan WHERE noBom = :noBom ORDER BY tanggal DESC")
    fun getPenerimaanByNoBom(noBom: String): Flow<List<PenerimaanItem>>

    @Query("SELECT COALESCE(SUM(qtyPesanan), 0.0) FROM penerimaan WHERE noBom = :noBom")
    suspend fun getTotalQtyByNoBom(noBom: String): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPenerimaan(item: PenerimaanItem): Long

    @Update
    suspend fun updatePenerimaan(item: PenerimaanItem)

    @Query("DELETE FROM penerimaan WHERE id = :id")
    suspend fun deletePenerimaanById(id: Long)

    // Penerimaan Details (auto-generated from BOM with editable Qty)
    @Query("SELECT * FROM penerimaan_detail WHERE penerimaanId = :penerimaanId ORDER BY id ASC")
    fun getDetailsByPenerimaanId(penerimaanId: Long): Flow<List<PenerimaanDetailItem>>

    @Query("SELECT * FROM penerimaan_detail WHERE penerimaanId = :penerimaanId ORDER BY id ASC")
    suspend fun getDetailsByPenerimaanIdList(penerimaanId: Long): List<PenerimaanDetailItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPenerimaanDetails(items: List<PenerimaanDetailItem>)

    @Query("UPDATE penerimaan_detail SET qty = :newQty, keterangan = :keterangan WHERE id = :id")
    suspend fun updatePenerimaanDetailQty(id: Long, newQty: Double, keterangan: String = "")

    @Query("DELETE FROM penerimaan_detail WHERE penerimaanId = :penerimaanId")
    suspend fun deleteDetailsByPenerimaanId(penerimaanId: Long)
}

@Dao
interface PemakaianDao {
    @Query("SELECT * FROM pemakaian ORDER BY tanggal DESC, id DESC")
    fun getAllPemakaian(): Flow<List<PemakaianItem>>

    @Query("SELECT * FROM pemakaian WHERE id = :id LIMIT 1")
    suspend fun getPemakaianById(id: Long): PemakaianItem?

    @Query("SELECT * FROM pemakaian WHERE noBom = :noBom ORDER BY tanggal DESC")
    fun getPemakaianByNoBom(noBom: String): Flow<List<PemakaianItem>>

    @Query("SELECT COALESCE(SUM(qtyPemakaian), 0.0) FROM pemakaian WHERE noBom = :noBom")
    suspend fun getTotalQtyByNoBom(noBom: String): Double

    @Query("SELECT COALESCE(SUM(qtyPemakaian), 0.0) FROM pemakaian WHERE noBom = :noBom AND id != :excludeId")
    suspend fun getTotalQtyByNoBomExcludingId(noBom: String, excludeId: Long): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPemakaian(item: PemakaianItem): Long

    @Update
    suspend fun updatePemakaian(item: PemakaianItem)

    @Query("DELETE FROM pemakaian WHERE id = :id")
    suspend fun deletePemakaianById(id: Long)

    // Pemakaian Details (auto-generated from BOM with editable Qty)
    @Query("SELECT * FROM pemakaian_detail WHERE pemakaianId = :pemakaianId ORDER BY id ASC")
    fun getDetailsByPemakaianId(pemakaianId: Long): Flow<List<PemakaianDetailItem>>

    @Query("SELECT * FROM pemakaian_detail WHERE pemakaianId = :pemakaianId ORDER BY id ASC")
    suspend fun getDetailsByPemakaianIdList(pemakaianId: Long): List<PemakaianDetailItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPemakaianDetails(items: List<PemakaianDetailItem>)

    @Query("UPDATE pemakaian_detail SET qty = :newQty, keterangan = :keterangan WHERE id = :id")
    suspend fun updatePemakaianDetailQty(id: Long, newQty: Double, keterangan: String = "")

    @Query("DELETE FROM pemakaian_detail WHERE pemakaianId = :pemakaianId")
    suspend fun deleteDetailsByPemakaianId(pemakaianId: Long)
}
