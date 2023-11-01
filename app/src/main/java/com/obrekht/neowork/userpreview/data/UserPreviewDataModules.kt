package com.obrekht.neowork.userpreview.data

import android.content.Context
import androidx.room.Room
import com.obrekht.neowork.userpreview.data.local.UserPreviewDatabase
import com.obrekht.neowork.userpreview.data.local.dao.UserPreviewDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UserPreviewDatabaseModule {

    @Singleton
    @Provides
    fun providePostDatabase(@ApplicationContext context: Context): UserPreviewDatabase =
        Room.databaseBuilder(
            context,
            UserPreviewDatabase::class.java,
            "user_preview.db"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideUserPreviewDao(database: UserPreviewDatabase): UserPreviewDao =
        database.userPreviewDao()
}
