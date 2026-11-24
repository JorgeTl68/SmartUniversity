package com.smartuniversity.tv.data

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Reloj de toda la app. Emite la fecha y hora cada segundo; en modo demostración arranca
 * en un día hábil a las 10:45 y avanza normalmente desde ahí.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RelojUniversidad(ajustes: AjustesPantalla, alcance: CoroutineScope) {

    val ahora: StateFlow<LocalDateTime> = ajustes.modoDemostracion
        .flatMapLatest { demo -> tictac(demo) }
        .stateIn(alcance, SharingStarted.Eagerly, momentoActual(ajustes.modoDemostracion.value, 0L))

    /** Fecha que la pantalla considera "hoy". */
    fun hoy(): LocalDate = ahora.value.toLocalDate()

    private fun tictac(demo: Boolean): Flow<LocalDateTime> = flow {
        val arranque = SystemClock.elapsedRealtime()
        while (true) {
            emit(momentoActual(demo, SystemClock.elapsedRealtime() - arranque))
            // Espera al inicio del siguiente segundo para que el reloj no se atrase.
            delay(1_000L - System.currentTimeMillis() % 1_000L)
        }
    }

    private fun momentoActual(demo: Boolean, transcurridoMs: Long): LocalDateTime =
        if (demo) {
            inicioDemostracion(LocalDate.now()).plusSeconds(transcurridoMs / 1_000L)
        } else {
            LocalDateTime.now().withNano(0)
        }

    companion object {
        val HORA_DEMOSTRACION: LocalTime = LocalTime.of(10, 45)

        fun inicioDemostracion(hoy: LocalDate): LocalDateTime =
            LocalDateTime.of(hoy.diaHabil(), HORA_DEMOSTRACION)
    }
}
