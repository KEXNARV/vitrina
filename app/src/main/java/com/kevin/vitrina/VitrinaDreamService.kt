package com.kevin.vitrina

import android.service.dreams.DreamService
import com.kevin.vitrina.scene.Scene
import com.kevin.vitrina.scene.SceneView
import com.kevin.vitrina.scene.Environment
import com.kevin.vitrina.scene.layers.SkyLayer
import com.kevin.vitrina.scene.layers.DebugHud

/**
 * El screensaver del sistema: muestra exactamente la misma escena que
 * el modo idle. Como la escena es un SurfaceView crudo, no hay ciclo
 * de vida de Compose que fabricar.
 */
class VitrinaDreamService : DreamService() {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        isInteractive = false   // cualquier toque despierta el teléfono
        isFullscreen = true
        isScreenBright = true

        Environment.start(this)
        val scene = Scene().add(
            SkyLayer(),
            DebugHud(),
        )
        setContentView(SceneView(this, scene))
    }
}
