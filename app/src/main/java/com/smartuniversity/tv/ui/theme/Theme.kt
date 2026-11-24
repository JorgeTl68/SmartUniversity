package com.smartuniversity.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import com.smartuniversity.tv.data.Prioridad

/** Paleta de Smart University: fondo azul marino con acentos claros, legible a distancia. */
object ColoresSU {
    val Fondo = Color(0xFF0B1626)
    val Superficie = Color(0xFF13233A)
    val SuperficieAlta = Color(0xFF1C3152)
    val Primario = Color(0xFF5AB2FF)
    val SobrePrimario = Color(0xFF00284A)
    val Acento = Color(0xFFFFC857)
    val Urgente = Color(0xFFFF6B6B)
    val Importante = Color(0xFFFFB74D)
    val General = Color(0xFF64B5F6)
    val Exito = Color(0xFF5ED6A0)
    val Texto = Color(0xFFEAF2FF)
    val TextoSecundario = Color(0xFFA3B6CF)
    val Borde = Color(0xFF2B4468)
}

fun colorDePrioridad(prioridad: Prioridad): Color = when (prioridad) {
    Prioridad.URGENTE -> ColoresSU.Urgente
    Prioridad.IMPORTANTE -> ColoresSU.Importante
    Prioridad.GENERAL -> ColoresSU.General
}

private val esquemaOscuro = darkColorScheme(
    primary = ColoresSU.Primario,
    onPrimary = ColoresSU.SobrePrimario,
    primaryContainer = ColoresSU.SuperficieAlta,
    onPrimaryContainer = ColoresSU.Texto,
    secondary = ColoresSU.Acento,
    onSecondary = Color(0xFF3A2A00),
    background = ColoresSU.Fondo,
    onBackground = ColoresSU.Texto,
    surface = ColoresSU.Superficie,
    onSurface = ColoresSU.Texto,
    surfaceVariant = ColoresSU.SuperficieAlta,
    onSurfaceVariant = ColoresSU.TextoSecundario,
    error = ColoresSU.Urgente,
    border = ColoresSU.Borde,
)

/** Tamaños un poco mayores que los de un teléfono: la pantalla se lee a varios metros. */
private val tipografia = Typography(
    displaySmall = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, lineHeight = 46.sp),
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    titleLarge = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 21.sp),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp),
)

@Composable
fun SmartUniversityTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = esquemaOscuro, typography = tipografia) {
        // Fuera de una Surface el texto sería negro: se fija el color claro para toda la app.
        CompositionLocalProvider(LocalContentColor provides ColoresSU.Texto, content = content)
    }
}
