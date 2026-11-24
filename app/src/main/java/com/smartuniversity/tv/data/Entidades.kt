package com.smartuniversity.tv.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

/** Nivel de importancia de un aviso; define su color y su orden en la pantalla. */
@Serializable
enum class Prioridad(val etiqueta: String) {
    URGENTE("Urgente"),
    IMPORTANTE("Importante"),
    GENERAL("General"),
}

/** Una clase del horario semanal de un grupo. [dia] va de 1 (lunes) a 5 (viernes). */
@Entity(tableName = "clases")
data class Clase(
    @PrimaryKey val id: Int,
    val grupo: String,
    val dia: Int,
    val horaInicio: LocalTime,
    val horaFin: LocalTime,
    val materia: String,
    val docente: String,
    val aula: String,
    val edificio: String,
)

/** Aviso publicado por un área de la universidad. Se muestra mientras siga vigente. */
@Entity(tableName = "avisos")
data class Aviso(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val mensaje: String,
    val prioridad: Prioridad,
    val area: String,
    val fechaPublicacion: LocalDate,
    val vigenteHasta: LocalDate,
)

/** Evento del calendario universitario (conferencias, ferias, torneos, talleres...). */
@Entity(tableName = "eventos")
data class Evento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val descripcion: String,
    val fecha: LocalDate,
    val horaInicio: LocalTime,
    val horaFin: LocalTime,
    val lugar: String,
    val categoria: String,
    val organizador: String,
)

/**
 * Cambio de aula de una clase en una fecha concreta. Guarda una copia de los datos de la
 * clase para poder mostrarlo aunque el horario cambie después.
 */
@Entity(tableName = "cambios_aula")
data class CambioAula(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val claseId: Int,
    val grupo: String,
    val materia: String,
    val aulaAnterior: String,
    val aulaNueva: String,
    val fecha: LocalDate,
    val horaInicio: LocalTime,
    val horaFin: LocalTime,
    val motivo: String,
)

/** Una clase junto con su cambio de aula del día, si lo tiene. */
data class ClaseConAula(
    val clase: Clase,
    val cambio: CambioAula? = null,
) {
    /** Aula en la que realmente se imparte la clase ese día. */
    val aulaActual: String get() = cambio?.aulaNueva ?: clase.aula
}
