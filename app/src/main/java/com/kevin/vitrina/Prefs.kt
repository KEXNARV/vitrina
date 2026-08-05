package com.kevin.vitrina

import android.content.Context

/**
 * Configuración persistente mínima: dónde está el endpoint del WMS
 * y las coordenadas para el clima. SharedPreferences alcanza de sobra.
 */
object Prefs {
    private const val FILE = "vitrina"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var Context.wmsUrl: String
        get() = sp(this).getString("wms_url", "") ?: ""
        set(v) = sp(this).edit().putString("wms_url", v.trim()).apply()

    var Context.wmsToken: String
        get() = sp(this).getString("wms_token", "") ?: ""
        set(v) = sp(this).edit().putString("wms_token", v.trim()).apply()

    // Ciudad de Panamá por defecto
    var Context.latitude: Double
        get() = sp(this).getFloat("lat", 8.9824f).toDouble()
        set(v) = sp(this).edit().putFloat("lat", v.toFloat()).apply()

    var Context.longitude: Double
        get() = sp(this).getFloat("lon", -79.5199f).toDouble()
        set(v) = sp(this).edit().putFloat("lon", v.toFloat()).apply()
}
