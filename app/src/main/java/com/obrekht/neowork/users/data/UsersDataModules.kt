package com.obrekht.neowork.users.data

import android.content.Context
import androidx.room.Room
import com.obrekht.neowork.posts.data.local.PostDatabase
import com.obrekht.neowork.posts.data.local.dao.CommentDao
import com.obrekht.neowork.posts.data.local.dao.PostDao
import com.obrekht.neowork.posts.data.remote.CommentApiService
import com.obrekht.neowork.posts.data.remote.PostApiService
import com.obrekht.neowork.posts.data.repository.CachedCommentRepository
import com.obrekht.neowork.posts.data.repository.CachedPostRepository
import com.obrekht.neowork.posts.data.repository.CommentRepository
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

    @Singleton
    @Binds
    abstract fun bindCommentRepository(repository: CachedCommentRepository): CommentRepository
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

    @Provides
    fun provideCommentDao(postDatabase: PostDatabase): CommentDao = postDatabase.commentDao()
}

@Module
@InstallIn(SingletonComponent::class)
object PostServiceModule {

    @Singleton
    @Provides
    fun providePostService(retrofit: Retrofit): PostApiService = retrofit.create()

    @Singleton
    @Provides
    fun provideCommentService(retrofit: Retrofit): CommentApiService = retrofit.create()
}