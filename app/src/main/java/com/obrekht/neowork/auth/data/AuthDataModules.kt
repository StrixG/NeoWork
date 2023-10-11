package com.obrekht.neowork.auth.data

import com.obrekht.neowork.auth.data.remote.AuthApiService
import com.obrekht.neowork.auth.data.repository.AuthRepository
import com.obrekht.neowork.auth.data.repository.DefaultAuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthRepositoryModule {

    @Singleton
    @Binds
    abstract fun bindAuthRepository(repository: DefaultAuthRepository): AuthRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AuthServiceModule {
    @Singleton
    @Provides
    fun provideAuthService(retrofit: Retrofit): AuthApiService = retrofit.create()
}
