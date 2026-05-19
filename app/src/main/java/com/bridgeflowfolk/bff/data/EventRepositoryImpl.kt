package com.bridgeflowfolk.bff.data

import com.bridgeflowfolk.bff.data.local.EventDao
import com.bridgeflowfolk.bff.data.local.EventEntity
import com.bridgeflowfolk.bff.data.remote.BffApiService
import com.bridgeflowfolk.bff.data.remote.EventDto
import com.bridgeflowfolk.bff.domain.Event
import com.bridgeflowfolk.bff.domain.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val dao: EventDao,
    private val api: BffApiService
) : EventRepository {

    override fun observeEvents(): Flow<List<Event>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun searchEvents(query: String): Flow<List<Event>> =
        dao.search(query).map { list -> list.map { it.toDomain() } }

    override suspend fun syncFromNetwork(): List<String> {
        val remote = api.getEvents()
        val existingIds = dao.allIds().toSet()

        // Préserver reminderScheduled pour les événements déjà connus
        val entities = remote.map { dto ->
            val existing = dao.findById(dto.id)
            dto.toEntity().copy(
                notified          = existing?.notified          ?: false,
                reminderScheduled = existing?.reminderScheduled ?: false
            )
        }
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
