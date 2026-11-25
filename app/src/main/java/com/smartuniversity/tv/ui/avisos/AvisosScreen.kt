package com.smartuniversity.tv.ui.avisos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.smartuniversity.tv.data.Aviso
import com.smartuniversity.tv.ui.componentes.ChipFiltro
import com.smartuniversity.tv.ui.componentes.EncabezadoSeccion
import com.smartuniversity.tv.ui.componentes.EstadoVacio
import com.smartuniversity.tv.ui.componentes.EtiquetaPrioridad
import com.smartuniversity.tv.ui.componentes.PuntoPrioridad
import com.smartuniversity.tv.ui.componentes.TarjetaEnfocable
import com.smartuniversity.tv.ui.theme.ColoresSU
import com.smartuniversity.tv.ui.theme.colorDePrioridad
import com.smartuniversity.tv.util.fechaLarga
import com.smartuniversity.tv.util.relativaA
import java.time.LocalDate

/**
 * Avisos vigentes en formato lista-detalle: al mover el foco por la lista de la izquierda,
 * el panel de la derecha muestra el aviso completo.
 */
@Composable
fun AvisosScreen(viewModel: AvisosViewModel = viewModel(factory = AvisosViewModel.Factory)) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var idEnfocado by remember { mutableLongStateOf(-1L) }
    val avisoDetalle = estado.avisos.firstOrNull { it.id == idEnfocado } ?: estado.avisos.firstOrNull()
    val primerAviso = remember { FocusRequester() }
    LaunchedEffect(estado.avisos.isNotEmpty()) {
        if (estado.avisos.isNotEmpty()) runCatching { primerAviso.requestFocus() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 40.dp, top = 20.dp),
    ) {
        EncabezadoSeccion(
            titulo = "Avisos",
            descripcion = "Comunicados vigentes de las áreas de la universidad",
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FiltroAvisos.entries.forEach { filtro ->
                ChipFiltro(
                    texto = "${filtro.etiqueta} (${estado.conteos[filtro] ?: 0})",
                    seleccionado = estado.filtro == filtro,
                    onClick = { viewModel.cambiarFiltro(filtro) },
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        if (estado.avisos.isEmpty()) {
            EstadoVacio("No hay avisos con este filtro.")
            return@Column
        }

        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            LazyColumn(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(estado.avisos, key = { it.id }) { aviso ->
                    ElementoAviso(
                        aviso = aviso,
                        hoy = estado.hoy,
                        modifier = Modifier
                            .then(if (aviso == estado.avisos.first()) Modifier.focusRequester(primerAviso) else Modifier)
                            .onFocusChanged { if (it.isFocused) idEnfocado = aviso.id },
                    )
                }
            }
            avisoDetalle?.let { DetalleAviso(it, estado.hoy, Modifier.weight(0.55f)) }
        }
    }
}

@Composable
private fun ElementoAviso(aviso: Aviso, hoy: LocalDate, modifier: Modifier = Modifier) {
    TarjetaEnfocable(onClick = {}, modifier = modifier.fillMaxWidth(), relleno = 14.dp) {
        Row(verticalAlignment = Alignment.Top) {
            PuntoPrioridad(aviso.prioridad, Modifier.padding(top = 7.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(aviso.titulo, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${aviso.area} · ${aviso.fechaPublicacion.relativaA(hoy)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColoresSU.TextoSecundario,
                )
            }
        }
    }
}

@Composable
private fun DetalleAviso(aviso: Aviso, hoy: LocalDate, modifier: Modifier = Modifier) {
    val color = colorDePrioridad(aviso.prioridad)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ColoresSU.Superficie)
            .padding(24.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
            Spacer(Modifier.height(14.dp))
            EtiquetaPrioridad(aviso.prioridad)
            Spacer(Modifier.height(10.dp))
            Text(aviso.titulo, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(aviso.area, style = MaterialTheme.typography.titleMedium, color = ColoresSU.Primario)
            Spacer(Modifier.height(16.dp))
            Text(aviso.mensaje, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))
            Text(
                "Publicado: ${aviso.fechaPublicacion.fechaLarga()}",
                style = MaterialTheme.typography.bodySmall,
                color = ColoresSU.TextoSecundario,
            )
            Text(
                if (aviso.vigenteHasta == hoy) "Vigente solo hoy" else "Vigente hasta el ${aviso.vigenteHasta.fechaLarga().lowercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = ColoresSU.TextoSecundario,
            )
        }
    }
}
