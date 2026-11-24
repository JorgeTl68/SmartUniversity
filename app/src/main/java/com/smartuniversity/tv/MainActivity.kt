package com.smartuniversity.tv

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.smartuniversity.tv.ui.navegacion.NavegacionPrincipal
import com.smartuniversity.tv.ui.theme.SmartUniversityTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Una pantalla informativa no debe apagarse ni mostrar el protector de pantalla.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            SmartUniversityTheme {
                NavegacionPrincipal()
            }
        }
    }
}
