package com.bridgeflowfolk.bff.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

data class Event(
    val id: String,
    val title: String,
    val dateTime: LocalDateTime,
    val location: String,
    val description: String,
    val imageUrl: String?,
    val eventUrl: String? = null   // URL dédiée à l'événement (optionnel)
)

interface EventRepository {
    fun observeEvents(): Flow<List<Event>>
    fun searchEvents(query: String): Flow<List<Event>>
    suspend fun syncFromNetwork(): List<String>
}
