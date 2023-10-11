package com.obrekht.neowork.core.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.obrekht.neowork.BuildConfig
import com.obrekht.neowork.auth.data.local.AppAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun provideOkHttpClient(appAuth: AppAuth): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            if (BuildConfig.DEBUG) {
                level = HttpLoggingInterceptor.Level.BODY
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .addHeader("Api-Key", BuildConfig.API_KEY)
                    .apply {
                        appAuth.state.value.token?.let { token ->
                            addHeader("Authorization", token)
                        }
                    }

                return@addInterceptor chain.proceed(newRequest.build())
            }
            .build()
    }

    @Singleton
    @Provides
    fun provideRetrofitClient(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .addConverterFactory(Json.asConverterFactory(contentType))
            .baseUrl("${BuildConfig.BASE_URL}/api/")
            .client(okHttpClient)
            .build()
    }
}
