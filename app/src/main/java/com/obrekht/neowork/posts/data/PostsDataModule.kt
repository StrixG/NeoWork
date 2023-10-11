package com.obrekht.neowork.posts.data

import android.content.Context
import androidx.room.Room
import com.obrekht.neowork.posts.data.local.PostDatabase
import com.obrekht.neowork.posts.data.local.dao.PostDao
import com.obrekht.neowork.posts.data.remote.PostApiService
import com.obrekht.neowork.posts.data.repository.CachedPostRepository
import com.obrekht.neowork.posts.data.repository.PostRepository
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
abstract class PostRepositoryModule {

    @Singleton
    @Binds
    abstract fun bindPostRepository(repository: CachedPostRepository): PostRepository
}

@Module
@InstallIn(SingletonComponent::class)
object PostDatabaseModule {

    @Singleton
    @Provides
    fun providePostDatabase(@ApplicationContext context: Context): PostDatabase =
        Room.databaseBuilder(
            context,
            PostDatabase::class.java,
            "posts.db"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePostDao(postDatabase: PostDatabase): PostDao = postDatabase.postDao()
}

@Module
@InstallIn(SingletonComponent::class)
object PostServiceModule {

    @Singleton
    @Provides
    fun providePostsService(retrofit: Retrofit): PostApiService = retrofit.create()
}