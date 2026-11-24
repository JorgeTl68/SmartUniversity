package com.smartuniversity.tv.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smartuniversity.tv.SmartUniversityApp
import com.smartuniversity.tv.data.AjustesPantalla
import com.smartuniversity.tv.data.Aviso
import com.smartuniversity.tv.data.CambioAula
import com.smartuniversity.tv.data.ClaseConAula
import com.smartuniversity.tv.data.Evento
import com.smartuniversity.tv.data.Prioridad
import com.smartuniversity.tv.data.RelojUniversidad
import com.smartuniversity.tv.data.UniversidadRepository
import com.smartuniversity.tv.data.delDia
import com.smartuniversity.tv.data.enCurso
import com.smartuniversity.tv.data.proximos
import com.smartuniversity.tv.data.siguienteBloque
import com.smartuniversity.tv.data.vigentes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime

/** Todo lo que muestra el panel de inicio (y el modo pantalla) en un momento dado. */
data class InicioUiState(
    val ahora: LocalDateTime = LocalDateTime.now(),
    val modoDemostracion: Boolean = false,
    val enCurso: List<ClaseConAula> = emptyList(),
    val siguientes: List<ClaseConAula> = emptyList(),
    val avisos: List<Aviso> = emptyList(),
    val eventos: List<Evento> = emptyList(),
    val cambiosHoy: List<CambioAula> = emptyList(),
) {
    val urgentes: List<Aviso> get() = avisos.filter { it.prioridad == Prioridad.URGENTE }
}

class InicioViewModel(
    repositorio: UniversidadRepository,
    reloj: RelojUniversidad,
    ajustes: AjustesPantalla,
) : ViewModel() {

    /** Se recalcula cada segundo con el reloj y cada vez que cambia algo en la base de datos. */
    val estado: StateFlow<InicioUiState> = combine(
        reloj.ahora,
        repositorio.clases,
        repositorio.avisos,
        repositorio.eventos,
        repositorio.cambiosAula,
    ) { ahora, clases, avisos, eventos, cambios ->
        val hoy = ahora.toLocalDate()
        val hora = ahora.toLocalTime()
        val clasesDeHoy = clases.delDia(hoy, cambios)
        InicioUiState(
            ahora = ahora,
            enCurso = clasesDeHoy.enCurso(hora),
            siguientes = clasesDeHoy.siguienteBloque(hora),
            avisos = avisos.vigentes(hoy),
            eventos = eventos.proximos(ahora),
            cambiosHoy = cambios.filter { it.fecha == hoy },
        )
    }.combine(ajustes.modoDemostracion) { estado, demo -> estado.copy(modoDemostracion = demo) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val contenedor = (this[APPLICATION_KEY] as SmartUniversityApp).contenedor
                InicioViewModel(contenedor.repositorio, contenedor.reloj, contenedor.ajustes)
            }
        }
    }
}
