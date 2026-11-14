package com.gideon.buecherwelt.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gideon.buecherwelt.data.settings.LlmConfig
import com.gideon.buecherwelt.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val themeMode: StateFlow<String> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val llmConfig: StateFlow<LlmConfig> = settingsRepository.llmConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LlmConfig())

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun saveLlmConfig(config: LlmConfig) {
        viewModelScope.launch { settingsRepository.setLlmConfig(config) }
    }
}
