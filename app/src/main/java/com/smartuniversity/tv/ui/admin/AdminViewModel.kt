package com.smartuniversity.tv.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smartuniversity.tv.SmartUniversityApp
import com.smartuniversity.tv.data.AjustesPantalla
import com.smartuniversity.tv.data.Aviso
import com.smartuniversity.tv.data.CambioAula
import com.smartuniversity.tv.data.Clase
import com.smartuniversity.tv.data.Prioridad
import com.smartuniversity.tv.data.RelojUniversidad
import com.smartuniversity.tv.data.UniversidadRepository
import com.smartuniversity.tv.data.desde
import com.smartuniversity.tv.data.sumarDiasHabiles
import com.smartuniversity.tv.data.vigentes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Áreas que pueden publicar avisos. */
val AREAS_AVISO = listOf(
    "Dirección Académica",
    "Servicios Escolares",
    "Servicios Estudiantiles",
    "Dirección de Carrera",
    "Protección Civil",
)

/** Días de vigencia de un aviso y su texto. */
val VIGENCIAS_AVISO = listOf(0 to "Solo hoy", 3 to "3 días", 7 to "1 semana", 14 to "2 semanas")

val MOTIVOS_CAMBIO = listOf(
    "Mantenimiento del salón",
    "Falla de equipo",
    "Examen departamental",
    "Evento especial",
    "Práctica de laboratorio",
)

enum class PestanaAdmin(val titulo: String) {
    AVISO("Publicar aviso"),
    CAMBIO("Cambio de aula"),
    PUBLICADOS("Publicados"),
    AJUSTES("Ajustes"),
}

data class FormularioAviso(
    val titulo: String = "",
    val mensaje: String = "",
    val prioridad: Prioridad = Prioridad.IMPORTANTE,
    val area: String = AREAS_AVISO.first(),
    val diasVigencia: Int = 3,
) {
    val esValido: Boolean get() = titulo.isNotBlank() && mensaje.isNotBlank()
}

data class FormularioCambio(
    val fecha: LocalDate? = null,
    val grupo: String? = null,
    val claseId: Int? = null,
    val aulaNueva: String? = null,
    val motivo: String = MOTIVOS_CAMBIO.first(),
)

/** Lo que solo existe mientras se usa la pantalla de administración. */
private data class EstadoLocal(
    val desbloqueado: Boolean = false,
    val pin: String = "",
    val pinIncorrecto: Boolean = false,
    val pestana: PestanaAdmin = PestanaAdmin.AVISO,
    val aviso: FormularioAviso = FormularioAviso(),
    val cambio: FormularioCambio = FormularioCambio(),
    val mensaje: String? = null,
)

data class AdminUiState(
    val desbloqueado: Boolean = false,
    val pin: String = "",
    val pinIncorrecto: Boolean = false,
    val pestana: PestanaAdmin = PestanaAdmin.AVISO,
    val aviso: FormularioAviso = FormularioAviso(),
    val cambio: FormularioCambio = FormularioCambio(),
    val fechasCambio: List<LocalDate> = emptyList(),
    val grupos: List<String> = emptyList(),
    val clasesDelGrupo: List<Clase> = emptyList(),
    val aulas: List<String> = emptyList(),
    val hoy: LocalDate = LocalDate.now(),
    val avisosPublicados: List<Aviso> = emptyList(),
    val cambiosPublicados: List<CambioAula> = emptyList(),
    val modoDemostracion: Boolean = false,
    val modoPantallaAutomatico: Boolean = true,
    val mensaje: String? = null,
) {
    val claseElegida: Clase? get() = clasesDelGrupo.firstOrNull { it.id == cambio.claseId }
}

private data class DatosAdmin(
    val hoy: LocalDate,
    val clases: List<Clase>,
    val avisos: List<Aviso>,
    val cambios: List<CambioAula>,
    val aulas: List<String>,
)

/** Permite publicar avisos y cambios de aula desde la propia pantalla, protegido con un PIN. */
class AdminViewModel(
    private val repositorio: UniversidadRepository,
    reloj: RelojUniversidad,
    private val ajustes: AjustesPantalla,
) : ViewModel() {

    private val local = MutableStateFlow(EstadoLocal())

    private val datos = combine(
        reloj.ahora.map { it.toLocalDate() }.distinctUntilChanged(),
        repositorio.clases,
        repositorio.avisos,
        repositorio.cambiosAula,
        repositorio.aulas,
    ) { hoy, clases, avisos, cambios, aulas -> DatosAdmin(hoy, clases, avisos, cambios, aulas) }

    private val preferencias = combine(ajustes.modoDemostracion, ajustes.modoPantallaAutomatico) { demo, auto -> demo to auto }

    val estado: StateFlow<AdminUiState> = combine(local, datos, preferencias) { l, d, (demo, auto) ->
        // Se pueden registrar cambios para hoy y los dos días hábiles siguientes.
        val fechas = (0..2).map { d.hoy.sumarDiasHabiles(it) }
        val fecha = l.cambio.fecha?.takeIf { it in fechas } ?: fechas.first()
        val grupos = d.clases.sortedBy { it.id }.map { it.grupo }.distinct()
        val grupo = l.cambio.grupo ?: grupos.firstOrNull()
        AdminUiState(
            desbloqueado = l.desbloqueado,
            pin = l.pin,
            pinIncorrecto = l.pinIncorrecto,
            pestana = l.pestana,
            aviso = l.aviso,
            cambio = l.cambio.copy(fecha = fecha, grupo = grupo),
            fechasCambio = fechas,
            grupos = grupos,
            clasesDelGrupo = d.clases
                .filter { it.grupo == grupo && it.dia == fecha.dayOfWeek.value }
                .sortedBy { it.horaInicio },
            aulas = d.aulas,
            hoy = d.hoy,
            avisosPublicados = d.avisos.vigentes(d.hoy),
            cambiosPublicados = d.cambios.desde(d.hoy),
            modoDemostracion = demo,
            modoPantallaAutomatico = auto,
            mensaje = l.mensaje,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminUiState())

    // ---- PIN ----

    fun escribirDigito(digito: Char) {
        local.update { if (it.pin.length < LONGITUD_PIN) it.copy(pin = it.pin + digito, pinIncorrecto = false) else it }
        if (local.value.pin.length == LONGITUD_PIN) validarPin()
    }

    fun borrarDigito() = local.update { it.copy(pin = it.pin.dropLast(1)) }

    private fun validarPin() = local.update {
        if (it.pin == AjustesPantalla.PIN_ADMINISTRACION) {
            it.copy(desbloqueado = true, pin = "", pinIncorrecto = false)
        } else {
            it.copy(pin = "", pinIncorrecto = true)
        }
    }

    /** Cierra la sesión y limpia los formularios. */
    fun bloquear() {
        local.value = EstadoLocal()
    }

    fun elegirPestana(pestana: PestanaAdmin) = local.update { it.copy(pestana = pestana) }

    fun descartarMensaje() = local.update { it.copy(mensaje = null) }

    // ---- Avisos ----

    fun editarAviso(cambio: (FormularioAviso) -> FormularioAviso) = local.update { it.copy(aviso = cambio(it.aviso)) }

    fun publicarAviso() {
        val formulario = local.value.aviso
        if (!formulario.esValido) {
            mostrar("Escribe el título y el mensaje del aviso.")
            return
        }
        viewModelScope.launch {
            repositorio.publicarAviso(
                titulo = formulario.titulo,
                mensaje = formulario.mensaje,
                prioridad = formulario.prioridad,
                area = formulario.area,
                diasVigencia = formulario.diasVigencia,
            )
            local.update { it.copy(aviso = FormularioAviso(), mensaje = "Aviso publicado: ya aparece en la pantalla.") }
        }
    }

    fun eliminarAviso(aviso: Aviso) {
        viewModelScope.launch {
            repositorio.eliminarAviso(aviso)
            mostrar("Se retiró el aviso \"${aviso.titulo}\".")
        }
    }

    // ---- Cambios de aula ----

    fun elegirFechaCambio(fecha: LocalDate) =
        local.update { it.copy(cambio = it.cambio.copy(fecha = fecha, claseId = null, aulaNueva = null)) }

    fun elegirGrupoCambio(grupo: String) =
        local.update { it.copy(cambio = it.cambio.copy(grupo = grupo, claseId = null, aulaNueva = null)) }

    fun elegirClaseCambio(claseId: Int) =
        local.update { it.copy(cambio = it.cambio.copy(claseId = claseId, aulaNueva = null)) }

    fun elegirAulaCambio(aula: String) = local.update { it.copy(cambio = it.cambio.copy(aulaNueva = aula)) }

    fun elegirMotivoCambio(motivo: String) = local.update { it.copy(cambio = it.cambio.copy(motivo = motivo)) }

    fun registrarCambio() {
        val actual = estado.value
        val clase = actual.claseElegida
        val aula = actual.cambio.aulaNueva
        val fecha = actual.cambio.fecha
        if (clase == null || aula == null || fecha == null) {
            mostrar("Elige la clase y el aula nueva.")
            return
        }
        viewModelScope.launch {
            repositorio.registrarCambioAula(clase, fecha, aula, actual.cambio.motivo)
            local.update {
                it.copy(
                    cambio = it.cambio.copy(claseId = null, aulaNueva = null),
                    mensaje = "Cambio registrado: ${clase.grupo}, ${clase.materia} pasa de ${clase.aula} a $aula.",
                )
            }
        }
    }

    fun eliminarCambio(cambio: CambioAula) {
        viewModelScope.launch {
            repositorio.eliminarCambioAula(cambio)
            mostrar("Se canceló el cambio de ${cambio.grupo} (${cambio.materia}).")
        }
    }

    // ---- Ajustes ----

    fun cambiarModoDemostracion(activo: Boolean) = ajustes.cambiarModoDemostracion(activo)

    fun cambiarModoPantallaAutomatico(activo: Boolean) = ajustes.cambiarModoPantallaAutomatico(activo)

    fun restablecerDatos() {
        viewModelScope.launch {
            repositorio.cargarDatosDeEjemplo()
            mostrar("Se restablecieron los datos de ejemplo con fechas a partir de hoy.")
        }
    }

    private fun mostrar(texto: String) = local.update { it.copy(mensaje = texto) }

    companion object {
        const val LONGITUD_PIN = 4

        val Factory = viewModelFactory {
            initializer {
                val contenedor = (this[APPLICATION_KEY] as SmartUniversityApp).contenedor
                AdminViewModel(contenedor.repositorio, contenedor.reloj, contenedor.ajustes)
            }
        }
    }
}
