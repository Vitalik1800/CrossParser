package com.vs18.crossparser.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun CrossParserTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF00b7eb),
            background = Color(0xFF1e1e1e),
            surface = Color(0xFF252526)
        ),
        content = content
    )
}