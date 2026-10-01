package com.liebeblack.divtrack.domain.model

import com.liebeblack.divtrack.core.common.utils.SourceKeys

/**
 * Las dos tasas que la app rastrea. La [key] es el identificador canónico que viaja por
 * Room, los proveedores remotos y las preferencias: cambiar el texto que ve el usuario
 * nunca obliga a migrar datos.
 */
enum class RateSource(
    val key: String,
    val displayOrder: Int,
) {
    OFICIAL(SourceKeys.OFICIAL, 0),
    PARALELO(SourceKeys.PARALELO, 1);

    companion object {
        fun fromKey(key: String?): RateSource? {
            val normalized = key?.trim()?.lowercase() ?: return null
            return entries.firstOrNull { it.key == normalized }
        }

        fun ordered(): List<RateSource> = entries.sortedBy { it.displayOrder }
    }
}
