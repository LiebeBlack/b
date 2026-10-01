package com.liebeblack.divtrack.core.database.di

import android.content.Context
import androidx.room.Room
import com.liebeblack.divtrack.core.database.DivTrackDatabase
import com.liebeblack.divtrack.core.database.datasource.RateLocalDataSource
import com.liebeblack.divtrack.core.database.datasource.RoomRateLocalDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DivTrackDatabase =
        Room.databaseBuilder(
            context,
            DivTrackDatabase::class.java,
            DivTrackDatabase.NAME,
        ).build()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseBindingsModule {

    @Binds
    @Singleton
    abstract fun bindRateLocalDataSource(
        impl: RoomRateLocalDataSource,
    ): RateLocalDataSource
}
