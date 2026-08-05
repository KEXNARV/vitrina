package com.kevin.vitrina.scene

import android.content.Context
import android.graphics.Color
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * El lienzo crudo: un SurfaceView con hilo de render propio.
 * Cada frame: update(dt) → render(canvas) → post. Sin layouts, sin
 * widgets, sin Compose: la escena es dueña de cada pixel.
 */
class SceneView(
    context: Context,
    private val scene: Scene,
    /** 0.5 = render a media resolución; el compositor escala por hardware */
    private val renderScale: Float = 0.4f,
    /** Techo de fps: un idle screen corre horas — 30 estable calienta 3x
     *  menos que 90 y las nubes ni se enteran */
    private val maxFps: Int = 30,
) : SurfaceView(context), SurfaceHolder.Callback {

    private var renderThread: RenderThread? = null

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (renderScale < 1f && width > 0 && height > 0) {
            holder.setFixedSize(
                (width * renderScale).toInt(),
                (height * renderScale).toInt(),
            )
        }
        renderThread = RenderThread(holder).also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        renderThread?.shutdown()
        renderThread = null
    }

    private inner class RenderThread(private val holder: SurfaceHolder) : Thread("vitrina-render") {

        @Volatile private var running = true

        fun shutdown() {
            running = false
            join(1000)
        }

        override fun run() {
            val born = System.nanoTime()
            var lastFrame = born
            val frameBudgetNs = 1_000_000_000L / maxFps

            while (running) {
                val frameStart = System.nanoTime()
                val dt = (frameStart - lastFrame) / 1_000_000_000f
                val t = (frameStart - born) / 1_000_000_000f
                lastFrame = frameStart

                // Canvas acelerado por GPU; el software queda de plan B
                val canvas = try {
                    holder.lockHardwareCanvas() ?: holder.lockCanvas()
                } catch (e: Exception) {
                    holder.lockCanvas()
                } ?: continue

                try {
                    canvas.drawColor(Color.BLACK)
                    scene.update(dt.coerceAtMost(0.1f), t, canvas.width, canvas.height)
                    scene.render(canvas, canvas.width, canvas.height)
                } finally {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        // superficie destruida a mitad de frame: el loop termina solo
                    }
                }

                // Frame pacing: dormir lo que sobre del presupuesto
                val elapsed = System.nanoTime() - frameStart
                val sleepMs = (frameBudgetNs - elapsed) / 1_000_000L
                if (sleepMs > 1) {
                    try {
                        sleep(sleepMs)
                    } catch (e: InterruptedException) {
                        // shutdown en curso
                    }
                }
            }
        }
    }
}
