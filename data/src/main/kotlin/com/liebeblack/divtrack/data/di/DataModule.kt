package com.liebeblack.divtrack.data.di

import com.liebeblack.divtrack.core.common.time.SystemTimeProvider
import com.liebeblack.divtrack.core.common.time.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    /**
     * Reloj de la app en hora de Venezuela. Los tests lo sustituyen por un reloj fijo:
     * el "cierre anterior" y el corte del día dejan de depender de cuándo se ejecuten.
     */
    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = SystemTimeProvider()
}
