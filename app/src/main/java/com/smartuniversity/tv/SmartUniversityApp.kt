package com.smartuniversity.tv

import android.app.Application
import android.content.Context
import com.smartuniversity.tv.data.AjustesPantalla
import com.smartuniversity.tv.data.RelojUniversidad
import com.smartuniversity.tv.data.SmartUniversityDatabase
import com.smartuniversity.tv.data.UniversidadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmartUniversityApp : Application() {

    lateinit var contenedor: ContenedorApp
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = ContenedorApp(this)
        contenedor.alcance.launch { contenedor.repositorio.cargarDatosSiHaceFalta() }
    }
}

/** Crea una sola vez las piezas que comparten todas las pantallas (inyección manual). */
class ContenedorApp(context: Context) {
    val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val ajustes = AjustesPantalla(context)
    val reloj = RelojUniversidad(ajustes, alcance)
    private val baseDeDatos = SmartUniversityDatabase.crear(context)
    val repositorio = UniversidadRepository(baseDeDatos, context.assets, reloj)
}
