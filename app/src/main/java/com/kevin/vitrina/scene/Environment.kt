package com.kevin.vitrina.scene

import android.content.Context
import com.kevin.vitrina.Prefs.latitude
import com.kevin.vitrina.Prefs.longitude
import com.kevin.vitrina.data.Weather
import com.kevin.vitrina.data.WeatherNow
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * El estado del mundo que las capas leen cada frame: qué clima hace y
 * qué hora es. Un hilo daemon refresca el clima cada 30 minutos; si
 * falla (sin red), reintenta al minuto.
 */
object Environment {

    @Volatile
    var weather: WeatherNow? = null
        private set

    private val started = AtomicBoolean(false)

    fun start(ctx: Context) {
        if (!started.compareAndSet(false, true)) return
        val app = ctx.applicationContext
        thread(isDaemon = true, name = "vitrina-weather") {
            while (true) {
                val w = runBlocking { Weather.fetch(app.latitude, app.longitude) }
                if (w != null) weather = w
                Thread.sleep(if (w == null) 60_000L else 30 * 60_000L)
            }
        }
    }

    /** Hora local como float: 14.5 = 2:30 pm. */
    fun hourOfDay(): Float {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f +
            cal.get(Calendar.SECOND) / 3600f
    }

    /** Cobertura de nubes 0..1 según el código WMO actual. */
    fun cloudCover(): Float = when (weather?.code ?: -1) {
        -1 -> 0.35f            // sin datos aún: cielo neutro
        0 -> 0.06f             // despejado
        1, 2 -> 0.35f          // parcialmente nublado
        3 -> 0.85f             // nublado
        45, 48 -> 0.92f        // niebla
        in 51..67 -> 0.75f     // llovizna / lluvia
        in 71..77 -> 0.80f     // nieve
        in 80..82 -> 0.82f     // aguaceros
        in 95..99 -> 0.93f     // tormenta
        else -> 0.4f
    }

    /** Qué tan plomizas están las nubes 0..1 (tormenta = casi negras). */
    fun cloudDarkness(): Float = when (weather?.code ?: -1) {
        3 -> 0.20f
        45, 48 -> 0.12f
        in 51..67 -> 0.38f
        in 80..82 -> 0.50f
        in 95..99 -> 0.68f
        else -> 0f
    }
}
