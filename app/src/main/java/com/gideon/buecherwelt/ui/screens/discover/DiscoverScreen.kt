package com.gideon.buecherwelt.ui.screens.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gideon.buecherwelt.di.AppContainer
import com.gideon.buecherwelt.domain.model.WishlistItem
import com.gideon.buecherwelt.domain.recommendation.ScoredCandidate
import com.gideon.buecherwelt.ui.components.BookCover
import com.gideon.buecherwelt.ui.components.EmptyState
import com.gideon.buecherwelt.ui.components.SectionHeader
import com.gideon.buecherwelt.ui.navigation.Screen
import com.gideon.buecherwelt.ui.viewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    container: AppContainer,
    navController: NavController
) {
    val viewModel: DiscoverViewModel = viewModel(
        factory = viewModelFactory {
            DiscoverViewModel(
                bookRepository = container.bookRepository,
                wishlistRepository = container.wishlistRepository,
                recommendationEngine = container.recommendationEngine,
                metadataFetcher = container.metadataFetcher,
                llmService = container.llmService,
                addToLibrary = container.addToLibrary,
                settingsRepository = container.settingsRepository
            )
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Entdecken") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                if (state.error != null || state.message != null) {
                    val text = state.error ?: state.message!!
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (state.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            androidx.compose.material3.TextButton(onClick = viewModel::dismissMessages) {
                                Text("OK")
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "Basierend auf ${state.profile.totalBooks} Büchern in deiner Bibliothek" +
                        (state.profile.topGenres.firstOrNull()?.let { " – Top-Genre: ${it.first}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                SectionHeader("Regelbasierte Empfehlungen", modifier = Modifier.padding(top = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = viewModel::loadRuleBased,
                        enabled = !state.ruleLoading
                    ) {
                        if (state.ruleLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Empfehlungen laden")
                    }
                    if (state.llmConfigured) {
                        OutlinedButton(
                            onClick = viewModel::loadLlmRecommendations,
                            enabled = !state.llmLoading
                        ) {
                            if (state.llmLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Per LLM empfehlen")
                        }
                    }
                }
            }
            if (state.ruleCandidates.isEmpty() && state.llmCandidates.isEmpty() && !state.ruleLoading) {
                item {
                    EmptyState(
                        title = "Noch keine Empfehlungen",
                        subtitle = "Lade regelbasierte Empfehlungen oder aktiviere in den Einstellungen einen LLM-Key."
                    )
                }
            }
            items(state.llmCandidates, key = { "llm" + (it.metadata.isbn ?: it.metadata.title) }) { candidate ->
                CandidateCard(
                    candidate = candidate,
                    inWishlist = state.wishlist.any { w ->
                        (candidate.metadata.isbn != null && w.isbn == candidate.metadata.isbn) ||
                            w.title.equals(candidate.metadata.title, ignoreCase = true)
                    },
                    onAdd = { viewModel.addToLibrary(candidate) },
                    onToggleWishlist = { viewModel.toggleWishlist(candidate) }
                )
            }
            items(state.ruleCandidates, key = { "rule" + (it.metadata.isbn ?: it.metadata.title) }) { candidate ->
                CandidateCard(
                    candidate = candidate,
                    inWishlist = state.wishlist.any { w ->
                        (candidate.metadata.isbn != null && w.isbn == candidate.metadata.isbn) ||
                            w.title.equals(candidate.metadata.title, ignoreCase = true)
                    },
                    onAdd = { viewModel.addToLibrary(candidate) },
                    onToggleWishlist = { viewModel.toggleWishlist(candidate) }
                )
            }
            item {
                SectionHeader("Merkliste (${state.wishlist.size})")
                if (state.wishlist.isEmpty()) {
                    Text(
                        "Hier landen Bücher aus den Empfehlungen, die du dir merken möchtest.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(state.wishlist, key = { it.id }) { item ->
                WishlistCard(
                    item = item,
                    onAdd = {
                        scope.launch {
                            val result = container.addToLibrary.addMetadata(
                                com.gideon.buecherwelt.data.network.BookMetadata(
                                    isbn = item.isbn,
                                    title = item.title,
                                    author = item.author,
                                    coverUrl = item.coverUrl
                                )
                            )
                            snackbarHostState.showSnackbar(
                                result.fold(
                                    onSuccess = { "Zur Bibliothek hinzugefügt: ${item.title}" },
                                    onFailure = { e -> e.message ?: "Fehler" }
                                )
                            )
                        }
                    },
                    onRemove = { viewModel.removeFromWishlist(item) }
                )
            }
        }
    }
}

@Composable
private fun CandidateCard(
    candidate: ScoredCandidate,
    inWishlist: Boolean,
    onAdd: () -> Unit,
    onToggleWishlist: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            BookCover(
                coverPath = null,
                coverUrl = candidate.metadata.coverUrl,
                contentDescription = candidate.metadata.title,
                modifier = Modifier
                    .width(56.dp)
                    .aspectRatio(2f / 3f)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    candidate.metadata.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (candidate.metadata.author.isNotBlank()) {
                    Text(
                        candidate.metadata.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                candidate.reasons.forEach { reason ->
                    Text(
                        "• $reason",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Zur Bibliothek hinzufügen")
                }
                IconButton(onClick = onToggleWishlist) {
                    Icon(
                        if (inWishlist) Icons.Default.BookmarkRemove else Icons.Default.BookmarkAdd,
                        contentDescription = "Merkliste"
                    )
                }
            }
        }
    }
}

@Composable
private fun WishlistCard(
    item: WishlistItem,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(
                coverPath = item.coverPath,
                coverUrl = item.coverUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .width(40.dp)
                    .aspectRatio(2f / 3f)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.author.isNotBlank()) {
                    Text(item.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (item.reason.isNotBlank()) {
                    Text(item.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Per ISBN/ASIN zur Bibliothek hinzufügen")
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.BookmarkRemove, contentDescription = "Von Merkliste entfernen")
            }
        }
    }
}
