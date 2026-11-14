package com.gideon.buecherwelt.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gideon.buecherwelt.di.AppContainer
import com.gideon.buecherwelt.domain.recommendation.ScoredCandidate
import com.gideon.buecherwelt.ui.components.BookCover
import com.gideon.buecherwelt.ui.components.InfoRow
import com.gideon.buecherwelt.ui.components.SectionHeader
import com.gideon.buecherwelt.ui.components.SpiceFlames
import com.gideon.buecherwelt.ui.components.StarRating
import com.gideon.buecherwelt.ui.components.StatusChip
import com.gideon.buecherwelt.ui.navigation.Screen
import com.gideon.buecherwelt.ui.viewModelFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookDetailScreen(
    container: AppContainer,
    navController: NavController,
    bookId: Long
) {
    val viewModel: BookDetailViewModel = viewModel(
        factory = viewModelFactory {
            BookDetailViewModel(container.bookRepository, container.recommendationEngine, bookId)
        }
    )
    val book by viewModel.book.collectAsStateWithLifecycle()
    val similar by viewModel.similar.collectAsStateWithLifecycle()
    val similarLoading by viewModel.similarLoading.collectAsStateWithLifecycle()
    val similarError by viewModel.similarError.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var similarRequested by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(similarRequested) {
        if (similarRequested) viewModel.loadSimilar()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(book?.title ?: "Buch") },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.AddBook.create(bookId)) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Löschen")
                    }
                }
            )
        }
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val current = book
        if (current == null) {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            item {
                Row {
                    BookCover(
                        coverPath = current.coverPath,
                        coverUrl = current.coverUrl,
                        contentDescription = current.title,
                        modifier = Modifier
                            .width(120.dp)
                            .aspectRatio(2f / 3f)
                    )
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(current.title, style = MaterialTheme.typography.titleLarge)
                        if (current.author.isNotBlank()) {
                            Text(
                                current.author,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        StatusChip(status = current.status)
                        Spacer(Modifier.height(8.dp))
                        StarRating(rating = current.rating ?: 0, size = 24)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Spice:", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.width(4.dp))
                            SpiceFlames(level = current.spiceLevel)
                            if (current.spiceLevel != null) {
                                Text(" ${current.spiceLevel}/5", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            item {
                SectionHeader("Details")
                InfoRow("ISBN", current.isbn ?: "–")
                InfoRow("Verlag", current.publisher)
                InfoRow("Erscheinungsjahr", current.publishedYear?.toString() ?: "–")
                InfoRow("Genre", current.genre)
                InfoRow("Subgenre", current.subgenre)
            }
            if (current.tropes.isNotEmpty()) {
                item {
                    SectionHeader("Tropes / Tags")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        current.tropes.forEach { trope ->
                            FilterChip(selected = true, onClick = {}, label = { Text(trope.name) })
                        }
                    }
                }
            }
            if (current.description.isNotBlank()) {
                item {
                    SectionHeader("Beschreibung")
                    Text(current.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (current.notes.isNotBlank()) {
                item {
                    SectionHeader("Meine Notizen")
                    Text(current.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                SectionHeader("Ähnliche Bücher")
                OutlinedButton(
                    onClick = { similarRequested = true },
                    enabled = !similarLoading
                ) {
                    if (similarLoading) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Ähnliche Bücher laden")
                }
                if (similarError != null) {
                    Text(
                        similarError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                similar.forEach { candidate ->
                    SimilarBookCard(
                        candidate = candidate,
                        onAdd = {
                            scope.launch {
                                val result = container.addToLibrary.addFromCandidate(candidate)
                                snackbarHostState.showSnackbar(
                                    result.fold(
                                        onSuccess = { "Zur Bibliothek hinzugefügt." },
                                        onFailure = { it.message ?: "Fehler" }
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Buch löschen?") },
            text = { Text("„${book?.title}“ wird dauerhaft aus deiner Bibliothek entfernt.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete { navController.popBackStack() }
                }) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Abbrechen") }
            }
        )
    }
}

@Composable
private fun SimilarBookCard(
    candidate: ScoredCandidate,
    onAdd: () -> Unit
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            BookCover(
                coverPath = null,
                coverUrl = candidate.metadata.coverUrl,
                contentDescription = candidate.metadata.title,
                modifier = Modifier
                    .width(48.dp)
                    .aspectRatio(2f / 3f)
            )
            Column(modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)) {
                Text(candidate.metadata.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Zur Bibliothek hinzufügen")
            }
        }
    }
}
