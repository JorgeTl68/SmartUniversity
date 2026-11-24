package com.smartuniversity.tv.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.smartuniversity.tv.data.AjustesPantalla
import com.smartuniversity.tv.data.Prioridad
import com.smartuniversity.tv.ui.componentes.ChipFiltro
import com.smartuniversity.tv.ui.componentes.EncabezadoSeccion
import com.smartuniversity.tv.ui.componentes.EstadoVacio
import com.smartuniversity.tv.ui.componentes.EtiquetaPrioridad
import com.smartuniversity.tv.ui.componentes.Logotipo
import com.smartuniversity.tv.ui.componentes.Pastilla
import com.smartuniversity.tv.ui.componentes.TarjetaEnfocable
import com.smartuniversity.tv.ui.theme.ColoresSU
import com.smartuniversity.tv.util.fechaCorta
import com.smartuniversity.tv.util.formatoHora
import com.smartuniversity.tv.util.relativaA
import kotlinx.coroutines.delay

/** Sección protegida con PIN para publicar avisos, registrar cambios de aula y ajustar la pantalla. */
@Composable
fun AdminScreen(viewModel: AdminViewModel = viewModel(factory = AdminViewModel.Factory)) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    if (!estado.desbloqueado) {
        PantallaPin(
            pin = estado.pin,
            incorrecto = estado.pinIncorrecto,
            onDigito = viewModel::escribirDigito,
            onBorrar = viewModel::borrarDigito,
        )
        return
    }

    // El mensaje de confirmación desaparece solo después de unos segundos.
    LaunchedEffect(estado.mensaje) {
        if (estado.mensaje != null) {
            delay(4_000)
            viewModel.descartarMensaje()
        }
    }

    // Al desbloquear, el foco pasa a la pestaña activa para seguir usando el control.
    val focoPestana = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focoPestana.requestFocus() } }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 40.dp, top = 20.dp),
        ) {
            EncabezadoSeccion(
                titulo = "Administración",
                descripcion = "Publica información en la pantalla sin reinstalar la app",
            )
            Spacer(Modifier.height(12.dp))
            TabRow(selectedTabIndex = estado.pestana.ordinal) {
                PestanaAdmin.entries.forEach { pestana ->
                    Tab(
                        selected = estado.pestana == pestana,
                        onFocus = { viewModel.elegirPestana(pestana) },
                        onClick = { viewModel.elegirPestana(pestana) },
                        modifier = if (estado.pestana == pestana) Modifier.focusRequester(focoPestana) else Modifier,
                    ) {
                        Text(
                            pestana.titulo,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            when (estado.pestana) {
                PestanaAdmin.AVISO -> FormularioNuevoAviso(estado, viewModel)
                PestanaAdmin.CAMBIO -> FormularioNuevoCambio(estado, viewModel)
                PestanaAdmin.PUBLICADOS -> ListasPublicadas(estado, viewModel)
                PestanaAdmin.AJUSTES -> PanelAjustes(estado, viewModel)
            }
        }
        // Confirmación flotante en la esquina inferior derecha.
        estado.mensaje?.let {
            Pastilla(
                texto = it,
                color = ColoresSU.Exito,
                solida = true,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 40.dp, bottom = 24.dp),
            )
        }
    }
}

// ---------------------------------------------------------------- PIN

@Composable
private fun PantallaPin(pin: String, incorrecto: Boolean, onDigito: (Char) -> Unit, onBorrar: () -> Unit) {
    val primeraTecla = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { primeraTecla.requestFocus() } }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 40.dp, top = 20.dp, bottom = 20.dp)
            // También acepta los números del control remoto o de un teclado.
            .onPreviewKeyEvent { evento ->
                if (evento.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val digito = digitoDeTecla(evento.key)
                when {
                    digito != null -> { onDigito(digito); true }
                    evento.key == Key.Backspace || evento.key == Key.Delete -> { onBorrar(); true }
                    else -> false
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Logotipo(tamano = 64.dp)
            Spacer(Modifier.height(16.dp))
            Text("Administración", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "Ingresa el PIN para publicar avisos, registrar cambios de aula y cambiar los ajustes de la pantalla.",
                style = MaterialTheme.typography.bodyLarge,
                color = ColoresSU.TextoSecundario,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(AdminViewModel.LONGITUD_PIN) { indice ->
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (indice < pin.length) ColoresSU.Primario else ColoresSU.SuperficieAlta),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (incorrecto) {
                Text("PIN incorrecto. Inténtalo de nuevo.", style = MaterialTheme.typography.titleMedium, color = ColoresSU.Urgente)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "PIN de demostración: ${AjustesPantalla.PIN_ADMINISTRACION}",
                style = MaterialTheme.typography.bodySmall,
                color = ColoresSU.TextoSecundario,
            )
        }
        Spacer(Modifier.width(40.dp))
        // Teclado numérico que se recorre con las flechas del control.
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("123", "456", "789").forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    fila.forEach { digito ->
                        TeclaPin(
                            texto = digito.toString(),
                            onClick = { onDigito(digito) },
                            modifier = if (digito == '1') Modifier.focusRequester(primeraTecla) else Modifier,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TeclaPin(texto = "Borrar", onClick = onBorrar, ancho = 154.dp)
                TeclaPin(texto = "0", onClick = { onDigito('0') })
            }
        }
    }
}

@Composable
private fun TeclaPin(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, ancho: Dp = 72.dp) {
    Button(onClick = onClick, modifier = modifier.width(ancho).height(56.dp)) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(texto, style = MaterialTheme.typography.titleLarge)
        }
    }
}

private fun digitoDeTecla(tecla: Key): Char? = when (tecla) {
    Key.Zero, Key.NumPad0 -> '0'
    Key.One, Key.NumPad1 -> '1'
    Key.Two, Key.NumPad2 -> '2'
    Key.Three, Key.NumPad3 -> '3'
    Key.Four, Key.NumPad4 -> '4'
    Key.Five, Key.NumPad5 -> '5'
    Key.Six, Key.NumPad6 -> '6'
    Key.Seven, Key.NumPad7 -> '7'
    Key.Eight, Key.NumPad8 -> '8'
    Key.Nine, Key.NumPad9 -> '9'
    else -> null
}

// ---------------------------------------------------------------- Publicar aviso

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormularioNuevoAviso(estado: AdminUiState, viewModel: AdminViewModel) {
    val aviso = estado.aviso
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            CampoTexto(
                etiqueta = "Título",
                valor = aviso.titulo,
                onCambio = { texto -> viewModel.editarAviso { it.copy(titulo = texto.take(70)) } },
                ayuda = "Ej. Suspensión de clases en el edificio C",
            )
            Spacer(Modifier.height(12.dp))
            CampoTexto(
                etiqueta = "Mensaje",
                valor = aviso.mensaje,
                onCambio = { texto -> viewModel.editarAviso { it.copy(mensaje = texto.take(280)) } },
                ayuda = "Explica qué pasa, a quién afecta y qué deben hacer",
                altura = 120.dp,
                unaLinea = false,
            )
            Spacer(Modifier.height(6.dp))
            Text("${aviso.mensaje.length}/280 caracteres", style = MaterialTheme.typography.bodySmall, color = ColoresSU.TextoSecundario)
        }
        Column(modifier = Modifier.weight(1f)) {
            Etiqueta("Prioridad")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Prioridad.entries.forEach { prioridad ->
                    ChipFiltro(prioridad.etiqueta, aviso.prioridad == prioridad, { viewModel.editarAviso { it.copy(prioridad = prioridad) } })
                }
            }
            Spacer(Modifier.height(10.dp))
            Etiqueta("Área que publica")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AREAS_AVISO.forEach { area ->
                    ChipFiltro(area, aviso.area == area, { viewModel.editarAviso { it.copy(area = area) } })
                }
            }
            Spacer(Modifier.height(10.dp))
            Etiqueta("Vigencia")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VIGENCIAS_AVISO.forEach { (dias, texto) ->
                    ChipFiltro(texto, aviso.diasVigencia == dias, { viewModel.editarAviso { it.copy(diasVigencia = dias) } })
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = viewModel::publicarAviso) {
                Text("Publicar aviso", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ---------------------------------------------------------------- Cambio de aula

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormularioNuevoCambio(estado: AdminUiState, viewModel: AdminViewModel) {
    val cambio = estado.cambio
    val clase = estado.claseElegida
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Etiqueta("1. Día")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            estado.fechasCambio.forEach { fecha ->
                ChipFiltro("${fecha.relativaA(estado.hoy)} · ${fecha.fechaCorta()}", cambio.fecha == fecha, { viewModel.elegirFechaCambio(fecha) })
            }
        }
        Spacer(Modifier.height(10.dp))
        Etiqueta("2. Grupo")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            estado.grupos.forEach { grupo ->
                ChipFiltro(grupo, cambio.grupo == grupo, { viewModel.elegirGrupoCambio(grupo) })
            }
        }
        Spacer(Modifier.height(10.dp))
        Etiqueta("3. Clase")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            estado.clasesDelGrupo.forEach { opcion ->
                ChipFiltro(
                    "${opcion.horaInicio.formatoHora()} ${opcion.materia} (${opcion.aula})",
                    cambio.claseId == opcion.id,
                    { viewModel.elegirClaseCambio(opcion.id) },
                )
            }
        }
        if (clase != null) {
            Spacer(Modifier.height(10.dp))
            Etiqueta("4. Aula nueva")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                estado.aulas.filter { it != clase.aula }.forEach { aula ->
                    ChipFiltro(aula, cambio.aulaNueva == aula, { viewModel.elegirAulaCambio(aula) })
                }
            }
            Spacer(Modifier.height(10.dp))
            Etiqueta("5. Motivo")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MOTIVOS_CAMBIO.forEach { motivo ->
                    ChipFiltro(motivo, cambio.motivo == motivo, { viewModel.elegirMotivoCambio(motivo) })
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = viewModel::registrarCambio) {
                Text("Registrar cambio", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = if (clase != null && cambio.aulaNueva != null) {
                    "${clase.grupo} · ${clase.materia}: ${clase.aula} → ${cambio.aulaNueva}"
                } else {
                    "Elige la clase y el aula a la que se mueve."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if (clase != null && cambio.aulaNueva != null) ColoresSU.Acento else ColoresSU.TextoSecundario,
            )
        }
    }
}

// ---------------------------------------------------------------- Publicados

@Composable
private fun ListasPublicadas(estado: AdminUiState, viewModel: AdminViewModel) {
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Etiqueta("Avisos vigentes (${estado.avisosPublicados.size})")
            if (estado.avisosPublicados.isEmpty()) EstadoVacio("No hay avisos vigentes.")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(estado.avisosPublicados, key = { it.id }) { aviso ->
                    FilaConAccion(
                        titulo = aviso.titulo,
                        detalle = "${aviso.area} · hasta ${aviso.vigenteHasta.fechaCorta()}",
                        etiqueta = { EtiquetaPrioridad(aviso.prioridad) },
                        textoAccion = "Retirar",
                        onAccion = { viewModel.eliminarAviso(aviso) },
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Etiqueta("Cambios de aula (${estado.cambiosPublicados.size})")
            if (estado.cambiosPublicados.isEmpty()) EstadoVacio("No hay cambios de aula programados.")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(estado.cambiosPublicados, key = { it.id }) { cambio ->
                    FilaConAccion(
                        titulo = "${cambio.grupo} · ${cambio.materia}",
                        detalle = "${cambio.fecha.fechaCorta()} ${cambio.horaInicio.formatoHora()} · ${cambio.aulaAnterior} → ${cambio.aulaNueva}",
                        etiqueta = { Pastilla(cambio.fecha.relativaA(estado.hoy), ColoresSU.Acento) },
                        textoAccion = "Cancelar",
                        onAccion = { viewModel.eliminarCambio(cambio) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaConAccion(
    titulo: String,
    detalle: String,
    etiqueta: @Composable () -> Unit,
    textoAccion: String,
    onAccion: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ColoresSU.Superficie)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            etiqueta()
            Spacer(Modifier.height(4.dp))
            Text(titulo, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = ColoresSU.TextoSecundario, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        Button(onClick = onAccion) { Text(textoAccion) }
    }
}

// ---------------------------------------------------------------- Ajustes

@Composable
private fun PanelAjustes(estado: AdminUiState, viewModel: AdminViewModel) {
    Column(modifier = Modifier.fillMaxWidth(0.75f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FilaAjuste(
            titulo = "Modo demostración",
            descripcion = "Fija el reloj en un día hábil a las 10:45 para mostrar la app con clases en curso a cualquier hora.",
            activo = estado.modoDemostracion,
            onCambiar = { viewModel.cambiarModoDemostracion(!estado.modoDemostracion) },
        )
        FilaAjuste(
            titulo = "Modo pantalla automático",
            descripcion = "Si nadie usa el control durante ${AjustesPantalla.MINUTOS_INACTIVIDAD} minutos, la app muestra el carrusel informativo.",
            activo = estado.modoPantallaAutomatico,
            onCambiar = { viewModel.cambiarModoPantallaAutomatico(!estado.modoPantallaAutomatico) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = viewModel::restablecerDatos) { Text("Restablecer datos de ejemplo") }
            Button(onClick = viewModel::bloquear) { Text("Cerrar sesión") }
        }
    }
}

@Composable
private fun FilaAjuste(titulo: String, descripcion: String, activo: Boolean, onCambiar: () -> Unit) {
    TarjetaEnfocable(onClick = onCambiar, modifier = Modifier.fillMaxWidth(), escalaEnfoque = 1.01f) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleLarge)
                Text(descripcion, style = MaterialTheme.typography.bodyMedium, color = ColoresSU.TextoSecundario)
            }
            Spacer(Modifier.width(16.dp))
            Pastilla(
                texto = if (activo) "Activado" else "Desactivado",
                color = if (activo) ColoresSU.Exito else ColoresSU.TextoSecundario,
                solida = activo,
            )
        }
    }
}

// ---------------------------------------------------------------- Piezas comunes

@Composable
private fun Etiqueta(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.labelLarge,
        color = ColoresSU.TextoSecundario,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

/**
 * Campo de texto adaptado a TV: con el foco se resalta el borde y OK abre el teclado en pantalla.
 * Las flechas arriba/abajo siempre mueven el foco para que el control nunca se quede atrapado, y
 * el botón de acción del teclado lleva al siguiente elemento.
 */
@Composable
private fun CampoTexto(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    ayuda: String,
    altura: Dp = 48.dp,
    unaLinea: Boolean = true,
) {
    var enfocado by remember { mutableStateOf(false) }
    val focos = LocalFocusManager.current
    Etiqueta(etiqueta)
    BasicTextField(
        value = valor,
        onValueChange = onCambio,
        singleLine = unaLinea,
        // El botón del teclado en pantalla pasa al siguiente campo (Título) o a las opciones (Mensaje).
        keyboardOptions = KeyboardOptions(imeAction = if (unaLinea) ImeAction.Next else ImeAction.Done),
        keyboardActions = KeyboardActions(
            onNext = { focos.moveFocus(FocusDirection.Down) },
            onDone = { focos.moveFocus(FocusDirection.Right) },
        ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = ColoresSU.Texto),
        cursorBrush = SolidColor(ColoresSU.Primario),
        modifier = Modifier
            .fillMaxWidth()
            .height(altura)
            .onFocusChanged { enfocado = it.isFocused }
            .onPreviewKeyEvent { evento ->
                if (evento.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (evento.key) {
                    Key.DirectionDown -> focos.moveFocus(FocusDirection.Down)
                    Key.DirectionUp -> focos.moveFocus(FocusDirection.Up)
                    else -> false
                }
            }
            .clip(RoundedCornerShape(12.dp))
            .background(if (enfocado) ColoresSU.SuperficieAlta else ColoresSU.Superficie)
            .border(
                width = if (enfocado) 2.dp else 1.dp,
                color = if (enfocado) ColoresSU.Primario else ColoresSU.Borde,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { campo ->
            Box {
                if (valor.isEmpty()) {
                    Text(ayuda, style = MaterialTheme.typography.bodyLarge, color = ColoresSU.TextoSecundario.copy(alpha = 0.7f))
                }
                campo()
            }
        },
    )
}
