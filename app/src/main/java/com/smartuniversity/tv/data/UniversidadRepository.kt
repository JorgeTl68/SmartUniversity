package com.smartuniversity.tv.data

import android.content.res.AssetManager
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Punto único de acceso a los datos. Las pantallas observan los Flow de Room, así que
 * cualquier aviso o cambio de aula que se publique aparece al instante en la pantalla.
 */
class UniversidadRepository(
    private val baseDeDatos: SmartUniversityDatabase,
    private val assets: AssetManager,
    private val reloj: RelojUniversidad,
) {
    private val claseDao = baseDeDatos.claseDao()
    private val avisoDao = baseDeDatos.avisoDao()
    private val eventoDao = baseDeDatos.eventoDao()
    private val cambioAulaDao = baseDeDatos.cambioAulaDao()

    val clases: Flow<List<Clase>> = claseDao.observarTodas()
    val avisos: Flow<List<Aviso>> = avisoDao.observarTodos()
    val eventos: Flow<List<Evento>> = eventoDao.observarTodos()
    val cambiosAula: Flow<List<CambioAula>> = cambioAulaDao.observarTodos()

    /** Grupos en el orden en que aparecen en el archivo de datos. */
    val grupos: Flow<List<String>> = clases.map { lista -> lista.sortedBy { it.id }.map { it.grupo }.distinct() }

    /** Aulas a las que se puede mover una clase: las del horario más los espacios comunes. */
    val aulas: Flow<List<String>> = clases.map { lista -> (lista.map { it.aula } + ESPACIOS_COMUNES).distinct().sorted() }

    /** Carga los datos de ejemplo solo la primera vez que se abre la app. */
    suspend fun cargarDatosSiHaceFalta() {
        if (claseDao.contar() == 0) cargarDatosDeEjemplo()
    }

    /** Borra todo y vuelve a cargar el JSON con las fechas relativas a hoy. */
    suspend fun cargarDatosDeEjemplo() {
        val contenido = CargadorDatosSemilla.leer(assets).aEntidades(reloj.hoy())
        baseDeDatos.withTransaction {
            cambioAulaDao.borrarTodo()
            avisoDao.borrarTodo()
            eventoDao.borrarTodo()
            claseDao.borrarTodo()
            claseDao.insertar(contenido.clases)
            avisoDao.insertarTodos(contenido.avisos)
            eventoDao.insertarTodos(contenido.eventos)
            cambioAulaDao.insertarTodos(contenido.cambios)
        }
    }

    suspend fun publicarAviso(
        titulo: String,
        mensaje: String,
        prioridad: Prioridad,
        area: String,
        diasVigencia: Int,
    ): Long {
        val hoy = reloj.hoy()
        return avisoDao.insertar(
            Aviso(
                titulo = titulo.trim(),
                mensaje = mensaje.trim(),
                prioridad = prioridad,
                area = area,
                fechaPublicacion = hoy,
                vigenteHasta = hoy.plusDays(diasVigencia.toLong()),
            )
        )
    }

    suspend fun eliminarAviso(aviso: Aviso) = avisoDao.eliminar(aviso)

    /** Registra que [clase] se imparte en [aulaNueva] el día [fecha]; reemplaza un cambio previo. */
    suspend fun registrarCambioAula(clase: Clase, fecha: LocalDate, aulaNueva: String, motivo: String): Long =
        baseDeDatos.withTransaction {
            cambioAulaDao.eliminarDeClase(clase.id, fecha)
            cambioAulaDao.insertar(
                CambioAula(
                    claseId = clase.id,
                    grupo = clase.grupo,
                    materia = clase.materia,
                    aulaAnterior = clase.aula,
                    aulaNueva = aulaNueva,
                    fecha = fecha,
                    horaInicio = clase.horaInicio,
                    horaFin = clase.horaFin,
                    motivo = motivo.trim(),
                )
            )
        }

    suspend fun eliminarCambioAula(cambio: CambioAula) = cambioAulaDao.eliminar(cambio)

    companion object {
        val ESPACIOS_COMUNES = listOf("A-106", "A-204", "Auditorio", "Sala Audiovisual", "Sala de usos múltiples")
    }
}
