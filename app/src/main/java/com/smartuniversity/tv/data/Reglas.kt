package com.smartuniversity.tv.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/*
 * Reglas de negocio de la pantalla. Son funciones puras (no dependen de Android),
 * así que las pantallas las comparten y se prueban con JUnit en app/src/test.
 */

/** Devuelve la misma fecha si es día hábil, o el lunes siguiente si cae en fin de semana. */
fun LocalDate.diaHabil(): LocalDate = when (dayOfWeek) {
    DayOfWeek.SATURDAY -> plusDays(2)
    DayOfWeek.SUNDAY -> plusDays(1)
    else -> this
}

/** El día hábil número [n] a partir de esta fecha (0 = esta fecha o el lunes siguiente). */
fun LocalDate.sumarDiasHabiles(n: Int): LocalDate {
    var fecha = diaHabil()
    repeat(n) { fecha = fecha.plusDays(1).diaHabil() }
    return fecha
}

/** Las clases que se imparten en [fecha], ordenadas por hora, cada una con su cambio de aula. */
fun List<Clase>.delDia(fecha: LocalDate, cambios: List<CambioAula>): List<ClaseConAula> {
    val cambiosDelDia = cambios.filter { it.fecha == fecha }.associateBy { it.claseId }
    return filter { it.dia == fecha.dayOfWeek.value }
        .sortedWith(compareBy({ it.horaInicio }, { it.grupo }))
        .map { ClaseConAula(it, cambiosDelDia[it.id]) }
}

/** ¿La clase está ocurriendo a esta hora? El inicio cuenta y el fin no. */
fun Clase.estaEnCurso(hora: LocalTime): Boolean =
    !hora.isBefore(horaInicio) && hora.isBefore(horaFin)

/** Clases que están ocurriendo a esta hora. */
fun List<ClaseConAula>.enCurso(hora: LocalTime): List<ClaseConAula> =
    filter { it.clase.estaEnCurso(hora) }

/** Clases del siguiente bloque que todavía no empieza (todas las que comienzan a la misma hora). */
fun List<ClaseConAula>.siguienteBloque(hora: LocalTime): List<ClaseConAula> {
    val siguienteInicio = map { it.clase.horaInicio }.filter { it.isAfter(hora) }.minOrNull()
        ?: return emptyList()
    return filter { it.clase.horaInicio == siguienteInicio }
}

/** Avisos ya publicados que no han vencido. */
fun List<Aviso>.vigentes(hoy: LocalDate): List<Aviso> =
    filter { !it.fechaPublicacion.isAfter(hoy) && !it.vigenteHasta.isBefore(hoy) }

/** Eventos que no han terminado: los de días futuros y los de hoy que aún no acaban. */
fun List<Evento>.proximos(ahora: LocalDateTime): List<Evento> = filter {
    it.fecha.isAfter(ahora.toLocalDate()) ||
        (it.fecha == ahora.toLocalDate() && it.horaFin.isAfter(ahora.toLocalTime()))
}

/** Cambios de aula de [fecha] en adelante. */
fun List<CambioAula>.desde(fecha: LocalDate): List<CambioAula> =
    filter { !it.fecha.isBefore(fecha) }
