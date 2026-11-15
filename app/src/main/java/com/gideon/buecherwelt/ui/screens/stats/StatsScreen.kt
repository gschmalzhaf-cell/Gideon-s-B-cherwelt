package com.gideon.buecherwelt.ui.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gideon.buecherwelt.data.local.ReadingStatus
import com.gideon.buecherwelt.di.AppContainer
import com.gideon.buecherwelt.ui.components.EmptyState
import com.gideon.buecherwelt.ui.components.SectionHeader
import com.gideon.buecherwelt.ui.components.SpiceFlames
import com.gideon.buecherwelt.ui.viewModelFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(container: AppContainer) {
    val viewModel: StatsViewModel = viewModel(
        factory = viewModelFactory { StatsViewModel(container.bookRepository) }
    )
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Meine Lesevorlieben") }) }
    ) { padding ->
        if (profile.totalBooks == 0) {
            EmptyState(
                title = "Noch keine Daten",
                subtitle = "Erfasse ein paar Bücher – die Auswertung erscheint automatisch.",
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            item {
                Card(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Bibliothek", style = MaterialTheme.typography.titleMedium)
                        ReadingStatus.entries.forEach { status ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(status.label)
                                Text("${profile.statusCounts[status] ?: 0}")
                            }
                        }
                        Text("Gesamt: ${profile.totalBooks} Bücher")
                    }
                }
            }
            item {
                SectionHeader("Top-Genres")
                ChipList(profile.topGenres.map { it.first })
            }
            item {
                SectionHeader("Genre-Verteilung")
                val max = profile.genreDistribution.maxOfOrNull { it.second } ?: 1
                profile.genreDistribution.forEach { (genre, count) ->
                    Column(Modifier.padding(vertical = 2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(genre, style = MaterialTheme.typography.bodyMedium)
                            Text("$count", style = MaterialTheme.typography.bodySmall)
                        }
                        LinearProgressIndicator(
                            progress = count.toFloat() / max,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )
                    }
                }
            }
            item {
                SectionHeader("Top-Tropes")
                ChipList(profile.topTropes.map { it.first })
            }
            item {
                SectionHeader("Lieblingsautor:innen")
                ChipList(profile.topAuthors.map { it.first })
            }
            item {
                SectionHeader("Lieblingsverlage")
                ChipList(profile.topPublishers.map { it.first })
            }
            item {
                SectionHeader("Spice-Level im Schnitt")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (profile.avgSpice != null) {
                        Text(
                            "%.1f / 5".format(profile.avgSpice!!),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer4()
                        SpiceFlames(level = profile.avgSpice!!.toInt().coerceAtLeast(1).coerceAtMost(5))
                    } else {
                        Text("noch keine Angaben")
                    }
                }
            }
            item {
                SectionHeader("Lieblingsbücher (gelesen, 4-5 Sterne)")
                if (profile.favorites.isEmpty()) {
                    Text("Noch keine – bewerte gelesene Bücher mit 4 oder 5 Sternen.")
                } else {
                    profile.favorites.forEach { book ->
                        Text("• ${book.title} – ${book.author} (${book.rating}/5)")
                    }
                }
            }
        }
    }
}

@Composable
private fun Spacer4() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(4.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipList(items: List<String>) {
    if (items.isEmpty()) {
        Text("–", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { item ->
            androidx.compose.material3.AssistChip(onClick = {}, label = { Text(item) })
        }
    }
}
