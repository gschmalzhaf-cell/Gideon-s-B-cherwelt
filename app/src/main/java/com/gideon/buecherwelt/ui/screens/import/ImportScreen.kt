package com.gideon.buecherwelt.ui.screens.import

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.gideon.buecherwelt.ui.components.SectionHeader
import com.gideon.buecherwelt.ui.viewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    container: AppContainer,
    navController: NavController
) {
    val viewModel: ImportViewModel = viewModel(
        factory = viewModelFactory { ImportViewModel(container.csvImporter) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.import(it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("StoryGraph-Import") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Wähle deine StoryGraph-CSV-Exportdatei. Titel, Autor:in, Lesestatus, Bewertung und eigene Tags werden übernommen; Buchdaten (Cover, Verlag, Klappentext, ISBN) werden – falls vorhanden – per ISBN über Google Books / Open Library nachgeladen. Duplikate werden übersprungen.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = { picker.launch("*/*") },
                enabled = !state.running,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CSV-Datei auswählen")
            }
            if (state.running) {
                val progress = state.progress
                if (progress != null) {
                    Text("Import läuft: ${progress.done}/${progress.total} – ${progress.currentTitle}")
                    LinearProgressIndicator(
                        progress = progress.done.toFloat() / progress.total.coerceAtLeast(1),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Importiert: ${progress.imported}, übersprungen: ${progress.skipped}")
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            state.summary?.let { summary ->
                SectionHeader("Ergebnis", modifier = Modifier.padding(top = 8.dp))
                Text("Importiert: ${summary.imported} – Übersprungen: ${summary.skipped}")
                summary.messages.take(30).forEach { message ->
                    Text("• $message", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
