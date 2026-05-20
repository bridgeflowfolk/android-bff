package com.bridgeflowfolk.bff.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bridgeflowfolk.bff.ui.EventsViewModel
import com.bridgeflowfolk.bff.ui.components.EmptyState
import com.bridgeflowfolk.bff.ui.components.EventCard
import kotlinx.coroutines.launch

// ─── Skeleton shimmer ─────────────────────────────────────────────────────────

@Composable
private fun ShimmerBrush(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceVariant,
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start  = Offset.Zero,
        end    = Offset(translateAnim, translateAnim)
    )
}

@Composable
private fun EventCardSkeleton(brush: Brush) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column {
            // Image placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(brush)
            )
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Titre
                Box(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                // Date
                Box(modifier = Modifier.fillMaxWidth(0.45f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                // Lieu
                Box(modifier = Modifier.fillMaxWidth(0.55f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                Spacer(Modifier.height(4.dp))
                // Boutons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        Box(modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonList() {
    val brush = ShimmerBrush()
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false
    ) {
        items(4) { EventCardSkeleton(brush) }
    }
}

// ─── Écran événements ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(viewModel: EventsViewModel = hiltViewModel()) {
    val state        by viewModel.uiState.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }
    val scope        = rememberCoroutineScope()
    // État PullToRefresh séparé pour accéder à l'indicateur personnalisé
    val pullState    = rememberPullToRefreshState()

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Barre recherche + filtre ──────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value         = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder   = { Text("Rechercher…") },
                leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine    = true,
                modifier      = Modifier.weight(1f),
                shape         = MaterialTheme.shapes.large
            )
            FilterChip(
                selected    = state.hidePassedEvents,
                onClick     = { viewModel.onToggleHidePassedEvents(!state.hidePassedEvents) },
                label       = { Text("À venir") },
                leadingIcon = {
                    Icon(Icons.Default.FilterList, contentDescription = null,
                        modifier = Modifier.size(16.dp))
                }
            )
        }

        // ── Erreur réseau ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.error != null,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut()
        ) {
            state.error?.let { msg ->
                Card(
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text     = msg,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // ── Contenu principal ─────────────────────────────────────────────
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh    = {
                scope.launch {
                    isRefreshing = true
                    viewModel.refreshSuspending()
                    isRefreshing = false
                }
            },
            state    = pullState,
            modifier = Modifier.fillMaxSize(),
            // Indicateur Material3 avec couleurs du thème BFF
            indicator = {
                Indicator(
                    modifier     = Modifier.align(Alignment.TopCenter),
                    isRefreshing = isRefreshing,
                    state        = pullState,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    color          = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            when {
                // ── Skeleton au premier chargement ────────────────────────
                state.isLoading && state.events.isEmpty() -> SkeletonList()

                // ── État vide ─────────────────────────────────────────────
                state.events.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) { EmptyState(query = state.searchQuery) }
                }

                // ── Liste avec animation d'entrée par carte ───────────────
                else -> {
                    LazyColumn(
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier            = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(state.events, key = { _, e -> e.id }) { index, event ->
                            // AnimatedVisibility requiert un ColumnScope comme receiver implicite,
                            // indisponible dans LazyItemScope. On encapsule dans un Column léger
                            // pour fournir ce contexte sans coût de layout supplémentaire.
                            val delay = (index * 40).coerceAtMost(240)
                            Column {
                                AnimatedVisibility(
                                    visible = true,
                                    enter   = fadeIn(tween(300, delayMillis = delay)) +
                                              slideInVertically(
                                                  tween(300, delayMillis = delay),
                                                  initialOffsetY = { it / 5 }
                                              )
                                ) {
                                    EventCard(event = event)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
