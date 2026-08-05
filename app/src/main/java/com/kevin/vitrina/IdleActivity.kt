package com.kevin.vitrina

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kevin.vitrina.scene.Scene
import com.kevin.vitrina.scene.SceneView
import com.kevin.vitrina.scene.Environment
import com.kevin.vitrina.scene.layers.SkyLayer
import com.kevin.vitrina.scene.layers.DebugHud

/**
 * El modo idle: pantalla completa, siempre encendida, y adentro SOLO
 * la escena — ni un view de Android más. Un toque la cierra.
 */
class IdleActivity : ComponentActivity() {

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // La pantalla no se apaga mientras Vitrina esté al frente
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Modo inmersivo: sin barra de estado ni de navegación
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        Environment.start(this)
        val scene = Scene().add(
            SkyLayer(),
            DebugHud(),
        )
        val view = SceneView(this, scene)
        view.setOnClickListener { finish() }
        setContentView(view)
    }
}
