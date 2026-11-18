package com.islandos.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.islandos.android.ui.screens.CustomizationScreen
import com.islandos.android.ui.screens.FeaturesScreen
import com.islandos.android.ui.screens.HomeScreen
import com.islandos.android.ui.screens.OnboardingScreen
import com.islandos.android.ui.screens.SettingsScreen
import com.islandos.android.ui.theme.IslandColors
import com.islandos.android.ui.theme.IslandOSTheme
import com.islandos.android.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            IslandOSTheme {
                val settings by viewModel.settingsFlow.collectAsStateWithLifecycle()
                val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
                val permissions by viewModel.permissions.collectAsStateWithLifecycle()
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refreshPermissions()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                if (!settings.onboardingCompleted && !permissions.corePermissionsReady) {
                    OnboardingScreen(
                        permissions = permissions,
                        onRefresh = { viewModel.refreshPermissions() },
                        onContinue = { viewModel.completeOnboarding() }
                    )
                } else {
                    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                    val tabs = listOf(
                        "Home" to Icons.Rounded.Home,
                        "Customize" to Icons.Rounded.Tune,
                        "Features" to Icons.Rounded.AutoAwesome,
                        "Settings" to Icons.Rounded.Settings
                    )

                    Scaffold(
                        containerColor = IslandColors.OledBlack,
                        bottomBar = {
                            NavigationBar(containerColor = IslandColors.SurfaceDark) {
                                tabs.forEachIndexed { index, (label, icon) ->
                                    NavigationBarItem(
                                        selected = selectedTab == index,
                                        onClick = { selectedTab = index },
                                        icon = { Icon(icon, contentDescription = label) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (selectedTab) {
                                0 -> HomeScreen(
                                    uiState = uiState,
                                    settings = settings,
                                    permissions = permissions,
                                    viewModel = viewModel,
                                    onNavigateCustomize = { selectedTab = 1 },
                                    onNavigateFeatures = { selectedTab = 2 }
                                )
                                1 -> CustomizationScreen(
                                    uiState = uiState,
                                    settings = settings,
                                    viewModel = viewModel
                                )
                                2 -> FeaturesScreen(
                                    uiState = uiState,
                                    settings = settings,
                                    viewModel = viewModel
                                )
                                else -> SettingsScreen(
                                    settings = settings,
                                    permissions = permissions,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
