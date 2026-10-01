package com.liebeblack.divtrack.presentation.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/** Copia texto al portapapeles del sistema (API de Android, sin deprecaciones de Compose). */
fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
