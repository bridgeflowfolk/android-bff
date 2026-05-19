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
    val eventUrl: String? = null,                  // URL dédiée (nullable)
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNew(events: List<EventEntity>): List<Long>

    @Update
    suspend fun update(event: EventEntity)

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): EventEntity?

    @Query("SELECT * FROM events WHERE date > :now AND reminderScheduled = 0 ORDER BY date ASC")
    suspend fun upcomingWithoutReminder(now: String): List<EventEntity>

    @Query("UPDATE events SET reminderScheduled = 1 WHERE id = :id")
    suspend fun markReminderScheduled(id: String)
}

// Version 2 : ajout du champ eventUrl
@Database(entities = [EventEntity::class], version = 2, exportSchema = false)
abstract class BffDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}
