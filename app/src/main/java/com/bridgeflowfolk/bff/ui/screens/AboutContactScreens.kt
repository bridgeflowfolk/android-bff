package com.bridgeflowfolk.bff.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bridgeflowfolk.bff.R
import com.bridgeflowfolk.bff.ui.NotifPrefsViewModel
import com.bridgeflowfolk.bff.ui.theme.BffColors
import com.bridgeflowfolk.bff.ui.components.hapticTick
import kotlin.math.roundToInt

// ─── À propos (WebView) ───────────────────────────────────────────────────────

@Composable
fun AboutScreen() {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient          = WebViewClient()
                settings.javaScriptEnabled  = true
                settings.domStorageEnabled  = true
                loadUrl("https://bridgeflowfolk.github.io/apropos.html")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

// ─── Contact ──────────────────────────────────────────────────────────────────

@Composable
fun ContactScreen(prefsViewModel: NotifPrefsViewModel = hiltViewModel()) {
    val ctx   = LocalContext.current
    val prefs by prefsViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            text  = "Nous contacter",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text  = "Retrouvez-nous ou contactez-nous directement.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(4.dp))

        ContactIconButton(
            label          = "Appeler le 06 18 29 18 73",
            icon           = Icons.Default.Call,
            containerColor = MaterialTheme.colorScheme.primary,
            onClick        = {
                ctx.hapticTick()
                ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:0618291873")))
            }
        )

        ContactIconButton(
            label          = "bridgeflow.f@gmail.com",
            icon           = Icons.Default.Email,
            containerColor = MaterialTheme.colorScheme.secondary,
            onClick        = {
                ctx.hapticTick()
                ctx.startActivity(
                    Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:bridgeflow.f@gmail.com")
                    }
                )
            }
        )

        Button(
            onClick = {
                ctx.hapticTick()
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/33618291873")))
            },
            colors   = ButtonDefaults.buttonColors(containerColor = BffColors.SageGreen),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = MaterialTheme.shapes.medium
        ) {
            Icon(painterResource(R.drawable.ic_whatsapp), contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("WhatsApp", style = MaterialTheme.typography.labelLarge)
        }

        Button(
            onClick = {
                ctx.hapticTick()
                ctx.startActivity(
                    Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.facebook.com/profile.php?id=61587252715739"))
                )
            },
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = MaterialTheme.shapes.medium
        ) {
            Icon(painterResource(R.drawable.ic_facebook), contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("Notre page Facebook", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()

        Text(
            text     = "Préférences de notifications",
            style    = MaterialTheme.typography.titleMedium,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        NotificationsToggle(
            enabled  = prefs.notificationsEnabled,
            onToggle = { ctx.hapticTick(); prefsViewModel.setNotificationsEnabled(it) }
        )

        // Sliders animés : apparition/disparition fluide selon le toggle
        AnimatedVisibility(
            visible = prefs.notificationsEnabled,
            enter   = expandVertically(tween(300)) + fadeIn(tween(300)),
            exit    = shrinkVertically(tween(250)) + fadeOut(tween(200))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NotifSlider(
                    icon          = Icons.Default.Schedule,
                    label         = "Vérification toutes les",
                    value         = prefs.syncIntervalHours,
                    onValueChange = { prefsViewModel.setSyncInterval(it) },
                    valueRange    = 1f..24f,
                    unit          = "h",
                    description   = "Fréquence de synchronisation des nouveaux événements"
                )
                NotifSlider(
                    icon          = Icons.Default.Notifications,
                    label         = "Rappel avant l'événement",
                    value         = prefs.reminderHoursBefore,
                    onValueChange = { prefsViewModel.setReminderHoursBefore(it) },
                    valueRange    = 1f..24f,
                    unit          = "h",
                    description   = "Vous serez notifié X heures avant le début"
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text  = "Association Bridge & Flow Folk\nNogent-le-Roi, Eure-et-Loir (28)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Toggle notifications ─────────────────────────────────────────────────────

@Composable
private fun NotificationsToggle(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icône animée entre les deux états
                Crossfade(
                    targetState = enabled,
                    animationSpec = tween(200),
                    label = "notif_icon"
                ) { on ->
                    Icon(
                        imageVector        = if (on) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint               = if (on) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text  = "Notifications",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text  = if (enabled) "Activées" else "Désactivées",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (enabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

// ─── Slider de préférence ─────────────────────────────────────────────────────

@Composable
private fun NotifSlider(
    icon: ImageVector,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    description: String
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape    = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(
                    text  = "$label : ${sliderValue.roundToInt()}$unit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value               = sliderValue,
                onValueChange       = { sliderValue = it },
                onValueChangeFinished = { onValueChange(sliderValue) },
                valueRange          = valueRange,
                steps               = (valueRange.endInclusive - valueRange.start - 1).toInt(),
                modifier            = Modifier.fillMaxWidth()
            )
            Text(
                text  = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

// ─── Bouton contact générique ─────────────────────────────────────────────────

@Composable
private fun ContactIconButton(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick  = onClick,
        colors   = ButtonDefaults.buttonColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape    = MaterialTheme.shapes.medium
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
