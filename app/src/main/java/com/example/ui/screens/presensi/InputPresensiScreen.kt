package com.example.ui.screens.presensi

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AttendanceDate
import com.example.data.AttendanceStatus
import com.example.data.EmployeeAttendanceInput
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputPresensiScreen(
    date: AttendanceDate,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val items by viewModel.inputAttendanceItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.inputSearchQuery.collectAsStateWithLifecycle()
    val isLoading by viewModel.isInputLoading.collectAsStateWithLifecycle()

    val formattedDate = remember(date.tanggal) {
        formatIndonesianDate(date.tanggal)
    }

    val isNewEntry = remember(items) {
        items.none { it.recordId != null }
    }

    // Filter items by search query
    val filteredItems by remember(items, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) items
            else items.filter {
                it.nama.contains(searchQuery, ignoreCase = true) ||
                it.nik.contains(searchQuery, ignoreCase = true) ||
                it.jabatan.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Live counts
    val totalCount = items.size
    val hadirCount = items.count { it.status == AttendanceStatus.HADIR }
    val izinCount = items.count { it.status == AttendanceStatus.IZIN }
    val alpaCount = items.count { it.status == AttendanceStatus.ALPA }
    val sakitCount = items.count { it.status == AttendanceStatus.SAKIT }
    val cutiCount = items.count { it.status == AttendanceStatus.CUTI }
    val shCount = items.count { it.status == AttendanceStatus.SETENGAH_HARI }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isNewEntry) "Tambah Data Presensi" else "Edit Data Presensi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.closeInputPresensi() },
                        modifier = Modifier.testTag("btn_back_input_presensi")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.saveInputPresensi {
                                Toast.makeText(context, "Data presensi berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("btn_simpan_presensi_top")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.closeInputPresensi() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_batal_input_presensi")
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            viewModel.saveInputPresensi {
                                Toast.makeText(context, "Data presensi berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("btn_simpan_presensi_bottom")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simpan Presensi ($totalCount)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Belum Ada Data Karyawan",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Silakan tambahkan data karyawan terlebih dahulu di Menu Karyawan sebelum mengisi presensi.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Stats summary chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatBadge("Total: $totalCount", MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.surfaceVariant)
                    StatBadge("Hadir: $hadirCount", Color(0xFF15803D), Color(0xFFDCFCE7))
                    StatBadge("Izin: $izinCount", Color(0xFFB45309), Color(0xFFFEF3C7))
                    StatBadge("Sakit: $sakitCount", Color(0xFFC2410C), Color(0xFFFFEDD5))
                    StatBadge("Cuti: $cutiCount", Color(0xFF7E22CE), Color(0xFFF3E8FF))
                    StatBadge("Alpa: $alpaCount", Color(0xFFB91C1C), Color(0xFFFEE2E2))
                    StatBadge("1/2 Hari: $shCount", Color(0xFF1D4ED8), Color(0xFFDBEAFE))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick bulk action shortcuts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Aksi Cepat:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AssistChip(
                        onClick = { viewModel.setAllEmployeesStatus(AttendanceStatus.HADIR) },
                        label = { Text("Set Semua Hadir", fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFF15803D)),
                        modifier = Modifier.testTag("btn_quick_all_hadir")
                    )

                    AssistChip(
                        onClick = { viewModel.setAllEmployeesStatus(AttendanceStatus.IZIN) },
                        label = { Text("Set Semua Izin", fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFFB45309)),
                        modifier = Modifier.testTag("btn_quick_all_izin")
                    )

                    AssistChip(
                        onClick = { viewModel.setAllEmployeesStatus(AttendanceStatus.CUTI) },
                        label = { Text("Set Semua Cuti", fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFF7E22CE)),
                        modifier = Modifier.testTag("btn_quick_all_cuti")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setInputSearchQuery(it) },
                    placeholder = { Text("Cari karyawan (nama, NIK, jabatan)...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setInputSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_karyawan_presensi")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Table of Employees with status selection
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Table Header
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
                                    text = "No",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.width(28.dp)
                                )
                                Text(
                                    text = "Karyawan & Jabatan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Pilih Status Kehadiran",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        HorizontalDivider()

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            itemsIndexed(filteredItems, key = { _, item -> item.employeeId }) { index, item ->
                                val rowBg = if (index % 2 == 1) {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }

                                EmployeeAttendanceRow(
                                    number = index + 1,
                                    item = item,
                                    backgroundColor = rowBg,
                                    onStatusChanged = { newStatus ->
                                        viewModel.updateEmployeeStatusInInput(item.employeeId, newStatus)
                                    },
                                    onKeteranganChanged = { newKeterangan ->
                                        viewModel.updateEmployeeKeteranganInInput(item.employeeId, newKeterangan)
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun EmployeeAttendanceRow(
    number: Int,
    item: EmployeeAttendanceInput,
    backgroundColor: Color,
    onStatusChanged: (AttendanceStatus) -> Unit,
    onKeteranganChanged: (String) -> Unit
) {
    var isExpandedNote by remember { mutableStateOf(item.keterangan.isNotBlank()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Row number
            Text(
                text = "$number",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp)
            )

            // Employee Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.nama,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.nik} • ${item.jabatan}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Note toggle button
            IconButton(
                onClick = { isExpandedNote = !isExpandedNote },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = "Catatan",
                    tint = if (item.keterangan.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Status Choice Pills: Hadir, Izin, Alpa, Sakit, Cuti, Setengah Hari
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AttendanceStatus.entries.forEach { status ->
                val isSelected = item.status == status
                val (selectedBg, selectedText) = when (status) {
                    AttendanceStatus.HADIR -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D))
                    AttendanceStatus.IZIN -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
                    AttendanceStatus.ALPA -> Pair(Color(0xFFFEE2E2), Color(0xFFB91C1C))
                    AttendanceStatus.SAKIT -> Pair(Color(0xFFFFEDD5), Color(0xFFC2410C))
                    AttendanceStatus.CUTI -> Pair(Color(0xFFF3E8FF), Color(0xFF7E22CE))
                    AttendanceStatus.SETENGAH_HARI -> Pair(Color(0xFFDBEAFE), Color(0xFF1D4ED8))
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) selectedBg else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) selectedText else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onStatusChanged(status) }
                        .testTag("status_chip_${item.employeeId}_${status.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = selectedText,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                        Text(
                            text = status.label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) selectedText else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Inline Note Field (expanded when user taps note button or note exists)
        AnimatedVisibility(visible = isExpandedNote) {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                OutlinedTextField(
                    value = item.keterangan,
                    onValueChange = onKeteranganChanged,
                    placeholder = { Text("Keterangan presensi (misal: Sakit flu, Cuti tahunan, Dinas luar)...", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_keterangan_${item.employeeId}")
                )
            }
        }
    }
}

@Composable
private fun StatBadge(text: String, textColor: Color, bgColor: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}
