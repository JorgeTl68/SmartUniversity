package com.smartuniversity.tv.ui.eventos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smartuniversity.tv.SmartUniversityApp
import com.smartuniversity.tv.data.Evento
import com.smartuniversity.tv.data.RelojUniversidad
import com.smartuniversity.tv.data.UniversidadRepository
import com.smartuniversity.tv.data.proximos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class EventosUiState(
    val ahora: LocalDateTime = LocalDateTime.now(),
    val categorias: List<String> = emptyList(),
    /** Categoría elegida; nula significa "todas". */
    val categoria: String? = null,
    val eventos: List<Evento> = emptyList(),
)

class EventosViewModel(
    repositorio: UniversidadRepository,
    reloj: RelojUniversidad,
) : ViewModel() {

    private val categoria = MutableStateFlow<String?>(null)

    // Se actualiza una vez por minuto: suficiente para ocultar los eventos que ya terminaron.
    private val minuto = reloj.ahora.map { it.truncatedTo(ChronoUnit.MINUTES) }.distinctUntilChanged()

    val estado: StateFlow<EventosUiState> = combine(minuto, repositorio.eventos, categoria) { ahora,
eventos, elegida ->
        val proximos = eventos.proximos(ahora)
        EventosUiState(
            ahora = ahora,
            categorias = proximos.map { it.categoria }.distinct().sorted(),
            categoria = elegida,
            eventos = proximos.filter { elegida == null || it.categoria == elegida },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventosUiState())

    fun elegirCategoria(nueva: String?) {
        categoria.value = nueva
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val contenedor = (this[APPLICATION_KEY] as SmartUniversityApp).contenedor
                EventosViewModel(contenedor.repositorio, contenedor.reloj)
            }
        }
    }
}
