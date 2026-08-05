package com.kevin.vitrina.scene.layers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.kevin.vitrina.scene.Layer

/**
 * El pulsómetro de la escena: fps promedio y tiempo de vida.
 * Vive mientras construimos; se quita (o se apaga) al final.
 */
class DebugHud(var enabled: Boolean = true) : Layer {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(150, 0x22, 0xD3, 0xEE)
        textSize = 28f
        typeface = Typeface.MONOSPACE
    }

    private var fps = 0f
    private var accum = 0f
    private var frames = 0
    private var uptime = 0f

    override fun update(dt: Float, t: Float, w: Int, h: Int) {
        uptime = t
        accum += dt
        frames++
        if (accum >= 0.5f) {          // refresca la lectura 2 veces por segundo
            fps = frames / accum
            accum = 0f
            frames = 0
        }
    }

    override fun draw(canvas: Canvas, w: Int, h: Int) {
        if (!enabled) return
        canvas.drawText("%3.0f fps · %.0fs".format(fps, uptime), 24f, h - 24f, paint)
    }
}
