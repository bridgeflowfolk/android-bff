package com.bridgeflowfolk.bff.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

// ─── Modèle domaine (découplé de Room et Retrofit) ───────────────────────────

data class Event(
    val id: String,
    val title: String,
    val dateTime: LocalDateTime,
    val location: String,
    val description: String,
    val imageUrl: String?
)

// ─── Contrat du repository ───────────────────────────────────────────────────

interface EventRepository {
    /** Flux live depuis Room — disponible hors ligne */
    fun observeEvents(): Flow<List<Event>>

    /** Recherche filtrée */
    fun searchEvents(query: String): Flow<List<Event>>

    /**
     * Synchronise le JSON distant → Room.
     * Retourne les IDs des nouveaux événements détectés.
     */
    suspend fun syncFromNetwork(): List<String>
}
