package com.bridgeflowfolk.bff.di

import android.content.Context
import androidx.room.Room
import com.bridgeflowfolk.bff.data.EventRepositoryImpl
import com.bridgeflowfolk.bff.data.local.BffDatabase
import com.bridgeflowfolk.bff.data.local.EventDao
import com.bridgeflowfolk.bff.data.remote.BffApiService
import com.bridgeflowfolk.bff.domain.EventRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val BASE_URL = "https://bridgeflowfolk.github.io/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton
    fun provideBffApi(retrofit: Retrofit): BffApiService =
        retrofit.create(BffApiService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): BffDatabase =
        Room.databaseBuilder(ctx, BffDatabase::class.java, "bff_events.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideEventDao(db: BffDatabase): EventDao = db.eventDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository
}
