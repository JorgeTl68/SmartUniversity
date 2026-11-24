package com.smartuniversity.tv.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Ajustes de la pantalla que se guardan en el equipo y sobreviven a un reinicio. */
class AjustesPantalla(context: Context) {

    private val preferencias = context.getSharedPreferences("ajustes_pantalla", Context.MODE_PRIVATE)

    private val _modoDemostracion = MutableStateFlow(preferencias.getBoolean(CLAVE_DEMO, false))

    /** Fija el reloj en un día hábil a las 10:45 para enseñar la app a cualquier hora. */
    val modoDemostracion: StateFlow<Boolean> = _modoDemostracion.asStateFlow()

    private val _modoPantallaAutomatico = MutableStateFlow(preferencias.getBoolean(CLAVE_AUTOMATICO, true))

    /** Si está activo, la app entra sola al modo pantalla después de un rato sin usar el control. */
    val modoPantallaAutomatico: StateFlow<Boolean> = _modoPantallaAutomatico.asStateFlow()

    fun cambiarModoDemostracion(activo: Boolean) {
        preferencias.edit { putBoolean(CLAVE_DEMO, activo) }
        _modoDemostracion.value = activo
    }

    fun cambiarModoPantallaAutomatico(activo: Boolean) {
        preferencias.edit { putBoolean(CLAVE_AUTOMATICO, activo) }
        _modoPantallaAutomatico.value = activo
    }

    companion object {
        private const val CLAVE_DEMO = "modo_demostracion"
        private const val CLAVE_AUTOMATICO = "modo_pantalla_automatico"

        /** PIN de la sección de administración (valor de demostración). */
        const val PIN_ADMINISTRACION = "2026"

        /** Minutos sin usar el control antes de entrar solo al modo pantalla. */
        const val MINUTOS_INACTIVIDAD = 2L
    }
}
