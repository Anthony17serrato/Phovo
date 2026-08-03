package com.serratocreations.phovo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.serratocreations.phovo.data.permissions.AndroidPermissionRepository
import com.serratocreations.phovo.data.permissions.PermissionRequestResult
import com.serratocreations.phovo.ui.PhovoApp
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    // Lazy injection - created when first accessed
    private val permissionRepository: AndroidPermissionRepository by inject()
    val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { isGranted: Map<String, Boolean> ->
        val result = isGranted.map { (permission, isGranted) ->
            PermissionRequestResult(
                permission = permission,
                isGranted = isGranted,
                shouldShowRequestPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity, permission
                )
            )
        }
        permissionRepository.onPermissionResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        splash.setKeepOnScreenCondition { permissionRepository.permissionsState.value == null }
        // Turn off the decor fitting system windows, which allows us to handle insets,
        // including IME animations, and go edge-to-edge
        // This also sets up the initial system bar style based on the platform theme
        enableEdgeToEdge()
        registerPermissionsHandler()

        setContent {
            val darkTheme = isSystemInDarkTheme()
            // Update the edge to edge configuration to match the theme
            // This is the same parameters as the default enableEdgeToEdge call, but we manually
            // resolve whether or not to show dark theme using uiState, since it can be different
            // than the configuration's dark theme value based on the user preference.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim,
                        darkScrim,
                    ) { darkTheme },
                )
                onDispose {}
            }

            PhovoApp()
        }
    }

    private fun registerPermissionsHandler() {
        lifecycleScope.launch {
            permissionRepository.requestEventQueue.collect { event ->
                // Suspend until safely resumed, then execute the synchronous launch
                lifecycle.withResumed {
                    requestPermissionLauncher.launch(event.permissions)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionRepository.updatePermissionsStateSynchronous()
    }
}

/**
 * The default light scrim, as defined by androidx and the platform:
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt;l=35-38;drc=27e7d52e8604a080133e8b842db10c89b4482598
 */
private val lightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)

/**
 * The default dark scrim, as defined by androidx and the platform:
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt;l=40-44;drc=27e7d52e8604a080133e8b842db10c89b4482598
 */
private val darkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

@Preview
@Composable
fun AppAndroidPreview() {
    PhovoApp()
}