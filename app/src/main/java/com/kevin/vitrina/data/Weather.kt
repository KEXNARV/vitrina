package com.kevin.vitrina.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HourForecast(
    val label: String,   // "3 pm"
    val emoji: String,
    val tempC: Int,
)

data class WeatherNow(
    val tempC: Double,
    val feelsLikeC: Double,
    val humidity: Int,
    val description: String,
    val emoji: String,
    val maxC: Double,
    val minC: Double,
    val rainChance: Int,
    val raining: Boolean,
    val code: Int,                     // código WMO crudo, para el fondo teatral
    val hourly: List<HourForecast>,    // próximas horas (+3h, +6h, +9h)
)

/**
 * Clima vía Open-Meteo: gratis, sin API key, sin registro.
 * Un solo GET trae condición actual + máx/mín/lluvia del día + pronóstico horario.
 */
object Weather {

    suspend fun fetch(lat: Double, lon: Double): WeatherNow? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code" +
                "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                "&hourly=temperature_2m,weather_code" +
                "&timezone=auto&forecast_days=2"
            val body = httpGet(url) ?: return@withContext null
            val json = JSONObject(body)
            val cur = json.getJSONObject("current")
            val daily = json.getJSONObject("daily")
            val code = cur.getInt("weather_code")
            val (desc, emoji) = describe(code)
            WeatherNow(
                tempC = cur.getDouble("temperature_2m"),
                feelsLikeC = cur.getDouble("apparent_temperature"),
                humidity = cur.getInt("relative_humidity_2m"),
                description = desc,
                emoji = emoji,
                maxC = daily.getJSONArray("temperature_2m_max").getDouble(0),
                minC = daily.getJSONArray("temperature_2m_min").getDouble(0),
                rainChance = daily.getJSONArray("precipitation_probability_max").getInt(0),
                // Códigos WMO de llovizna, lluvia, aguaceros y tormenta
                raining = code in 51..67 || code in 80..99,
                code = code,
                hourly = parseHourly(json),
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseHourly(json: JSONObject): List<HourForecast> = try {
        val hourly = json.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val temps = hourly.getJSONArray("temperature_2m")
        val codes = hourly.getJSONArray("weather_code")

        // Open-Meteo devuelve horas locales tipo "2026-07-03T11:00":
        // ubicamos la hora actual y saltamos desde ahí
        val nowKey = SimpleDateFormat("yyyy-MM-dd'T'HH", Locale.US).format(Date())
        var idx = 0
        for (i in 0 until times.length()) {
            if (times.getString(i).startsWith(nowKey)) { idx = i; break }
        }
        listOf(3, 6, 9).mapNotNull { offset ->
            val j = idx + offset
            if (j >= times.length()) return@mapNotNull null
            val hr = times.getString(j).substring(11, 13).toInt()
            val label = "${if (hr % 12 == 0) 12 else hr % 12} ${if (hr < 12) "am" else "pm"}"
            HourForecast(
                label = label,
                emoji = describe(codes.getInt(j)).second,
                tempC = temps.getDouble(j).toInt(),
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    // Códigos WMO estándar que usa Open-Meteo
    private fun describe(code: Int): Pair<String, String> = when (code) {
        0 -> "Despejado" to "☀️"
        1, 2 -> "Parcialmente nublado" to "🌤️"
        3 -> "Nublado" to "☁️"
        45, 48 -> "Niebla" to "🌫️"
        in 51..57 -> "Llovizna" to "🌦️"
        in 61..67 -> "Lluvia" to "🌧️"
        in 71..77 -> "Nieve" to "🌨️"
        in 80..82 -> "Aguaceros" to "🌧️"
        in 95..99 -> "Tormenta" to "⛈️"
        else -> "—" to "🌡️"
    }
}

internal fun httpGet(urlStr: String, bearer: String? = null): String? {
    var conn: HttpURLConnection? = null
    return try {
        conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        if (!bearer.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $bearer")
        if (conn.responseCode in 200..299) conn.inputStream.bufferedReader().readText() else null
    } catch (e: Exception) {
        null
    } finally {
        conn?.disconnect()
    }
}
