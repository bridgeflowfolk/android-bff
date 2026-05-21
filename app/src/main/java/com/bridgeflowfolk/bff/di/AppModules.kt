package com.bridgeflowfolk.bff.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bridgeflowfolk.bff.data.EventRepositoryImpl
import com.bridgeflowfolk.bff.data.InAppNotificationRepositoryImpl
import com.bridgeflowfolk.bff.data.UserPreferencesRepositoryImpl
import com.bridgeflowfolk.bff.data.local.BffDatabase
import com.bridgeflowfolk.bff.data.local.EventDao
import com.bridgeflowfolk.bff.data.local.InAppNotificationDao
import com.bridgeflowfolk.bff.data.remote.BffApiService
import com.bridgeflowfolk.bff.domain.EventRepository
import com.bridgeflowfolk.bff.domain.InAppNotificationRepository
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

// ─── Migrations Room ──────────────────────────────────────────────────────────

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) { /* schéma inchangé */ }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Création de la table des notifications in-app
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `in_app_notifications` (
                `id`         TEXT    NOT NULL,
                `title`      TEXT    NOT NULL,
                `detail`     TEXT    NOT NULL,
                `url`        TEXT,
                `receivedAt` INTEGER NOT NULL,
                `isRead`     INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): BffDatabase =
        Room.databaseBuilder(ctx, BffDatabase::class.java, "bff.db")
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)
            .build()

    @Provides fun provideEventDao(db: BffDatabase): EventDao = db.eventDao()
    @Provides fun provideInAppNotificationDao(db: BffDatabase): InAppNotificationDao = db.inAppNotificationDao()
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

    @Binds @Singleton
    abstract fun bindInAppNotificationRepository(impl: InAppNotificationRepositoryImpl): InAppNotificationRepository
}
