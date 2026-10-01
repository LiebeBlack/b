package com.liebeblack.divtrack.core.network.di

import javax.inject.Qualifier

/** Retrofit apuntando a DolarAPI Venezuela (oficial + paralelo + histórico). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DolarApiRetrofit

/** Retrofit apuntando a Yadio (fallback del paralelo). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class YadioRetrofit
