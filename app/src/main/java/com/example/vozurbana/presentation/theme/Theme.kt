package com.example.vozurbana.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

@Composable
fun VozurbanaTheme(
    content: @Composable () -> Unit
) {
    /**
     * Tema personalizado para la aplicación Voz Urbana en Wear OS
     * Utiliza los colores y tipografía de Material Design para Wear
     */
    MaterialTheme(
        /* colors = ..., */
        /* typography = ..., */
        content = content
    )
}
