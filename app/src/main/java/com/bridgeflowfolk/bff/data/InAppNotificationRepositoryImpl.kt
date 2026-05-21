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
     * - Les nouvelles entrées sont insérées (isRead=false, receivedAt=now).
     * - Les entrées existantes conservent leur état isRead et receivedAt.
     * - Les entrées absentes du JSON distant sont supprimées (suppression intentionnelle par l'admin).
     * - Si le réseau est absent ou en erreur → retour silencieux, aucune suppression locale.
     */
    override suspend fun syncFromNetwork(): List<String> {
        val remote = try {
            api.getNotifications()
        } catch (e: Exception) {
            android.util.Log.d("NotifRepo", "Fetch notifications ignoré : ${e.message}")
            return emptyList()
        }

        val remoteIds   = remote.map { it.id }
        val existingIds = dao.allIds().toSet()
        val now         = System.currentTimeMillis()

        // 1. Supprimer ce qui n'est plus dans le JSON distant
        dao.deleteRemovedIds(remoteIds)

        // 2. Insérer uniquement les nouvelles entrées (les existantes gardent isRead/receivedAt)
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
