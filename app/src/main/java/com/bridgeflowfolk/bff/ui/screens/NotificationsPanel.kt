package com.bridgeflowfolk.bff.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeflowfolk.bff.domain.InAppNotification
import com.bridgeflowfolk.bff.ui.InAppNotificationViewModel

// ─── Icône cloche avec badge ──────────────────────────────────────────────────

/**
 * Badge positionné à gauche de la cloche (TopEnd de la TopAppBar = bord gauche de l'icône
 * dans le flux RTL-safe).
 *
 * On utilise BadgedBox de Material 3 : il gère le positionnement du badge en TopEnd
 * du contenu, ce qui correspond visuellement au coin supérieur-gauche de la cloche
 * (la cloche est alignée à droite dans la TopAppBar, donc TopEnd = côté gauche visible).
 * Aucun offset manuel → pas de troncature par le clip de la TopAppBar.
 */
@Composable
fun NotificationBellIcon(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = when {
        unreadCount <= 0  -> null
        unreadCount > 99  -> "99+"
        else              -> unreadCount.toString()
    }

    BadgedBox(
        badge = {
            AnimatedVisibility(
                visible = label != null,
                enter   = scaleIn(tween(200)) + fadeIn(tween(200)),
                exit    = scaleOut(tween(150)) + fadeOut(tween(150))
            ) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor   = MaterialTheme.colorScheme.onError
                ) {
                    if (label != null) {
                        Text(
                            text       = label,
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector        = Icons.Default.Notifications,
            contentDescription = "Notifications (${unreadCount} non lues)",
            tint               = MaterialTheme.colorScheme.onPrimary,
            modifier           = Modifier.size(26.dp)
        )
    }
}

// ─── Panneau notifications (BottomSheet) ──────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsBottomSheet(
    onDismiss: () -> Unit,
    viewModel: InAppNotificationViewModel
) {
    val state   by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Capture les ids non lus au moment de l'ouverture, avant markAllRead.
    // Permet au filtre "Non lues" de montrer ce qui était non lu à l'ouverture,
    // même si markAllRead() les a déjà marqués isRead=true en base.
    val unreadIdsOnOpen = remember { state.notifications.filter { !it.isRead }.map { it.id }.toSet() }

    // Filtre local : masquer les lues. Par défaut on montre tout pour que
    // l'utilisateur voit l'historique complet à l'ouverture.
    var hideRead by remember { mutableStateOf(false) }

    // Marquer tout comme lu dès que le panneau est ouvert
    LaunchedEffect(Unit) { viewModel.markAllRead() }

    val visibleNotifs = remember(state.notifications, hideRead) {
        if (hideRead) state.notifications.filter { it.id in unreadIdsOnOpen }
        else          state.notifications
    }

    // Le bouton filtre n'est utile que s'il y avait des notifs lues à l'ouverture
    val hadReadOnOpen = remember {
        state.notifications.any { it.isRead }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle       = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // ── En-tête ──────────────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = "Informations",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bouton filtre — visible si des notifs étaient déjà lues à l'ouverture
                    // OU si des non-lues viennent d'être marquées lues (unreadIdsOnOpen non vide)
                    AnimatedVisibility(visible = hadReadOnOpen || unreadIdsOnOpen.isNotEmpty()) {
                        FilterChip(
                            selected = hideRead,
                            onClick  = { hideRead = !hideRead },
                            label    = {
                                Text(
                                    text  = if (hideRead) "Toutes" else "Non lues",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (hideRead) Icons.Default.Visibility
                                                  else          Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }

                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            HorizontalDivider()

            when {
                // ── Chargement initial ────────────────────────────────────────
                state.isLoading && state.notifications.isEmpty() -> {
                    Box(
                        modifier         = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }

                // ── Aucune notification du tout ───────────────────────────────
                state.notifications.isEmpty() -> {
                    EmptyState(message = "Aucune information pour l'instant")
                }

                // ── Filtre actif mais tout est lu ─────────────────────────────
                visibleNotifs.isEmpty() -> {
                    EmptyState(message = "Tout a été lu ✓")
                }

                // ── Liste ─────────────────────────────────────────────────────
                else -> {
                    LazyColumn(
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(visibleNotifs, key = { it.id }) { notif ->
                            NotificationCard(notification = notif, context = context)
                        }
                    }
                }
            }
        }
    }
}

// ─── État vide ────────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.NotificationsNone,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint     = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            text  = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Carte notification ───────────────────────────────────────────────────────

@Composable
private fun NotificationCard(
    notification: InAppNotification,
    context: Context
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(250)),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ── Titre + toggle expand ─────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text     = notification.title,
                    style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color    = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector        = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Réduire" else "Développer",
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(20.dp)
                )
            }

            // ── Détail expansible ─────────────────────────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(tween(250)) + fadeIn(tween(200)),
                exit    = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text  = notification.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!notification.url.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier          = Modifier.clickable {
                                openUrlSafe(context, notification.url)
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.primary,
                                modifier           = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text  = "En savoir plus",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    textDecoration = TextDecoration.Underline
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Utilitaire ──────────────────────────────────────────────────────────────

private fun openUrlSafe(context: Context, url: String) {
    val uri = if (url.startsWith("http")) Uri.parse(url) else Uri.parse("https://$url")
    try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    catch (e: Exception) { Log.w("NotifPanel", "Ouverture URL échouée : ${e.message}") }
}
