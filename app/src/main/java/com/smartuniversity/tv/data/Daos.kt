package com.smartuniversity.tv.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ClaseDao {
    @Query("SELECT * FROM clases ORDER BY dia, horaInicio, grupo")
    fun observarTodas(): Flow<List<Clase>>

    @Query("SELECT COUNT(*) FROM clases")
    suspend fun contar(): Int

    @Insert
    suspend fun insertar(clases: List<Clase>)

    @Query("DELETE FROM clases")
    suspend fun borrarTodo()
}

@Dao
interface AvisoDao {
    /** Primero los urgentes, luego los importantes y al final los generales; los más nuevos arriba. */
    @Query(
        """
        SELECT * FROM avisos
        ORDER BY CASE prioridad WHEN 'URGENTE' THEN 0 WHEN 'IMPORTANTE' THEN 1 ELSE 2 END,
                 fechaPublicacion DESC, id DESC
        """
    )
    fun observarTodos(): Flow<List<Aviso>>

    @Insert
    suspend fun insertar(aviso: Aviso): Long

    @Insert
    suspend fun insertarTodos(avisos: List<Aviso>)

    @Delete
    suspend fun eliminar(aviso: Aviso)

    @Query("DELETE FROM avisos")
    suspend fun borrarTodo()
}

@Dao
interface EventoDao {
    @Query("SELECT * FROM eventos ORDER BY fecha, horaInicio")
    fun observarTodos(): Flow<List<Evento>>

    @Insert
    suspend fun insertarTodos(eventos: List<Evento>)

    @Query("DELETE FROM eventos")
    suspend fun borrarTodo()
}

@Dao
interface CambioAulaDao {
    @Query("SELECT * FROM cambios_aula ORDER BY fecha, horaInicio, grupo")
    fun observarTodos(): Flow<List<CambioAula>>

    @Insert
    suspend fun insertar(cambio: CambioAula): Long

    @Insert
    suspend fun insertarTodos(cambios: List<CambioAula>)

    @Delete
    suspend fun eliminar(cambio: CambioAula)

    /** Una clase solo puede tener un cambio por día: se borra el anterior antes de guardar otro. */
    @Query("DELETE FROM cambios_aula WHERE claseId = :claseId AND fecha = :fecha")
    suspend fun eliminarDeClase(claseId: Int, fecha: LocalDate)

    @Query("DELETE FROM cambios_aula")
    suspend fun borrarTodo()
}
