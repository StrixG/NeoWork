package com.obrekht.neowork.events.data

import android.content.Context
import androidx.room.Room
import com.obrekht.neowork.events.data.local.EventDatabase
import com.obrekht.neowork.events.data.local.dao.EventDao
import com.obrekht.neowork.events.data.remote.EventApiService
import com.obrekht.neowork.events.data.repository.CachedEventRepository
import com.obrekht.neowork.events.data.repository.EventRepository
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
abstract class EventRepositoryModule {

    @Singleton
    @Binds
    abstract fun bindEventRepository(repository: CachedEventRepository): EventRepository
}

@Module
@InstallIn(SingletonComponent::class)
object EventDatabaseModule {

    @Singleton
    @Provides
    fun provideEventDatabase(@ApplicationContext context: Context): EventDatabase =
        Room.databaseBuilder(
            context,
            EventDatabase::class.java,
            "events.db"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideEventDao(database: EventDatabase): EventDao = database.eventDao()
}

@Module
@InstallIn(SingletonComponent::class)
object EventServiceModule {

    @Singleton
    @Provides
    fun provideEventService(retrofit: Retrofit): EventApiService = retrofit.create()
}
