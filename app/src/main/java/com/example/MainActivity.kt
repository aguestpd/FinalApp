package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.karyawan.KaryawanScreen
import com.example.ui.screens.material.DetailBomScreen
import com.example.ui.screens.material.MaterialScreen
import com.example.ui.screens.presensi.DetailPresensiScreen
import com.example.ui.screens.presensi.InputPresensiScreen
import com.example.ui.screens.presensi.PresensiScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedDateForDetail by viewModel.selectedDateForDetail.collectAsStateWithLifecycle()
    val selectedDateForInput by viewModel.selectedDateForInput.collectAsStateWithLifecycle()
    val selectedBomForDetail by viewModel.selectedBomForDetail.collectAsStateWithLifecycle()

    // Handle system back button when in input, detail presensi, or detail BOM screen
    BackHandler(enabled = selectedDateForInput != null) {
        viewModel.closeInputPresensi()
    }
    BackHandler(enabled = selectedDateForDetail != null && selectedDateForInput == null) {
        viewModel.closeDetailPresensi()
    }
    BackHandler(enabled = selectedBomForDetail != null) {
        viewModel.closeDetailBom()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            // Show bottom navigation menu when not in detail, input, or BOM detail screen
            if (selectedDateForDetail == null && selectedDateForInput == null && selectedBomForDetail == null) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        // 1. Menu Karyawan
                        NavigationBarItem(
                            selected = currentTab == AppTab.KARYAWAN,
                            onClick = { viewModel.selectTab(AppTab.KARYAWAN) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == AppTab.KARYAWAN) Icons.Filled.People else Icons.Outlined.People,
                                    contentDescription = "Menu Karyawan"
                                )
                            },
                            label = {
                                Text(
                                    text = AppTab.KARYAWAN.label,
                                    fontWeight = if (currentTab == AppTab.KARYAWAN) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("tab_karyawan")
                        )

                        // 2. Menu Presensi
                        NavigationBarItem(
                            selected = currentTab == AppTab.PRESENSI,
                            onClick = { viewModel.selectTab(AppTab.PRESENSI) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == AppTab.PRESENSI) Icons.Filled.EventAvailable else Icons.Outlined.EventAvailable,
                                    contentDescription = "Menu Presensi"
                                )
                            },
                            label = {
                                Text(
                                    text = AppTab.PRESENSI.label,
                                    fontWeight = if (currentTab == AppTab.PRESENSI) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("tab_presensi")
                        )

                        // 3. Menu Material
                        NavigationBarItem(
                            selected = currentTab == AppTab.MATERIAL,
                            onClick = { viewModel.selectTab(AppTab.MATERIAL) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == AppTab.MATERIAL) Icons.Filled.Category else Icons.Outlined.Category,
                                    contentDescription = "Menu Material"
                                )
                            },
                            label = {
                                Text(
                                    text = AppTab.MATERIAL.label,
                                    fontWeight = if (currentTab == AppTab.MATERIAL) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("tab_material")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            val inputDate = selectedDateForInput
            val detailDate = selectedDateForDetail
            val detailBom = selectedBomForDetail

            if (inputDate != null) {
                InputPresensiScreen(
                    date = inputDate,
                    viewModel = viewModel
                )
            } else if (detailDate != null) {
                DetailPresensiScreen(
                    date = detailDate,
                    viewModel = viewModel
                )
            } else if (detailBom != null) {
                DetailBomScreen(
                    bom = detailBom,
                    viewModel = viewModel
                )
            } else {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_transition"
                ) { tab ->
                    when (tab) {
                        AppTab.KARYAWAN -> KaryawanScreen(viewModel = viewModel)
                        AppTab.PRESENSI -> PresensiScreen(viewModel = viewModel)
                        AppTab.MATERIAL -> MaterialScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
