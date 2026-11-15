package com.gideon.buecherwelt.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gideon.buecherwelt.data.local.ReadingStatus

@Composable
fun FilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    genres: List<String>,
    selectedGenre: String?,
    onGenreChange: (String?) -> Unit,
    selectedStatus: ReadingStatus?,
    onStatusChange: (ReadingStatus?) -> Unit,
    minRating: Int,
    onMinRatingChange: (Int) -> Unit,
    tropes: List<String>,
    selectedTrope: String?,
    onTropeChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Titel, Autor:in oder ISBN suchen") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(top = 8.dp)
                .fillMaxWidth()
        ) {
            FilterDropdown(
                label = "Genre",
                options = genres,
                selected = selectedGenre,
                onSelected = onGenreChange,
                display = { it }
            )
            FilterDropdown(
                label = "Status",
                options = ReadingStatus.entries.toList(),
                selected = selectedStatus,
                onSelected = onStatusChange,
                display = { it.label }
            )
            FilterDropdown(
                label = "Bewertung",
                options = listOf(5, 4, 3, 2, 1),
                selected = if (minRating == 0) null else minRating,
                onSelected = { onMinRatingChange(it ?: 0) },
                display = { "$it+ Sterne" }
            )
            FilterDropdown(
                label = "Trope",
                options = tropes,
                selected = selectedTrope,
                onSelected = onTropeChange,
                display = { it }
            )
        }
    }
}

@Composable
private fun <T> FilterDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    onSelected: (T?) -> Unit,
    display: (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            shape = CircleShape,
            label = {
                Text(
                    text = selected?.let { display(it) } ?: label,
                    style = MaterialTheme.typography.labelMedium
                )
            },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Alle") },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(display(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
