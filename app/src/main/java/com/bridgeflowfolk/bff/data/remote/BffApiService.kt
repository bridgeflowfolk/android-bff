package com.bridgeflowfolk.bff.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

data class EventDto(
    val id: String,
    val title: String,
    val date: String,
    val location: String,
    val description: String,
    val image: String? = null,
    val url: String? = null       // URL dédiée à l'événement (champ optionnel dans le JSON)
)

interface BffApiService {
    @GET("info.json")
    suspend fun getEvents(        
        @Query("t") timestamp: Long = System.currentTimeMillis() 
    ): List<EventDto>
}
