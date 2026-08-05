package com.kevin.vitrina.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class WmsMetric(val label: String, val value: String, val hint: String?)
data class WmsBoard(val title: String, val metrics: List<WmsMetric>)

/**
 * Tablero de trabajo genérico: apunta a cualquier endpoint que devuelva
 *
 *   { "title": "Operación hoy",
 *     "metrics": [ {"label": "Pedidos", "value": "142", "hint": "+12% vs ayer"}, ... ] }
 *
 * Así el idle screen no queda amarrado a un sistema: cualquier módulo del
 * WMS (o lo que sea) puede exponer su resumen con ese contrato.
 */
object Wms {

    suspend fun fetch(url: String, token: String): WmsBoard? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        try {
            val body = httpGet(url, token.ifBlank { null }) ?: return@withContext null
            val json = JSONObject(body)
            val arr = json.optJSONArray("metrics") ?: return@withContext null
            val metrics = (0 until arr.length()).map { i ->
                val m = arr.getJSONObject(i)
                WmsMetric(
                    label = m.optString("label", "—"),
                    value = m.optString("value", "—"),
                    hint = m.optString("hint").ifBlank { null },
                )
            }
            WmsBoard(title = json.optString("title", "Operación"), metrics = metrics)
        } catch (e: Exception) {
            null
        }
    }
}
