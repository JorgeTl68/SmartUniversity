package com.smartuniversity.tv.ui.navegacion

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.NavigationDrawerScope
import androidx.tv.material3.Text
import com.smartuniversity.tv.SmartUniversityApp
import com.smartuniversity.tv.data.AjustesPantalla
import com.smartuniversity.tv.ui.admin.AdminScreen
import com.smartuniversity.tv.ui.avisos.AvisosScreen
import com.smartuniversity.tv.ui.cambios.CambiosAulaScreen
import com.smartuniversity.tv.ui.componentes.Logotipo
import com.smartuniversity.tv.ui.eventos.EventosScreen
import com.smartuniversity.tv.ui.horarios.HorariosScreen
import com.smartuniversity.tv.ui.inicio.InicioScreen
import com.smartuniversity.tv.ui.pantalla.ModoPantallaScreen
import com.smartuniversity.tv.ui.theme.ColoresSU
import kotlinx.coroutines.delay

/** Secciones del menú lateral. */
enum class Seccion(val titulo: String, val icono: ImageVector) {
    INICIO("Inicio", Icons.Default.Home),
    HORARIOS("Horarios", Icons.Default.DateRange),
    AVISOS("Avisos", Icons.Default.Notifications),
    EVENTOS("Eventos", Icons.Default.Star),
    CAMBIOS_AULA("Cambios de aula", Icons.Default.Place),
    MODO_PANTALLA("Modo pantalla", Icons.Default.PlayArrow),
    ADMINISTRACION("Administración", Icons.Default.Settings),
}

/** Ancho del menú cuando está cerrado (solo iconos); el contenido empieza después. */
private val ANCHO_MENU_CERRADO = 80.dp

/**
 * Estructura principal: menú lateral de TV a la izquierda y la sección elegida a la derecha.
 * Si nadie usa el control durante un rato, la app entra sola al modo pantalla (carrusel).
 */
@Composable
fun NavegacionPrincipal() {
    val contenedor = (LocalContext.current.applicationContext as SmartUniversityApp).contenedor
    val modoAutomatico by contenedor.ajustes.modoPantallaAutomatico.collectAsStateWithLifecycle()

    var seccion by rememberSaveable { mutableStateOf(Seccion.INICIO) }
    var enModoPantalla by rememberSaveable { mutableStateOf(false) }
    var ultimaInteraccion by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    // Apunta a la opción del menú de la sección abierta.
    val focoMenu = remember { FocusRequester() }

    // Cada tecla reinicia la cuenta; en administración nunca se activa para no interrumpir.
    LaunchedEffect(ultimaInteraccion, seccion, enModoPantalla, modoAutomatico) {
        if (modoAutomatico && !enModoPantalla && seccion != Seccion.ADMINISTRACION) {
            delay(AjustesPantalla.MINUTOS_INACTIVIDAD * 60_000L)
            enModoPantalla = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColoresSU.Fondo)
            .onPreviewKeyEvent {
                ultimaInteraccion = SystemClock.uptimeMillis()
                false
            },
    ) {
        if (enModoPantalla) {
            ModoPantallaScreen(onSalir = { enModoPantalla = false })
        } else {
            // ATRÁS en cualquier sección regresa al inicio; en el inicio cierra la app.
            BackHandler(enabled = seccion != Seccion.INICIO) { seccion = Seccion.INICIO }

            ModalNavigationDrawer(
                drawerContent = { estado ->
                    MenuLateral(
                        estado = estado,
                        seleccionada = seccion,
                        focoSeleccionada = focoMenu,
                        onSeleccionar = { elegida ->
                            if (elegida == Seccion.MODO_PANTALLA) enModoPantalla = true else seccion = elegida
                        },
                    )
                },
            ) {
                // Al volver del menú, el foco regresa al último elemento que tenía.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = ANCHO_MENU_CERRADO)
                        .focusRestorer(FocusRequester.Default)
                        .focusGroup(),
                ) {
                    // Solo la flecha izquierda saca el foco del contenido, y lo lleva a la sección
                    // abierta del menú (no a la opción que esté más cerca en la pantalla).
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .focusProperties {
                                onExit = {
                                    when (requestedFocusDirection) {
                                        FocusDirection.Left -> runCatching { focoMenu.requestFocus() }
                                        FocusDirection.Up, FocusDirection.Down, FocusDirection.Right -> cancelFocusChange()
                                        else -> Unit // requestFocus() desde el código: se permite.
                                    }
                                }
                            }
                            .focusGroup(),
                    ) {
                        when (seccion) {
                            Seccion.INICIO -> InicioScreen(onAbrirSeccion = { seccion = it })
                            Seccion.HORARIOS -> HorariosScreen()
                            Seccion.AVISOS -> AvisosScreen()
                            Seccion.EVENTOS -> EventosScreen()
                            Seccion.CAMBIOS_AULA -> CambiosAulaScreen()
                            Seccion.ADMINISTRACION -> AdminScreen()
                            Seccion.MODO_PANTALLA -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationDrawerScope.MenuLateral(
    estado: DrawerValue,
    seleccionada: Seccion,
    focoSeleccionada: FocusRequester,
    onSeleccionar: (Seccion) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(ColoresSU.Fondo.copy(alpha = 0.96f))
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .selectableGroup(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
            Logotipo(tamano = 40.dp)
            if (estado == DrawerValue.Open) {
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Smart University", style = MaterialTheme.typography.titleLarge, color = ColoresSU.Texto)
                    Text("Pantalla informativa", style = MaterialTheme.typography.bodySmall, color = ColoresSU.TextoSecundario)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Seccion.entries.forEach { opcion ->
            if (opcion == Seccion.ADMINISTRACION) Spacer(Modifier.weight(1f))
            NavigationDrawerItem(
                selected = opcion == seleccionada,
                onClick = { onSeleccionar(opcion) },
                leadingContent = { Icon(opcion.icono, contentDescription = null) },
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .then(if (opcion == seleccionada) Modifier.focusRequester(focoSeleccionada) else Modifier),
            ) {
                Text(opcion.titulo)
            }
        }
    }
}
