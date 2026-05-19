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

    // REPLACE garantit que les mises à jour de contenu (lieu, description…) sont appliquées,
    // mais remet reminderScheduled à 0 → les rappels seront recalculés au prochain sync.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<EventEntity>): List<Long>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): EventEntity?

    @Query("SELECT * FROM events WHERE date > :now AND reminderScheduled = 0 ORDER BY date ASC")
    suspend fun upcomingWithoutReminder(now: String): List<EventEntity>

    @Query("UPDATE events SET reminderScheduled = 1 WHERE id = :id")
    suspend fun markReminderScheduled(id: String)

    // Préserve le flag reminderScheduled lors d'un upsert
    @Query("UPDATE events SET reminderScheduled = :scheduled WHERE id = :id")
    suspend fun setReminderScheduled(id: String, scheduled: Boolean)
}

// Version 3 : migration vers REPLACE strategy (schéma inchangé)
@Database(entities = [EventEntity::class], version = 3, exportSchema = false)
abstract class BffDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}
