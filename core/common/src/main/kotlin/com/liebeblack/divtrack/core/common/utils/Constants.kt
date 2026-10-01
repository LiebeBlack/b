package com.liebeblack.divtrack.core.common.utils

/** Constantes compartidas por todas las capas. Sin contexto de Android. */
object AppConstants {

    /** Impuesto a las Grandes Transacciones Financieras (alícuota vigente). */
    const val IGTF_RATE: Double = 0.03

    /** Etiqueta del IGTF lista para UI/copiar al portapapeles. */
    const val IGTF_LABEL: String = "3 %"

    /** WorkManager no permite periodicidad menor a 15 minutos. */
    const val SYNC_MIN_INTERVAL_MINUTES: Int = 15
    const val SYNC_DEFAULT_INTERVAL_MINUTES: Int = 30

    /** El histórico oficial/paralelo se refresca como máximo una vez al día. */
    const val HISTORY_TTL_HOURS: Long = 24

    /** Retención del histórico en Room (años). */
    const val HISTORY_KEEP_YEARS: Long = 2

    const val DATABASE_NAME: String = "divtrack.db"

    /** Caché HTTP en disco: 10 MB alcanzan de sobra para dos endpoints pequeños. */
    const val HTTP_CACHE_BYTES: Long = 10L * 1024L * 1024L

    /** Hora legal de Venezuela: todas las "fechas de cierre" se calculan aquí. */
    const val TIME_ZONE_ID: String = "America/Caracas"

    /** Locale de formateo (coma decimal, punto de miles). */
    const val LOCALE_TAG: String = "es-VE"

    /** Reintentos máximos del interceptor de red. */
    const val HTTP_MAX_RETRIES: Int = 2

    const val HTTP_CONNECT_TIMEOUT_SECONDS: Long = 10
    const val HTTP_READ_TIMEOUT_SECONDS: Long = 15
    const val HTTP_WRITE_TIMEOUT_SECONDS: Long = 10
}

/**
 * Claves canónicas de cada tasa. Se usan como identificador en Room, en los proveedores
 * remotos y en los mapeos, de modo que un proveedor nuevo no obliga a tocar el esquema.
 */
object SourceKeys {
    const val OFICIAL: String = "oficial"
    const val PARALELO: String = "paralelo"

    val all: List<String> = listOf(OFICIAL, PARALELO)
}
