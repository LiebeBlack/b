package com.liebeblack.divtrack.data.fake

import com.liebeblack.divtrack.core.network.monitor.ConnectivityObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Conectividad simulada y conmutable: permite probar el fallo rápido sin red real. */
class FakeConnectivityObserver(
    var isOnline: Boolean = true,
) : ConnectivityObserver {

    private val state = MutableStateFlow(isOnline)

    override fun isOnline(): Boolean = isOnline

    override fun observe(): Flow<Boolean> = state
}
