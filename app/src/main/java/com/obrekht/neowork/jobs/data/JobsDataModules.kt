package com.obrekht.neowork.jobs.data

import android.content.Context
import androidx.room.Room
import com.obrekht.neowork.jobs.data.local.JobDatabase
import com.obrekht.neowork.jobs.data.local.dao.JobDao
import com.obrekht.neowork.jobs.data.remote.JobApiService
import com.obrekht.neowork.jobs.data.repository.CachedJobRepository
import com.obrekht.neowork.jobs.data.repository.JobRepository
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
abstract class JobRepositoryModule {

    @Singleton
    @Binds
    abstract fun bindJobRepository(repository: CachedJobRepository): JobRepository
}

@Module
@InstallIn(SingletonComponent::class)
object JobDatabaseModule {

    @Singleton
    @Provides
    fun provideJobDatabase(@ApplicationContext context: Context): JobDatabase =
        Room.databaseBuilder(
            context,
            JobDatabase::class.java,
            "jobs.db"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideJobDao(jobDatabase: JobDatabase): JobDao = jobDatabase.jobDao()
}

@Module
@InstallIn(SingletonComponent::class)
object JobServiceModule {

    @Singleton
    @Provides
    fun provideJobService(retrofit: Retrofit): JobApiService = retrofit.create()
}