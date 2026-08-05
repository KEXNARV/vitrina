package com.kevin.vitrina.scene

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.ColorSpace
import android.graphics.Shader
import android.os.Build
import java.util.Random

/**
 * Textura de ruido precomputada para el raymarcher: la GPU la muestrea
 * con filtrado bilinear, ~10x más barato que calcular hash aritmético.
 *
 * Canal R: ruido crudo. Canal G: el MISMO ruido desplazado (37,17) px.
 * Con eso, un solo tap entrega dos "rebanadas" consecutivas de ruido 3D
 * (técnica de iq): slice(z) lee R, slice(z+1) lee G, y se interpola.
 */
object NoiseTex {

    const val SIZE = 256

    val shader: BitmapShader by lazy {
        val n = SIZE
        val rnd = Random(20260703L)
        val base = IntArray(n * n) { rnd.nextInt(256) }
        val px = IntArray(n * n)
        for (y in 0 until n) {
            for (x in 0 until n) {
                val r = base[y * n + x]
                val g = base[((y + 17) and 255) * n + ((x + 37) and 255)]
                px[y * n + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or r
            }
        }
        // Espacio LINEAL: el shader debe leer el ruido tal cual, sin que
        // la conversión sRGB→linear le deforme la distribución
        val bmp = Bitmap.createBitmap(
            n, n, Bitmap.Config.ARGB_8888, false,
            ColorSpace.get(ColorSpace.Named.LINEAR_SRGB),
        )
        bmp.setPixels(px, 0, n, 0, 0, n, n)
        BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).apply {
            // CRÍTICO: sin filtrado LINEAR el sampler lee "pixel más
            // cercano" y el ruido sale escalonado → nubes cuadradas
            if (Build.VERSION.SDK_INT >= 33) {
                filterMode = BitmapShader.FILTER_MODE_LINEAR
            }
        }
    }
}
