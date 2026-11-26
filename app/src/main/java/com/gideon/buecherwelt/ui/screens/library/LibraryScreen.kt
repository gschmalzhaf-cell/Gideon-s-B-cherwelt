package com.gideon.buecherwelt.ui.screens.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gideon.buecherwelt.di.AppContainer
import com.gideon.buecherwelt.domain.model.Book
import com.gideon.buecherwelt.ui.components.AppTextField
import com.gideon.buecherwelt.ui.components.BookCard
import com.gideon.buecherwelt.ui.components.BookCover
import com.gideon.buecherwelt.ui.components.EmptyState
import com.gideon.buecherwelt.ui.components.FilterBar
import com.gideon.buecherwelt.ui.navigation.Screen
import com.gideon.buecherwelt.ui.theme.PlayfairDisplay
import com.gideon.buecherwelt.ui.viewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    container: AppContainer,
    navController: NavController
) {
    val viewModel: LibraryViewModel = viewModel(
        factory = viewModelFactory {
            LibraryViewModel(
                container.bookRepository,
                container.tropeRepository,
                container.llmService,
                container.settingsRepository,
                container.bookEnrichmentService
            )
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Gideon's Bücherwelt",
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = PlayfairDisplay,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            if (state.books.isNotEmpty()) {
                                Text(
                                    text = "${state.profile.archetype.badge} · ${state.books.size} Werke im Regal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (state.books.isNotEmpty()) {
                        IconButton(
                            onClick = viewModel::refreshLibraryCoversAndDetails,
                            enabled = !state.enriching
                        ) {
                            if (state.enriching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Cover, Reihen & Buchdetails aktualisieren"
                                )
                            }
                        }
                    }
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddBook.create()) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Buch hinzufügen")
            }
        }
    ) { padding ->
        val filtered = state.filtered
        val readingBooks = state.readingBooks
        val showReadingShowcase = readingBooks.isNotEmpty() &&
            state.query.isBlank() &&
            state.status == null &&
            !state.semanticActive

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 84.dp),
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    FilterBar(
                        query = state.query,
                        onQueryChange = viewModel::setQuery,
                        genres = state.genres,
                        selectedGenre = state.genre,
                        onGenreChange = viewModel::setGenre,
                        selectedStatus = state.status,
                        onStatusChange = viewModel::setStatus,
                        minRating = state.minRating,
                        onMinRatingChange = viewModel::setMinRating,
                        tropes = state.tropes,
                        selectedTrope = state.trope,
                        onTropeChange = viewModel::setTrope,
                        seriesNames = state.seriesNames,
                        selectedSeries = state.series,
                        onSeriesChange = viewModel::setSeries,
                        universeNames = state.universeNames,
                        selectedUniverse = state.universe,
                        onUniverseChange = viewModel::setUniverse,
                        sortOrder = state.sortOrder,
                        onSortOrderChange = viewModel::setSortOrder,
                        onResetAll = viewModel::resetFilters
                    )
                    if (state.enriching || state.enrichMessage != null) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val progressText = state.enrichProgress?.let { (done, total) ->
                                        "Aktualisiere Bibliothek ($done/$total)…"
                                    } ?: state.enrichMessage.orEmpty()
                                    Text(
                                        text = progressText,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (!state.enriching) {
                                        IconButton(
                                            onClick = viewModel::dismissEnrichMessage,
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Schließen",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                val prog = state.enrichProgress
                                if (state.enriching && prog != null && prog.second > 0) {
                                    LinearProgressIndicator(
                                        progress = { prog.first.toFloat() / prog.second.toFloat() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (state.books.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth()
                        ) {
                            AppTextField(
                                value = state.semanticQuery,
                                onValueChange = viewModel::setSemanticQuery,
                                placeholder = {
                                    Text(
                                        if (state.llmConfigured) {
                                            "Smart- & KI-Suche, z. B. „düster & ungelesen“"
                                        } else {
                                            "Smart-Suche, z. B. „ungelesen & spicy“ oder „kurz“"
                                        }
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                                trailingIcon = if (state.semanticActive || state.semanticQuery.isNotBlank()) {
                                    {
                                        IconButton(onClick = viewModel::clearSemantic) {
                                            Icon(Icons.Default.Close, contentDescription = "Smart-Suche zurücksetzen")
                                        }
                                    }
                                } else null,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { viewModel.runSemanticSearch() }),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { viewModel.runSemanticSearch() },
                                enabled = !state.semanticLoading && state.semanticQuery.isNotBlank()
                            ) {
                                if (state.semanticLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "Smart-Suche starten")
                                }
                            }
                        }
                        val smartPresets = listOf(
                            "✨ Highlight" to "ungelesen nächstes highlight",
                            "🌙 Düster" to "düster",
                            "🔥 Spicy" to "spicy",
                            "☕ Cozy & Kurz" to "warmherzig kurz",
                            "🐉 Magisch" to "magisch",
                            "⭐ Favoriten" to "favoriten",
                            "❓ Unbewertet" to "unbewertet"
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            items(smartPresets, key = { it.first }) { (label, presetQuery) ->
                                val active = state.semanticActive && state.semanticQuery.equals(presetQuery, ignoreCase = true)
                                androidx.compose.material3.FilterChip(
                                    selected = active,
                                    onClick = {
                                        if (active) viewModel.clearSemantic()
                                        else viewModel.runQuickSmartPreset(presetQuery)
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    shape = CircleShape
                                )
                            }
                        }
                        if (state.semanticActive && state.semanticExplanation != null) {
                            Text(
                                text = state.semanticExplanation!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        if (state.semanticError != null) {
                            Text(
                                text = state.semanticError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            if (filtered.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = "Keine Bücher gefunden",
                        subtitle = if (state.semanticActive) {
                            "Die KI-Suche hat keine passenden Bücher gefunden – Frage oder Filter anpassen."
                        } else if (state.books.isEmpty()) {
                            "Deine Regale warten auf ihr erstes Werk. Füge Bücher per ISBN-Scan, Cover-Foto oder manuell über das + hinzu."
                        } else {
                            "Kein Buch entspricht den gewählten Filtern."
                        },
                        action = if (state.hasActiveFilters) {
                            {
                                OutlinedButton(onClick = { viewModel.resetAllIncludingSemantic() }) {
                                    Icon(
                                        Icons.Default.RestartAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Suche & Filter zurücksetzen")
                                }
                            }
                        } else null
                    )
                }
            } else {
                if (showReadingShowcase) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ReadingDeskShowcase(
                            books = readingBooks,
                            onBookClick = { navController.navigate(Screen.BookDetail.create(it.id)) }
                        )
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bücherregal (${filtered.size} von ${state.books.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = PlayfairDisplay,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Sortiert: ${state.sortOrder.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(filtered, key = { it.id }) { book ->
                    BookCard(
                        book = book,
                        onClick = { navController.navigate(Screen.BookDetail.create(book.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingDeskShowcase(
    books: List<Book>,
    onBookClick: (Book) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Aktuell auf deinem Lesepult (${books.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(books, key = { "desk_${it.id}" }) { book ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .width(230.dp)
                            .clickable { onBookClick(book) }
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BookCover(
                                coverPath = book.coverPath,
                                coverUrl = book.coverUrl,
                                contentDescription = book.title,
                                title = book.title,
                                author = book.author,
                                cornerRadius = 6.dp,
                                modifier = Modifier
                                    .width(42.dp)
                                    .aspectRatio(2f / 3f)
                            )
                            Column(modifier = Modifier.padding(start = 10.dp)) {
                                Text(
                                    text = book.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = PlayfairDisplay,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (book.author.isNotBlank()) {
                                    Text(
                                        text = book.author,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                val sub = buildList {
                                    if (book.seriesName.isNotBlank()) {
                                        add(
                                            book.seriesName + (book.seriesIndex?.let { " #$it" } ?: "")
                                        )
                                    } else if (book.genre.isNotBlank()) {
                                        add(book.genre)
                                    }
                                }.joinToString(" · ")
                                if (sub.isNotBlank()) {
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

