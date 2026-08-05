package com.kevin.vitrina.data

import android.Manifest
import android.content.Context
import android.content.ContentUris
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AgendaItem(
    val title: String,
    val startMillis: Long,
    val allDay: Boolean,
    val calendarName: String,
) {
    val timeLabel: String
        get() {
            if (allDay) return "Todo el día"
            val fmt = SimpleDateFormat("h:mm a", Locale("es"))
            val day = SimpleDateFormat("EEE", Locale("es"))
            val start = Date(startMillis)
            val isToday = Calendar.getInstance().let { now ->
                val ev = Calendar.getInstance().apply { time = start }
                now.get(Calendar.DAY_OF_YEAR) == ev.get(Calendar.DAY_OF_YEAR) &&
                    now.get(Calendar.YEAR) == ev.get(Calendar.YEAR)
            }
            return if (isToday) fmt.format(start)
            else "${day.format(start).replaceFirstChar { it.uppercase() }} ${fmt.format(start)}"
        }
}

/**
 * Lee los eventos de las próximas 36 horas de TODOS los calendarios
 * sincronizados en el teléfono (Google Calendar incluido), usando la
 * tabla Instances que ya expande eventos recurrentes.
 */
object Agenda {

    fun hasPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun fetch(ctx: Context, limit: Int = 6): List<AgendaItem>? = withContext(Dispatchers.IO) {
        if (!hasPermission(ctx)) return@withContext null
        try {
            val begin = System.currentTimeMillis()
            val end = begin + 36L * 3600 * 1000
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, begin)
            ContentUris.appendId(builder, end)

            val projection = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            )
            val items = mutableListOf<AgendaItem>()
            ctx.contentResolver.query(builder.build(), projection, null, null, "begin ASC")?.use { c ->
                while (c.moveToNext() && items.size < limit) {
                    items += AgendaItem(
                        title = c.getString(0) ?: "(sin título)",
                        startMillis = c.getLong(1),
                        allDay = c.getInt(2) == 1,
                        calendarName = c.getString(3) ?: "",
                    )
                }
            }
            items
        } catch (e: Exception) {
            null
        }
    }
}
