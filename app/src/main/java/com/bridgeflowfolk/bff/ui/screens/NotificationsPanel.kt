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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bridgeflowfolk.bff.domain.InAppNotification
import com.bridgeflowfolk.bff.ui.InAppNotificationViewModel

// ─── Icône cloche avec badge ──────────────────────────────────────────────────

/**
 * Icône cloche destinée à être placée dans la TopAppBar.
 * Affiche un badge rouge avec le nombre de notifications non lues (max affiché : 99).
 * Si tout est lu, aucun badge n'est visible.
 */
@Composable
fun NotificationBellIcon(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier  = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = Icons.Default.Notifications,
            contentDescription = "Notifications",
            tint               = MaterialTheme.colorScheme.onPrimary,
            modifier           = Modifier.size(26.dp)
        )

        // Badge non lu
        AnimatedVisibility(
            visible = unreadCount > 0,
            enter   = scaleIn(tween(200)) + fadeIn(tween(200)),
            exit    = scaleOut(tween(150)) + fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            val label = if (unreadCount > 99) "99+" else unreadCount.toString()
            Box(
                modifier = Modifier
                    .offset(x = 2.dp, y = (-2).dp)
                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text     = label,
                    color    = MaterialTheme.colorScheme.onError,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 3.dp)
                )
            }
        }
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

    // Marquer tout comme lu dès que le panneau est ouvert
    LaunchedEffect(Unit) {
        viewModel.markAllRead()
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
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier  = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            HorizontalDivider()

            when {
                // ── Chargement initial ────────────────────────────────────────
                state.isLoading && state.notifications.isEmpty() -> {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                // ── Aucune notification ───────────────────────────────────────
                state.notifications.isEmpty() -> {
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
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
                            text  = "Aucune information pour l'instant",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Liste des notifications ───────────────────────────────────
                else -> {
                    LazyColumn(
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.notifications, key = { it.id }) { notif ->
                            NotificationCard(
                                notification = notif,
                                context      = context
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Carte notification ───────────────────────────────────────────────────────

@Composable
private fun NotificationCard(
    notification: InAppNotification,
    context: Context
) {
    var expanded by remember { mutableStateOf(false) }
    val hasUrl   = !notification.url.isNullOrBlank()

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

            // ── Ligne titre + toggle expand ───────────────────────────────────
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
                // Icône +/- pour déplier le détail
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

                    // ── Lien optionnel ────────────────────────────────────────
                    if (hasUrl) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier          = Modifier.clickable {
                                openUrlSafe(context, notification.url!!)
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
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: Exception) {
        Log.w("NotifPanel", "Ouverture URL échouée : ${e.message}")
    }
}
