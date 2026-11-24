package com.smartuniversity.tv.ui.pantalla

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.smartuniversity.tv.data.Aviso
import com.smartuniversity.tv.data.CambioAula
import com.smartuniversity.tv.data.ClaseConAula
import com.smartuniversity.tv.data.Evento
import com.smartuniversity.tv.ui.componentes.BloqueFecha
import com.smartuniversity.tv.ui.componentes.CintaAvisosUrgentes
import com.smartuniversity.tv.ui.componentes.EtiquetaPrioridad
import com.smartuniversity.tv.ui.componentes.Logotipo
import com.smartuniversity.tv.ui.componentes.Pastilla
import com.smartuniversity.tv.ui.componentes.RelojEnVivo
import com.smartuniversity.tv.ui.inicio.InicioUiState
import com.smartuniversity.tv.ui.inicio.InicioViewModel
import com.smartuniversity.tv.ui.theme.ColoresSU
import com.smartuniversity.tv.ui.theme.colorDePrioridad
import com.smartuniversity.tv.util.formatoHora
import com.smartuniversity.tv.util.rangoHoras
import com.smartuniversity.tv.util.relativaA

/** Diapositivas del carrusel; cada una se muestra solo si tiene contenido. */
enum class Diapositiva(val titulo: String) {
    CLASES("Clases en este momento"),
    AVISOS("Avisos importantes"),
    EVENTOS("Próximos eventos"),
    CAMBIOS("Cambios de aula de hoy"),
}

/** Segundos que dura cada diapositiva antes de pasar a la siguiente. */
private const val SEGUNDOS_POR_DIAPOSITIVA = 12

/**
 * Modo pantalla (kiosco): carrusel a pantalla completa pensado para pasillos y vestíbulos.
 * Avanza solo; las flechas izquierda/derecha cambian de diapositiva y ATRÁS sale.
 */
@Composable
fun ModoPantallaScreen(
    onSalir: () -> Unit,
    viewModel: InicioViewModel = viewModel(factory = InicioViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    BackHandler(onBack = onSalir)

    val diapositivas = diapositivasDisponibles(estado)
    var indice by remember { mutableIntStateOf(0) }
    val actual = diapositivas[indice % diapositivas.size]
    val progreso = remember { Animatable(0f) }

    // Cada vez que cambia la diapositiva, la barra se reinicia y al llenarse avanza a la siguiente.
    LaunchedEffect(indice, diapositivas.size) {
        progreso.snapTo(0f)
        progreso.animateTo(1f, tween(SEGUNDOS_POR_DIAPOSITIVA * 1_000, easing = LinearEasing))
        indice = (indice + 1) % diapositivas.size
    }

    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColoresSU.Fondo)
            .focusRequester(foco)
            .onKeyEvent { evento ->
                if (evento.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (evento.key) {
                    Key.DirectionRight -> { indice = (indice + 1) % diapositivas.size; true }
                    Key.DirectionLeft -> { indice = (indice - 1 + diapositivas.size) % diapositivas.size; true }
                    else -> false
                }
            }
            .focusable()
            .padding(horizontal = 40.dp, vertical = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Logotipo(tamano = 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Smart University", style = MaterialTheme.typography.headlineMedium)
                Text(actual.titulo, style = MaterialTheme.typography.titleLarge, color = ColoresSU.Primario)
            }
            RelojEnVivo(estado.ahora, grande = true)
        }
        Spacer(Modifier.height(18.dp))

        AnimatedContent(
            targetState = actual,
            transitionSpec = { (fadeIn(tween(500)) + slideInHorizontally { it / 8 }) togetherWith fadeOut(tween(300)) },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            label = "diapositiva",
        ) { diapositiva ->
            when (diapositiva) {
                Diapositiva.CLASES -> DiapositivaClases(estado.enCurso, estado.siguientes)
                Diapositiva.AVISOS -> DiapositivaAvisos(estado.avisos)
                Diapositiva.EVENTOS -> DiapositivaEventos(estado.eventos, estado)
                Diapositiva.CAMBIOS -> DiapositivaCambios(estado.cambiosHoy)
            }
        }

        if (estado.urgentes.isNotEmpty() && actual != Diapositiva.AVISOS) {
            Spacer(Modifier.height(12.dp))
            CintaAvisosUrgentes(estado.urgentes)
        }
        Spacer(Modifier.height(12.dp))
        PiePagina(diapositivas, actual, progreso.value)
    }
}

private fun diapositivasDisponibles(estado: InicioUiState): List<Diapositiva> {
    val lista = buildList {
        if (estado.enCurso.isNotEmpty() || estado.siguientes.isNotEmpty()) add(Diapositiva.CLASES)
        if (estado.avisos.isNotEmpty()) add(Diapositiva.AVISOS)
        if (estado.eventos.isNotEmpty()) add(Diapositiva.EVENTOS)
        if (estado.cambiosHoy.isNotEmpty()) add(Diapositiva.CAMBIOS)
    }
    return lista.ifEmpty { listOf(Diapositiva.CLASES) }
}

@Composable
private fun PiePagina(diapositivas: List<Diapositiva>, actual: Diapositiva, progreso: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        diapositivas.forEach { d ->
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(if (d == actual) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (d == actual) ColoresSU.Primario else ColoresSU.Borde),
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(ColoresSU.SuperficieAlta),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progreso)
                    .fillMaxHeight()
                    .background(ColoresSU.Primario),
            )
        }
        Spacer(Modifier.width(16.dp))
        Text("◀ ▶ cambiar   ·   ATRÁS salir", style = MaterialTheme.typography.labelMedium, color = ColoresSU.TextoSecundario)
    }
}

@Composable
private fun Tarjeta(modifier: Modifier = Modifier, color: Color = ColoresSU.Superficie, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .padding(18.dp),
    ) { content() }
}

@Composable
private fun DiapositivaClases(enCurso: List<ClaseConAula>, siguientes: List<ClaseConAula>) {
    Column {
        if (enCurso.isEmpty()) {
            Text("No hay clases en este momento.", style = MaterialTheme.typography.headlineSmall, color = ColoresSU.TextoSecundario)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                enCurso.take(4).forEach { item ->
                    Tarjeta(modifier = Modifier.weight(1f)) {
                        Column {
                            Text(item.clase.grupo, style = MaterialTheme.typography.titleLarge, color = ColoresSU.Primario)
                            Spacer(Modifier.height(4.dp))
                            Text(item.clase.materia, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(10.dp))
                            Text("Aula", style = MaterialTheme.typography.labelMedium, color = ColoresSU.TextoSecundario)
                            Text(
                                item.aulaActual,
                                style = MaterialTheme.typography.displaySmall,
                                color = if (item.cambio != null) ColoresSU.Acento else ColoresSU.Texto,
                            )
                            if (item.cambio != null) Pastilla("Cambio: antes ${item.cambio.aulaAnterior}", ColoresSU.Acento)
                            Spacer(Modifier.height(8.dp))
                            Text(rangoHoras(item.clase.horaInicio, item.clase.horaFin), style = MaterialTheme.typography.bodyMedium)
                            Text(item.clase.docente, style = MaterialTheme.typography.bodySmall, color = ColoresSU.TextoSecundario)
                        }
                    }
                }
            }
        }
        if (siguientes.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Siguen a las ${siguientes.first().clase.horaInicio.formatoHora()}",
                style = MaterialTheme.typography.titleLarge,
                color = ColoresSU.Primario,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                siguientes.joinToString("   ·   ") { "${it.clase.grupo}: ${it.clase.materia} (${it.aulaActual})" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DiapositivaAvisos(avisos: List<Aviso>) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        avisos.take(3).forEach { aviso ->
            Tarjeta(modifier = Modifier.weight(1f).fillMaxHeight(), color = colorDePrioridad(aviso.prioridad).copy(alpha = 0.12f)) {
                Column {
                    EtiquetaPrioridad(aviso.prioridad)
                    Spacer(Modifier.height(10.dp))
                    Text(aviso.titulo, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(aviso.area, style = MaterialTheme.typography.titleSmall, color = ColoresSU.Primario)
                    Spacer(Modifier.height(10.dp))
                    Text(aviso.mensaje, style = MaterialTheme.typography.bodyLarge, maxLines = 7, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun DiapositivaEventos(eventos: List<Evento>, estado: InicioUiState) {
    val hoy = estado.ahora.toLocalDate()
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        eventos.take(4).forEach { evento ->
            Tarjeta(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column {
                    BloqueFecha(evento.fecha, tamano = 72.dp, color = if (evento.fecha == hoy) ColoresSU.Exito else ColoresSU.Primario)
                    Spacer(Modifier.height(10.dp))
                    Pastilla(evento.fecha.relativaA(hoy), ColoresSU.Primario)
                    Spacer(Modifier.height(8.dp))
                    Text(evento.titulo, style = MaterialTheme.typography.titleLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text(rangoHoras(evento.horaInicio, evento.horaFin), style = MaterialTheme.typography.bodyLarge, color = ColoresSU.Primario)
                    Text(evento.lugar, style = MaterialTheme.typography.bodyMedium, color = ColoresSU.TextoSecundario)
                }
            }
        }
    }
}

@Composable
private fun DiapositivaCambios(cambios: List<CambioAula>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cambios.take(5).forEach { cambio ->
            Tarjeta(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cambio.horaInicio.formatoHora(), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.width(90.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${cambio.grupo} · ${cambio.materia}", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(cambio.motivo, style = MaterialTheme.typography.bodyMedium, color = ColoresSU.TextoSecundario)
                    }
                    Text(cambio.aulaAnterior, style = MaterialTheme.typography.headlineSmall, color = ColoresSU.TextoSecundario)
                    Text("  →  ", style = MaterialTheme.typography.headlineSmall, color = ColoresSU.TextoSecundario)
                    Text(cambio.aulaNueva, style = MaterialTheme.typography.headlineMedium, color = ColoresSU.Acento)
                }
            }
        }
    }
}
