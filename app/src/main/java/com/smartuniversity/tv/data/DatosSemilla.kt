package com.smartuniversity.tv.data

import android.content.res.AssetManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime

/*
 * Formato del archivo assets/datos_universidad.json. Las fechas son relativas al día en que
 * se cargan los datos (por ejemplo "enDias": 2 es pasado mañana), así la demostración
 * siempre muestra eventos próximos y cambios de aula del día.
 */

@Serializable
data class DatosSemilla(
    val clases: List<ClaseSemilla>,
    val avisos: List<AvisoSemilla>,
    val eventos: List<EventoSemilla>,
    val cambiosAula: List<CambioSemilla>,
)

@Serializable
data class ClaseSemilla(
    val grupo: String,
    val dia: Int,
    val inicio: String,
    val fin: String,
    val materia: String,
    val docente: String,
    val aula: String,
    val edificio: String,
)

@Serializable
data class AvisoSemilla(
    val titulo: String,
    val mensaje: String,
    val prioridad: Prioridad,
    val area: String,
    val publicadoHaceDias: Int = 0,
    val diasVigencia: Int,
)

@Serializable
data class EventoSemilla(
    val titulo: String,
    val descripcion: String,
    val enDias: Int,
    val inicio: String,
    val fin: String,
    val lugar: String,
    val categoria: String,
    val organizador: String,
)

@Serializable
data class CambioSemilla(
    val grupo: String,
    val diaHabil: Int,
    val materia: String,
    val aulaNueva: String,
    val motivo: String,
)

/** Datos listos para guardarse en Room. */
data class ContenidoInicial(
    val clases: List<Clase>,
    val avisos: List<Aviso>,
    val eventos: List<Evento>,
    val cambios: List<CambioAula>,
)

object CargadorDatosSemilla {
    const val ARCHIVO = "datos_universidad.json"

    private val json = Json { ignoreUnknownKeys = true }

    fun leer(assets: AssetManager, archivo: String = ARCHIVO): DatosSemilla =
        assets.open(archivo).bufferedReader().use { json.decodeFromString(it.readText()) }
}

/** Convierte las fechas relativas del JSON en fechas reales tomando [hoy] como referencia. */
fun DatosSemilla.aEntidades(hoy: LocalDate): ContenidoInicial {
    val clasesConvertidas = clases.mapIndexed { indice, c ->
        Clase(
            id = indice + 1,
            grupo = c.grupo,
            dia = c.dia,
            horaInicio = LocalTime.parse(c.inicio),
            horaFin = LocalTime.parse(c.fin),
            materia = c.materia,
            docente = c.docente,
            aula = c.aula,
            edificio = c.edificio,
        )
    }
    val avisosConvertidos = avisos.map {
        Aviso(
            titulo = it.titulo,
            mensaje = it.mensaje,
            prioridad = it.prioridad,
            area = it.area,
            fechaPublicacion = hoy.minusDays(it.publicadoHaceDias.toLong()),
            vigenteHasta = hoy.plusDays(it.diasVigencia.toLong()),
        )
    }
    val eventosConvertidos = eventos.map {
        Evento(
            titulo = it.titulo,
            descripcion = it.descripcion,
            fecha = hoy.plusDays(it.enDias.toLong()),
            horaInicio = LocalTime.parse(it.inicio),
            horaFin = LocalTime.parse(it.fin),
            lugar = it.lugar,
            categoria = it.categoria,
            organizador = it.organizador,
        )
    }
    // Cada cambio apunta a la primera clase de esa materia en el día hábil indicado.
    val cambiosConvertidos = cambiosAula.mapNotNull { semilla ->
        val fecha = hoy.sumarDiasHabiles(semilla.diaHabil)
        val clase = clasesConvertidas
            .filter { it.grupo == semilla.grupo && it.materia == semilla.materia && it.dia == fecha.dayOfWeek.value }
            .minByOrNull { it.horaInicio }
            ?: return@mapNotNull null
        CambioAula(
            claseId = clase.id,
            grupo = clase.grupo,
            materia = clase.materia,
            aulaAnterior = clase.aula,
            aulaNueva = semilla.aulaNueva,
            fecha = fecha,
            horaInicio = clase.horaInicio,
            horaFin = clase.horaFin,
            motivo = semilla.motivo,
        )
    }
    return ContenidoInicial(clasesConvertidas, avisosConvertidos, eventosConvertidos, cambiosConvertidos)
}
