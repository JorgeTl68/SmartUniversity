package com.smartuniversity.tv.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.smartuniversity.tv.R
import com.smartuniversity.tv.data.Aviso
import com.smartuniversity.tv.data.Prioridad
import com.smartuniversity.tv.ui.theme.ColoresSU
import com.smartuniversity.tv.ui.theme.colorDePrioridad
import com.smartuniversity.tv.util.fechaLarga
import com.smartuniversity.tv.util.formatoHora
import com.smartuniversity.tv.util.mesCorto
import java.time.LocalDate
import java.time.LocalDateTime

/* Piezas visuales que comparten todas las pantallas de la app. */

private val FORMA_TARJETA = RoundedCornerShape(16.dp)

/** Título y descripción de cada sección, con espacio a la derecha para el reloj u otras acciones. */
@Composable
fun EncabezadoSeccion(
    titulo: String,
    descripcion: String,
    modifier: Modifier = Modifier,
    acciones: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.headlineLarge, color = ColoresSU.Texto)
            Text(descripcion, style = MaterialTheme.typography.bodyMedium, color = ColoresSU.TextoSecundario)
        }
        acciones()
    }
}

/** Hora grande y fecha larga; se actualiza con el reloj de la app. */
@Composable
fun RelojEnVivo(ahora: LocalDateTime, modifier: Modifier = Modifier, grande: Boolean = false) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(
            text = ahora.toLocalTime().formatoHora(),
            style = if (grande) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineLarge,
            color = ColoresSU.Texto,
        )
        Text(
            text = ahora.toLocalDate().fechaLarga(),
            style = MaterialTheme.typography.bodyMedium,
            color = ColoresSU.TextoSecundario,
        )
    }
}

/** Logotipo de la app (el mismo dibujo del icono). */
@Composable
fun Logotipo(modifier: Modifier = Modifier, tamano: Dp = 40.dp) {
    Box(
        modifier = modifier
            .size(tamano)
            .clip(RoundedCornerShape(tamano / 4))
            .background(Color(0xFF0B1F3A)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier.size(tamano * 0.78f),
        )
    }
}

/** Etiqueta pequeña de color (estado, categoría, aula...). */
@Composable
fun Pastilla(texto: String, color: Color, modifier: Modifier = Modifier, solida: Boolean = false) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (solida) color else color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = if (solida) ColoresSU.Fondo else color,
            maxLines = 1,
        )
    }
}

@Composable
fun EtiquetaPrioridad(prioridad: Prioridad, modifier: Modifier = Modifier) {
    Pastilla(prioridad.etiqueta, colorDePrioridad(prioridad), modifier)
}

/** Círculo de color que indica la prioridad de un aviso. */
@Composable
fun PuntoPrioridad(prioridad: Prioridad, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(colorDePrioridad(prioridad)),
    )
}

/**
 * Tarjeta que se puede enfocar con las flechas del control: al recibir el foco crece un poco
 * y muestra un borde, como piden las guías de diseño para TV.
 */
@Composable
fun TarjetaEnfocable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colorFondo: Color = ColoresSU.Superficie,
    escalaEnfoque: Float = 1.02f,
    relleno: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(FORMA_TARJETA),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colorFondo,
            contentColor = ColoresSU.Texto,
            focusedContainerColor = ColoresSU.SuperficieAlta,
            focusedContentColor = ColoresSU.Texto,
            pressedContainerColor = ColoresSU.SuperficieAlta,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = escalaEnfoque),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, ColoresSU.Primario), shape = FORMA_TARJETA),
        ),
    ) {
        Column(modifier = Modifier.padding(relleno), content = content)
    }
}

/** Panel del inicio: tarjeta enfocable con icono y título que abre su sección al presionar OK. */
@Composable
fun PanelInformativo(
    titulo: String,
    icono: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detalle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    TarjetaEnfocable(onClick = onClick, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, tint = ColoresSU.Primario, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(titulo, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (detalle != null) {
                Text(detalle, style = MaterialTheme.typography.labelMedium, color = ColoresSU.TextoSecundario)
            }
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

/** Cuadro con el día y el mes, como la hoja de un calendario. */
@Composable
fun BloqueFecha(fecha: LocalDate, modifier: Modifier = Modifier, color: Color = ColoresSU.Primario, tamano: Dp = 56.dp) {
    Column(
        modifier = modifier
            .size(tamano)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.18f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = fecha.dayOfMonth.toString(),
            style = if (tamano >= 64.dp) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
            color = color,
        )
        Text(fecha.mesCorto(), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** Mensaje centrado para cuando una lista no tiene elementos. */
@Composable
fun EstadoVacio(mensaje: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
        Text(mensaje, style = MaterialTheme.typography.bodyLarge, color = ColoresSU.TextoSecundario)
    }
}

/** Chip de filtro con los colores de la app. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChipFiltro(texto: String, seleccionado: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(selected = seleccionado, onClick = onClick, modifier = modifier) {
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}

/** Cinta inferior con los avisos urgentes desplazándose de derecha a izquierda. */
@Composable
fun CintaAvisosUrgentes(avisos: List<Aviso>, modifier: Modifier = Modifier) {
    if (avisos.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ColoresSU.Urgente.copy(alpha = 0.16f))
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .background(ColoresSU.Urgente)
                .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Text("URGENTE", style = MaterialTheme.typography.labelLarge, color = ColoresSU.Fondo, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = avisos.joinToString(separator = "      •      ") { "${it.titulo}: ${it.mensaje}" },
            style = MaterialTheme.typography.titleMedium,
            color = ColoresSU.Texto,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .weight(1f)
                .basicMarquee(iterations = Int.MAX_VALUE),
        )
    }
}

/** Pantalla provisional para una sección que todavía no se integra. */
@Composable
fun PantallaEnConstruccion(titulo: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 24.dp),
    ) {
        EncabezadoSeccion(titulo = titulo, descripcion = "Sección en desarrollo")
        Spacer(Modifier.height(24.dp))
        TarjetaEnfocable(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text("Esta sección la integra otro integrante del equipo.", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "Cuando su rama se fusione con main, aquí aparecerá la pantalla completa.",
                style = MaterialTheme.typography.bodyLarge,
                color = ColoresSU.TextoSecundario,
            )
        }
    }
}
