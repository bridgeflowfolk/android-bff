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

        // Insère uniquement les nouveaux (IGNORE = pas d'écrasement des existants)
        dao.insertNew(remote.map { it.toEntity() })

        return remote
            .map { it.id }
            .filter { it !in existingIds }
    }
}

// ─── Mappers ─────────────────────────────────────────────────────────────────

private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

fun EventDto.toEntity() = EventEntity(
    id = id,
    title = title,
    date = date,
    location = location,
    description = description,
    imageUrl = image
)

fun EventEntity.toDomain() = Event(
    id = id,
    title = title,
    dateTime = LocalDateTime.parse(date, formatter),
    location = location,
    description = description,
    imageUrl = imageUrl
)
