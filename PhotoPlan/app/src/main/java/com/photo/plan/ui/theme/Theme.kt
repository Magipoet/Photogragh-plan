package com.photo.plan.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Green500,
    onPrimary = White,
    primaryContainer = Green100,
    onPrimaryContainer = Green700,
    secondary = Green300,
    onSecondary = Gray900,
    background = Gray50,
    onBackground = Gray900,
    surface = White,
    onSurface = Gray900,
    error = Red500,
    onError = White
)

private val DarkColorScheme = darkColorScheme(
    primary = Green300,
    onPrimary = Gray900,
    primaryContainer = Green700,
    onPrimaryContainer = Green100,
    secondary = Green500,
    onSecondary = White,
    background = Gray900,
    onBackground = Gray50,
    surface = Gray900,
    onSurface = Gray50,
    error = Red500,
    onError = White
)

@Composable
fun PhotoPlanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
