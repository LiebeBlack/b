package com.liebeblack.divtrack.domain.model

/** Modo de tema elegido por el usuario (dinámico: sigue al sistema por defecto). */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
    }
}
