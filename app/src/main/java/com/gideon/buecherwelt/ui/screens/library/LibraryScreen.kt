package com.gideon.buecherwelt.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gideon.buecherwelt.di.AppContainer
import com.gideon.buecherwelt.ui.components.BookCard
import com.gideon.buecherwelt.ui.components.EmptyState
import com.gideon.buecherwelt.ui.components.FilterBar
import com.gideon.buecherwelt.ui.navigation.Screen
import com.gideon.buecherwelt.ui.viewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    container: AppContainer,
    navController: NavController
) {
    val viewModel: LibraryViewModel = viewModel(
        factory = viewModelFactory {
            LibraryViewModel(container.bookRepository, container.tropeRepository)
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gideon's Bücherwelt") },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.AddBook.create()) }) {
                Icon(Icons.Default.Add, contentDescription = "Buch hinzufügen")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
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
                modifier = Modifier.padding(top = 12.dp)
            )
            val filtered = state.filtered
            if (filtered.isEmpty()) {
                EmptyState(
                    title = "Keine Bücher gefunden",
                    subtitle = "Füge dein erstes Buch per ISBN-Scan oder manuell über das + hinzu.",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = "${filtered.size} von ${state.books.size} Büchern",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
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
}
