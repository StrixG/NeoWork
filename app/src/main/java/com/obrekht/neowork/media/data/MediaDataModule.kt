package com.obrekht.neowork.media.data

import com.obrekht.neowork.media.data.remote.MediaApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaDataModule {

    @Singleton
    @Provides
    fun provideMediaService(retrofit: Retrofit): MediaApiService = retrofit.create()
}
