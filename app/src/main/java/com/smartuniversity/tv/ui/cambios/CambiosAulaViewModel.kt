package com.smartuniversity.tv.ui.cambios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smartuniversity.tv.SmartUniversityApp
import com.smartuniversity.tv.data.CambioAula
import com.smartuniversity.tv.data.RelojUniversidad
import com.smartuniversity.tv.data.UniversidadRepository
import com.smartuniversity.tv.data.desde
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalDateTime

data class CambiosAulaUiState(val ahora: LocalDateTime = LocalDateTime.now(), val cambiosHoy: List<CambioAula> = emptyList(), val proximos: Map<LocalDate, List<CambioAula>> = emptyMap())
class CambiosAulaViewModel(repositorio: UniversidadRepository, reloj: RelojUniversidad) : ViewModel() {
    val estado: StateFlow<CambiosAulaUiState> = combine(reloj.ahora, repositorio.cambiosAula) { ahora, cambios -> val hoy = ahora.toLocalDate(); val vigentes = cambios.desde(hoy); CambiosAulaUiState(ahora, vigentes.filter { it.fecha == hoy }, vigentes.filter { it.fecha.isAfter(hoy) }.groupBy { it.fecha }.toSortedMap()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CambiosAulaUiState())
    companion object { val Factory = viewModelFactory { initializer { val contenedor = (this[APPLICATION_KEY] as SmartUniversityApp).contenedor; CambiosAulaViewModel(contenedor.repositorio, contenedor.reloj) } } }
}
