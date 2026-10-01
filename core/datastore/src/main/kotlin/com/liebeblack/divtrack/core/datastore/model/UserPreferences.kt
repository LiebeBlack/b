package com.liebeblack.divtrack.core.datastore.model

import com.liebeblack.divtrack.core.common.utils.AppConstants
import com.liebeblack.divtrack.core.common.utils.SourceKeys

/**
 * Preferencias del usuario tal como se persisten. Se mantienen como tipos primitivos para
 * que este módulo no dependa del dominio: el mapeo a `ThemeMode`/`RateSource` vive en `:data`
 * (misma dirección de dependencias que el resto del proyecto).
 */
data class UserPreferences(
    /** `SYSTEM` | `LIGHT` | `DARK` (nombres del enum de dominio). */
    val themeMode: String = THEME_SYSTEM,

    /** Clave canónica de la fuente por defecto: `oficial` | `paralelo`. */
    val defaultSourceKey: String = SourceKeys.OFICIAL,

    /** IGTF activado por defecto en la calculadora. */
    val igtfEnabled: Boolean = false,

    /** Sincronización periódica en segundo plano (WorkManager). */
    val autoSyncEnabled: Boolean = true,

    val syncIntervalMinutes: Int = AppConstants.SYNC_DEFAULT_INTERVAL_MINUTES,

    /** Última sincronización de tasas vigentes. */
    val lastSyncAtMillis: Long? = null,

    /** Última importación del histórico diario. */
    val lastHistorySyncAtMillis: Long? = null,
) {
    companion object {
        const val THEME_SYSTEM: String = "SYSTEM"
        const val THEME_LIGHT: String = "LIGHT"
        const val THEME_DARK: String = "DARK"
    }
}
