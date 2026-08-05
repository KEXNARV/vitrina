package com.kevin.vitrina.scene

import android.graphics.Canvas

/**
 * Una capa de la escena. Se actualiza con el tiempo y se dibuja sobre
 * el canvas crudo. Nada de views ni componentes: pixeles nuestros.
 *
 *  - dt: segundos desde el frame anterior (para animar a velocidad real)
 *  - t:  segundos desde que nació la escena (para osciladores)
 */
interface Layer {
    fun update(dt: Float, t: Float, w: Int, h: Int) {}
    fun draw(canvas: Canvas, w: Int, h: Int)
}

/**
 * La escena: una lista ordenada de capas. Lo primero en la lista queda
 * al fondo; lo último, al frente. El view que la contiene la palpita
 * una vez por frame.
 */
class Scene {
    private val layers = mutableListOf<Layer>()

    fun add(vararg layer: Layer) = apply { layers.addAll(layer) }

    fun update(dt: Float, t: Float, w: Int, h: Int) {
        for (layer in layers) layer.update(dt, t, w, h)
    }

    fun render(canvas: Canvas, w: Int, h: Int) {
        for (layer in layers) layer.draw(canvas, w, h)
    }
}
