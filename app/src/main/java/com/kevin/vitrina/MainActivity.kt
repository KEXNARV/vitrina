package com.kevin.vitrina

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kevin.vitrina.Prefs.wmsToken
import com.kevin.vitrina.Prefs.wmsUrl
import com.kevin.vitrina.data.Agenda

/**
 * Configuración: pedir permiso de calendario, apuntar el tablero WMS
 * y previsualizar el screensaver sin esperar a conectar el cargador.
 */
class MainActivity : ComponentActivity() {

    private val askCalendar = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Toast.makeText(
            this,
            if (granted) "Calendario conectado ✓" else "Sin permiso: la agenda quedará vacía",
            Toast.LENGTH_SHORT,
        ).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF22D3EE),
                    background = Color.Black,
                    surface = Color.Black,
                )
            ) {
                Surface(Modifier.fillMaxSize()) {
                    SettingsForm(
                        onAskCalendar = { askCalendar.launch(Manifest.permission.READ_CALENDAR) },
                        onStartIdle = { startActivity(Intent(this, IdleActivity::class.java)) },
                        onOpenSystemSettings = {
                            // Pantalla del sistema donde se elige el screensaver activo
                            startActivity(Intent("android.settings.DREAM_SETTINGS"))
                        },
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun SettingsForm(
    onAskCalendar: () -> Unit,
    onStartIdle: () -> Unit,
    onOpenSystemSettings: () -> Unit,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var url by remember { mutableStateOf(ctx.wmsUrl) }
    var token by remember { mutableStateOf(ctx.wmsToken) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Vitrina", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Idle screen con hora, clima, agenda y datos de operación. " +
                "La pantalla queda encendida hasta que la toques.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF8A93A6),
        )

        Spacer(Modifier.height(8.dp))

        Button(onClick = onStartIdle, modifier = Modifier.fillMaxWidth()) {
            Text("Encender Vitrina")
        }
        Text(
            "Tip: agrega el tile \"Vitrina\" en los ajustes rápidos para " +
                "encenderla desde la cortina de notificaciones.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8A93A6),
        )

        Spacer(Modifier.height(8.dp))

        Text("1 · Calendario", style = MaterialTheme.typography.titleMedium)
        val hasCal = Agenda.hasPermission(ctx)
        Button(onClick = onAskCalendar, enabled = !hasCal) {
            Text(if (hasCal) "Permiso otorgado ✓" else "Conectar calendario")
        }

        Spacer(Modifier.height(8.dp))

        Text("2 · Tablero de trabajo (opcional)", style = MaterialTheme.typography.titleMedium)
        Text(
            "URL de un endpoint que devuelva {title, metrics:[{label, value, hint}]}.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8A93A6),
        )
        OutlinedTextField(
            value = url,
            onValueChange = { url = it; ctx.wmsUrl = it },
            label = { Text("URL del tablero") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = token,
            onValueChange = { token = it; ctx.wmsToken = it },
            label = { Text("Token (opcional, va como Bearer)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(8.dp))

        Text("3 · Extra (opcional)", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = onOpenSystemSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Usar también como screensaver del sistema")
        }
    }
}
