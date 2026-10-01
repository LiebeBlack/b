package com.liebeblack.divtrack.domain.repository

import com.liebeblack.divtrack.core.common.result.Result
import com.liebeblack.divtrack.domain.model.ExchangeRate
import com.liebeblack.divtrack.domain.model.HistoryRange
import com.liebeblack.divtrack.domain.model.RateHistoryPoint
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
 */
interface RateRepository {

    fun observeRates(): Flow<List<ExchangeRate>>

    fun observeHistory(range: HistoryRange): Flow<List<RateHistoryPoint>>

    suspend fun refreshRates(): Result<SyncSummary>

    /**
     * Importa el histórico diario desde el proveedor que lo soporte.
     *
     * @param force ignora el TTL de 24 h (acción manual del usuario).
     * @return número de puntos importados ([Result.Success] puede ser 0 si ya estaba fresco).
     */
    suspend fun syncHistory(force: Boolean = false): Result<Int>
}
