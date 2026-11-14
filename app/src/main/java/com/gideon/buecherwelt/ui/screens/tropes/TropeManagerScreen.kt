package com.gideon.buecherwelt.ui.screens.tropes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gideon.buecherwelt.di.AppContainer
import com.gideon.buecherwelt.domain.model.Trope
import com.gideon.buecherwelt.domain.repository.TropeRepository
import com.gideon.buecherwelt.ui.viewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TropeManagerScreen(
    container: AppContainer,
    navController: NavController
) {
    val tropeRepository: TropeRepository = container.tropeRepository
    val scope = rememberCoroutineScope()
    val tropes by tropeRepository.observeAll().collectAsStateWithLifecycle(emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<Trope?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tropes verwalten") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Trope hinzufügen")
            }
        }
    ) { padding ->
        val grouped = tropes.groupBy { it.category }
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            grouped.forEach { (category, items) ->
                item {
                    Text(
                        category,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                items(items, key = { it.id }) { trope ->
                    ListItem(
                        headlineContent = { Text(trope.name) },
                        supportingContent = {
                            Text(if (trope.isCustom) "eigene Trope" else "Katalog")
                        },
                        trailingContent = {
                            if (trope.isCustom) {
                                IconButton(onClick = { deleteCandidate = trope }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Löschen")
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Eigen") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Eigene Trope anlegen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategorie") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            tropeRepository.getOrCreateCustom(name.trim(), category.trim().ifBlank { "Eigen" })
                        }
                        showAddDialog = false
                    },
                    enabled = name.isNotBlank()
                ) { Text("Anlegen") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    deleteCandidate?.let { trope ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Trope löschen?") },
            text = { Text("„${trope.name}“ wird aus dem Katalog entfernt (bei Büchern bleiben bestehende Zuordnungen unberührt).") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { tropeRepository.delete(trope) }
                    deleteCandidate = null
                }) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("Abbrechen") }
            }
        )
    }
}
