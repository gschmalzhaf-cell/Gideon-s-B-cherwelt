package com.gideon.buecherwelt.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "settings")

data class LlmConfig(
    val apiKey: String = "",
    val baseUrl: String = "https://api.mistral.ai/v1",
    val model: String = "mistral-medium-latest"
) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()
}

class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LLM_API_KEY = stringPreferencesKey("llm_api_key")
        val LLM_BASE_URL = stringPreferencesKey("llm_base_url")
        val LLM_MODEL = stringPreferencesKey("llm_model")
    }

    val themeMode: Flow<String> =
        dataStore.data.map { it[Keys.THEME_MODE] ?: "system" }

    val llmConfig: Flow<LlmConfig> = combine(
        dataStore.data.map { it[Keys.LLM_API_KEY] ?: "" },
        dataStore.data.map { it[Keys.LLM_BASE_URL] ?: "https://api.mistral.ai/v1" },
        dataStore.data.map { it[Keys.LLM_MODEL] ?: "mistral-medium-latest" }
    ) { apiKey, baseUrl, model ->
        LlmConfig(apiKey = apiKey, baseUrl = baseUrl, model = model)
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setLlmConfig(config: LlmConfig) {
        dataStore.edit {
            it[Keys.LLM_API_KEY] = config.apiKey.trim()
            it[Keys.LLM_BASE_URL] = config.baseUrl.trim()
            it[Keys.LLM_MODEL] = config.model.trim()
        }
    }
}
