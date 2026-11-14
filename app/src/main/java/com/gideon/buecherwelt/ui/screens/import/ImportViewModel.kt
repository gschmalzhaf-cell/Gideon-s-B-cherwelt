package com.gideon.buecherwelt.ui.screens.import

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gideon.buecherwelt.data.csv.StoryGraphCsvImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImportUiState(
    val running: Boolean = false,
    val progress: StoryGraphCsvImporter.Progress? = null,
    val summary: StoryGraphCsvImporter.Summary? = null
)

class ImportViewModel(
    private val importer: StoryGraphCsvImporter
) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    fun import(uri: Uri) {
        viewModelScope.launch {
            _state.update { ImportUiState(running = true) }
            val summary = importer.import(uri) { progress ->
                _state.update { it.copy(progress = progress) }
            }
            _state.update { ImportUiState(running = false, summary = summary) }
        }
    }
}
