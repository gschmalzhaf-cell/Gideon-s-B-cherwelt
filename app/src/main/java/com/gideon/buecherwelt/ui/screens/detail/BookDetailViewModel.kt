package com.gideon.buecherwelt.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gideon.buecherwelt.data.settings.LlmConfig
import com.gideon.buecherwelt.data.settings.SettingsRepository
import com.gideon.buecherwelt.domain.analysis.ReadingStatsCalculator
import com.gideon.buecherwelt.domain.llm.LlmService
import com.gideon.buecherwelt.domain.model.Book
import com.gideon.buecherwelt.domain.recommendation.RecommendationEngine
import com.gideon.buecherwelt.domain.recommendation.ScoredCandidate
import com.gideon.buecherwelt.domain.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BookDetailViewModel(
    private val bookRepository: BookRepository,
    private val recommendationEngine: RecommendationEngine,
    private val llmService: LlmService,
    settingsRepository: SettingsRepository,
    private val bookId: Long
) : ViewModel() {

    val book: StateFlow<Book?> = bookRepository.observeBook(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _llmConfigured = MutableStateFlow(false)
    val llmConfigured: StateFlow<Boolean> = _llmConfigured.asStateFlow()

    private val _insightsLoading = MutableStateFlow(false)
    val insightsLoading: StateFlow<Boolean> = _insightsLoading.asStateFlow()

    private val _insightsError = MutableStateFlow<String?>(null)
    val insightsError: StateFlow<String?> = _insightsError.asStateFlow()

    private var llmConfig: LlmConfig = LlmConfig()

    init {
        viewModelScope.launch {
            settingsRepository.llmConfig.collect { config ->
                llmConfig = config
                _llmConfigured.update { config.isConfigured }
            }
        }
    }

    fun loadInsights(force: Boolean = false) {
        viewModelScope.launch {
            val current = book.value ?: return@launch
            if (!force && current.aiSummary != null) return@launch
            if (!llmConfig.isConfigured) {
                if (force) _insightsError.value = "Kein LLM-API-Key konfiguriert (Einstellungen)."
                return@launch
            }
            _insightsLoading.value = true
            _insightsError.value = null
            try {
                val profile = ReadingStatsCalculator.compute(bookRepository.getAllBooks())
                val insights = llmService.bookInsights(llmConfig, current, profile)
                if (insights == null) {
                    _insightsError.value = "KI-Analyse fehlgeschlagen (offline oder API-Fehler)."
                } else {
                    bookRepository.saveAiInsights(
                        current.id,
                        insights.summary,
                        insights.fitScore,
                        insights.fitReason
                    )
                }
            } catch (e: Exception) {
                _insightsError.value = "KI-Analyse fehlgeschlagen."
            } finally {
                _insightsLoading.value = false
            }
        }
    }

    private val _similar = MutableStateFlow<List<ScoredCandidate>>(emptyList())
    val similar: StateFlow<List<ScoredCandidate>> = _similar.asStateFlow()

    private val _similarLoading = MutableStateFlow(false)
    val similarLoading: StateFlow<Boolean> = _similarLoading.asStateFlow()

    private val _similarError = MutableStateFlow<String?>(null)
    val similarError: StateFlow<String?> = _similarError.asStateFlow()

    fun loadSimilar() {
        viewModelScope.launch {
            val current = book.value ?: return@launch
            _similarLoading.value = true
            _similarError.value = null
            try {
                val exclude = bookRepository.getAllBooks()
                    .mapNotNull { b -> RecommendationEngine.keyOf(
                        com.gideon.buecherwelt.data.network.BookMetadata(
                            isbn = b.isbn, title = b.title, author = b.author
                        )
                    ) }
                    .toSet() + RecommendationEngine.keyOf(
                        com.gideon.buecherwelt.data.network.BookMetadata(
                            isbn = current.isbn, title = current.title, author = current.author
                        )
                    )
                _similar.value = recommendationEngine.similarTo(current, exclude)
            } catch (e: Exception) {
                _similarError.value = "Ähnliche Bücher konnten nicht geladen werden (offline?)."
            } finally {
                _similarLoading.value = false
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            book.value?.let { bookRepository.delete(it) }
            onDone()
        }
    }
}
