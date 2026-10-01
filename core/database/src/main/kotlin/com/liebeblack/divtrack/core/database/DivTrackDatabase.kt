package com.liebeblack.divtrack.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.liebeblack.divtrack.core.common.utils.AppConstants
import com.liebeblack.divtrack.core.database.dao.RateDao
import com.liebeblack.divtrack.core.database.entity.CurrentRateEntity
import com.liebeblack.divtrack.core.database.entity.RateHistoryEntity

/**
 * Base de datos local. `exportSchema = true` + el plugin de Room publican el JSON del
 * esquema en `core/database/schemas`, de modo que la primera migración real tendrá
 * referencia exacta de la versión 1.
 *
 * Sin `fallbackToDestructiveMigration`: en una app financiera perder datos en silencio no
 * es una opción.
 */
@Database(
    entities = [
        CurrentRateEntity::class,
        RateHistoryEntity::class,
    ],
    version = DivTrackDatabase.VERSION,
    exportSchema = true,
)
abstract class DivTrackDatabase : RoomDatabase() {

    abstract fun rateDao(): RateDao

    companion object {
        const val VERSION: Int = 1
        const val NAME: String = AppConstants.DATABASE_NAME
    }
}
