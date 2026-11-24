package com.smartuniversity.tv

import com.smartuniversity.tv.data.Aviso
import com.smartuniversity.tv.data.CambioAula
import com.smartuniversity.tv.data.Clase
import com.smartuniversity.tv.data.ClaseSemilla
import com.smartuniversity.tv.data.CambioSemilla
import com.smartuniversity.tv.data.DatosSemilla
import com.smartuniversity.tv.data.Evento
import com.smartuniversity.tv.data.Prioridad
import com.smartuniversity.tv.data.aEntidades
import com.smartuniversity.tv.data.delDia
import com.smartuniversity.tv.data.diaHabil
import com.smartuniversity.tv.data.enCurso
import com.smartuniversity.tv.data.proximos
import com.smartuniversity.tv.data.siguienteBloque
import com.smartuniversity.tv.data.sumarDiasHabiles
import com.smartuniversity.tv.data.vigentes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReglasTest {

    private val lunes = LocalDate.of(2026, 11, 23)
    private val viernes = LocalDate.of(2026, 11, 27)
    private val sabado = LocalDate.of(2026, 11, 28)

    private fun clase(id: Int, inicio: String, fin: String, dia: Int = 1, grupo: String = "9° A IDGS") = Clase(
        id = id, grupo = grupo, dia = dia,
        horaInicio = LocalTime.parse(inicio), horaFin = LocalTime.parse(fin),
        materia = "Materia $id", docente = "Docente", aula = "A-10$id", edificio = "A",
    )

    @Test
    fun `el fin de semana se recorre al lunes`() {
        assertEquals(lunes.plusDays(7), sabado.diaHabil())
        assertEquals(lunes.plusDays(7), sabado.plusDays(1).diaHabil())
        assertEquals(viernes, viernes.diaHabil())
    }

    @Test
    fun `sumar dias habiles salta el fin de semana`() {
        assertEquals(lunes.plusDays(7), viernes.sumarDiasHabiles(1))
        assertEquals(lunes.plusDays(1), lunes.sumarDiasHabiles(1))
        assertEquals(lunes.plusDays(8), sabado.sumarDiasHabiles(1))
    }

    @Test
    fun `una clase esta en curso desde su inicio y hasta antes de su fin`() {
        val clases = listOf(clase(1, "07:00", "08:00"), clase(2, "08:00", "09:00")).delDia(lunes, emptyList())
        assertEquals(listOf(1), clases.enCurso(LocalTime.of(7, 0)).map { it.clase.id })
        assertEquals(listOf(2), clases.enCurso(LocalTime.of(8, 0)).map { it.clase.id })
        assertTrue(clases.enCurso(LocalTime.of(9, 0)).isEmpty())
    }

    @Test
    fun `el siguiente bloque son las clases que empiezan mas pronto`() {
        val clases = listOf(
            clase(1, "07:00", "08:00"),
            clase(2, "08:00", "09:00", grupo = "6° A DSM"),
            clase(3, "08:00", "09:00"),
            clase(4, "09:00", "10:00"),
        ).delDia(lunes, emptyList())
        assertEquals(setOf(2, 3), clases.siguienteBloque(LocalTime.of(7, 15)).map { it.clase.id }.toSet())
        assertTrue(clases.siguienteBloque(LocalTime.of(9, 30)).isEmpty())
    }

    @Test
    fun `el cambio de aula solo aplica en su fecha`() {
        val c = clase(1, "07:00", "08:00")
        val cambio = CambioAula(
            id = 1, claseId = 1, grupo = c.grupo, materia = c.materia, aulaAnterior = c.aula, aulaNueva = "Lab. 1",
            fecha = lunes, horaInicio = c.horaInicio, horaFin = c.horaFin, motivo = "Prueba",
        )
        assertEquals("Lab. 1", listOf(c).delDia(lunes, listOf(cambio)).single().aulaActual)
        assertEquals("A-101", listOf(c).delDia(lunes.plusDays(7), listOf(cambio)).single().aulaActual)
    }

    @Test
    fun `los avisos vencidos o futuros no se muestran`() {
        fun aviso(publicado: LocalDate, hasta: LocalDate) =
            Aviso(titulo = "t", mensaje = "m", prioridad = Prioridad.GENERAL, area = "a", fechaPublicacion = publicado, vigenteHasta = hasta)
        val avisos = listOf(
            aviso(lunes, lunes),
            aviso(lunes.minusDays(5), lunes.minusDays(1)),
            aviso(lunes.plusDays(1), lunes.plusDays(3)),
        )
        assertEquals(1, avisos.vigentes(lunes).size)
    }

    @Test
    fun `un evento de hoy deja de ser proximo cuando termina`() {
        val evento = Evento(
            titulo = "Taller", descripcion = "", fecha = lunes,
            horaInicio = LocalTime.of(13, 0), horaFin = LocalTime.of(15, 0),
            lugar = "Lab", categoria = "Taller", organizador = "IDGS",
        )
        assertEquals(1, listOf(evento).proximos(LocalDateTime.of(lunes, LocalTime.of(14, 0))).size)
        assertTrue(listOf(evento).proximos(LocalDateTime.of(lunes, LocalTime.of(15, 0))).isEmpty())
    }

    @Test
    fun `los datos de ejemplo crean el cambio en el dia habil indicado`() {
        val datos = DatosSemilla(
            clases = listOf(
                ClaseSemilla("9° A IDGS", 1, "07:00", "08:00", "Inglés IX", "Lic.", "B-105", "B"),
                ClaseSemilla("9° A IDGS", 2, "07:00", "08:00", "Inglés IX", "Lic.", "B-105", "B"),
            ),
            avisos = emptyList(),
            eventos = emptyList(),
            cambiosAula = listOf(CambioSemilla("9° A IDGS", 1, "Inglés IX", "A-204", "Mantenimiento")),
        )
        val contenido = datos.aEntidades(lunes)
        val cambio = contenido.cambios.single()
        assertEquals(lunes.plusDays(1), cambio.fecha)
        assertEquals(2, cambio.claseId)
        assertEquals("B-105", cambio.aulaAnterior)
    }
}
