package com.bridgeflowfolk.bff.data.remote

import retrofit2.http.GET

// ─── DTO (ce qui vient du JSON GitHub) ───────────────────────────────────────

data class EventDto(
    val id: String,
    val title: String,
    val date: String,           // ISO 8601 : "2025-06-15T14:00:00"
    val location: String,
    val description: String,
    val image: String? = null   // URL de l'image (optionnel)
)

// ─── Retrofit service ────────────────────────────────────────────────────────

interface BffApiService {
    /**
     * Récupère la liste des événements depuis le JSON hébergé sur GitHub Pages.
     * Pas d'authentification, lecture seule.
     */
    @GET("info.json")
    suspend fun getEvents(): List<EventDto>
}
