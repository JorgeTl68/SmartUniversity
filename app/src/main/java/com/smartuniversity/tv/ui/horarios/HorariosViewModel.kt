package com.smartuniversity.tv.ui.horarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smartuniversity.tv.SmartUniversityApp
import com.smartuniversity.tv.data.ClaseConAula
import com.smartuniversity.tv.data.RelojUniversidad
import com.smartuniversity.tv.data.UniversidadRepository
import com.smartuniversity.tv.data.delDia
import com.smartuniversity.tv.data.diaHabil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

data class HorariosUiState(val ahora: LocalDateTime = LocalDateTime.now(), val grupos: List<String> = emptyList(), val grupoSeleccionado: String? = null, val diaSeleccionado: Int = 1, val semana: List<LocalDate> = emptyList(), val clases: List<ClaseConAula> = emptyList()) {
    val fechaSeleccionada: LocalDate? get() = semana.getOrNull(diaSeleccionado - 1)
}

class HorariosViewModel(repositorio: UniversidadRepository, reloj: RelojUniversidad) : ViewModel() {
    private val grupoElegido = MutableStateFlow<String?>(null)
    private val diaElegido = MutableStateFlow(reloj.hoy().diaHabil().dayOfWeek.value)
    val estado: StateFlow<HorariosUiState> = combine(reloj.ahora, repositorio.clases, repositorio.cambiosAula, grupoElegido, diaElegido) { ahora, clases, cambios, grupo, dia ->
        val grupos = clases.sortedBy { it.id }.map { it.grupo }.distinct(); val grupoActual = grupo ?: grupos.firstOrNull(); val lunes = ahora.toLocalDate().diaHabil().with(DayOfWeek.MONDAY); val semana = (0L..4L).map { lunes.plusDays(it) }; val fecha = semana[dia - 1]
        HorariosUiState(ahora, grupos, grupoActual, dia, semana, clases.filter { it.grupo == grupoActual }.delDia(fecha, cambios))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HorariosUiState())
    fun seleccionarGrupo(grupo: String) { grupoElegido.value = grupo }
    fun seleccionarDia(dia: Int) { diaElegido.value = dia.coerceIn(1, 5) }
    companion object { val Factory = viewModelFactory { initializer { val contenedor = (this[APPLICATION_KEY] as SmartUniversityApp).contenedor; HorariosViewModel(contenedor.repositorio, contenedor.reloj) } } }
}
