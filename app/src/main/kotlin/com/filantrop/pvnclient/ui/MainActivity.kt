package com.filantrop.pvnclient.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.filantrop.pvnclient.viewmodel.AuthActivityUiState
import com.filantrop.pvnclient.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .collect {
                        System.out.println("moggot state: $it")
                        when (it) {
                            is AuthActivityUiState.Loading -> System.out.println("moggot Loading")
                            is AuthActivityUiState.Login -> System.out.println("moggot Login")
                            is AuthActivityUiState.Success -> System.out.println("moggot Success")
                        }
                    }
            }
        }
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value.shouldKeepSplashScreen() }

        setContent {
//            val appState = rememberPvnAppState(
//                networkMonitor = networkMonitor,
//            )
//
//            CompositionLocalProvider(
//                LocalAnalyticsHelper provides analyticsHelper,
//            ) {
//                NiaTheme(
//                    darkTheme = themeSettings.darkTheme,
//                    androidTheme = themeSettings.androidTheme,
//                    disableDynamicTheming = themeSettings.disableDynamicTheming,
//                ) {
//                    NiaApp(appState)
//                }
//            }
        }
    }
}
