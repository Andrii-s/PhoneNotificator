package com.example.autodialer.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.autodialer.BuildConfig
import com.example.autodialer.R
import com.example.autodialer.ui.theme.AutoDialerTheme
import kotlinx.coroutines.delay

/** Duration of the splash animation (ms). */
private const val SPLASH_DURATION_MS = 2_500L

/**
 * Splash screen shown at app launch.
 *
 * Displays the app icon, name and version while a progress indicator
 * animates from 0 → 1 over [SPLASH_DURATION_MS]. Once the delay
 * completes the caller is notified via [onNavigateToSettings].
 */
@Composable
fun SplashScreen(
    onNavigateToSettings: () -> Unit,
) {
    // ── Progress animation ────────────────────────────────────────────────────
    var progressTarget by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(
            durationMillis = SPLASH_DURATION_MS.toInt(),
            easing = LinearEasing,
        ),
        label = "splashProgress",
    )

    // ── Side effect: start animation then navigate ────────────────────────────
    LaunchedEffect(Unit) {
        progressTarget = 1f          // triggers the float animation
        delay(SPLASH_DURATION_MS)
        onNavigateToSettings()
    }

    // ── UI ───────────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // App icon
        Icon(
            imageVector = Icons.Filled.Phone,
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // App title
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Version label — reads from BuildConfig so it always stays in sync
        Text(
            text = stringResource(R.string.splash_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Animated progress bar
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview(showBackground = true, name = "Splash — Light")
@Composable
private fun SplashScreenPreviewLight() {
    AutoDialerTheme(darkTheme = false) {
        SplashScreen(onNavigateToSettings = {})
    }
}

@Preview(showBackground = true, name = "Splash — Dark")
@Composable
private fun SplashScreenPreviewDark() {
    AutoDialerTheme(darkTheme = true) {
        SplashScreen(onNavigateToSettings = {})
    }
}
