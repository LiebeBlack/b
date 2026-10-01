package com.liebeblack.divtrack.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.Flow

/**
 * Recolecta eventos de un solo uso (`SharedFlow`) mientras la pantalla está compuesta.
 *
 * El manejador es `suspend` a propósito: casi todos los efectos acaban en algo que suspende
 * (`SnackbarHostState.showSnackbar`), y resolverlo aquí evita que cada pantalla tenga que
 * abrir su propio corrutina para mostrar un aviso.
 *
 * `rememberUpdatedState` evita el clásico bug de capturar un lambda obsoleto, y al
 * terminar la composición la recolección se cancela sola: no hay trabajo en background
 * cuando el usuario cambia de pestaña.
 */
@Composable
fun <T> CollectEffects(
    effects: Flow<T>,
    onEffect: suspend (T) -> Unit,
) {
    val currentOnEffect by rememberUpdatedState(onEffect)

    LaunchedEffect(effects) {
        effects.collect { effect -> currentOnEffect(effect) }
    }
}
