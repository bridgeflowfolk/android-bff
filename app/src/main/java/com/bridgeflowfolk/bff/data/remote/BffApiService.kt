package com.bridgeflowfolk.bff.data.remote

import retrofit2.http.GET

data class EventDto(
    val id: String,
    val title: String,
    val date: String,
    val location: String,
    val description: String,
    val image: String? = null,
    val url: String? = null       // URL dédiée à l'événement (champ optionnel dans le JSON)
)

/**
 * DTO pour les notifications informatives publiées sur GitHub Pages.
 * Champs obligatoires : id, title, detail.
 * Champ optionnel : url (lien associé à la notification).
 */
data class NotificationDto(
    val id: String,
    val title: String,
    val detail: String,
    val url: String? = null
)

interface BffApiService {
    @GET("info.json")
    suspend fun getEvents(): List<EventDto>

    @GET("notification.json")
    suspend fun getNotifications(): List<NotificationDto>
}
