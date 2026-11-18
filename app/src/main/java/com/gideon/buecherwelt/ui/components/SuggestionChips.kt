package com.gideon.buecherwelt.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gideon.buecherwelt.domain.llm.TropeSuggestion

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuggestionChips(
    suggestions: TropeSuggestion,
    selectedTropes: Set<String>,
    onTropeToggle: (String) -> Unit,
    selectedSpice: Int?,
    onSpiceSelected: (Int) -> Unit,
    selectedGenres: Set<String>,
    onGenreToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Intelligente Vorschläge – antippen zum Übernehmen",
                style = MaterialTheme.typography.labelLarge
            )
            if (suggestions.tropes.isNotEmpty()) {
                Text(
                    text = "Tropes",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.tropes.forEach { trope ->
                        FilterChip(
                            selected = trope in selectedTropes,
                            onClick = { onTropeToggle(trope) },
                            label = { Text(trope) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
            if (suggestions.genres.isNotEmpty()) {
                Text(
                    text = "Genre",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.genres.forEach { genre ->
                        FilterChip(
                            selected = genre in selectedGenres,
                            onClick = { onGenreToggle(genre) },
                            label = { Text(genre) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
            if (suggestions.spice != null) {
                Text(
                    text = "Spice-Level",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedSpice == suggestions.spice,
                        onClick = { onSpiceSelected(suggestions.spice!!) },
                        label = { Text("Spice ${suggestions.spice}") },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
    }
}
