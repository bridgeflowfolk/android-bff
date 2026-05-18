package com.bridgeflowfolk.bff.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─── Entité Room ─────────────────────────────────────────────────────────────

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val date: String,           // stocké en ISO 8601
    val location: String,
    val description: String,
    val imageUrl: String?,
    val notified: Boolean = false,      // rappel 2h planifié ?
    val reminderScheduled: Boolean = false
)

// ─── DAO ─────────────────────────────────────────────────────────────────────

@Dao
interface EventDao {

    /** Flux réactif pour l'UI — tri chronologique */
    @Query("SELECT * FROM events ORDER BY date ASC")
    fun observeAll(): Flow<List<EventEntity>>

    /** Recherche sur titre ET lieu */
    @Query("""
        SELECT * FROM events
        WHERE title LIKE '%' || :q || '%'
           OR location LIKE '%' || :q || '%'
        ORDER BY date ASC
    """)
    fun search(q: String): Flow<List<EventEntity>>

    /** Récupère les IDs déjà connus (pour détecter les nouveaux) */
    @Query("SELECT id FROM events")
    suspend fun allIds(): List<String>

    /** Upsert : insère ou remplace silencieusement */
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

// ─── Database ────────────────────────────────────────────────────────────────

@Database(entities = [EventEntity::class], version = 1, exportSchema = false)
abstract class BffDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}
