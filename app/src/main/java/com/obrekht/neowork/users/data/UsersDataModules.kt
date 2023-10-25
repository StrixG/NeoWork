package com.obrekht.neowork.users.data

import android.content.Context
import androidx.room.Room
import com.obrekht.neowork.users.data.local.UserDatabase
import com.obrekht.neowork.users.data.local.dao.UserDao
import com.obrekht.neowork.users.data.remote.UserApiService
import com.obrekht.neowork.users.data.repository.CachedUserRepository
import com.obrekht.neowork.users.data.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UserRepositoryModule {

    @Singleton
    @Binds
    abstract fun bindUserRepository(repository: CachedUserRepository): UserRepository
}

@Module
@InstallIn(SingletonComponent::class)
object UserDatabaseModule {

    @Singleton
    @Provides
    fun provideUserDatabase(@ApplicationContext context: Context): UserDatabase =
        Room.databaseBuilder(
            context,
            UserDatabase::class.java,
            "users.db"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideUserDao(userDatabase: UserDatabase): UserDao = userDatabase.userDao()
}

@Module
@InstallIn(SingletonComponent::class)
object UserServiceModule {

    @Singleton
    @Provides
    fun provideUserService(retrofit: Retrofit): UserApiService = retrofit.create()
}