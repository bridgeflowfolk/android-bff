package com.bridgeflowfolk.bff.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val date: String,
    val location: String,
    val description: String,
    val imageUrl: String?,
    val eventUrl: String? = null,
    val notified: Boolean = false,
    val reminderScheduled: Boolean = false
)

@Dao
interface EventDao {

    @Query("SELECT * FROM events ORDER BY date ASC")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("""
        SELECT * FROM events
        WHERE title    LIKE '%' || :q || '%'
           OR location LIKE '%' || :q || '%'
        ORDER BY date ASC
    """)
    fun search(q: String): Flow<List<EventEntity>>

    @Query("SELECT id FROM events")
    suspend fun allIds(): List<String>

    @Upsert
    suspend fun upsertAll(events: List<EventEntity>)

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): EventEntity?

    @Query("SELECT * FROM events WHERE date > :now ORDER BY date ASC")
    suspend fun upcomingAll(now: String): List<EventEntity>

    @Query("SELECT * FROM events WHERE date > :now AND reminderScheduled = 0 ORDER BY date ASC")
    suspend fun upcomingWithoutReminder(now: String): List<EventEntity>

    @Query("UPDATE events SET reminderScheduled = 1 WHERE id = :id")
    suspend fun markReminderScheduled(id: String)

    @Query("UPDATE events SET reminderScheduled = :scheduled WHERE id = :id")
    suspend fun setReminderScheduled(id: String, scheduled: Boolean)

    /** Remet reminderScheduled = false sur tous les événements à venir.
     *  Appelé après cancelAllWorkByTag(TAG_REMINDER) pour forcer la replanification. */
    @Query("UPDATE events SET reminderScheduled = 0 WHERE date > :now")
    suspend fun resetAllReminderScheduled(now: String)
}

// ─── Entité notifications in-app ──────────────────────────────────────────────

@Entity(tableName = "in_app_notifications")
data class InAppNotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val detail: String,
    val url: String? = null,
    // Timestamp de premier fetch (epoch ms) — utilisé pour trier par ordre de réception
    val receivedAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Dao
interface InAppNotificationDao {

    @Query("SELECT * FROM in_app_notifications ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<InAppNotificationEntity>>

    @Query("SELECT COUNT(*) FROM in_app_notifications WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("SELECT id FROM in_app_notifications")
    suspend fun allIds(): List<String>

    // Upsert : ne met PAS à jour receivedAt ni isRead si l'entrée existe déjà
    // → géré dans InAppNotificationRepositoryImpl
    @Upsert
    suspend fun upsertAll(notifications: List<InAppNotificationEntity>)

    @Query("UPDATE in_app_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE in_app_notifications SET isRead = 1")
    suspend fun markAllRead()
}

// Version 4 : ajout table in_app_notifications (migration DDL explicite)
@Database(
    entities = [EventEntity::class, InAppNotificationEntity::class],
    version = 4,
    exportSchema = false
)
abstract class BffDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun inAppNotificationDao(): InAppNotificationDao
}
