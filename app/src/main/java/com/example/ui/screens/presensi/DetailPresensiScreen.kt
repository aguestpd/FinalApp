package com.example.ui.screens.presensi

import android.widget.Toast
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AttendanceDate
import com.example.data.AttendanceRecord
import com.example.data.AttendanceStatus
import com.example.ui.components.AttendanceStatusBadge
import com.example.ui.viewmodel.MainViewModel
import com.example.util.PdfGenerator
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailPresensiScreen(
    date: AttendanceDate,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allRecords by viewModel.currentDetailRecords.collectAsStateWithLifecycle()
    val filteredRecords by viewModel.filteredDetailRecords.collectAsStateWithLifecycle()
    val currentStatusFilter by viewModel.detailStatusFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.detailSearch.collectAsStateWithLifecycle()

    var recordToEditStatus by remember { mutableStateOf<AttendanceRecord?>(null) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var pdfOptionsFile by remember { mutableStateOf<File?>(null) }

    val formattedDate = formatIndonesianDate(date.tanggal)

    // Statistics counts
    val totalCount = allRecords.size
    val hadirCount = allRecords.count { it.status == AttendanceStatus.HADIR }
    val alpaCount = allRecords.count { it.status == AttendanceStatus.ALPA }
    val izinCount = allRecords.count { it.status == AttendanceStatus.IZIN }
    val sakitCount = allRecords.count { it.status == AttendanceStatus.SAKIT }
    val cutiCount = allRecords.count { it.status == AttendanceStatus.CUTI }
    val shCount = allRecords.count { it.status == AttendanceStatus.SETENGAH_HARI }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Detail Presensi",
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
                        onClick = { viewModel.closeDetailPresensi() },
                        modifier = Modifier.testTag("btn_back_detail_presensi")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    // Edit Presensi Button
                    IconButton(
                        onClick = { viewModel.openInputPresensi(date.id) },
                        modifier = Modifier.testTag("btn_edit_detail_presensi")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Data Presensi",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Export / Print PDF Button
                    Button(
                        onClick = {
                            isGeneratingPdf = true
                            viewModel.generatePdfForCurrentDetail { file ->
                                isGeneratingPdf = false
                                if (file != null) {
                                    pdfOptionsFile = file
                                } else {
                                    Toast.makeText(context, "Gagal membuat dokumen PDF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("btn_ekspor_pdf")
                    ) {
                        if (isGeneratingPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cetak / PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Summary Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(title = "Total", count = totalCount, color = MaterialTheme.colorScheme.primary)
                StatCard(title = "Hadir", count = hadirCount, color = Color(0xFF15803D))
                StatCard(title = "Izin", count = izinCount, color = Color(0xFFB45309))
                StatCard(title = "Sakit", count = sakitCount, color = Color(0xFFC2410C))
                StatCard(title = "Cuti", count = cutiCount, color = Color(0xFF7E22CE))
                StatCard(title = "Alpa", count = alpaCount, color = Color(0xFFB91C1C))
                StatCard(title = "1/2 Hari", count = shCount, color = Color(0xFF1D4ED8))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setDetailSearch(it) },
                placeholder = { Text("Cari nama, NIK, atau jabatan...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setDetailSearch("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_detail_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter status chips
            Text(
                text = "Filter Status Kehadiran:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = currentStatusFilter == null,
                    onClick = { viewModel.setDetailStatusFilter(null) },
                    label = { Text("Semua ($totalCount)", fontSize = 11.5.sp) },
                    modifier = Modifier.testTag("chip_filter_all")
                )

                AttendanceStatus.entries.forEach { status ->
                    val count = when (status) {
                        AttendanceStatus.HADIR -> hadirCount
                        AttendanceStatus.ALPA -> alpaCount
                        AttendanceStatus.IZIN -> izinCount
                        AttendanceStatus.SAKIT -> sakitCount
                        AttendanceStatus.CUTI -> cutiCount
                        AttendanceStatus.SETENGAH_HARI -> shCount
                    }
                    FilterChip(
                        selected = currentStatusFilter == status,
                        onClick = { viewModel.setDetailStatusFilter(status) },
                        label = { Text("${status.label} ($count)", fontSize = 11.5.sp) },
                        modifier = Modifier.testTag("chip_filter_${status.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Table of Attendance
            if (allRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Belum Ada Data Presensi",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Rekapan presensi untuk tanggal ini masih kosong. Silakan tekan tombol 'Tambah Presensi' di bawah untuk mengisi data presensi seluruh karyawan.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.openInputPresensi(date.id) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_tambah_detail_presensi")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tambah Data Presensi", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (filteredRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada karyawan dengan filter ini",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Table Column Titles: Nama Karyawan, NIK, Status
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Nama Karyawan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1.3f)
                                )
                                Text(
                                    text = "NIK",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(0.9f)
                                )
                                Text(
                                    text = "Status Kehadiran",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1.1f)
                                )
                            }
                        }
                        HorizontalDivider()

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            itemsIndexed(filteredRecords, key = { _, r -> r.id }) { index, record ->
                                val rowBg = if (index % 2 == 1) {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBg)
                                        .clickable { recordToEditStatus = record }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Column 1: Nama & Jabatan
                                    Column(modifier = Modifier.weight(1.3f)) {
                                        Text(
                                            text = record.namaKaryawan,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = record.jabatan,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (record.keterangan.isNotBlank()) {
                                            Text(
                                                text = "Ket: ${record.keterangan}",
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Column 2: NIK
                                    Text(
                                        text = record.nik,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(0.9f)
                                    )

                                    // Column 3: Status Badge with tap-to-change
                                    Box(
                                        modifier = Modifier.weight(1.1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        AttendanceStatusBadge(
                                            status = record.status,
                                            onClick = { recordToEditStatus = record },
                                            modifier = Modifier.testTag("badge_status_${record.id}")
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Status Selector Dialog (Tap to update attendance status)
    if (recordToEditStatus != null) {
        ChangeStatusDialog(
            record = recordToEditStatus!!,
            onUpdate = { newStatus, keterangan ->
                viewModel.updateRecordStatus(recordToEditStatus!!.id, newStatus, keterangan)
                recordToEditStatus = null
            },
            onDismiss = { recordToEditStatus = null }
        )
    }

    // PDF Actions Dialog (Print or Share/Save)
    if (pdfOptionsFile != null) {
        val file = pdfOptionsFile!!
        AlertDialog(
            onDismissRequest = { pdfOptionsFile = null },
            icon = {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Laporan PDF Siap",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "File '${file.name}' berhasil digenerate. Silakan pilih opsi di bawah:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = {
                            PdfGenerator.printPdf(context, file, "Presensi_${date.tanggal}")
                            pdfOptionsFile = null
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_print_pdf")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cetak Langsung (Print)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            PdfGenerator.shareOrOpenPdf(context, file)
                            pdfOptionsFile = null
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_share_pdf")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ekspor / Bagikan PDF", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pdfOptionsFile = null }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    count: Int,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = Modifier.widthIn(min = 72.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = color
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun ChangeStatusDialog(
    record: AttendanceRecord,
    onUpdate: (AttendanceStatus, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStatus by remember(record) { mutableStateOf(record.status) }
    var keterangan by remember(record) { mutableStateOf(record.keterangan) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "Ubah Status Presensi", fontWeight = FontWeight.Bold)
                Text(
                    text = "${record.namaKaryawan} (${record.nik})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Pilih Status Kehadiran:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                AttendanceStatus.entries.forEach { status ->
                    val isSelected = selectedStatus == status
                    val (bgColor, textColor) = when (status) {
                        AttendanceStatus.HADIR -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D))
                        AttendanceStatus.ALPA -> Pair(Color(0xFFFEE2E2), Color(0xFFB91C1C))
                        AttendanceStatus.IZIN -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
                        AttendanceStatus.SAKIT -> Pair(Color(0xFFFFEDD5), Color(0xFFC2410C))
                        AttendanceStatus.CUTI -> Pair(Color(0xFFF3E8FF), Color(0xFF7E22CE))
                        AttendanceStatus.SETENGAH_HARI -> Pair(Color(0xFFDBEAFE), Color(0xFF1D4ED8))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) bgColor else MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) textColor else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedStatus = status }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = status.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) textColor else MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = keterangan,
                    onValueChange = { keterangan = it },
                    label = { Text("Keterangan (Opsional)") },
                    placeholder = { Text("Misal: Flu, Urusan Keluarga, Dinas Luar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_keterangan_status")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpdate(selectedStatus, keterangan) },
                modifier = Modifier.testTag("btn_simpan_status")
            ) {
                Text("Simpan Status")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
