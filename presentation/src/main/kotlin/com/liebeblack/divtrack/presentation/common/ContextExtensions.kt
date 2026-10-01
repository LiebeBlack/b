package com.liebeblack.divtrack.presentation.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Copia texto al portapapeles del sistema (API de Android, sin deprecaciones de Compose). */
fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

/**
 * Abre la pantalla de ajustes de red del sistema.
 *
 * Es la salida honesta cuando no hay conectividad: un aviso de "sin conexión" que no ofrece
 * nada que hacer deja al usuario atascado. Aquí se le lleva justo al sitio donde puede
 * arreglarlo (datos móviles, wifi, modo avión).
 *
 * No necesita ningún permiso: se lanza una Activity del sistema. Además se prueban dos
 * acciones en orden, porque hay ROMs que no exponen `ACTION_WIRELESS_SETTINGS`; si la
 * primera no resuelve, se cae a los ajustes generales.
 */
fun Context.openNetworkSettings() {
    val actions = listOf(Settings.ACTION_WIRELESS_SETTINGS, Settings.ACTION_SETTINGS)
    actions.forEach { action ->
        val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { startActivity(intent) }.isSuccess) return
    }
}
