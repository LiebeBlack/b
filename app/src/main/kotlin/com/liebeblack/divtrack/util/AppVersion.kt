package com.liebeblack.divtrack.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * `versionName` realmente instalado, leído del PackageManager.
 *
 * Se evita depender de `BuildConfig.VERSION_NAME` por un motivo práctico: la versión que
 * muestra la pantalla de ajustes coincide siempre con el APK que el usuario tiene delante,
 * incluso en variantes con sufijo de aplicación (`.debug`).
 *
 * Nunca lanza: si el paquete no se puede consultar, devuelve cadena vacía y la UI pinta "—".
 */
fun Context.appVersionName(): String = try {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }
    info.versionName.orEmpty()
} catch (error: PackageManager.NameNotFoundException) {
    ""
}
