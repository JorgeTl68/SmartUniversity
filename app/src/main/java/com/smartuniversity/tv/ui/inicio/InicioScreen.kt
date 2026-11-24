package com.smartuniversity.tv.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.smartuniversity.tv.data.Aviso
import com.smartuniversity.tv.data.CambioAula
import com.smartuniversity.tv.data.ClaseConAula
import com.smartuniversity.tv.ui.componentes.BloqueFecha
import com.smartuniversity.tv.ui.componentes.CintaAvisosUrgentes
import com.smartuniversity.tv.ui.componentes.Pastilla
import com.smartuniversity.tv.ui.componentes.PanelInformativo
import com.smartuniversity.tv.ui.componentes.PuntoPrioridad
import com.smartuniversity.tv.ui.componentes.RelojEnVivo
import com.smartuniversity.tv.ui.navegacion.Seccion
import com.smartuniversity.tv.ui.theme.ColoresSU
import com.smartuniversity.tv.util.formatoHora
import com.smartuniversity.tv.util.rangoHoras
import com.smartuniversity.tv.util.relativaA

/**
 * Panel principal de la pantalla: reloj, clases en curso, avisos vigentes, próximo evento y
 * cambios de aula del día. Cada panel se enfoca con el control y abre su sección con OK.
 */
@Composable
fun InicioScreen(
    onAbrirSeccion: (Seccion) -> Unit,
    viewModel: InicioViewModel = viewModel(factory = InicioViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val primerPanel = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { primerPanel.requestFocus() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 40.dp, top = 20.dp, bottom = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Bienvenido a Smart University", style = MaterialTheme.typography.headlineLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Horarios, avisos, eventos y cambios de aula del campus",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColoresSU.TextoSecundario,
                    )
                    if (estado.modoDemostracion) {
                        Spacer(Modifier.width(12.dp))
                        Pastilla("Modo demostración", ColoresSU.Acento)
                    }
                }
            }
            RelojEnVivo(ahora = estado.ahora, grande = true)
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PanelInformativo(
                titulo = "Clases en curso",
                icono = Icons.Default.DateRange,
                onClick = { onAbrirSeccion(Seccion.HORARIOS) },
                detalle = "${estado.enCurso.size} grupos",
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .focusRequester(primerPanel),
            ) {
                if (estado.enCurso.isEmpty()) {
                    TextoSecundario("No hay clases en este momento.")
                } else {
                    estado.enCurso.take(4).forEach { FilaClase(it) }
                    if (estado.enCurso.size > 4) TextoSecundario("y ${estado.enCurso.size - 4} grupos más")
                }
                if (estado.siguientes.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Siguen a las ${estado.siguientes.first().clase.horaInicio.formatoHora()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = ColoresSU.Primario,
                    )
                    Spacer(Modifier.height(4.dp))
                    estado.siguientes.take(3).forEach { FilaClase(it) }
                }
            }

            PanelInformativo(
                titulo = "Avisos",
                icono = Icons.Default.Notifications,
                onClick = { onAbrirSeccion(Seccion.AVISOS) },
                detalle = "${estado.avisos.size} vigentes",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                if (estado.avisos.isEmpty()) TextoSecundario("Sin avisos por ahora.")
                estado.avisos.take(4).forEach { FilaAviso(it) }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PanelInformativo(
                    titulo = "Próximo evento",
                    icono = Icons.Default.Star,
                    onClick = { onAbrirSeccion(Seccion.EVENTOS) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    val evento = estado.eventos.firstOrNull()
                    if (evento == null) {
                        TextoSecundario("No hay eventos programados.")
                    } else {
                        Row {
                            BloqueFecha(evento.fecha)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(evento.titulo, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${evento.fecha.relativaA(estado.ahora.toLocalDate())} · ${rangoHoras(evento.horaInicio, evento.horaFin)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ColoresSU.Primario,
                                )
                                Text(evento.lugar, style = MaterialTheme.typography.bodySmall, color = ColoresSU.TextoSecundario)
                            }
                        }
                        if (estado.eventos.size > 1) {
                            Spacer(Modifier.height(6.dp))
                            TextoSecundario("y ${estado.eventos.size - 1} eventos más")
                        }
                    }
                }

                PanelInformativo(
                    titulo = "Cambios de aula",
                    icono = Icons.Default.Place,
                    onClick = { onAbrirSeccion(Seccion.CAMBIOS_AULA) },
                    detalle = "${estado.cambiosHoy.size} hoy",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    if (estado.cambiosHoy.isEmpty()) TextoSecundario("Sin cambios de aula hoy.")
                    estado.cambiosHoy.take(2).forEach { FilaCambio(it) }
                    if (estado.cambiosHoy.size > 2) TextoSecundario("y ${estado.cambiosHoy.size - 2} más")
                }
            }
        }

        if (estado.urgentes.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            CintaAvisosUrgentes(estado.urgentes)
        }
    }
}

@Composable
private fun FilaClase(item: ClaseConAula) {
    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.clase.grupo, style = MaterialTheme.typography.titleSmall, color = ColoresSU.Primario)
            Text(item.clase.materia, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        if (item.cambio != null) {
            Pastilla(item.aulaActual, ColoresSU.Acento, solida = true)
        } else {
            Pastilla(item.aulaActual, ColoresSU.General)
        }
    }
}

@Composable
private fun FilaAviso(aviso: Aviso) {
    Row(modifier = Modifier.padding(vertical = 5.dp)) {
        PuntoPrioridad(aviso.prioridad, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(aviso.titulo, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(aviso.area, style = MaterialTheme.typography.bodySmall, color = ColoresSU.TextoSecundario)
        }
    }
}

@Composable
private fun FilaCambio(cambio: CambioAula) {
    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${cambio.grupo} · ${cambio.materia}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = ColoresSU.TextoSecundario,
            )
            Text("${cambio.aulaAnterior} → ${cambio.aulaNueva}", style = MaterialTheme.typography.titleSmall, color = ColoresSU.Acento)
        }
        Text(cambio.horaInicio.formatoHora(), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun TextoSecundario(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = ColoresSU.TextoSecundario)
}
