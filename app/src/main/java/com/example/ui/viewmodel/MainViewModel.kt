package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.AttendanceDate
import com.example.data.AttendanceDateSummary
import com.example.data.AttendanceRecord
import com.example.data.AttendanceStatus
import com.example.data.BomHeader
import com.example.data.BomItem
import com.example.data.Employee
import com.example.data.EmployeeAttendanceInput
import com.example.data.MaterialItem
import com.example.data.PemakaianDetailItem
import com.example.data.PemakaianItem
import com.example.data.PenerimaanDetailItem
import com.example.data.PenerimaanItem
import com.example.util.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppTab(val label: String) {
    KARYAWAN("Karyawan"),
    PRESENSI("Presensi"),
    MATERIAL("Material")
}

enum class MaterialSubTab(val label: String) {
    MATERIAL("Material"),
    BOM("BOM"),
    PENERIMAAN("Penerimaan"),
    PEMAKAIAN("Pemakaian")
}

class MainViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    // --- NAVIGATION STATE ---
    private val _currentTab = MutableStateFlow(AppTab.PRESENSI)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _selectedDateForDetail = MutableStateFlow<AttendanceDate?>(null)
    val selectedDateForDetail: StateFlow<AttendanceDate?> = _selectedDateForDetail.asStateFlow()

    private val _selectedDateForInput = MutableStateFlow<AttendanceDate?>(null)
    val selectedDateForInput: StateFlow<AttendanceDate?> = _selectedDateForInput.asStateFlow()

    private val _inputAttendanceItems = MutableStateFlow<List<EmployeeAttendanceInput>>(emptyList())
    val inputAttendanceItems: StateFlow<List<EmployeeAttendanceInput>> = _inputAttendanceItems.asStateFlow()

    private val _inputSearchQuery = MutableStateFlow("")
    val inputSearchQuery: StateFlow<String> = _inputSearchQuery.asStateFlow()

    private val _isInputLoading = MutableStateFlow(false)
    val isInputLoading: StateFlow<Boolean> = _isInputLoading.asStateFlow()

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
        _selectedDateForDetail.value = null
        _selectedDateForInput.value = null
    }

    fun openDetailPresensi(dateId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val date = repository.getDateById(dateId)
            _selectedDateForDetail.value = date
        }
    }

    fun closeDetailPresensi() {
        _selectedDateForDetail.value = null
    }

    fun openInputPresensi(dateId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _isInputLoading.value = true
            val date = repository.getDateById(dateId)
            _selectedDateForInput.value = date
            if (date != null) {
                val employees = repository.getAllEmployeesSync()
                val existingRecords = repository.getRecordsByDateIdSync(dateId)
                val recordMap = existingRecords.associateBy { it.employeeId }

                val items = employees.map { emp ->
                    val existing = recordMap[emp.id]
                    EmployeeAttendanceInput(
                        employeeId = emp.id,
                        nik = emp.nik,
                        nama = emp.nama,
                        jabatan = emp.jabatan,
                        status = existing?.status ?: AttendanceStatus.HADIR,
                        keterangan = existing?.keterangan ?: "",
                        recordId = existing?.id
                    )
                }
                _inputAttendanceItems.value = items
            }
            _inputSearchQuery.value = ""
            _isInputLoading.value = false
        }
    }

    fun closeInputPresensi() {
        _selectedDateForInput.value = null
        _inputAttendanceItems.value = emptyList()
        _inputSearchQuery.value = ""
    }

    fun setInputSearchQuery(query: String) {
        _inputSearchQuery.value = query
    }

    fun updateEmployeeStatusInInput(employeeId: Long, newStatus: AttendanceStatus) {
        _inputAttendanceItems.value = _inputAttendanceItems.value.map { item ->
            if (item.employeeId == employeeId) item.copy(status = newStatus)
            else item
        }
    }

    fun updateEmployeeKeteranganInInput(employeeId: Long, newKeterangan: String) {
        _inputAttendanceItems.value = _inputAttendanceItems.value.map { item ->
            if (item.employeeId == employeeId) item.copy(keterangan = newKeterangan)
            else item
        }
    }

    fun setAllEmployeesStatus(status: AttendanceStatus) {
        _inputAttendanceItems.value = _inputAttendanceItems.value.map { item ->
            item.copy(status = status)
        }
    }

    fun saveInputPresensi(onSuccess: () -> Unit) {
        val date = _selectedDateForInput.value ?: return
        val items = _inputAttendanceItems.value
        viewModelScope.launch(Dispatchers.IO) {
            val records = items.map { item ->
                AttendanceRecord(
                    attendanceDateId = date.id,
                    employeeId = item.employeeId,
                    namaKaryawan = item.nama,
                    nik = item.nik,
                    jabatan = item.jabatan,
                    status = item.status,
                    keterangan = item.keterangan.trim()
                )
            }
            repository.saveRecordsForDate(date.id, records)
            launch(Dispatchers.Main) {
                closeInputPresensi()
                onSuccess()
            }
        }
    }

    // --- KARYAWAN STATE ---
    val allEmployees: StateFlow<List<Employee>> = repository.allEmployees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _employeeSearch = MutableStateFlow("")
    val employeeSearch: StateFlow<String> = _employeeSearch.asStateFlow()

    val filteredEmployees: StateFlow<List<Employee>> = combine(allEmployees, _employeeSearch) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.nama.contains(query, ignoreCase = true) ||
            it.nik.contains(query, ignoreCase = true) ||
            it.jabatan.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _employeeDialogVisible = MutableStateFlow(false)
    val employeeDialogVisible: StateFlow<Boolean> = _employeeDialogVisible.asStateFlow()

    private val _employeeToEdit = MutableStateFlow<Employee?>(null)
    val employeeToEdit: StateFlow<Employee?> = _employeeToEdit.asStateFlow()

    private val _employeeToDelete = MutableStateFlow<Employee?>(null)
    val employeeToDelete: StateFlow<Employee?> = _employeeToDelete.asStateFlow()

    private val _employeeErrorMessage = MutableStateFlow<String?>(null)
    val employeeErrorMessage: StateFlow<String?> = _employeeErrorMessage.asStateFlow()

    fun setEmployeeSearch(query: String) {
        _employeeSearch.value = query
    }

    fun openAddEmployeeDialog() {
        _employeeToEdit.value = null
        _employeeErrorMessage.value = null
        _employeeDialogVisible.value = true
    }

    fun openEditEmployeeDialog(employee: Employee) {
        _employeeToEdit.value = employee
        _employeeErrorMessage.value = null
        _employeeDialogVisible.value = true
    }

    fun closeEmployeeDialog() {
        _employeeDialogVisible.value = false
        _employeeToEdit.value = null
        _employeeErrorMessage.value = null
    }

    fun saveEmployee(nik: String, nama: String, jabatan: String, nomorHp: String) {
        val trimmedNik = nik.trim()
        val trimmedNama = nama.trim()
        val trimmedJabatan = jabatan.trim()

        if (trimmedNik.isEmpty()) {
            _employeeErrorMessage.value = "NIK tidak boleh kosong!"
            return
        }
        if (trimmedNama.isEmpty()) {
            _employeeErrorMessage.value = "Nama karyawan tidak boleh kosong!"
            return
        }
        if (trimmedJabatan.isEmpty()) {
            _employeeErrorMessage.value = "Jabatan tidak boleh kosong!"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val toEdit = _employeeToEdit.value
            val result = if (toEdit == null) {
                repository.insertEmployee(
                    Employee(nik = trimmedNik, nama = trimmedNama, jabatan = trimmedJabatan, nomorHp = nomorHp.trim())
                )
            } else {
                repository.updateEmployee(
                    toEdit.copy(nik = trimmedNik, nama = trimmedNama, jabatan = trimmedJabatan, nomorHp = nomorHp.trim())
                )
            }

            result.fold(
                onSuccess = {
                    closeEmployeeDialog()
                },
                onFailure = { error ->
                    _employeeErrorMessage.value = error.message ?: "Gagal menyimpan data karyawan"
                }
            )
        }
    }

    fun promptDeleteEmployee(employee: Employee) {
        _employeeToDelete.value = employee
    }

    fun dismissDeleteEmployee() {
        _employeeToDelete.value = null
    }

    fun confirmDeleteEmployee() {
        val employee = _employeeToDelete.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteEmployee(employee)
            _employeeToDelete.value = null
        }
    }

    // --- PRESENSI STATE ---
    private val _startDateFilter = MutableStateFlow<String?>(null)
    val startDateFilter: StateFlow<String?> = _startDateFilter.asStateFlow()

    private val _endDateFilter = MutableStateFlow<String?>(null)
    val endDateFilter: StateFlow<String?> = _endDateFilter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val attendanceSummaries: StateFlow<List<AttendanceDateSummary>> = combine(
        _startDateFilter,
        _endDateFilter
    ) { start, end ->
        Pair(start, end)
    }.flatMapLatest { (start, end) ->
        if (!start.isNullOrBlank() && !end.isNullOrBlank()) {
            repository.getAttendanceSummariesByRange(start, end)
        } else {
            repository.allAttendanceSummaries
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _addDateDialogVisible = MutableStateFlow(false)
    val addDateDialogVisible: StateFlow<Boolean> = _addDateDialogVisible.asStateFlow()

    private val _dateValidationError = MutableStateFlow<String?>(null)
    val dateValidationError: StateFlow<String?> = _dateValidationError.asStateFlow()

    private val _dateToDelete = MutableStateFlow<AttendanceDateSummary?>(null)
    val dateToDelete: StateFlow<AttendanceDateSummary?> = _dateToDelete.asStateFlow()

    fun setDateRangeFilter(start: String?, end: String?) {
        _startDateFilter.value = start
        _endDateFilter.value = end
    }

    fun clearDateFilter() {
        _startDateFilter.value = null
        _endDateFilter.value = null
    }

    fun openAddDateDialog() {
        _dateValidationError.value = null
        _addDateDialogVisible.value = true
    }

    fun closeAddDateDialog() {
        _addDateDialogVisible.value = false
        _dateValidationError.value = null
    }

    fun createAttendanceDate(tanggal: String, catatan: String) {
        val trimmedDate = tanggal.trim()
        if (trimmedDate.isEmpty()) {
            _dateValidationError.value = "Pilih tanggal presensi terlebih dahulu!"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Check for duplicate date in database
            val existing = repository.getDateByTanggal(trimmedDate)
            if (existing != null) {
                _dateValidationError.value = "Tanggal $trimmedDate sudah ada dalam rekap presensi! Tanggal tidak boleh dipilih ganda."
                return@launch
            }

            val result = repository.createAttendanceDate(trimmedDate, catatan, autoPopulate = false)
            result.fold(
                onSuccess = { newDateId ->
                    _dateValidationError.value = null
                    _addDateDialogVisible.value = false
                    // Automatically open input presensi for this new date
                    openInputPresensi(newDateId)
                },
                onFailure = { error ->
                    _dateValidationError.value = error.message ?: "Gagal membuat entri presensi baru"
                }
            )
        }
    }

    fun promptDeleteDate(summary: AttendanceDateSummary) {
        _dateToDelete.value = summary
    }

    fun dismissDeleteDate() {
        _dateToDelete.value = null
    }

    fun confirmDeleteDate() {
        val summary = _dateToDelete.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDateById(summary.id)
            _dateToDelete.value = null
        }
    }

    // --- DETAIL PRESENSI STATE ---
    private val _detailStatusFilter = MutableStateFlow<AttendanceStatus?>(null)
    val detailStatusFilter: StateFlow<AttendanceStatus?> = _detailStatusFilter.asStateFlow()

    private val _detailSearch = MutableStateFlow("")
    val detailSearch: StateFlow<String> = _detailSearch.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentDetailRecords: StateFlow<List<AttendanceRecord>> = _selectedDateForDetail
        .flatMapLatest { date ->
            if (date != null) repository.getRecordsByDateId(date.id)
            else MutableStateFlow(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredDetailRecords: StateFlow<List<AttendanceRecord>> = combine(
        currentDetailRecords,
        _detailStatusFilter,
        _detailSearch
    ) { records, statusFilter, query ->
        records.filter { record ->
            val matchStatus = statusFilter == null || record.status == statusFilter
            val matchQuery = query.isBlank() ||
                record.namaKaryawan.contains(query, ignoreCase = true) ||
                record.nik.contains(query, ignoreCase = true) ||
                record.jabatan.contains(query, ignoreCase = true)
            matchStatus && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setDetailStatusFilter(status: AttendanceStatus?) {
        _detailStatusFilter.value = status
    }

    fun setDetailSearch(query: String) {
        _detailSearch.value = query
    }

    fun updateRecordStatus(recordId: Long, newStatus: AttendanceStatus, keterangan: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRecordStatus(recordId, newStatus, keterangan)
        }
    }

    // --- PDF EXPORT & PRINT ---
    fun generatePdfForCurrentDetail(onReady: (File?) -> Unit) {
        val date = _selectedDateForDetail.value ?: run {
            onReady(null)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val records = repository.getRecordsByDateIdSync(date.id)
            val file = PdfGenerator.generateAttendancePdf(
                getApplication(),
                date,
                records
            )
            launch(Dispatchers.Main) {
                onReady(file)
            }
        }
    }

    // --- MATERIAL STATE ---
    val allMaterials: StateFlow<List<MaterialItem>> = repository.allMaterials
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _materialSearch = MutableStateFlow("")
    val materialSearch: StateFlow<String> = _materialSearch.asStateFlow()

    private val _materialCategoryFilter = MutableStateFlow<String?>(null)
    val materialCategoryFilter: StateFlow<String?> = _materialCategoryFilter.asStateFlow()

    val filteredMaterials: StateFlow<List<MaterialItem>> = combine(
        allMaterials,
        _materialSearch,
        _materialCategoryFilter
    ) { list, query, category ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.nama.contains(query, ignoreCase = true) ||
                item.kode.contains(query, ignoreCase = true) ||
                item.kategori.contains(query, ignoreCase = true)
            val matchesCategory = category == null || item.kategori.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _materialDialogVisible = MutableStateFlow(false)
    val materialDialogVisible: StateFlow<Boolean> = _materialDialogVisible.asStateFlow()

    private val _materialToEdit = MutableStateFlow<MaterialItem?>(null)
    val materialToEdit: StateFlow<MaterialItem?> = _materialToEdit.asStateFlow()

    private val _materialToDelete = MutableStateFlow<MaterialItem?>(null)
    val materialToDelete: StateFlow<MaterialItem?> = _materialToDelete.asStateFlow()

    private val _materialErrorMessage = MutableStateFlow<String?>(null)
    val materialErrorMessage: StateFlow<String?> = _materialErrorMessage.asStateFlow()

    fun setMaterialSearch(query: String) {
        _materialSearch.value = query
    }

    fun setMaterialCategoryFilter(category: String?) {
        _materialCategoryFilter.value = category
    }

    fun openAddMaterialDialog() {
        _materialToEdit.value = null
        _materialErrorMessage.value = null
        _materialDialogVisible.value = true
    }

    fun openEditMaterialDialog(material: MaterialItem) {
        _materialToEdit.value = material
        _materialErrorMessage.value = null
        _materialDialogVisible.value = true
    }

    fun closeMaterialDialog() {
        _materialDialogVisible.value = false
        _materialToEdit.value = null
        _materialErrorMessage.value = null
    }

    fun saveMaterial(kode: String, nama: String, satuan: String, stok: Double, kategori: String, lokasi: String, keterangan: String) {
        val trimmedKode = kode.trim()
        val trimmedNama = nama.trim()
        val trimmedSatuan = satuan.trim()
        val trimmedKategori = kategori.trim()

        if (trimmedKode.isEmpty()) {
            _materialErrorMessage.value = "Kode material tidak boleh kosong!"
            return
        }
        if (trimmedNama.isEmpty()) {
            _materialErrorMessage.value = "Nama material tidak boleh kosong!"
            return
        }
        if (trimmedSatuan.isEmpty()) {
            _materialErrorMessage.value = "Satuan tidak boleh kosong!"
            return
        }
        if (trimmedKategori.isEmpty()) {
            _materialErrorMessage.value = "Kategori tidak boleh kosong!"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val toEdit = _materialToEdit.value
            val result = if (toEdit == null) {
                repository.insertMaterial(
                    MaterialItem(
                        kode = trimmedKode,
                        nama = trimmedNama,
                        satuan = trimmedSatuan,
                        stok = stok,
                        kategori = trimmedKategori,
                        lokasi = lokasi.trim(),
                        keterangan = keterangan.trim()
                    )
                )
            } else {
                repository.updateMaterial(
                    toEdit.copy(
                        kode = trimmedKode,
                        nama = trimmedNama,
                        satuan = trimmedSatuan,
                        stok = stok,
                        kategori = trimmedKategori,
                        lokasi = lokasi.trim(),
                        keterangan = keterangan.trim()
                    )
                )
            }

            result.fold(
                onSuccess = {
                    closeMaterialDialog()
                },
                onFailure = { error ->
                    _materialErrorMessage.value = error.message ?: "Gagal menyimpan material"
                }
            )
        }
    }

    fun quickAdjustStock(material: MaterialItem, delta: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val newStock = (material.stok + delta).coerceAtLeast(0.0)
            repository.updateStock(material.id, newStock)
        }
    }

    fun promptDeleteMaterial(material: MaterialItem) {
        _materialToDelete.value = material
    }

    fun dismissDeleteMaterial() {
        _materialToDelete.value = null
    }

    fun confirmDeleteMaterial() {
        val material = _materialToDelete.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMaterialById(material.id)
            _materialToDelete.value = null
        }
    }

    // --- MATERIAL SUB-TABS (Material, BOM, Penerimaan, Pemakaian) ---
    private val _materialSubTab = MutableStateFlow(MaterialSubTab.MATERIAL)
    val materialSubTab: StateFlow<MaterialSubTab> = _materialSubTab.asStateFlow()

    fun selectMaterialSubTab(tab: MaterialSubTab) {
        _materialSubTab.value = tab
        _selectedBomForDetail.value = null
        _selectedPenerimaanForDetail.value = null
        _selectedPemakaianForDetail.value = null
    }

    // --- BOM MASTER OPERATIONS ---
    val allBomHeaders: StateFlow<List<BomHeader>> = repository.allBomHeaders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _bomSearchQuery = MutableStateFlow("")
    val bomSearchQuery: StateFlow<String> = _bomSearchQuery.asStateFlow()

    val filteredBomHeaders: StateFlow<List<BomHeader>> = combine(allBomHeaders, _bomSearchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.noBom.contains(query, ignoreCase = true) ||
            it.model.contains(query, ignoreCase = true) ||
            it.keterangan.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _bomHeaderDialogVisible = MutableStateFlow(false)
    val bomHeaderDialogVisible: StateFlow<Boolean> = _bomHeaderDialogVisible.asStateFlow()

    private val _bomHeaderToEdit = MutableStateFlow<BomHeader?>(null)
    val bomHeaderToEdit: StateFlow<BomHeader?> = _bomHeaderToEdit.asStateFlow()

    private val _bomHeaderToDelete = MutableStateFlow<BomHeader?>(null)
    val bomHeaderToDelete: StateFlow<BomHeader?> = _bomHeaderToDelete.asStateFlow()

    private val _bomHeaderErrorMessage = MutableStateFlow<String?>(null)
    val bomHeaderErrorMessage: StateFlow<String?> = _bomHeaderErrorMessage.asStateFlow()

    fun setBomSearchQuery(query: String) {
        _bomSearchQuery.value = query
    }

    fun openAddBomHeaderDialog() {
        _bomHeaderToEdit.value = null
        _bomHeaderErrorMessage.value = null
        _bomHeaderDialogVisible.value = true
    }

    fun openEditBomHeaderDialog(bom: BomHeader) {
        _bomHeaderToEdit.value = bom
        _bomHeaderErrorMessage.value = null
        _bomHeaderDialogVisible.value = true
    }

    fun closeBomHeaderDialog() {
        _bomHeaderDialogVisible.value = false
        _bomHeaderToEdit.value = null
        _bomHeaderErrorMessage.value = null
    }

    fun saveBomHeader(noBom: String, model: String, keterangan: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val toEdit = _bomHeaderToEdit.value
            val result = if (toEdit == null) {
                repository.insertBomHeader(noBom, model, keterangan)
            } else {
                repository.updateBomHeader(toEdit.id, noBom, model, keterangan)
            }
            result.fold(
                onSuccess = { closeBomHeaderDialog() },
                onFailure = { error -> _bomHeaderErrorMessage.value = error.message ?: "Gagal menyimpan BOM" }
            )
        }
    }

    fun promptDeleteBomHeader(bom: BomHeader) {
        _bomHeaderToDelete.value = bom
    }

    fun dismissDeleteBomHeader() {
        _bomHeaderToDelete.value = null
    }

    fun confirmDeleteBomHeader() {
        val bom = _bomHeaderToDelete.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBomHeaderById(bom.id)
            _bomHeaderToDelete.value = null
            if (_selectedBomForDetail.value?.id == bom.id) {
                _selectedBomForDetail.value = null
            }
        }
    }

    // --- BOM DETAIL (ITEMS) OPERATIONS ---
    private val _selectedBomForDetail = MutableStateFlow<BomHeader?>(null)
    val selectedBomForDetail: StateFlow<BomHeader?> = _selectedBomForDetail.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentBomItems: StateFlow<List<BomItem>> = _selectedBomForDetail
        .flatMapLatest { header ->
            if (header != null) repository.getItemsByBomHeaderId(header.id)
            else MutableStateFlow(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _bomItemDialogVisible = MutableStateFlow(false)
    val bomItemDialogVisible: StateFlow<Boolean> = _bomItemDialogVisible.asStateFlow()

    private val _bomItemToEdit = MutableStateFlow<BomItem?>(null)
    val bomItemToEdit: StateFlow<BomItem?> = _bomItemToEdit.asStateFlow()

    private val _bomItemToDelete = MutableStateFlow<BomItem?>(null)
    val bomItemToDelete: StateFlow<BomItem?> = _bomItemToDelete.asStateFlow()

    private val _bomItemErrorMessage = MutableStateFlow<String?>(null)
    val bomItemErrorMessage: StateFlow<String?> = _bomItemErrorMessage.asStateFlow()

    fun openDetailBom(bom: BomHeader) {
        _selectedBomForDetail.value = bom
    }

    fun closeDetailBom() {
        _selectedBomForDetail.value = null
    }

    fun openAddBomItemDialog() {
        _bomItemToEdit.value = null
        _bomItemErrorMessage.value = null
        _bomItemDialogVisible.value = true
    }

    fun openEditBomItemDialog(item: BomItem) {
        _bomItemToEdit.value = item
        _bomItemErrorMessage.value = null
        _bomItemDialogVisible.value = true
    }

    fun closeBomItemDialog() {
        _bomItemDialogVisible.value = false
        _bomItemToEdit.value = null
        _bomItemErrorMessage.value = null
    }

    fun saveBomItem(kodeMaterial: String, kebutuhan: Double, keterangan: String) {
        val bom = _selectedBomForDetail.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val toEdit = _bomItemToEdit.value
            val result = if (toEdit == null) {
                repository.insertBomItem(bom.id, bom.noBom, kodeMaterial, kebutuhan, keterangan)
            } else {
                repository.updateBomItem(toEdit, kebutuhan, keterangan)
            }
            result.fold(
                onSuccess = { closeBomItemDialog() },
                onFailure = { error -> _bomItemErrorMessage.value = error.message ?: "Gagal menyimpan item BOM" }
            )
        }
    }

    fun promptDeleteBomItem(item: BomItem) {
        _bomItemToDelete.value = item
    }

    fun dismissDeleteBomItem() {
        _bomItemToDelete.value = null
    }

    fun confirmDeleteBomItem() {
        val item = _bomItemToDelete.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBomItemById(item.id)
            _bomItemToDelete.value = null
        }
    }

    // --- PENERIMAAN OPERATIONS ---
    val allPenerimaan: StateFlow<List<PenerimaanItem>> = repository.allPenerimaan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _penerimaanSearchQuery = MutableStateFlow("")
    val penerimaanSearchQuery: StateFlow<String> = _penerimaanSearchQuery.asStateFlow()

    val filteredPenerimaan: StateFlow<List<PenerimaanItem>> = combine(allPenerimaan, _penerimaanSearchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.noBom.contains(query, ignoreCase = true) ||
            it.tanggal.contains(query, ignoreCase = true) ||
            it.keterangan.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _penerimaanDialogVisible = MutableStateFlow(false)
    val penerimaanDialogVisible: StateFlow<Boolean> = _penerimaanDialogVisible.asStateFlow()

    private val _penerimaanToEdit = MutableStateFlow<PenerimaanItem?>(null)
    val penerimaanToEdit: StateFlow<PenerimaanItem?> = _penerimaanToEdit.asStateFlow()

    private val _penerimaanToDelete = MutableStateFlow<PenerimaanItem?>(null)
    val penerimaanToDelete: StateFlow<PenerimaanItem?> = _penerimaanToDelete.asStateFlow()

    private val _penerimaanErrorMessage = MutableStateFlow<String?>(null)
    val penerimaanErrorMessage: StateFlow<String?> = _penerimaanErrorMessage.asStateFlow()

    fun setPenerimaanSearchQuery(query: String) {
        _penerimaanSearchQuery.value = query
    }

    fun openAddPenerimaanDialog() {
        _penerimaanToEdit.value = null
        _penerimaanErrorMessage.value = null
        _penerimaanDialogVisible.value = true
    }

    fun openEditPenerimaanDialog(item: PenerimaanItem) {
        _penerimaanToEdit.value = item
        _penerimaanErrorMessage.value = null
        _penerimaanDialogVisible.value = true
    }

    fun closePenerimaanDialog() {
        _penerimaanDialogVisible.value = false
        _penerimaanToEdit.value = null
        _penerimaanErrorMessage.value = null
    }

    fun savePenerimaan(tanggal: String, noBom: String, qtyPesanan: Double, keterangan: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val toEdit = _penerimaanToEdit.value
            val result = if (toEdit == null) {
                repository.insertPenerimaan(tanggal, noBom, qtyPesanan, keterangan)
            } else {
                repository.updatePenerimaan(toEdit.id, tanggal, noBom, qtyPesanan, keterangan)
            }
            result.fold(
                onSuccess = { closePenerimaanDialog() },
                onFailure = { error -> _penerimaanErrorMessage.value = error.message ?: "Gagal menyimpan penerimaan" }
            )
        }
    }

    fun promptDeletePenerimaan(item: PenerimaanItem) {
        _penerimaanToDelete.value = item
    }

    fun dismissDeletePenerimaan() {
        _penerimaanToDelete.value = null
    }

    fun confirmDeletePenerimaan() {
        val item = _penerimaanToDelete.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePenerimaanById(item.id)
            _penerimaanToDelete.value = null
        }
    }

    // --- PEMAKAIAN OPERATIONS ---
    val allPemakaian: StateFlow<List<PemakaianItem>> = repository.allPemakaian
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pemakaianSearchQuery = MutableStateFlow("")
    val pemakaianSearchQuery: StateFlow<String> = _pemakaianSearchQuery.asStateFlow()

    val filteredPemakaian: StateFlow<List<PemakaianItem>> = combine(allPemakaian, _pemakaianSearchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.noBom.contains(query, ignoreCase = true) ||
            it.tanggal.contains(query, ignoreCase = true) ||
            it.keterangan.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pemakaianDialogVisible = MutableStateFlow(false)
    val pemakaianDialogVisible: StateFlow<Boolean> = _pemakaianDialogVisible.asStateFlow()

    private val _pemakaianToEdit = MutableStateFlow<PemakaianItem?>(null)
    val pemakaianToEdit: StateFlow<PemakaianItem?> = _pemakaianToEdit.asStateFlow()

    private val _pemakaianToDelete = MutableStateFlow<PemakaianItem?>(null)
    val pemakaianToDelete: StateFlow<PemakaianItem?> = _pemakaianToDelete.asStateFlow()

    private val _pemakaianErrorMessage = MutableStateFlow<String?>(null)
    val pemakaianErrorMessage: StateFlow<String?> = _pemakaianErrorMessage.asStateFlow()

    fun setPemakaianSearchQuery(query: String) {
        _pemakaianSearchQuery.value = query
    }

    fun openAddPemakaianDialog() {
        _pemakaianToEdit.value = null
        _pemakaianErrorMessage.value = null
        _pemakaianDialogVisible.value = true
    }

    fun openEditPemakaianDialog(item: PemakaianItem) {
        _pemakaianToEdit.value = item
        _pemakaianErrorMessage.value = null
        _pemakaianDialogVisible.value = true
    }

    fun closePemakaianDialog() {
        _pemakaianDialogVisible.value = false
        _pemakaianToEdit.value = null
        _pemakaianErrorMessage.value = null
    }

    fun savePemakaian(tanggal: String, noBom: String, qtyPemakaian: Double, keterangan: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val toEdit = _pemakaianToEdit.value
            val result = if (toEdit == null) {
                repository.insertPemakaian(tanggal, noBom, qtyPemakaian, keterangan)
            } else {
                repository.updatePemakaian(toEdit.id, tanggal, noBom, qtyPemakaian, keterangan)
            }
            result.fold(
                onSuccess = { closePemakaianDialog() },
                onFailure = { error -> _pemakaianErrorMessage.value = error.message ?: "Gagal menyimpan pemakaian" }
            )
        }
    }

    fun promptDeletePemakaian(item: PemakaianItem) {
        _pemakaianToDelete.value = item
    }

    fun dismissDeletePemakaian() {
        _pemakaianToDelete.value = null
    }

    fun confirmDeletePemakaian() {
        val item = _pemakaianToDelete.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePemakaianById(item.id)
            _pemakaianToDelete.value = null
        }
    }

    // --- DETAIL PENERIMAAN STATE & OPERATIONS ---
    private val _selectedPenerimaanForDetail = MutableStateFlow<PenerimaanItem?>(null)
    val selectedPenerimaanForDetail: StateFlow<PenerimaanItem?> = _selectedPenerimaanForDetail.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentPenerimaanDetails: StateFlow<List<PenerimaanDetailItem>> = _selectedPenerimaanForDetail
        .flatMapLatest { penerimaan ->
            if (penerimaan == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else repository.getPenerimaanDetailsFlow(penerimaan.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _editingPenerimaanDetail = MutableStateFlow<PenerimaanDetailItem?>(null)
    val editingPenerimaanDetail: StateFlow<PenerimaanDetailItem?> = _editingPenerimaanDetail.asStateFlow()

    fun openDetailPenerimaan(penerimaan: PenerimaanItem) {
        _selectedPenerimaanForDetail.value = penerimaan
        viewModelScope.launch(Dispatchers.IO) {
            repository.getOrGeneratePenerimaanDetails(penerimaan.id)
        }
    }

    fun closeDetailPenerimaan() {
        _selectedPenerimaanForDetail.value = null
        _editingPenerimaanDetail.value = null
    }

    fun openEditPenerimaanDetail(detail: PenerimaanDetailItem) {
        _editingPenerimaanDetail.value = detail
    }

    fun closeEditPenerimaanDetail() {
        _editingPenerimaanDetail.value = null
    }

    fun saveEditedPenerimaanDetailQty(id: Long, newQty: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePenerimaanDetailQty(id, newQty)
            _editingPenerimaanDetail.value = null
        }
    }

    // --- DETAIL PEMAKAIAN STATE & OPERATIONS ---
    private val _selectedPemakaianForDetail = MutableStateFlow<PemakaianItem?>(null)
    val selectedPemakaianForDetail: StateFlow<PemakaianItem?> = _selectedPemakaianForDetail.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentPemakaianDetails: StateFlow<List<PemakaianDetailItem>> = _selectedPemakaianForDetail
        .flatMapLatest { pemakaian ->
            if (pemakaian == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else repository.getPemakaianDetailsFlow(pemakaian.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _editingPemakaianDetail = MutableStateFlow<PemakaianDetailItem?>(null)
    val editingPemakaianDetail: StateFlow<PemakaianDetailItem?> = _editingPemakaianDetail.asStateFlow()

    fun openDetailPemakaian(pemakaian: PemakaianItem) {
        _selectedPemakaianForDetail.value = pemakaian
        viewModelScope.launch(Dispatchers.IO) {
            repository.getOrGeneratePemakaianDetails(pemakaian.id)
        }
    }

    fun closeDetailPemakaian() {
        _selectedPemakaianForDetail.value = null
        _editingPemakaianDetail.value = null
    }

    fun openEditPemakaianDetail(detail: PemakaianDetailItem) {
        _editingPemakaianDetail.value = detail
    }

    fun closeEditPemakaianDetail() {
        _editingPemakaianDetail.value = null
    }

    fun saveEditedPemakaianDetailQty(id: Long, newQty: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePemakaianDetailQty(id, newQty)
            _editingPemakaianDetail.value = null
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(application)
                    val repo = AppRepository(db)
                    return MainViewModel(application, repo) as T
                }
            }
    }
}
