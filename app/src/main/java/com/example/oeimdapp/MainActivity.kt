package com.example.oeimdapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.oeimdapp.ui.screens.CountdownFloatingToast
import com.example.oeimdapp.ui.screens.ExportVerificationDialog
import com.example.oeimdapp.ui.screens.SettingsScreen
import com.example.oeimdapp.ui.screens.StoreScreen
import com.example.oeimdapp.ui.screens.StoreSearchScreen
import com.example.oeimdapp.ui.theme.OeiAppTheme
import com.example.oeimdapp.ui.theme.OeiTheme
import com.example.oeimdapp.viewmodel.MainViewModel

private enum class AppScreen {
    SETTINGS,
    STORE_SEARCH,
    STORE_DETAIL
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OeiAppTheme(themeMode = viewModel.themeMode) {
                var currentScreen by rememberSaveable {
                    mutableStateOf(
                        if (viewModel.isConfigured) AppScreen.STORE_SEARCH
                        else AppScreen.SETTINGS
                    )
                }
                var previousScreen by rememberSaveable {
                    mutableStateOf(AppScreen.STORE_SEARCH)
                }
                var isExportDialogOpen by remember { mutableStateOf(false) }

                // Tarik pembaruan data otomatis saat aplikasi dibuka (jika URL & Token sudah diisi)
                LaunchedEffect(Unit) {
                    if (viewModel.isConfigured) {
                        viewModel.syncNow(forceRefresh = false)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = OeiTheme.colors.appBackground
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                if (targetState.ordinal > initialState.ordinal) {
                                    (slideInHorizontally { width -> width / 3 } + fadeIn(tween(250)))
                                        .togetherWith(slideOutHorizontally { width -> -width / 3 } + fadeOut(tween(200)))
                                } else {
                                    (slideInHorizontally { width -> -width / 3 } + fadeIn(tween(250)))
                                        .togetherWith(slideOutHorizontally { width -> width / 3 } + fadeOut(tween(200)))
                                }
                            },
                            label = "screenTransition"
                        ) { targetAppScreen ->
                            when (targetAppScreen) {
                                AppScreen.SETTINGS -> {
                                    BackHandler(enabled = viewModel.isConfigured) {
                                        viewModel.clearSyncFeedback()
                                        currentScreen = previousScreen
                                    }

                                    SettingsScreen(
                                        viewModel = viewModel,
                                        canNavigateBack = viewModel.isConfigured,
                                        onNavigateBack = {
                                            currentScreen = if (viewModel.isConfigured) {
                                                previousScreen
                                            } else {
                                                AppScreen.STORE_SEARCH
                                            }
                                        }
                                    )
                                }

                                AppScreen.STORE_SEARCH -> {
                                    StoreSearchScreen(
                                        viewModel = viewModel,
                                        onOpenStore = {
                                            currentScreen = AppScreen.STORE_DETAIL
                                        },
                                        onOpenSettings = {
                                            previousScreen = AppScreen.STORE_SEARCH
                                            currentScreen = AppScreen.SETTINGS
                                        }
                                    )
                                }

                                AppScreen.STORE_DETAIL -> {
                                    BackHandler {
                                        currentScreen = AppScreen.STORE_SEARCH
                                    }

                                    StoreScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = {
                                            currentScreen = AppScreen.STORE_SEARCH
                                        },
                                        onOpenSettings = {
                                            previousScreen = AppScreen.STORE_DETAIL
                                            currentScreen = AppScreen.SETTINGS
                                        },
                                        onOpenExportDialog = {
                                            isExportDialogOpen = true
                                        }
                                    )
                                }
                            }
                        }

                        if (isExportDialogOpen) {
                            ExportVerificationDialog(
                                viewModel = viewModel,
                                onDismiss = { isExportDialogOpen = false }
                            )
                        }

                        // Floating Toast Global di Layer Paling Atas (Root Overlay)
                        viewModel.globalToastMessage?.let { toastMsg ->
                            val bg = when (viewModel.globalToastType) {
                                MainViewModel.ToastType.ERROR -> OeiTheme.colors.habisCardBg
                                MainViewModel.ToastType.INFO -> OeiTheme.colors.kurangCardBg
                                MainViewModel.ToastType.SUCCESS -> OeiTheme.colors.penuhCardBg
                            }
                            val textCol = when (viewModel.globalToastType) {
                                MainViewModel.ToastType.ERROR -> OeiTheme.colors.habisStepperBtnText
                                MainViewModel.ToastType.INFO -> OeiTheme.colors.kurangBadgeText
                                MainViewModel.ToastType.SUCCESS -> OeiTheme.colors.penuhStepperBtnText
                            }
                            val borderCol = when (viewModel.globalToastType) {
                                MainViewModel.ToastType.ERROR -> OeiTheme.colors.habisBorder
                                MainViewModel.ToastType.INFO -> OeiTheme.colors.kurangBorder
                                MainViewModel.ToastType.SUCCESS -> OeiTheme.colors.penuhBorder
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .padding(top = 60.dp, start = 16.dp, end = 16.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                CountdownFloatingToast(
                                    message = toastMsg,
                                    onDismiss = { viewModel.dismissGlobalToast() },
                                    modifier = Modifier.fillMaxWidth(),
                                    durationMs = 5000L,
                                    backgroundColor = bg,
                                    textColor = textCol,
                                    borderColor = borderCol,
                                    progressColor = borderCol
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}