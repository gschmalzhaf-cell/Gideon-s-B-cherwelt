package com.gideon.buecherwelt.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gideon.buecherwelt.data.settings.LlmConfig
import com.gideon.buecherwelt.di.AppContainer
import com.gideon.buecherwelt.ui.components.SectionHeader
import com.gideon.buecherwelt.ui.navigation.Screen
import com.gideon.buecherwelt.ui.viewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    navController: NavController
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory { SettingsViewModel(container.settingsRepository) }
    )
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val llmConfig by viewModel.llmConfig.collectAsStateWithLifecycle()

    var apiKey by remember(llmConfig) { mutableStateOf(llmConfig.apiKey) }
    var baseUrl by remember(llmConfig) { mutableStateOf(llmConfig.baseUrl) }
    var model by remember(llmConfig) { mutableStateOf(llmConfig.model) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Einstellungen") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            SectionHeader("Darstellung", modifier = Modifier.padding(top = 0.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("system" to "System", "light" to "Hell", "dark" to "Dunkel").forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = themeMode == value,
                        onClick = { viewModel.setThemeMode(value) },
                        shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index, 3)
                    ) {
                        Text(label)
                    }
                }
            }

            SectionHeader("LLM (Tropes-Vorschläge & Empfehlungen)")
            Text(
                "API-Key wird nur lokal auf diesem Gerät gespeichert und nur für LLM-Aufrufe verwendet. Ohne Key funktionieren alle anderen Funktionen offline.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API-Key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("API-Basis-URL") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Modell") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            Button(
                onClick = {
                    viewModel.saveLlmConfig(LlmConfig(apiKey = apiKey, baseUrl = baseUrl, model = model))
                    savedMessage = "LLM-Konfiguration gespeichert."
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text("LLM-Konfiguration speichern")
            }
            if (savedMessage != null) {
                Text(
                    savedMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            SectionHeader("Über", modifier = Modifier.padding(top = 0.dp))
            Text(
                "Gideon's Bücherwelt – persönliche Bibliothek, 100 % lokal (Room-Datenbank). Netzwerk wird nur für ISBN-Abfragen, Cover-Downloads und optionale LLM-Aufrufe genutzt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
