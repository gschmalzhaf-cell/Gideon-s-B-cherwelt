package com.gideon.buecherwelt.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gideon.buecherwelt.domain.model.Trope

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TropePicker(
    allTropes: List<Trope>,
    selected: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val suggestions = remember(query, allTropes) {
        if (query.isBlank()) {
            emptyList()
        } else {
            allTropes.filter { it.name.contains(query.trim(), ignoreCase = true) }.take(15)
        }
    }
    Column(modifier = modifier) {
        if (selected.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                selected.forEach { name ->
                    FilterChip(
                        selected = true,
                        onClick = { onSelectionChange(selected - name) },
                        label = { Text(name) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Entfernen", modifier = Modifier.size(14.dp))
                        }
                    )
                }
            }
        }
        AppTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Trope suchen oder neu anlegen") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = {
                        onSelectionChange(selected + query.trim())
                        query = ""
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Eigene Trope hinzufügen")
                    }
                }
            }
        )
        if (suggestions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .weight(1f, fill = false)
            ) {
                suggestions.forEach { trope ->
                    val isSelected = trope.name in selected
                    ListItem(
                        headlineContent = { Text(trope.name) },
                        supportingContent = {
                            Text(
                                trope.category + if (trope.isCustom) " (eigen)" else "",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        trailingContent = {
                            if (isSelected) Icon(Icons.Default.Check, contentDescription = null)
                        },
                        modifier = Modifier.clickable(enabled = !isSelected) {
                            onSelectionChange(selected + trope.name)
                        }
                    )
                }
            }
        }
    }
}
