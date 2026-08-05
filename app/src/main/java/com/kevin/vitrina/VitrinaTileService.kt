package com.kevin.vitrina

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService

/**
 * Tile de ajustes rápidos: "Vitrina" aparece junto a Wi-Fi/Bluetooth
 * en la cortina de notificaciones y enciende el modo idle con un toque.
 */
class VitrinaTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, IdleActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
