package com.smartuniversity.tv.ui.horarios

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
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
import com.smartuniversity.tv.data.ClaseConAula
import com.smartuniversity.tv.data.estaEnCurso
import com.smartuniversity.tv.ui.componentes.*
import com.smartuniversity.tv.ui.theme.ColoresSU
import com.smartuniversity.tv.util.*
import java.time.LocalDateTime

@Composable
fun HorariosScreen(viewModel: HorariosViewModel = viewModel(factory = HorariosViewModel.Factory)) {
    val estado by viewModel.estado.collectAsStateWithLifecycle(); val primerChip = remember { FocusRequester() }
    LaunchedEffect(estado.grupos.isNotEmpty()) { if (estado.grupos.isNotEmpty()) runCatching { primerChip.requestFocus() } }
    Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 40.dp, top = 20.dp)) {
        EncabezadoSeccion("Horarios de clase", "Elige un grupo y un día para ver sus clases, aulas y docentes") { RelojEnVivo(estado.ahora) }; Spacer(Modifier.height(14.dp))
        Text("Grupo", style = MaterialTheme.typography.labelLarge, color = ColoresSU.TextoSecundario); Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) { items(estado.grupos) { grupo -> ChipFiltro(grupo, grupo == estado.grupoSeleccionado, { viewModel.seleccionarGrupo(grupo) }, if (grupo == estado.grupos.first()) Modifier.focusRequester(primerChip) else Modifier) } }
        Spacer(Modifier.height(10.dp)); Text("Día", style = MaterialTheme.typography.labelLarge, color = ColoresSU.TextoSecundario); Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { estado.semana.forEachIndexed { i, fecha -> ChipFiltro("${nombreDiaCorto(i + 1)} ${fecha.dayOfMonth}" + if (fecha == estado.ahora.toLocalDate()) " · Hoy" else "", estado.diaSeleccionado == i + 1, { viewModel.seleccionarDia(i + 1) }) } }
        Spacer(Modifier.height(12.dp)); estado.fechaSeleccionada?.let { Text("${estado.grupoSeleccionado ?: ""} · ${it.fechaLarga()} · ${estado.clases.size} clases", style = MaterialTheme.typography.titleMedium, color = ColoresSU.Primario) }; Spacer(Modifier.height(8.dp))
        if (estado.clases.isEmpty()) EstadoVacio("Este grupo no tiene clases ese día.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp, top = 4.dp)) { items(estado.clases, key = { it.clase.id }) { TarjetaClase(it, estado.ahora, estado.fechaSeleccionada == estado.ahora.toLocalDate()) } }
    }
}

@Composable private fun TarjetaClase(item: ClaseConAula, ahora: LocalDateTime, esHoy: Boolean) {
    val clase = item.clase; val hora = ahora.toLocalTime(); val enCurso = esHoy && clase.estaEnCurso(hora); val termino = esHoy && !hora.isBefore(clase.horaFin)
    TarjetaEnfocable({}, Modifier.fillMaxWidth(), if (enCurso) ColoresSU.Exito.copy(alpha = .12f) else ColoresSU.Superficie, relleno = 14.dp) { Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(96.dp)) { Text(clase.horaInicio.formatoHora(), style = MaterialTheme.typography.headlineSmall, color = if (termino) ColoresSU.TextoSecundario else ColoresSU.Texto); Text("a ${clase.horaFin.formatoHora()}", style = MaterialTheme.typography.bodySmall, color = ColoresSU.TextoSecundario) }
        Column(Modifier.weight(1f)) { Text(clase.materia, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (termino) ColoresSU.TextoSecundario else ColoresSU.Texto); Text(clase.docente, style = MaterialTheme.typography.bodyMedium, color = ColoresSU.TextoSecundario); item.cambio?.let { Text("Cambio de aula: ${it.motivo}", style = MaterialTheme.typography.bodySmall, color = ColoresSU.Acento) } }
        Spacer(Modifier.width(12.dp)); Column(horizontalAlignment = Alignment.End) { if (item.cambio != null) { Text("${item.cambio.aulaAnterior} →", style = MaterialTheme.typography.bodySmall, color = ColoresSU.TextoSecundario); Pastilla(item.aulaActual, ColoresSU.Acento, solida = true) } else { Text("Edificio ${clase.edificio}", style = MaterialTheme.typography.bodySmall, color = ColoresSU.TextoSecundario); Pastilla(clase.aula, ColoresSU.General) }; when { enCurso -> Pastilla("En curso", ColoresSU.Exito, Modifier.padding(top = 4.dp)); termino -> Pastilla("Terminó", ColoresSU.TextoSecundario, Modifier.padding(top = 4.dp)) } }
    } }
}
