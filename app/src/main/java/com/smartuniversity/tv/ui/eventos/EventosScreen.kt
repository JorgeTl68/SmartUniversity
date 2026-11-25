package com.smartuniversity.tv.ui.eventos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import com.smartuniversity.tv.data.Evento
import com.smartuniversity.tv.ui.componentes.BloqueFecha
import com.smartuniversity.tv.ui.componentes.ChipFiltro
import com.smartuniversity.tv.ui.componentes.EncabezadoSeccion
import com.smartuniversity.tv.ui.componentes.EstadoVacio
import com.smartuniversity.tv.ui.componentes.Pastilla
import com.smartuniversity.tv.ui.componentes.TarjetaEnfocable
import com.smartuniversity.tv.ui.theme.ColoresSU
import com.smartuniversity.tv.util.rangoHoras
import com.smartuniversity.tv.util.relativaA
import java.time.LocalDate

/** Calendario de eventos próximos en una cuadrícula de tarjetas, con filtro por categoría. */
@Composable
fun EventosScreen(viewModel: EventosViewModel = viewModel(factory = EventosViewModel.Factory)) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val hoy = estado.ahora.toLocalDate()
    val chipTodos = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { chipTodos.requestFocus() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 40.dp, top = 20.dp),
    ) {
        EncabezadoSeccion(
            titulo = "Eventos",
            descripcion = "Conferencias, ferias, talleres y actividades del campus",
        )
        Spacer(Modifier.height(14.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                ChipFiltro(
                    texto = "Todos",
                    seleccionado = estado.categoria == null,
                    onClick = { viewModel.elegirCategoria(null) },
                    modifier = Modifier.focusRequester(chipTodos),
                )
            }
            items(estado.categorias) { categoria ->
                ChipFiltro(
                    texto = categoria,
                    seleccionado = estado.categoria == categoria,
                    onClick = { viewModel.elegirCategoria(categoria) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        if (estado.eventos.isEmpty()) {
            EstadoVacio("No hay eventos próximos en esta categoría.")
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp, top = 4.dp, start = 4.dp, end = 4.dp),
        ) {
            items(estado.eventos, key = { it.id }) { evento ->
                TarjetaEvento(evento, hoy)
            }
        }
    }
}

@Composable
private fun TarjetaEvento(evento: Evento, hoy: LocalDate) {
    val esHoy = evento.fecha == hoy
    TarjetaEnfocable(
        onClick = {},
        modifier = Modifier.fillMaxWidth().height(232.dp),
        escalaEnfoque = 1.04f,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BloqueFecha(evento.fecha, color = if (esHoy) ColoresSU.Exito else ColoresSU.Primario)
            Spacer(Modifier.width(12.dp))
            Column {
                Pastilla(evento.fecha.relativaA(hoy), if (esHoy) ColoresSU.Exito else ColoresSU.Primario, solida = esHoy)
                Spacer(Modifier.height(4.dp))
                Pastilla(evento.categoria, ColoresSU.Acento)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(evento.titulo, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(
            "${rangoHoras(evento.horaInicio, evento.horaFin)} · ${evento.lugar}",
            style = MaterialTheme.typography.bodyMedium,
            color = ColoresSU.Primario,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            evento.descripcion,
            style = MaterialTheme.typography.bodySmall,
            color = ColoresSU.TextoSecundario,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        Text(
            "Organiza: ${evento.organizador}",
            style = MaterialTheme.typography.labelSmall,
            color = ColoresSU.TextoSecundario,
        )
    }
}
