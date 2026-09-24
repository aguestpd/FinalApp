package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val database: AppDatabase) {

    private val employeeDao = database.employeeDao()
    private val attendanceDao = database.attendanceDao()
    private val materialDao = database.materialDao()
    private val bomDao = database.bomDao()
    private val penerimaanDao = database.penerimaanDao()
    private val pemakaianDao = database.pemakaianDao()

    // --- EMPLOYEE OPERATIONS ---
    val allEmployees: Flow<List<Employee>> = employeeDao.getAllEmployees()

    suspend fun getEmployeeByNik(nik: String): Employee? {
        return employeeDao.getEmployeeByNik(nik.trim())
    }

    suspend fun insertEmployee(employee: Employee): Result<Long> {
        return try {
            val trimmedNik = employee.nik.trim()
            val existing = employeeDao.getEmployeeByNik(trimmedNik)
            if (existing != null) {
                Result.failure(IllegalArgumentException("NIK '$trimmedNik' sudah terdaftar!"))
            } else {
                val id = employeeDao.insertEmployee(employee.copy(nik = trimmedNik, nama = employee.nama.trim(), jabatan = employee.jabatan.trim()))
                Result.success(id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEmployee(employee: Employee): Result<Unit> {
        return try {
            val trimmedNik = employee.nik.trim()
            val existing = employeeDao.getEmployeeByNik(trimmedNik)
            if (existing != null && existing.id != employee.id) {
                Result.failure(IllegalArgumentException("NIK '$trimmedNik' sudah digunakan oleh karyawan lain!"))
            } else {
                employeeDao.updateEmployee(employee.copy(nik = trimmedNik, nama = employee.nama.trim(), jabatan = employee.jabatan.trim()))
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEmployee(employee: Employee) {
        attendanceDao.deleteRecordsByEmployeeId(employee.id)
        employeeDao.deleteEmployee(employee)
    }

    suspend fun deleteEmployeeById(id: Long) {
        attendanceDao.deleteRecordsByEmployeeId(id)
        employeeDao.deleteEmployeeById(id)
    }

    // --- ATTENDANCE OPERATIONS ---
    val allAttendanceSummaries: Flow<List<AttendanceDateSummary>> = attendanceDao.getAllSummaries()

    fun getAttendanceSummariesByRange(startDate: String, endDate: String): Flow<List<AttendanceDateSummary>> {
        return attendanceDao.getSummariesByDateRange(startDate, endDate)
    }

    suspend fun getDateById(id: Long): AttendanceDate? {
        return attendanceDao.getDateById(id)
    }

    suspend fun getDateByTanggal(tanggal: String): AttendanceDate? {
        return attendanceDao.getDateByTanggal(tanggal.trim())
    }

    suspend fun getAllEmployeesSync(): List<Employee> {
        return employeeDao.getAllEmployeesList()
    }

    suspend fun createAttendanceDate(tanggal: String, catatan: String = "", autoPopulate: Boolean = false): Result<Long> {
        val trimmedDate = tanggal.trim()
        if (trimmedDate.isEmpty()) {
            return Result.failure(IllegalArgumentException("Tanggal wajib diisi!"))
        }

        val existing = attendanceDao.getDateByTanggal(trimmedDate)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Tanggal $trimmedDate sudah ada dalam data presensi! Tanggal tidak bisa ganda."))
        }

        return try {
            val newDateId = attendanceDao.insertDate(
                AttendanceDate(
                    tanggal = trimmedDate,
                    catatan = catatan.trim()
                )
            )

            if (autoPopulate) {
                // Populate records from current employee list if requested
                val employees = employeeDao.getAllEmployeesList()
                if (employees.isNotEmpty()) {
                    val records = employees.map { emp ->
                        AttendanceRecord(
                            attendanceDateId = newDateId,
                            employeeId = emp.id,
                            namaKaryawan = emp.nama,
                            nik = emp.nik,
                            jabatan = emp.jabatan,
                            status = AttendanceStatus.HADIR,
                            keterangan = ""
                        )
                    }
                    attendanceDao.insertRecords(records)
                }
            }
            Result.success(newDateId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveRecordsForDate(dateId: Long, records: List<AttendanceRecord>): Result<Unit> {
        return try {
            attendanceDao.deleteRecordsByDateId(dateId)
            if (records.isNotEmpty()) {
                attendanceDao.insertRecords(records)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDateById(id: Long) {
        attendanceDao.deleteDateById(id)
    }

    fun getRecordsByDateId(dateId: Long): Flow<List<AttendanceRecord>> {
        return attendanceDao.getRecordsByDateId(dateId)
    }

    suspend fun getRecordsByDateIdSync(dateId: Long): List<AttendanceRecord> {
        return attendanceDao.getRecordsByDateIdSync(dateId)
    }

    suspend fun updateRecordStatus(recordId: Long, status: AttendanceStatus, keterangan: String) {
        attendanceDao.updateRecordStatus(recordId, status, keterangan.trim())
    }

    suspend fun updateRecord(record: AttendanceRecord) {
        attendanceDao.updateRecord(record)
    }

    // --- MATERIAL OPERATIONS ---
    val allMaterials: Flow<List<MaterialItem>> = materialDao.getAllMaterials()

    suspend fun insertMaterial(material: MaterialItem): Result<Long> {
        return try {
            val trimmedKode = material.kode.trim().uppercase()
            val existing = materialDao.getMaterialByKode(trimmedKode)
            if (existing != null) {
                Result.failure(IllegalArgumentException("Kode material '$trimmedKode' sudah terdaftar!"))
            } else {
                val id = materialDao.insertMaterial(
                    material.copy(
                        kode = trimmedKode,
                        nama = material.nama.trim(),
                        satuan = material.satuan.trim(),
                        kategori = material.kategori.trim()
                    )
                )
                Result.success(id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMaterial(material: MaterialItem): Result<Unit> {
        return try {
            val trimmedKode = material.kode.trim().uppercase()
            val existing = materialDao.getMaterialByKode(trimmedKode)
            if (existing != null && existing.id != material.id) {
                Result.failure(IllegalArgumentException("Kode material '$trimmedKode' sudah digunakan material lain!"))
            } else {
                materialDao.updateMaterial(
                    material.copy(
                        kode = trimmedKode,
                        nama = material.nama.trim(),
                        satuan = material.satuan.trim(),
                        kategori = material.kategori.trim(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMaterialById(id: Long) {
        materialDao.deleteMaterialById(id)
    }

    suspend fun updateStock(id: Long, newStock: Double) {
        materialDao.updateStock(id, newStock.coerceAtLeast(0.0))
    }

    // --- BOM OPERATIONS ---
    val allBomHeaders: Flow<List<BomHeader>> = bomDao.getAllBomHeaders()

    suspend fun getBomById(id: Long): BomHeader? = bomDao.getBomById(id)

    suspend fun getBomByNoBom(noBom: String): BomHeader? = bomDao.getBomByNoBom(noBom.trim().uppercase())

    suspend fun insertBomHeader(noBom: String, model: String, keterangan: String = ""): Result<Long> {
        return try {
            val trimmedNo = noBom.trim().uppercase()
            val trimmedModel = model.trim()
            if (trimmedNo.isEmpty()) {
                return Result.failure(IllegalArgumentException("No BOM wajib diisi!"))
            }
            if (trimmedModel.isEmpty()) {
                return Result.failure(IllegalArgumentException("Model wajib diisi!"))
            }
            val existing = bomDao.getBomByNoBom(trimmedNo)
            if (existing != null) {
                return Result.failure(IllegalArgumentException("No BOM '$trimmedNo' sudah ada! Gunakan No BOM yang lain."))
            }
            val id = bomDao.insertBomHeader(BomHeader(noBom = trimmedNo, model = trimmedModel, keterangan = keterangan.trim()))
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBomHeader(id: Long, noBom: String, model: String, keterangan: String = ""): Result<Unit> {
        return try {
            val trimmedNo = noBom.trim().uppercase()
            val trimmedModel = model.trim()
            if (trimmedNo.isEmpty()) {
                return Result.failure(IllegalArgumentException("No BOM wajib diisi!"))
            }
            if (trimmedModel.isEmpty()) {
                return Result.failure(IllegalArgumentException("Model wajib diisi!"))
            }
            val existing = bomDao.getBomByNoBom(trimmedNo)
            if (existing != null && existing.id != id) {
                return Result.failure(IllegalArgumentException("No BOM '$trimmedNo' sudah digunakan oleh master BOM lain!"))
            }
            bomDao.updateBomHeader(BomHeader(id = id, noBom = trimmedNo, model = trimmedModel, keterangan = keterangan.trim()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBomHeaderById(id: Long) {
        bomDao.deleteBomHeaderById(id)
    }

    fun getItemsByBomHeaderId(bomHeaderId: Long): Flow<List<BomItem>> = bomDao.getItemsByBomHeaderId(bomHeaderId)

    suspend fun insertBomItem(bomHeaderId: Long, noBom: String, kodeMaterial: String, kebutuhan: Double, keterangan: String = ""): Result<Long> {
        return try {
            val trimmedKode = kodeMaterial.trim().uppercase()
            if (trimmedKode.isEmpty()) {
                return Result.failure(IllegalArgumentException("Kode material tidak boleh kosong!"))
            }
            if (kebutuhan <= 0) {
                return Result.failure(IllegalArgumentException("Kebutuhan / Qty harus lebih dari 0!"))
            }
            // Validasi: periksa apakah kode material ada pada daftar material
            val material = materialDao.getMaterialByKode(trimmedKode)
            if (material == null) {
                return Result.failure(IllegalArgumentException("Kode Material '$trimmedKode' TIDAK DITEMUKAN pada daftar master Material! Data tidak bisa disimpan."))
            }

            val existingItem = bomDao.getBomItemByKode(bomHeaderId, trimmedKode)
            if (existingItem != null) {
                return Result.failure(IllegalArgumentException("Material '${material.nama}' ($trimmedKode) sudah ada dalam BOM ini! Silakan gunakan tombol edit."))
            }

            val id = bomDao.insertBomItem(
                BomItem(
                    bomHeaderId = bomHeaderId,
                    noBom = noBom.trim().uppercase(),
                    kodeMaterial = material.kode,
                    namaMaterial = material.nama,
                    satuan = material.satuan,
                    kebutuhan = kebutuhan,
                    keterangan = keterangan.trim()
                )
            )
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBomItem(item: BomItem, kebutuhan: Double, keterangan: String): Result<Unit> {
        return try {
            if (kebutuhan <= 0) {
                return Result.failure(IllegalArgumentException("Kebutuhan / Qty harus lebih dari 0!"))
            }
            bomDao.updateBomItem(item.copy(kebutuhan = kebutuhan, keterangan = keterangan.trim()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBomItemById(id: Long) {
        bomDao.deleteBomItemById(id)
    }

    // --- PENERIMAAN OPERATIONS ---
    val allPenerimaan: Flow<List<PenerimaanItem>> = penerimaanDao.getAllPenerimaan()

    suspend fun getTotalPenerimaanByNoBom(noBom: String): Double {
        return penerimaanDao.getTotalQtyByNoBom(noBom.trim().uppercase())
    }

    suspend fun insertPenerimaan(tanggal: String, noBom: String, qtyPesanan: Double, keterangan: String = ""): Result<Long> {
        return try {
            val trimmedTanggal = tanggal.trim()
            val trimmedNo = noBom.trim().uppercase()
            if (trimmedTanggal.isEmpty()) {
                return Result.failure(IllegalArgumentException("Tanggal penerimaan wajib diisi!"))
            }
            if (trimmedNo.isEmpty()) {
                return Result.failure(IllegalArgumentException("No BOM wajib diisi!"))
            }
            if (qtyPesanan <= 0) {
                return Result.failure(IllegalArgumentException("Qty pesanan harus lebih dari 0!"))
            }

            // Validasi: No BOM harus terdaftar pada master BOM
            val bom = bomDao.getBomByNoBom(trimmedNo)
            if (bom == null) {
                return Result.failure(IllegalArgumentException("No BOM '$trimmedNo' TIDAK DITEMUKAN pada daftar BOM! Data penerimaan gagal disimpan."))
            }

            val id = penerimaanDao.insertPenerimaan(
                PenerimaanItem(
                    tanggal = trimmedTanggal,
                    noBom = bom.noBom,
                    qtyPesanan = qtyPesanan,
                    keterangan = keterangan.trim()
                )
            )

            // Auto-generate Penerimaan Detail items from BOM
            val bomItems = bomDao.getBomItemsByNoBomList(bom.noBom)
            val details = bomItems.map { item ->
                PenerimaanDetailItem(
                    penerimaanId = id,
                    noBom = bom.noBom,
                    kodeMaterial = item.kodeMaterial,
                    namaMaterial = item.namaMaterial,
                    satuan = item.satuan,
                    kebutuhan = item.kebutuhan,
                    qty = item.kebutuhan * qtyPesanan,
                    keterangan = item.keterangan
                )
            }
            if (details.isNotEmpty()) {
                penerimaanDao.insertPenerimaanDetails(details)
            }

            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePenerimaan(id: Long, tanggal: String, noBom: String, qtyPesanan: Double, keterangan: String = ""): Result<Unit> {
        return try {
            val trimmedTanggal = tanggal.trim()
            val trimmedNo = noBom.trim().uppercase()
            if (trimmedTanggal.isEmpty()) {
                return Result.failure(IllegalArgumentException("Tanggal penerimaan wajib diisi!"))
            }
            if (trimmedNo.isEmpty()) {
                return Result.failure(IllegalArgumentException("No BOM wajib diisi!"))
            }
            if (qtyPesanan <= 0) {
                return Result.failure(IllegalArgumentException("Qty pesanan harus lebih dari 0!"))
            }

            val bom = bomDao.getBomByNoBom(trimmedNo)
            if (bom == null) {
                return Result.failure(IllegalArgumentException("No BOM '$trimmedNo' TIDAK DITEMUKAN pada daftar BOM! Data penerimaan gagal disimpan."))
            }

            penerimaanDao.updatePenerimaan(
                PenerimaanItem(
                    id = id,
                    tanggal = trimmedTanggal,
                    noBom = bom.noBom,
                    qtyPesanan = qtyPesanan,
                    keterangan = keterangan.trim()
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePenerimaanById(id: Long) {
        penerimaanDao.deletePenerimaanById(id)
    }

    // --- PEMAKAIAN OPERATIONS ---
    val allPemakaian: Flow<List<PemakaianItem>> = pemakaianDao.getAllPemakaian()

    suspend fun getTotalPemakaianByNoBom(noBom: String): Double {
        return pemakaianDao.getTotalQtyByNoBom(noBom.trim().uppercase())
    }

    suspend fun insertPemakaian(tanggal: String, noBom: String, qtyPemakaian: Double, keterangan: String = ""): Result<Long> {
        return try {
            val trimmedTanggal = tanggal.trim()
            val trimmedNo = noBom.trim().uppercase()
            if (trimmedTanggal.isEmpty()) {
                return Result.failure(IllegalArgumentException("Tanggal pemakaian wajib diisi!"))
            }
            if (trimmedNo.isEmpty()) {
                return Result.failure(IllegalArgumentException("No BOM wajib diisi!"))
            }
            if (qtyPemakaian <= 0) {
                return Result.failure(IllegalArgumentException("Qty pemakaian harus lebih dari 0!"))
            }

            // Validasi 1: No BOM harus terdaftar pada master BOM
            val bom = bomDao.getBomByNoBom(trimmedNo)
            if (bom == null) {
                return Result.failure(IllegalArgumentException("No BOM '$trimmedNo' TIDAK DITEMUKAN pada daftar BOM! Data pemakaian gagal disimpan."))
            }

            // Validasi 2: Kondisi Qty pemakaian tidak boleh lebih dari Qty penerimaan
            val totalPenerimaan = penerimaanDao.getTotalQtyByNoBom(trimmedNo)
            val currentPemakaian = pemakaianDao.getTotalQtyByNoBom(trimmedNo)

            if (totalPenerimaan <= 0.0) {
                return Result.failure(IllegalArgumentException("Belum ada data penerimaan untuk No BOM '$trimmedNo'! Qty pemakaian tidak boleh melebihi penerimaan (Total Penerimaan: 0)."))
            }

            if (currentPemakaian + qtyPemakaian > totalPenerimaan) {
                val sisaBoleh = (totalPenerimaan - currentPemakaian).coerceAtLeast(0.0)
                return Result.failure(IllegalArgumentException("Qty pemakaian ($qtyPemakaian) melebihi total penerimaan ($totalPenerimaan)! Pemakaian saat ini: $currentPemakaian. Maksimal pemakaian yang dapat diinput adalah $sisaBoleh."))
            }

            val id = pemakaianDao.insertPemakaian(
                PemakaianItem(
                    tanggal = trimmedTanggal,
                    noBom = bom.noBom,
                    qtyPemakaian = qtyPemakaian,
                    keterangan = keterangan.trim()
                )
            )

            // Auto-generate Pemakaian Detail items from BOM
            val bomItems = bomDao.getBomItemsByNoBomList(bom.noBom)
            val details = bomItems.map { item ->
                PemakaianDetailItem(
                    pemakaianId = id,
                    noBom = bom.noBom,
                    kodeMaterial = item.kodeMaterial,
                    namaMaterial = item.namaMaterial,
                    satuan = item.satuan,
                    kebutuhan = item.kebutuhan,
                    qty = item.kebutuhan * qtyPemakaian,
                    keterangan = item.keterangan
                )
            }
            if (details.isNotEmpty()) {
                pemakaianDao.insertPemakaianDetails(details)
            }

            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePemakaian(id: Long, tanggal: String, noBom: String, qtyPemakaian: Double, keterangan: String = ""): Result<Unit> {
        return try {
            val trimmedTanggal = tanggal.trim()
            val trimmedNo = noBom.trim().uppercase()
            if (trimmedTanggal.isEmpty()) {
                return Result.failure(IllegalArgumentException("Tanggal pemakaian wajib diisi!"))
            }
            if (trimmedNo.isEmpty()) {
                return Result.failure(IllegalArgumentException("No BOM wajib diisi!"))
            }
            if (qtyPemakaian <= 0) {
                return Result.failure(IllegalArgumentException("Qty pemakaian harus lebih dari 0!"))
            }

            val bom = bomDao.getBomByNoBom(trimmedNo)
            if (bom == null) {
                return Result.failure(IllegalArgumentException("No BOM '$trimmedNo' TIDAK DITEMUKAN pada daftar BOM! Data pemakaian gagal disimpan."))
            }

            val totalPenerimaan = penerimaanDao.getTotalQtyByNoBom(trimmedNo)
            val otherPemakaian = pemakaianDao.getTotalQtyByNoBomExcludingId(trimmedNo, id)

            if (totalPenerimaan <= 0.0) {
                return Result.failure(IllegalArgumentException("Belum ada data penerimaan untuk No BOM '$trimmedNo'! Qty pemakaian tidak boleh melebihi penerimaan (Total Penerimaan: 0)."))
            }

            if (otherPemakaian + qtyPemakaian > totalPenerimaan) {
                val sisaBoleh = (totalPenerimaan - otherPemakaian).coerceAtLeast(0.0)
                return Result.failure(IllegalArgumentException("Qty pemakaian ($qtyPemakaian) melebihi total penerimaan ($totalPenerimaan)! Pemakaian lainnya: $otherPemakaian. Maksimal yang dapat diinput adalah $sisaBoleh."))
            }

            pemakaianDao.updatePemakaian(
                PemakaianItem(
                    id = id,
                    tanggal = trimmedTanggal,
                    noBom = bom.noBom,
                    qtyPemakaian = qtyPemakaian,
                    keterangan = keterangan.trim()
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePemakaianById(id: Long) {
        pemakaianDao.deletePemakaianById(id)
    }

    // --- DETAIL PENERIMAAN (Auto-generated from BOM with editable Qty) ---
    fun getPenerimaanDetailsFlow(penerimaanId: Long): Flow<List<PenerimaanDetailItem>> {
        return penerimaanDao.getDetailsByPenerimaanId(penerimaanId)
    }

    suspend fun getOrGeneratePenerimaanDetails(penerimaanId: Long): List<PenerimaanDetailItem> {
        val existing = penerimaanDao.getDetailsByPenerimaanIdList(penerimaanId)
        if (existing.isNotEmpty()) {
            return existing
        }
        val penerimaan = penerimaanDao.getPenerimaanById(penerimaanId) ?: return emptyList()
        val bomItems = bomDao.getBomItemsByNoBomList(penerimaan.noBom)
        val generated = bomItems.map { item ->
            PenerimaanDetailItem(
                penerimaanId = penerimaan.id,
                noBom = penerimaan.noBom,
                kodeMaterial = item.kodeMaterial,
                namaMaterial = item.namaMaterial,
                satuan = item.satuan,
                kebutuhan = item.kebutuhan,
                qty = item.kebutuhan * penerimaan.qtyPesanan,
                keterangan = item.keterangan
            )
        }
        if (generated.isNotEmpty()) {
            penerimaanDao.insertPenerimaanDetails(generated)
        }
        return penerimaanDao.getDetailsByPenerimaanIdList(penerimaanId)
    }

    suspend fun updatePenerimaanDetailQty(id: Long, newQty: Double, keterangan: String = ""): Result<Unit> {
        return try {
            if (newQty < 0) {
                return Result.failure(IllegalArgumentException("Qty tidak boleh bernilai negatif!"))
            }
            penerimaanDao.updatePenerimaanDetailQty(id, newQty, keterangan)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- DETAIL PEMAKAIAN (Auto-generated from BOM with editable Qty) ---
    fun getPemakaianDetailsFlow(pemakaianId: Long): Flow<List<PemakaianDetailItem>> {
        return pemakaianDao.getDetailsByPemakaianId(pemakaianId)
    }

    suspend fun getOrGeneratePemakaianDetails(pemakaianId: Long): List<PemakaianDetailItem> {
        val existing = pemakaianDao.getDetailsByPemakaianIdList(pemakaianId)
        if (existing.isNotEmpty()) {
            return existing
        }
        val pemakaian = pemakaianDao.getPemakaianById(pemakaianId) ?: return emptyList()
        val bomItems = bomDao.getBomItemsByNoBomList(pemakaian.noBom)
        val generated = bomItems.map { item ->
            PemakaianDetailItem(
                pemakaianId = pemakaian.id,
                noBom = pemakaian.noBom,
                kodeMaterial = item.kodeMaterial,
                namaMaterial = item.namaMaterial,
                satuan = item.satuan,
                kebutuhan = item.kebutuhan,
                qty = item.kebutuhan * pemakaian.qtyPemakaian,
                keterangan = item.keterangan
            )
        }
        if (generated.isNotEmpty()) {
            pemakaianDao.insertPemakaianDetails(generated)
        }
        return pemakaianDao.getDetailsByPemakaianIdList(pemakaianId)
    }

    suspend fun updatePemakaianDetailQty(id: Long, newQty: Double, keterangan: String = ""): Result<Unit> {
        return try {
            if (newQty < 0) {
                return Result.failure(IllegalArgumentException("Qty tidak boleh bernilai negatif!"))
            }
            pemakaianDao.updatePemakaianDetailQty(id, newQty, keterangan)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
