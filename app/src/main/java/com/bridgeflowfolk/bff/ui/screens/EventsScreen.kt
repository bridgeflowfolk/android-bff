package com.bridgeflowfolk.bff.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bridgeflowfolk.bff.ui.EventsViewModel
import com.bridgeflowfolk.bff.ui.components.EventCard
import com.bridgeflowfolk.bff.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(viewModel: EventsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val pullState = rememberPullToRefreshState()

    // Déclenche le refresh sur pull
    if (pullState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
            pullState.endRefresh()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Barre de recherche ──────────────────────────────────────────────
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            placeholder = { Text("Rechercher un événement…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = MaterialTheme.shapes.large
        )

        // ── Erreur réseau ───────────────────────────────────────────────────
        state.error?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // ── Contenu principal ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullState.nestedScrollConnection)
        ) {
            when {
                state.isLoading && state.events.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.events.isEmpty() -> {
                    EmptyState(
                        modifier = Modifier.align(Alignment.Center),
                        query = state.searchQuery
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.events, key = { it.id }) { event ->
                            EventCard(event = event)
                        }
                    }
                }
            }

            PullToRefreshContainer(
                state = pullState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
