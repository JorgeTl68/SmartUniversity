package com.smartuniversity.tv.ui.cambios

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
import com.smartuniversity.tv.data.CambioAula
import com.smartuniversity.tv.ui.componentes.*
import com.smartuniversity.tv.ui.theme.ColoresSU
import com.smartuniversity.tv.util.*
import java.time.LocalTime

@Composable
fun CambiosAulaScreen(viewModel: CambiosAulaViewModel = viewModel(factory = CambiosAulaViewModel.Factory)) {
    val estado by viewModel.estado.collectAsStateWithLifecycle(); val hoy = estado.ahora.toLocalDate(); val primeraFila = remember { FocusRequester() }; val hayCambios = estado.cambiosHoy.isNotEmpty() || estado.proximos.isNotEmpty()
    LaunchedEffect(hayCambios) { if (hayCambios) runCatching { primeraFila.requestFocus() } }
    Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 40.dp, top = 20.dp)) {
        EncabezadoSeccion("Cambios de aula", "Clases que hoy o en los próximos días se imparten en otro salón") { RelojEnVivo(estado.ahora) }; Spacer(Modifier.height(16.dp))
        if (!hayCambios) { EstadoVacio("No hay cambios de aula programados. Todas las clases siguen en su salón."); return@Column }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { TituloFecha("Hoy · ${hoy.fechaLarga()}") }; if (estado.cambiosHoy.isEmpty()) item { EstadoVacio("Hoy no hay cambios de aula.") }
            items(estado.cambiosHoy, key = { it.id }) { cambio -> FilaCambio(cambio, estado.ahora.toLocalTime(), true, if (cambio == estado.cambiosHoy.first()) Modifier.focusRequester(primeraFila) else Modifier) }
            estado.proximos.forEach { (fecha, cambios) -> item(key = fecha.toString()) { TituloFecha("${fecha.relativaA(hoy)} · ${fecha.fechaLarga()}") }; items(cambios, key = { it.id }) { cambio -> FilaCambio(cambio, estado.ahora.toLocalTime(), false, if (estado.cambiosHoy.isEmpty() && cambio == cambios.first()) Modifier.focusRequester(primeraFila) else Modifier) } }
        }
    }
}
@Composable private fun TituloFecha(texto: String) { Text(texto, style = MaterialTheme.typography.titleLarge, color = ColoresSU.Primario, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) }
@Composable private fun FilaCambio(cambio: CambioAula, hora: LocalTime, esHoy: Boolean, modifier: Modifier = Modifier) { TarjetaEnfocable({}, modifier.fillMaxWidth(), relleno = 14.dp) { Row(verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.width(130.dp)) { Text(rangoHoras(cambio.horaInicio, cambio.horaFin), style = MaterialTheme.typography.titleMedium); Text(cambio.grupo, style = MaterialTheme.typography.bodyMedium, color = ColoresSU.Primario) }
    Column(Modifier.weight(1f)) { Text(cambio.materia, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("Motivo: ${cambio.motivo}", style = MaterialTheme.typography.bodyMedium, color = ColoresSU.TextoSecundario) }
    Spacer(Modifier.width(12.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Text(cambio.aulaAnterior, style = MaterialTheme.typography.titleMedium, color = ColoresSU.TextoSecundario); Text("  →  ", style = MaterialTheme.typography.titleMedium, color = ColoresSU.TextoSecundario); Pastilla(cambio.aulaNueva, ColoresSU.Acento, solida = true) }; if (esHoy) { Spacer(Modifier.width(12.dp)); when { !hora.isBefore(cambio.horaFin) -> Pastilla("Terminó", ColoresSU.TextoSecundario); !hora.isBefore(cambio.horaInicio) -> Pastilla("En curso", ColoresSU.Exito); else -> Pastilla("Próxima", ColoresSU.General) } }
} } }
