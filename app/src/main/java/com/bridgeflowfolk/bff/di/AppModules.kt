package com.bridgeflowfolk.bff.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bridgeflowfolk.bff.data.EventRepositoryImpl
import com.bridgeflowfolk.bff.data.UserPreferencesRepositoryImpl
import com.bridgeflowfolk.bff.data.local.BffDatabase
import com.bridgeflowfolk.bff.data.local.EventDao
import com.bridgeflowfolk.bff.data.remote.BffApiService
import com.bridgeflowfolk.bff.domain.EventRepository
import com.bridgeflowfolk.bff.domain.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

// Migration 2→3 : le schéma est identique, Room a juste besoin d'une migration déclarée
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) { /* aucun changement DDL */ }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): BffDatabase =
        Room.databaseBuilder(ctx, BffDatabase::class.java, "bff.db")
            .addMigrations(MIGRATION_2_3)
            .fallbackToDestructiveMigrationFrom(1)   // v1 → v2 était destructive (acceptable)
            .build()

    @Provides fun provideEventDao(db: BffDatabase): EventDao = db.eventDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("https://bridgeflowfolk.github.io/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton
    fun provideApiService(retrofit: Retrofit): BffApiService =
        retrofit.create(BffApiService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository

    @Binds @Singleton
    abstract fun bindUserPrefsRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository
}
