package com.liebeblack.divtrack.domain.repository

import com.liebeblack.divtrack.core.common.result.Result
import com.liebeblack.divtrack.domain.model.ExchangeRate
import com.liebeblack.divtrack.domain.model.SyncSummary
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de tasas.
 *
 * Patrón Online-First con Room como única fuente de verdad para la UI:
 * - Las lecturas son `Flow` de la base local: la UI se actualiza sola cuando la red
 *   escribe y sigue funcionando sin conexión.
 * - [refreshRates] intenta la red; si responde, escribe en Room (lo que re-emite a la UI);
 *   si falla, NO toca los datos y devuelve [Result.Error] para que la UI avise.
 *
 * El contrato expone **una sola** operación de escritura. Antes había también una lectura y
 * una importación de series históricas que solo consumía la pantalla de Histórico: al
 * eliminarla, mantenerlas habría dejado dos métodos públicos sin ningún llamador.
 */
interface RateRepository {

    fun observeRates(): Flow<List<ExchangeRate>>

    suspend fun refreshRates(): Result<SyncSummary>
}
