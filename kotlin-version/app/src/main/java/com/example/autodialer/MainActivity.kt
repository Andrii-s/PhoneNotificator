package com.example.autodialer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.autodialer.navigation.AppNavGraph
import com.example.autodialer.ui.theme.AutoDialerTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Single-activity host for the AutoDialer app.
 * All UI is handled through Navigation Compose inside [AppNavGraph].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // -------------------------------------------------------------------------
    // Permission launcher — requests all required runtime permissions at startup
    // -------------------------------------------------------------------------
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { (permission, granted) ->
            if (granted) {
                Timber.d("Permission granted: $permission")
            } else {
                Timber.w("Permission denied: $permission")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestRequiredPermissions()

        setContent {
            AutoDialerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph()
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun requestRequiredPermissions() {
        val permissions = buildList {
            add(Manifest.permission.CALL_PHONE)
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                @Suppress("DEPRECATION")
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}
