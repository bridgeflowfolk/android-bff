package com.bridgeflowfolk.bff.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bridgeflowfolk.bff.domain.Event
import java.net.URLEncoder
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val frDateFormatter = DateTimeFormatter
    .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    .withLocale(Locale.FRANCE)

// ─── Carte événement avec actions ─────────────────────────────────────────────

@Composable
fun EventCard(event: Event, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var descriptionExpanded by remember { mutableStateOf(false) }
    val isPast = event.dateTime.isBefore(java.time.LocalDateTime.now())

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPast) 1.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPast)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            // ── Image ────────────────────────────────────────────────────────
            event.imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = event.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {

                // ── Badge "passé" ─────────────────────────────────────────────
                if (isPast) {
                    Surface(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            "Événement passé",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Titre ─────────────────────────────────────────────────────
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                // ── Date ──────────────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday, contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = event.dateTime.format(frDateFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(4.dp))

                // ── Lieu ──────────────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn, contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ── Description expansible ────────────────────────────────────
                if (event.description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (descriptionExpanded) Int.MAX_VALUE else 3,
                        overflow = if (descriptionExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { descriptionExpanded = !descriptionExpanded }
                    )
                    if (!descriptionExpanded) {
                        Text(
                            text = "Voir plus…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { descriptionExpanded = true }
                                .padding(top = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))

                // ── Boutons d'action ──────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Agenda
                    EventActionButton(
                        icon = Icons.Default.CalendarMonth,
                        label = "Agenda",
                        modifier = Modifier.weight(1f),
                        onClick = { addToCalendar(context, event) }
                    )
                    // Partager
                    EventActionButton(
                        icon = Icons.Default.Share,
                        label = "Partager",
                        modifier = Modifier.weight(1f),
                        onClick = { shareEvent(context, event) }
                    )
                    // Itinéraire
                    EventActionButton(
                        icon = Icons.Default.Navigation,
                        label = "Itinéraire",
                        modifier = Modifier.weight(1f),
                        onClick = { openNavigation(context, event) }
                    )
                }
            }
        }
    }
}

// ─── Bouton action compact ────────────────────────────────────────────────────

@Composable
private fun EventActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        shape = MaterialTheme.shapes.small
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

// ─── Actions ─────────────────────────────────────────────────────────────────

private fun addToCalendar(context: Context, event: Event) {
    val startMs = event.dateTime
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    val endMs = startMs + 2 * 60 * 60 * 1000L // durée par défaut 2h

    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, event.title)
        putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
        putExtra(CalendarContract.Events.DESCRIPTION, event.description)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMs)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMs)
    }
    context.startActivity(intent)
}

private fun shareEvent(context: Context, event: Event) {
    val dateStr = event.dateTime.format(
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(Locale.FRANCE)
    )
    // URL dédiée à l'événement si le champ "url" est présent dans le JSON,
    // sinon fallback vers le site de l'association
    val url = event.eventUrl?.takeIf { it.isNotBlank() }
        ?: "https://bridgeflowfolk.github.io"
    val text = buildString {
        append("🌿 ${event.title}\n")
        append("📅 $dateStr\n")
        append("📍 ${event.location}\n")
        if (event.description.isNotBlank()) append("\n${event.description}\n")
        append("\n$url")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, event.title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Partager l'événement"))
}

private fun openNavigation(context: Context, event: Event) {
    val encoded = URLEncoder.encode(event.location, "UTF-8")
    // Tente Waze en priorité, fallback Google Maps
    val wazeUri  = Uri.parse("waze://?q=$encoded&navigate=yes")
    val mapsUri  = Uri.parse("https://maps.google.com/?q=$encoded")

    val wazeIntent = Intent(Intent.ACTION_VIEW, wazeUri)
    if (wazeIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(wazeIntent)
    } else {
        context.startActivity(Intent(Intent.ACTION_VIEW, mapsUri))
    }
}

// ─── État vide ────────────────────────────────────────────────────────────────

@Composable
fun EmptyState(modifier: Modifier = Modifier, query: String = "") {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (query.isBlank()) "Aucun événement pour l'instant"
                   else "Aucun résultat pour « $query »",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (query.isBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tirez vers le bas pour actualiser",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
