package com.example.ui.screens.material

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MaterialItem
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MaterialSubTab

@Composable
fun MaterialScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentSubTab by viewModel.materialSubTab.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Sub-navigation: Material, BOM, Penerimaan, Pemakaian
        MaterialSubTabBar(
            currentSubTab = currentSubTab,
            onTabSelected = { viewModel.selectMaterialSubTab(it) }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (currentSubTab) {
                MaterialSubTab.MATERIAL -> MaterialDataSubScreen(viewModel = viewModel)
                MaterialSubTab.BOM -> BomSubScreen(viewModel = viewModel)
                MaterialSubTab.PENERIMAAN -> PenerimaanSubScreen(viewModel = viewModel)
                MaterialSubTab.PEMAKAIAN -> PemakaianSubScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MaterialSubTabBar(
    currentSubTab: MaterialSubTab,
    onTabSelected: (MaterialSubTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf(
                Triple(MaterialSubTab.MATERIAL, Icons.Default.Inventory, "Material"),
                Triple(MaterialSubTab.BOM, Icons.Default.Description, "BOM"),
                Triple(MaterialSubTab.PENERIMAAN, Icons.Default.MoveToInbox, "Penerimaan"),
                Triple(MaterialSubTab.PEMAKAIAN, Icons.Default.Outbox, "Pemakaian")
            )

            tabs.forEach { (tab, icon, label) ->
                val isSelected = currentSubTab == tab
                Surface(
                    selected = isSelected,
                    onClick = { onTabSelected(tab) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("btn_sub_${tab.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MaterialDataSubScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val materials by viewModel.filteredMaterials.collectAsStateWithLifecycle()
    val allMaterialsList by viewModel.allMaterials.collectAsStateWithLifecycle()
    val searchQuery by viewModel.materialSearch.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.materialCategoryFilter.collectAsStateWithLifecycle()
    val dialogVisible by viewModel.materialDialogVisible.collectAsStateWithLifecycle()
    val materialToEdit by viewModel.materialToEdit.collectAsStateWithLifecycle()
    val materialToDelete by viewModel.materialToDelete.collectAsStateWithLifecycle()
    val errorMessage by viewModel.materialErrorMessage.collectAsStateWithLifecycle()

    val categories = remember(allMaterialsList) {
        allMaterialsList.map { it.kategori }.distinct().sorted()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openAddMaterialDialog() },
                icon = { Icon(Icons.Default.Add, contentDescription = "Tambah") },
                text = { Text("Tambah Material", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("btn_tambah_material")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Header Title & Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Inventaris Material",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Manajemen stok logistik dan bahan proyek",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${allMaterialsList.size} Item",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setMaterialSearch(it) },
                placeholder = { Text("Cari nama, kode, atau kategori...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setMaterialSearch("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_material_input")
            )

            // Category Filter Chips
            if (categories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = categoryFilter == null,
                        onClick = { viewModel.setMaterialCategoryFilter(null) },
                        label = { Text("Semua Kategori", fontSize = 12.sp) },
                        modifier = Modifier.testTag("chip_material_cat_all")
                    )

                    categories.forEach { cat ->
                        FilterChip(
                            selected = categoryFilter.equals(cat, ignoreCase = true),
                            onClick = { viewModel.setMaterialCategoryFilter(cat) },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Material List / Table
            if (materials.isEmpty()) {
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
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Inventory,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || categoryFilter != null) "Material tidak ditemukan" else "Belum ada data material",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Klik 'Tambah Material' untuk mencatat bahan material baru",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
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
                        // Table Headers
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
                                    text = "Material & Kode",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1.3f)
                                )
                                Text(
                                    text = "Stok",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1.1f)
                                )
                                Text(
                                    text = "Aksi",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.End,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.width(80.dp)
                                )
                            }
                        }
                        HorizontalDivider()

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 76.dp)
                        ) {
                            itemsIndexed(materials, key = { _, m -> m.id }) { index, item ->
                                val rowBg = if (index % 2 == 1) {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBg)
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Info
                                    Column(modifier = Modifier.weight(1.3f)) {
                                        Text(
                                            text = item.nama,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                            ) {
                                                Text(
                                                    text = item.kode,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = item.kategori,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Stok & Quick Adjust
                                    Row(
                                        modifier = Modifier.weight(1.1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.quickAdjustStock(item, -1.0) },
                                            modifier = Modifier.size(28.dp).testTag("btn_minus_stock_${item.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.Remove,
                                                contentDescription = "Kurangi Stok",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.widthIn(min = 48.dp)
                                        ) {
                                            val stockFormatted = if (item.stok % 1.0 == 0.0) item.stok.toInt().toString() else String.format("%.1f", item.stok)
                                            Text(
                                                text = stockFormatted,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (item.stok <= 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = item.satuan,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.quickAdjustStock(item, 1.0) },
                                            modifier = Modifier.size(28.dp).testTag("btn_plus_stock_${item.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Tambah Stok",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    // Actions: Edit and Delete
                                    Row(
                                        modifier = Modifier.width(80.dp),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.openEditMaterialDialog(item) },
                                            modifier = Modifier.size(34.dp).testTag("btn_edit_material_${item.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.promptDeleteMaterial(item) },
                                            modifier = Modifier.size(34.dp).testTag("btn_delete_material_${item.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Hapus",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
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

    // Add / Edit Material Dialog
    if (dialogVisible) {
        AddEditMaterialDialog(
            material = materialToEdit,
            errorMessage = errorMessage,
            onSave = { kode, nama, satuan, stok, kategori, lokasi, keterangan ->
                viewModel.saveMaterial(kode, nama, satuan, stok, kategori, lokasi, keterangan)
            },
            onDismiss = { viewModel.closeMaterialDialog() }
        )
    }

    // Confirm Delete Dialog
    if (materialToDelete != null) {
        ConfirmDeleteDialog(
            title = "Hapus Material",
            message = "Apakah Anda yakin ingin menghapus data material '${materialToDelete?.nama}' (Kode: ${materialToDelete?.kode})?",
            onConfirm = { viewModel.confirmDeleteMaterial() },
            onDismiss = { viewModel.dismissDeleteMaterial() }
        )
    }
}

@Composable
fun AddEditMaterialDialog(
    material: MaterialItem?,
    errorMessage: String?,
    onSave: (kode: String, nama: String, satuan: String, stok: Double, kategori: String, lokasi: String, keterangan: String) -> Unit,
    onDismiss: () -> Unit
) {
    var kode by remember(material) { mutableStateOf(material?.kode ?: "") }
    var nama by remember(material) { mutableStateOf(material?.nama ?: "") }
    var satuan by remember(material) { mutableStateOf(material?.satuan ?: "Sak") }
    var stokStr by remember(material) { mutableStateOf(material?.stok?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "10") }
    var kategori by remember(material) { mutableStateOf(material?.kategori ?: "Semen & Perekat") }
    var lokasi by remember(material) { mutableStateOf(material?.lokasi ?: "") }
    var keterangan by remember(material) { mutableStateOf(material?.keterangan ?: "") }

    val isEdit = material != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Edit Material" else "Tambah Material Baru",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = kode,
                    onValueChange = { kode = it },
                    label = { Text("Kode Material *") },
                    placeholder = { Text("Contoh: MAT-007") },
                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_kode_material")
                )

                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Material *") },
                    placeholder = { Text("Contoh: Besi Beton D13") },
                    leadingIcon = { Icon(Icons.Default.Inventory, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_nama_material")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = satuan,
                        onValueChange = { satuan = it },
                        label = { Text("Satuan *") },
                        placeholder = { Text("Sak, Kg, Pcs, M3") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_satuan_material")
                    )

                    OutlinedTextField(
                        value = stokStr,
                        onValueChange = { stokStr = it },
                        label = { Text("Jumlah Stok *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_stok_material")
                    )
                }

                OutlinedTextField(
                    value = kategori,
                    onValueChange = { kategori = it },
                    label = { Text("Kategori *") },
                    placeholder = { Text("Contoh: Besi, Pasir, Semen, Cat") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_kategori_material")
                )

                OutlinedTextField(
                    value = lokasi,
                    onValueChange = { lokasi = it },
                    label = { Text("Lokasi Penyimpanan (Opsional)") },
                    placeholder = { Text("Contoh: Gudang A, Rak 2") },
                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedStock = stokStr.toDoubleOrNull() ?: 0.0
                    onSave(kode, nama, satuan, parsedStock, kategori, lokasi, keterangan)
                },
                modifier = Modifier.testTag("btn_simpan_material")
            ) {
                Text(if (isEdit) "Simpan Perubahan" else "Simpan Material")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
