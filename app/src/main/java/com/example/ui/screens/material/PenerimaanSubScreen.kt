package com.example.ui.screens.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BomHeader
import com.example.data.PenerimaanItem
import com.example.ui.components.AppDatePickerDialog
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.screens.presensi.formatIndonesianDate
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun PenerimaanSubScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val list by viewModel.filteredPenerimaan.collectAsStateWithLifecycle()
    val allBoms by viewModel.allBomHeaders.collectAsStateWithLifecycle()
    val searchQuery by viewModel.penerimaanSearchQuery.collectAsStateWithLifecycle()
    val dialogVisible by viewModel.penerimaanDialogVisible.collectAsStateWithLifecycle()
    val itemToEdit by viewModel.penerimaanToEdit.collectAsStateWithLifecycle()
    val itemToDelete by viewModel.penerimaanToDelete.collectAsStateWithLifecycle()
    val errorMessage by viewModel.penerimaanErrorMessage.collectAsStateWithLifecycle()

    var showValidationAlert by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header row: Title & Tambah button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Data Penerimaan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Penerimaan pesanan material berdasarkan No BOM",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { viewModel.openAddPenerimaanDialog() },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_tambah_penerimaan")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tambah Data", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setPenerimaanSearchQuery(it) },
            placeholder = { Text("Cari tanggal atau No BOM...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setPenerimaanSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Hapus", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_penerimaan_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (list.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Inbox,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "Belum ada data penerimaan" else "Data tidak ditemukan",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tekan 'Tambah Data' untuk mencatat penerimaan pesanan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Table Header: Tanggal | No BOM | Qty Pesanan | Aksi
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tanggal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                modifier = Modifier.weight(1.2f)
                            )
                            Text(
                                text = "No BOM",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Qty Pesanan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                modifier = Modifier.weight(0.9f)
                            )
                            Text(
                                text = "Aksi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                modifier = Modifier.width(76.dp)
                            )
                        }
                    }
                    HorizontalDivider()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        itemsIndexed(list, key = { _, item -> item.id }) { index, item ->
                            val rowBg = if (index % 2 == 1) {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            } else {
                                MaterialTheme.colorScheme.surface
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBg)
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Tanggal
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text(
                                        text = formatIndonesianDate(item.tanggal),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.tanggal,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // No BOM
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = item.noBom,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Qty Pesanan
                                Column(modifier = Modifier.weight(0.9f)) {
                                    Text(
                                        text = "${item.qtyPesanan}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (item.keterangan.isNotBlank()) {
                                        Text(
                                            text = item.keterangan,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Action Buttons (Edit & Hapus)
                                Row(
                                    modifier = Modifier.width(76.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.openEditPenerimaanDialog(item) },
                                        modifier = Modifier.size(32.dp).testTag("btn_edit_penerimaan_${item.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = { viewModel.promptDeletePenerimaan(item) },
                                        modifier = Modifier.size(32.dp).testTag("btn_hapus_penerimaan_${item.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }

    // Modal Tambah / Edit Penerimaan
    if (dialogVisible) {
        AddEditPenerimaanDialog(
            itemToEdit = itemToEdit,
            availableBoms = allBoms,
            errorMessage = errorMessage,
            onSave = { tanggal, noBom, qty, ket ->
                // Validation: checks if No BOM exists
                val bomExists = allBoms.any { it.noBom.equals(noBom.trim(), ignoreCase = true) }
                if (!bomExists) {
                    showValidationAlert = "Nomor BOM '$noBom' TIDAK DITEMUKAN pada daftar master BOM! Data penerimaan gagal disimpan."
                } else {
                    viewModel.savePenerimaan(tanggal, noBom, qty, ket)
                }
            },
            onDismiss = { viewModel.closePenerimaanDialog() }
        )
    }

    // Modal Hapus Penerimaan
    if (itemToDelete != null) {
        ConfirmDeleteDialog(
            title = "Hapus Data Penerimaan",
            message = "Apakah Anda yakin ingin menghapus penerimaan tanggal ${itemToDelete?.tanggal} untuk No BOM '${itemToDelete?.noBom}' (Qty: ${itemToDelete?.qtyPesanan})?",
            onConfirm = { viewModel.confirmDeletePenerimaan() },
            onDismiss = { viewModel.dismissDeletePenerimaan() }
        )
    }

    // Alert Dialog Validasi Gagal
    if (showValidationAlert != null) {
        AlertDialog(
            onDismissRequest = { showValidationAlert = null },
            icon = {
                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(text = "Validasi Gagal", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text = showValidationAlert ?: "")
            },
            confirmButton = {
                Button(
                    onClick = { showValidationAlert = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("OK, Saya Mengerti")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPenerimaanDialog(
    itemToEdit: PenerimaanItem?,
    availableBoms: List<BomHeader>,
    errorMessage: String?,
    onSave: (tanggal: String, noBom: String, qtyPesanan: Double, keterangan: String) -> Unit,
    onDismiss: () -> Unit
) {
    val today = remember {
        val cal = Calendar.getInstance()
        String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    var tanggal by remember(itemToEdit) { mutableStateOf(itemToEdit?.tanggal ?: today) }
    var noBom by remember(itemToEdit) { mutableStateOf(itemToEdit?.noBom ?: "") }
    var qtyStr by remember(itemToEdit) { mutableStateOf(if (itemToEdit != null) itemToEdit.qtyPesanan.toString() else "") }
    var keterangan by remember(itemToEdit) { mutableStateOf(itemToEdit?.keterangan ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (itemToEdit == null) "Tambah Data Penerimaan" else "Edit Data Penerimaan",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = errorMessage, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
                        }
                    }
                }

                // Field Tanggal (dengan AppDatePickerDialog UTC fix)
                OutlinedTextField(
                    value = tanggal,
                    onValueChange = { tanggal = it },
                    label = { Text("Tanggal Penerimaan *") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Pilih Tanggal")
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_tanggal_penerimaan")
                )

                // Field No BOM (Dropdown / input with validation)
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = noBom,
                        onValueChange = {
                            noBom = it.uppercase()
                            isDropdownExpanded = true
                        },
                        label = { Text("No BOM *") },
                        placeholder = { Text("Pilih atau ketik No BOM...") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("input_no_bom_penerimaan")
                    )

                    if (availableBoms.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            availableBoms.forEach { b ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(b.noBom, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                            Text(b.model, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        noBom = b.noBom
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Field Qty Pesanan
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it },
                    label = { Text("Qty Pesanan *") },
                    placeholder = { Text("Contoh: 100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_qty_pesanan")
                )

                // Keterangan
                OutlinedTextField(
                    value = keterangan,
                    onValueChange = { keterangan = it },
                    label = { Text("Keterangan (Opsional)") },
                    placeholder = { Text("Catatan batch, PO, atau supplier") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyStr.toDoubleOrNull() ?: 0.0
                    onSave(tanggal, noBom, qty, keterangan)
                },
                modifier = Modifier.testTag("btn_simpan_penerimaan")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDateString = tanggal,
            onDateSelected = { selectedDate ->
                tanggal = selectedDate
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}
