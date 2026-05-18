package com.bridgeflowfolk.bff.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bridgeflowfolk.bff.R
import com.bridgeflowfolk.bff.ui.theme.BffColors

// ─── À propos (WebView) ───────────────────────────────────────────────────────

@Composable
fun AboutScreen() {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()          // reste dans l'app
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                loadUrl("https://bridgeflowfolk.github.io/apropos.html")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

// ─── Contact ─────────────────────────────────────────────────────────────────

@Composable
fun ContactScreen() {
    val ctx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Nous contacter",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Retrouvez-nous ou contactez-nous directement via les options ci-dessous.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        // ── Appel téléphonique ──────────────────────────────────────────────
        ContactButton(
            label = "Appeler le 06 18 29 18 73",
            icon = { Icon(Icons.Default.Call, contentDescription = null) },
            containerColor = MaterialTheme.colorScheme.primary,
            onClick = {
                ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:0618291873")))
            }
        )

        // ── WhatsApp ────────────────────────────────────────────────────────
        ContactButton(
            label = "WhatsApp",
            icon = { Icon(painterResource(R.drawable.ic_whatsapp), contentDescription = null) },
            containerColor = BffColors.SageGreen,
            onClick = {
                val uri = Uri.parse("https://wa.me/33618291873")
                ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        )

        // ── Facebook ────────────────────────────────────────────────────────
        ContactButton(
            label = "Notre page Facebook",
            icon = { Icon(painterResource(R.drawable.ic_facebook), contentDescription = null) },
            containerColor = Color(0xFF1877F2),
            onClick = {
                val uri = Uri.parse("https://www.facebook.com/profile.php?id=61587252715739")
                ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Association Bridge & Flow Folk\nNogent-le-Roi, Eure-et-Loir (28)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Composant bouton de contact réutilisable ─────────────────────────────────

@Composable
private fun ContactButton(
    label: String,
    icon: @Composable () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        icon()
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

private val Color = androidx.compose.ui.graphics.Color
