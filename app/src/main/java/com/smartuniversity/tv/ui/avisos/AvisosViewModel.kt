package com.smartuniversity.tv.ui.avisos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smartuniversity.tv.SmartUniversityApp
import com.smartuniversity.tv.data.Aviso
import com.smartuniversity.tv.data.Prioridad
import com.smartuniversity.tv.data.RelojUniversidad
import com.smartuniversity.tv.data.UniversidadRepository
import com.smartuniversity.tv.data.vigentes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** Filtros de la lista de avisos; [prioridad] nula significa "todos". */
enum class FiltroAvisos(val etiqueta: String, val prioridad: Prioridad?) {
    TODOS("Todos", null),
    URGENTES("Urgentes", Prioridad.URGENTE),
    IMPORTANTES("Importantes", Prioridad.IMPORTANTE),
    GENERALES("Generales", Prioridad.GENERAL),
}

data class AvisosUiState(
    val hoy: LocalDate = LocalDate.now(),
    val filtro: FiltroAvisos = FiltroAvisos.TODOS,
    val avisos: List<Aviso> = emptyList(),
    val conteos: Map<FiltroAvisos, Int> = emptyMap(),
)

class AvisosViewModel(
    repositorio: UniversidadRepository,
    reloj: RelojUniversidad,
) : ViewModel() {

    private val filtro = MutableStateFlow(FiltroAvisos.TODOS)

    // Solo interesa la fecha: así la lista no se recalcula cada segundo.
    private val hoy = reloj.ahora.map { it.toLocalDate() }.distinctUntilChanged()

    val estado: StateFlow<AvisosUiState> = combine(hoy, repositorio.avisos, filtro) { fecha, avisos,
filtroActual ->
        val vigentes = avisos.vigentes(fecha)
        AvisosUiState(
            hoy = fecha,
            filtro = filtroActual,
            avisos = vigentes.filter { filtroActual.prioridad == null || it.prioridad == filtroActual.prioridad },
            conteos = FiltroAvisos.entries.associateWith { f ->
                vigentes.count { f.prioridad == null || it.prioridad == f.prioridad }
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AvisosUiState())

    fun cambiarFiltro(nuevo: FiltroAvisos) {
        filtro.value = nuevo
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val contenedor = (this[APPLICATION_KEY] as SmartUniversityApp).contenedor
                AvisosViewModel(contenedor.repositorio, contenedor.reloj)
            }
        }
    }
}
