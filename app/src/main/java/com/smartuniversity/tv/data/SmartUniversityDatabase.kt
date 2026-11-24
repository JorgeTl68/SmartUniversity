package com.smartuniversity.tv.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDate
import java.time.LocalTime

/** Room no sabe guardar fechas y horas: se convierten a números. */
class Convertidores {
    @TypeConverter
    fun fechaANumero(fecha: LocalDate): Long = fecha.toEpochDay()

    @TypeConverter
    fun numeroAFecha(dias: Long): LocalDate = LocalDate.ofEpochDay(dias)

    /** La hora se guarda como minutos desde la medianoche para poder ordenar con SQL. */
    @TypeConverter
    fun horaANumero(hora: LocalTime): Int = hora.hour * 60 + hora.minute

    @TypeConverter
    fun numeroAHora(minutos: Int): LocalTime = LocalTime.of(minutos / 60, minutos % 60)
}

@Database(
    entities = [Clase::class, Aviso::class, Evento::class, CambioAula::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Convertidores::class)
abstract class SmartUniversityDatabase : RoomDatabase() {
    abstract fun claseDao(): ClaseDao
    abstract fun avisoDao(): AvisoDao
    abstract fun eventoDao(): EventoDao
    abstract fun cambioAulaDao(): CambioAulaDao

    companion object {
        fun crear(context: Context): SmartUniversityDatabase =
            Room.databaseBuilder(context, SmartUniversityDatabase::class.java, "smart_university.db")
                .build()
    }
}
