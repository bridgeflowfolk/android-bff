package com.bridgeflowfolk.bff.ui.screens

import android.content.Context
import android.content.Intent
import android.util.Log
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
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

// ─── Lancement d'Intent sécurisé (local) ─────────────────────────────────────

private fun Context.startSafe(intent: Intent) {
    try { startActivity(intent) }
    catch (e: Exception) { Log.w("BFF", "startActivity échoué : ${e.message}") }
}

// ─── À propos (WebView avec gestion back Android) ────────────────────────────

/**
 * WebView avec gestion correcte du bouton retour Android :
 * - Si la WebView peut reculer dans son historique → on recule dans la WebView.
 * - Sinon → comportement par défaut (remonte dans la navigation Compose).
 *
 * On passe webViewRef via AndroidView factory + update pour avoir une référence
 * stable sans recréer la WebView.
 */
@Composable
fun AboutScreen() {
    val webViewRef  = remember { mutableStateOf<WebView?>(null) }
    var canGoBack   by remember { mutableStateOf(false) }

    // BackHandler actif uniquement quand la WebView a un historique à dépiler.
    // Quand canGoBack = false, l'événement remonte à la navigation Compose normalement.
    BackHandler(enabled = canGoBack) {
        webViewRef.value?.goBack()
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        // Les liens vers d'autres domaines s'ouvrent dans le navigateur système
                        val url = request.url.toString()
                        return if (url.startsWith("https://bridgeflowfolk.github.io")) {
                            false // navigation interne → WebView gère
                        } else {
                            try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                            catch (e: Exception) { Log.w("AboutScreen", "URL ignorée : $url") }
                            true
                        }
                    }

                    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                        // Mise à jour réactive du flag → active/désactive le BackHandler
                        canGoBack = view.canGoBack()
                    }
                }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                loadUrl("https://bridgeflowfolk.github.io/apropos.html")
            }.also { webViewRef.value = it }
        },
        update = { wv -> webViewRef.value = wv },
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
                ctx.startSafe(Intent(Intent.ACTION_DIAL, Uri.parse("tel:0618291873")))
            }
        )

        ContactIconButton(
            label          = "bridgeflow.f@gmail.com",
            icon           = Icons.Default.Email,
            containerColor = MaterialTheme.colorScheme.secondary,
            onClick        = {
                ctx.hapticTick()
                ctx.startSafe(
                    Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:bridgeflow.f@gmail.com")
                    }
                )
            }
        )

        Button(
            onClick = {
                ctx.hapticTick()
                ctx.startSafe(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/33618291873")))
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
                ctx.startSafe(
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

        AnimatedVisibility(
            visible = prefs.notificationsEnabled,
            enter   = expandVertically(tween(300)) + fadeIn(tween(300)),
            exit    = shrinkVertically(tween(250)) + fadeOut(tween(200))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NotifSlider(
                    icon          = Icons.Default.Schedule,
                    label         = "Vérification des événements",
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
                NotifSlider(
                    icon          = Icons.Default.Notifications,
                    label         = "Vérification des informations",
                    value         = prefs.notifFetchIntervalHours,
                    onValueChange = { prefsViewModel.setNotifFetchInterval(it) },
                    valueRange    = 1f..24f,
                    unit          = "h",
                    description   = "Fréquence de récupération des nouvelles informations (cloche)"
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
                Crossfade(
                    targetState   = enabled,
                    animationSpec = tween(200),
                    label         = "notif_icon"
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
//
// Correction du bug original :
//   remember(value) → la valeur locale était réinitialisée à chaque recomposition
//   déclenchée par un état parent, ce qui rendait le glissement instable.
//
// Solution : état local indépendant, synchronisé avec `value` uniquement si la
// différence dépasse 0.5h (évite l'écrasement pendant le glissement).
// onValueChangeFinished persiste en DataStore ; onValueChange ne touche que l'état local.

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
    // État local indépendant — initialisé une fois, pas re-derivé de `value`
    var sliderValue by remember { mutableFloatStateOf(value) }

    // Synchronise l'état local si la valeur externe change significativement
    // (ex: chargement initial depuis DataStore, reset externe).
    // Le seuil de 0.4f évite de perturber le glissement en cours.
    LaunchedEffect(value) {
        if (kotlin.math.abs(value - sliderValue) > 0.4f) {
            sliderValue = value
        }
    }

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
