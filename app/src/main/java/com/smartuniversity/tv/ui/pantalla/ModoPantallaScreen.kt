package com.smartuniversity.tv.ui.pantalla

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.smartuniversity.tv.ui.componentes.PantallaEnConstruccion

/** Versión provisional: la pantalla completa llega en la rama de Jorge. */
@Composable
fun ModoPantallaScreen(onSalir: () -> Unit) {
    BackHandler(onBack = onSalir)
    PantallaEnConstruccion(titulo = "Modo pantalla")
}
