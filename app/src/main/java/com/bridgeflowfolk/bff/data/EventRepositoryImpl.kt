package com.bridgeflowfolk.bff.data

import android.content.Context
import androidx.work.WorkManager
import com.bridgeflowfolk.bff.data.local.EventDao
import com.bridgeflowfolk.bff.data.local.EventEntity
import com.bridgeflowfolk.bff.data.remote.BffApiService
import com.bridgeflowfolk.bff.data.remote.EventDto
import com.bridgeflowfolk.bff.domain.Event
import com.bridgeflowfolk.bff.domain.EventRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val dao: EventDao,
    private val api: BffApiService,
    @ApplicationContext private val context: Context
) : EventRepository {

    override fun observeEvents(): Flow<List<Event>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun searchEvents(query: String): Flow<List<Event>> =
        dao.search(query).map { list -> list.map { it.toDomain() } }

    override suspend fun syncFromNetwork(): List<String> {
        val remote      = api.getEvents()
        val existingIds = dao.allIds().toSet()

        val entities = remote.map { dto ->
            val existing    = dao.findById(dto.id)
            // Réinitialise reminderScheduled si la date a changé ou si c'est un nouvel événement,
            // afin que SyncWorker replanifie un rappel sur la nouvelle date.
            val dateChanged = existing != null && existing.date != dto.date
            if (dateChanged) {
                // Annule le reminder WorkManager planifié sur l'ancienne date
                WorkManager.getInstance(context).cancelUniqueWork("reminder_${dto.id}")
            }
            dto.toEntity().copy(
                notified          = existing?.notified ?: false,
                reminderScheduled = if (existing == null || dateChanged) false
                                    else existing.reminderScheduled
            )
        }
        // @Upsert en Room 2.7 : ne retourne plus List<Long>, appel sans valeur de retour
        dao.upsertAll(entities)

        return remote.map { it.id }.filter { it !in existingIds }
    }
}

private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

fun EventDto.toEntity() = EventEntity(
    id          = id,
    title       = title,
    date        = date,
    location    = location,
    description = description,
    imageUrl    = image,
    eventUrl    = url
)

fun EventEntity.toDomain() = Event(
    id          = id,
    title       = title,
    dateTime    = LocalDateTime.parse(date, formatter),
    location    = location,
    description = description,
    imageUrl    = imageUrl,
    eventUrl    = eventUrl
)
