package com.liebeblack.divtrack.core.common.logging

/**
 * Logger mínimo. La interfaz vive en el núcleo puro y la implementación con
 * `android.util.Log` en `:core:network`, para que el dominio nunca vea Android.
 */
interface Logger {
    fun debug(tag: String, message: String)
    fun warn(tag: String, message: String, throwable: Throwable? = null)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}

/** Implementación para tests y para builds donde no queremos ruido. */
object NoOpLogger : Logger {
    override fun debug(tag: String, message: String) = Unit
    override fun warn(tag: String, message: String, throwable: Throwable?) = Unit
    override fun error(tag: String, message: String, throwable: Throwable?) = Unit
}
