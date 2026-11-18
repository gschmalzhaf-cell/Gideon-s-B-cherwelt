package com.gideon.buecherwelt.ui.screens.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gideon.buecherwelt.data.local.ReadingStatus
import com.gideon.buecherwelt.di.AppContainer
import com.gideon.buecherwelt.ui.components.EmptyState
import com.gideon.buecherwelt.ui.components.SectionHeader
import com.gideon.buecherwelt.ui.components.SpiceFlames
import com.gideon.buecherwelt.ui.components.StatusChip
import com.gideon.buecherwelt.ui.theme.PlayfairDisplay
import com.gideon.buecherwelt.ui.theme.StarColor
import com.gideon.buecherwelt.ui.viewModelFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(container: AppContainer) {
    val viewModel: StatsViewModel = viewModel(
        factory = viewModelFactory { StatsViewModel(container.bookRepository) }
    )
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Lese-Chronik & Exlibris",
                        fontFamily = PlayfairDisplay,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        if (profile.totalBooks == 0) {
            EmptyState(
                title = "Noch keine Chronik-Einträge",
                subtitle = "Erfasse ein paar Bücher – deine Lese-Persönlichkeit und Statistik entstehen automatisch.",
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
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Exlibris Lese-Persönlichkeits-Karte
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "EXLIBRIS · LESE-ARCHETYP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = profile.archetype.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = profile.archetype.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = PlayfairDisplay,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = profile.archetype.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Bibliotheks-Bestand (${profile.totalBooks} Werke)",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = PlayfairDisplay,
                            fontWeight = FontWeight.Bold
                        )
                        ReadingStatus.entries.forEach { status ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusChip(status = status)
                                Text(
                                    text = "${profile.statusCounts[status] ?: 0}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Ø Bewertung: ", style = MaterialTheme.typography.bodyMedium)
                                if (profile.avgRating != null) {
                                    Text(
                                        "%.1f".format(profile.avgRating!!),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = StarColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Text("–", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Ø Spice: ", style = MaterialTheme.typography.bodyMedium)
                                if (profile.avgSpice != null) {
                                    Text(
                                        "%.1f / 5 ".format(profile.avgSpice!!),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    SpiceFlames(level = profile.avgSpice!!.toInt().coerceIn(1, 5), iconSize = 14)
                                } else {
                                    Text("–", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
            item {
                SectionHeader("Top-Genres")
                ChipList(profile.topGenres.map { "${it.first} (${it.second})" })
            }
            item {
                SectionHeader("Genre-Verteilung")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val max = profile.genreDistribution.maxOfOrNull { it.second } ?: 1
                        profile.genreDistribution.forEach { (genre, count) ->
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(genre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text("$count Pkt.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { count.toFloat() / max },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }
                    }
                }
            }
            item {
                SectionHeader("Top-Tropes & Motive")
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
            if (profile.dislikedGenres.isNotEmpty() || profile.dislikedAuthors.isNotEmpty() || profile.dislikedTropes.isNotEmpty()) {
                item {
                    SectionHeader("Weniger beliebt (Abbrüche & 1–2 Sterne)")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (profile.abandonedCount > 0) {
                                Text(
                                    "Abgebrochene Bücher: ${profile.abandonedCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (profile.dislikedGenres.isNotEmpty()) {
                                Text("Genres:", style = MaterialTheme.typography.labelMedium)
                                ChipList(profile.dislikedGenres.map { it.first })
                            }
                            if (profile.dislikedAuthors.isNotEmpty()) {
                                Text("Autor:innen:", style = MaterialTheme.typography.labelMedium)
                                ChipList(profile.dislikedAuthors.map { it.first })
                            }
                            if (profile.dislikedTropes.isNotEmpty()) {
                                Text("Tropes:", style = MaterialTheme.typography.labelMedium)
                                ChipList(profile.dislikedTropes.map { it.first })
                            }
                        }
                    }
                }
            }
            item {
                SectionHeader("Ehrenregal: Lieblingsbücher (4–5 Sterne)")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (profile.favorites.isEmpty()) {
                            Text(
                                "Noch keine – bewerte gelesene Bücher mit 4 oder 5 Sternen, damit sie ins Ehrenregal aufgenommen werden.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            profile.favorites.forEach { book ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = book.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontFamily = PlayfairDisplay,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (book.author.isNotBlank()) {
                                            Text(
                                                text = book.author,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${book.rating} ★",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = StarColor,
                                        fontWeight = FontWeight.Bold
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
            AssistChip(
                onClick = {},
                label = { Text(item) },
                shape = CircleShape
            )
        }
    }
}

