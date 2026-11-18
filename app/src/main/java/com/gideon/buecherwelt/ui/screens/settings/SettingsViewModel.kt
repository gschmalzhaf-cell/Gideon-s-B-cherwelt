package com.gideon.buecherwelt.ui.screens.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Database
import com.gideon.buecherwelt.BuildConfig
import com.gideon.buecherwelt.data.local.AppDatabase
import com.gideon.buecherwelt.data.settings.LlmConfig
import com.gideon.buecherwelt.data.settings.SettingsRepository
import com.gideon.buecherwelt.domain.analysis.ReadingStatsCalculator
import com.gideon.buecherwelt.domain.report.AppReportGenerator
import com.gideon.buecherwelt.domain.repository.BookRepository
import com.gideon.buecherwelt.domain.repository.TropeRepository
import com.gideon.buecherwelt.domain.repository.WishlistRepository
import com.gideon.buecherwelt.domain.usecase.BookEnrichmentService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val bookRepository: BookRepository,
    private val tropeRepository: TropeRepository,
    private val wishlistRepository: WishlistRepository,
    private val bookEnrichmentService: BookEnrichmentService
) : ViewModel() {

    val themeMode: StateFlow<String> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val llmConfig: StateFlow<LlmConfig> = settingsRepository.llmConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LlmConfig())

    private val _report = MutableStateFlow<String?>(null)
    val report: StateFlow<String?> = _report.asStateFlow()

    private val _reportLoading = MutableStateFlow(false)
    val reportLoading: StateFlow<Boolean> = _reportLoading.asStateFlow()

    private val _enriching = MutableStateFlow(false)
    val enriching: StateFlow<Boolean> = _enriching.asStateFlow()

    private val _enrichStatus = MutableStateFlow<String?>(null)
    val enrichStatus: StateFlow<String?> = _enrichStatus.asStateFlow()

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun saveLlmConfig(config: LlmConfig) {
        viewModelScope.launch { settingsRepository.setLlmConfig(config) }
    }

    fun refreshAllCoversAndDetails() {
        if (_enriching.value) return
        viewModelScope.launch {
            _enriching.value = true
            _enrichStatus.value = "Starte Abgleich aller Bücher…"
            try {
                val summary = bookEnrichmentService.refreshAllBooks(forceCovers = true) { done, total, currentTitle ->
                    _enrichStatus.value = "Prüfe Bücher ($done/$total): $currentTitle…"
                }
                _enrichStatus.value = buildString {
                    append("${summary.total} Bücher geprüft")
                    append(" · ${summary.coversUpdated} Cover geladen")
                    append(" · ${summary.seriesOrUniverseDetected} Reihen/Universen erkannt")
                    append(" · ${summary.detailsUpdated} Details/Tropes aktualisiert.")
                }
            } finally {
                _enriching.value = false
            }
        }
    }

    fun buildReport() {
        if (_reportLoading.value) return
        viewModelScope.launch {
            _reportLoading.value = true
            try {
                val books = bookRepository.getAllBooks()
                val tropes = tropeRepository.getAll()
                val wishlistCount = wishlistRepository.observeAll().first().size
                val profile = ReadingStatsCalculator.compute(books)
                val llm = settingsRepository.llmConfig.first()
                val theme = settingsRepository.themeMode.first()
                val env = AppReportGenerator.Env(
                    appVersionName = BuildConfig.VERSION_NAME,
                    appVersionCode = BuildConfig.VERSION_CODE,
                    androidRelease = Build.VERSION.RELEASE ?: "?",
                    androidSdk = Build.VERSION.SDK_INT,
                    deviceManufacturer = Build.MANUFACTURER ?: "?",
                    deviceModel = Build.MODEL ?: "?",
                    locale = Locale.getDefault().toString(),
                    dbVersion = AppDatabase::class.java.getAnnotation(Database::class.java)?.version ?: -1,
                    themeMode = theme,
                    llmConfigured = llm.isConfigured,
                    llmModel = llm.model,
                    llmBaseUrl = llm.baseUrl
                )
                _report.value = AppReportGenerator.generate(
                    env = env,
                    books = books,
                    tropeCount = tropes.size,
                    customTropeCount = tropes.count { it.isCustom },
                    wishlistCount = wishlistCount,
                    profile = profile
                )
            } finally {
                _reportLoading.value = false
            }
        }
    }

    fun dismissReport() {
        _report.value = null
    }
}
