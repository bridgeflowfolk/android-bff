package com.bridgeflowfolk.bff.domain

/**
 * Notification informative affichée dans le panneau "cloche" de l'application.
 * Distincte des notifications système (NotificationHelper).
 */
data class InAppNotification(
    val id: String,
    val title: String,
    val detail: String,
    val url: String? = null,
    val isRead: Boolean = false
)

interface InAppNotificationRepository {
    /** Flux réactif de toutes les notifications triées par ordre de réception (plus récent en tête). */
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<InAppNotification>>

    /** Nombre de notifications non lues. */
    fun observeUnreadCount(): kotlinx.coroutines.flow.Flow<Int>

    /** Synchronise depuis le réseau ; retourne les ids des nouvelles entrées. */
    suspend fun syncFromNetwork(): List<String>

    /** Marque une notification comme lue. */
    suspend fun markRead(id: String)

    /** Marque toutes les notifications comme lues. */
    suspend fun markAllRead()
}
