package com.gideon.buecherwelt.ui.screens.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gideon.buecherwelt.data.network.BookMetadata
import com.gideon.buecherwelt.data.network.BookMetadataFetcher
import com.gideon.buecherwelt.data.network.FetchResult
import com.gideon.buecherwelt.data.settings.LlmConfig
import com.gideon.buecherwelt.data.settings.SettingsRepository
import com.gideon.buecherwelt.domain.analysis.ReadingProfile
import com.gideon.buecherwelt.domain.analysis.ReadingStatsCalculator
import com.gideon.buecherwelt.domain.llm.LlmService
import com.gideon.buecherwelt.domain.model.WishlistItem
import com.gideon.buecherwelt.domain.recommendation.RecommendationEngine
import com.gideon.buecherwelt.domain.recommendation.ScoredCandidate
import com.gideon.buecherwelt.domain.repository.BookRepository
import com.gideon.buecherwelt.domain.repository.WishlistRepository
import com.gideon.buecherwelt.domain.usecase.AddToLibraryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val ruleCandidates: List<ScoredCandidate> = emptyList(),
    val llmCandidates: List<ScoredCandidate> = emptyList(),
    val wishlist: List<WishlistItem> = emptyList(),
    val profile: ReadingProfile = ReadingProfile(),
    val ruleLoading: Boolean = false,
    val llmLoading: Boolean = false,
    val llmConfigured: Boolean = false,
    val reranked: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class DiscoverViewModel(
    private val bookRepository: BookRepository,
    private val wishlistRepository: WishlistRepository,
    private val recommendationEngine: RecommendationEngine,
    private val metadataFetcher: BookMetadataFetcher,
    private val llmService: LlmService,
    private val addToLibrary: AddToLibraryUseCase,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = _state.asStateFlow()

    private var llmConfig: LlmConfig = LlmConfig()

    init {
        viewModelScope.launch {
            settingsRepository.llmConfig.collect { config ->
                llmConfig = config
                _state.update { it.copy(llmConfigured = config.isConfigured) }
            }
        }
        viewModelScope.launch {
            bookRepository.observeBooks().collect { books ->
                _state.update { it.copy(profile = ReadingStatsCalculator.compute(books)) }
            }
        }
        viewModelScope.launch {
            wishlistRepository.observeAll().collect { items ->
                _state.update { it.copy(wishlist = items) }
            }
        }
    }

    fun dismissMessages() = _state.update { it.copy(error = null, message = null) }

    fun loadRuleBased() {
        viewModelScope.launch {
            _state.update { it.copy(ruleLoading = true, error = null) }
            val all = bookRepository.getAllBooks()
            val exclude = all.map { b ->
                RecommendationEngine.keyOf(BookMetadata(isbn = b.isbn, title = b.title, author = b.author))
            }.toSet()
            val profile = ReadingStatsCalculator.compute(all)
            val candidates = recommendationEngine.candidatesForProfile(profile, exclude)
            var finalCandidates = candidates
            var reranked = false
            if (llmConfig.isConfigured && candidates.isNotEmpty()) {
                val top = candidates.take(12)
                val ranked = llmService.rerankCandidates(llmConfig, profile, top)
                if (ranked != null) {
                    finalCandidates = ranked + candidates.drop(top.size)
                    reranked = true
                }
            }
            _state.update {
                it.copy(
                    ruleLoading = false,
                    ruleCandidates = finalCandidates,
                    reranked = reranked,
                    error = if (finalCandidates.isEmpty()) {
                        "Keine Empfehlungen gefunden (offline oder keine Treffer)."
                    } else null
                )
            }
        }
    }

    fun loadLlmRecommendations() {
        if (!llmConfig.isConfigured) {
            _state.update { it.copy(error = "Kein LLM-API-Key konfiguriert (Einstellungen).") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(llmLoading = true, error = null) }
            val profile = ReadingStatsCalculator.compute(bookRepository.getAllBooks())
            val recs = llmService.recommendBooks(llmConfig, profile)
            if (recs.isEmpty()) {
                _state.update {
                    it.copy(
                        llmLoading = false,
                        error = "Keine LLM-Empfehlungen erhalten (offline oder API-Fehler)."
                    )
                }
                return@launch
            }
            val resolved = recs.map { rec ->
                val fallbackReason = rec.reason.ifBlank { "LLM-Empfehlung basierend auf deinem Profil" }
                when (val result = metadataFetcher.findByTitleAuthor(rec.title, rec.author)) {
                    is FetchResult.Success -> ScoredCandidate(
                        metadata = result.metadata,
                        score = 5,
                        reasons = listOf(fallbackReason)
                    )
                    else -> ScoredCandidate(
                        metadata = BookMetadata(title = rec.title, author = rec.author),
                        score = 5,
                        reasons = listOf(fallbackReason)
                    )
                }
            }
            _state.update { it.copy(llmLoading = false, llmCandidates = resolved) }
        }
    }

    fun addToLibrary(candidate: ScoredCandidate) {
        viewModelScope.launch {
            val result = addToLibrary.addFromCandidate(candidate)
            _state.update {
                it.copy(
                    message = result.fold(
                        onSuccess = { "Zur Bibliothek hinzugefügt: ${candidate.metadata.title}" },
                        onFailure = { e -> e.message ?: "Konnte nicht hinzugefügt werden." }
                    )
                )
            }
        }
    }

    fun toggleWishlist(candidate: ScoredCandidate) {
        viewModelScope.launch {
            val meta = candidate.metadata
            val existing = state.value.wishlist.firstOrNull {
                (meta.isbn != null && it.isbn == meta.isbn) ||
                    it.title.equals(meta.title, ignoreCase = true)
            }
            if (existing != null) {
                wishlistRepository.remove(existing)
                _state.update { it.copy(message = "Von der Merkliste entfernt: ${meta.title}") }
            } else {
                wishlistRepository.add(
                    metadata = meta,
                    reason = candidate.reasons.firstOrNull().orEmpty(),
                    source = "Entdecken"
                )
                _state.update { it.copy(message = "Zur Merkliste hinzugefügt: ${meta.title}") }
            }
        }
    }

    fun removeFromWishlist(item: WishlistItem) {
        viewModelScope.launch { wishlistRepository.remove(item) }
    }
}
