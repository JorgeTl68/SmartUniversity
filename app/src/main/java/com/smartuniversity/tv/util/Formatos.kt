package com.smartuniversity.tv.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/* Textos de fechas y horas en español de México para toda la app. */

private val ESPANOL_MX: Locale = Locale.forLanguageTag("es-MX")
private val FORMATO_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private val DIAS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
private val DIAS_CORTOS = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
private val MESES = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

fun String.capitalizar(): String = replaceFirstChar { it.titlecase(ESPANOL_MX) }

/** 07:00 */
fun LocalTime.formatoHora(): String = format(FORMATO_HORA)

/** 07:00 – 08:00 */
fun rangoHoras(inicio: LocalTime, fin: LocalTime): String = "${inicio.formatoHora()} – ${fin.formatoHora()}"

/** Lunes (para 1) ... Domingo (para 7). */
fun nombreDia(dia: Int): String = DIAS[dia - 1]

fun nombreDiaCorto(dia: Int): String = DIAS_CORTOS[dia - 1]

/** "NOV" */
fun LocalDate.mesCorto(): String = MESES[monthValue - 1].take(3).uppercase(ESPANOL_MX)

/** "Lunes 23 de noviembre" */
fun LocalDate.fechaLarga(): String = "${nombreDia(dayOfWeek.value)} $dayOfMonth de ${MESES[monthValue - 1]}"

/** "Lun 23 nov" */
fun LocalDate.fechaCorta(): String = "${nombreDiaCorto(dayOfWeek.value)} $dayOfMonth ${MESES[monthValue - 1].take(3)}"

/** "Hoy", "Mañana", "En 3 días", "Ayer"... respecto a [hoy]. */
fun LocalDate.relativaA(hoy: LocalDate): String {
    val dias = ChronoUnit.DAYS.between(hoy, this)
    return when {
        dias == 0L -> "Hoy"
        dias == 1L -> "Mañana"
        dias == -1L -> "Ayer"
        dias > 1L -> "En $dias días"
        else -> "Hace ${-dias} días"
    }
}
