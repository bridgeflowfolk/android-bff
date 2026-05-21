package com.bridgeflowfolk.bff.data

import com.bridgeflowfolk.bff.data.local.InAppNotificationDao
import com.bridgeflowfolk.bff.data.local.InAppNotificationEntity
import com.bridgeflowfolk.bff.data.remote.BffApiService
import com.bridgeflowfolk.bff.domain.InAppNotification
import com.bridgeflowfolk.bff.domain.InAppNotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InAppNotificationRepositoryImpl @Inject constructor(
    private val dao: InAppNotificationDao,
    private val api: BffApiService
) : InAppNotificationRepository {

    override fun observeAll(): Flow<List<InAppNotification>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeUnreadCount(): Flow<Int> = dao.observeUnreadCount()

    /**
     * Synchronise les notifications depuis le réseau.
     * - Les notifications existantes conservent leur état isRead et receivedAt.
     * - Une notification supprimée du JSON reste visible (historique local).
     * - Si le réseau est absent ou en erreur → retour silencieux, pas de crash.
     * - Convention : pour modifier le contenu d'une notif, l'admin doit changer son id.
     */
    override suspend fun syncFromNetwork(): List<String> {
        val remote = try {
            api.getNotifications()
        } catch (e: Exception) {
            android.util.Log.d("NotifRepo", "Fetch notifications ignoré : ${e.message}")
            return emptyList()
        }

        val existingIds = dao.allIds().toSet()
        val now         = System.currentTimeMillis()

        // Seules les nouvelles notifications sont insérées (on ne touche pas aux existantes)
        val newEntities = remote
            .filter { it.id !in existingIds }
            .map { dto ->
                InAppNotificationEntity(
                    id         = dto.id,
                    title      = dto.title,
                    detail     = dto.detail,
                    url        = dto.url,
                    receivedAt = now,
                    isRead     = false
                )
            }

        if (newEntities.isNotEmpty()) dao.upsertAll(newEntities)

        return newEntities.map { it.id }
    }

    override suspend fun markRead(id: String)  = dao.markRead(id)
    override suspend fun markAllRead()          = dao.markAllRead()
}

private fun InAppNotificationEntity.toDomain() = InAppNotification(
    id     = id,
    title  = title,
    detail = detail,
    url    = url,
    isRead = isRead
)
