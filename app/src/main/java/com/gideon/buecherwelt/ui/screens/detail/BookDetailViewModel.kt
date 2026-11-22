package com.gideon.buecherwelt.ui.screens.detail

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gideon.buecherwelt.data.local.ReadingStatus
import com.gideon.buecherwelt.data.network.BookMetadata
import com.gideon.buecherwelt.data.network.BookMetadataFetcher
import com.gideon.buecherwelt.data.network.CoverCache
import com.gideon.buecherwelt.data.settings.LlmConfig
import com.gideon.buecherwelt.data.settings.SettingsRepository
import com.gideon.buecherwelt.domain.analysis.ReadingStatsCalculator
import com.gideon.buecherwelt.domain.llm.LlmService
import com.gideon.buecherwelt.domain.model.Book
import com.gideon.buecherwelt.domain.model.Trope
import com.gideon.buecherwelt.domain.recommendation.RecommendationEngine
import com.gideon.buecherwelt.domain.recommendation.ScoredCandidate
import com.gideon.buecherwelt.domain.repository.BookRepository
import com.gideon.buecherwelt.domain.repository.TropeRepository
import com.gideon.buecherwelt.domain.usecase.BookEnrichmentService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BookDetailViewModel(
    private val bookRepository: BookRepository,
    private val tropeRepository: TropeRepository,
    private val metadataFetcher: BookMetadataFetcher,
    private val coverCache: CoverCache,
    private val enrichmentService: BookEnrichmentService,
    private val recommendationEngine: RecommendationEngine,
    private val llmService: LlmService,
    settingsRepository: SettingsRepository,
    private val bookId: Long
) : ViewModel() {

    val book: StateFlow<Book?> = bookRepository.observeBook(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allTropes: StateFlow<List<Trope>> = tropeRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val seriesNames: StateFlow<List<String>> = bookRepository.observeSeriesNames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val universeNames: StateFlow<List<String>> = bookRepository.observeUniverseNames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localFit: StateFlow<Pair<Int, String>?> = combine(book, bookRepository.observeBooks()) { current, all ->
        if (current == null) null
        else {
            val profile = ReadingStatsCalculator.compute(all.filter { it.id != current.id })
            ReadingStatsCalculator.computeLocalFit(current, profile, all)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val localSimilarBooks: StateFlow<List<com.gideon.buecherwelt.domain.analysis.SimilarLocalBook>> =
        combine(book, bookRepository.observeBooks()) { current, all ->
            if (current == null) emptyList()
            else com.gideon.buecherwelt.domain.analysis.SmartLibraryEngine.findSimilarInLibrary(current, all)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val characteristics: StateFlow<ReadingStatsCalculator.BookCharacteristics?> = combine(
        book,
        allTropes,
        seriesNames,
        universeNames
    ) { current, catalog, sNames, uNames ->
        if (current == null) null
        else ReadingStatsCalculator.analyzeBookCharacteristics(
            book = current,
            catalog = catalog,
            existingSeries = sNames,
            existingUniverses = uNames
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val seriesBooks: StateFlow<List<Book>> = combine(book, bookRepository.observeBooks()) { current, all ->
        if (current == null || current.seriesName.isBlank()) {
            emptyList()
        } else {
            all.filter { it.id != current.id && it.seriesName.equals(current.seriesName, ignoreCase = true) }
                .sortedWith(
                    compareBy(
                        { it.seriesIndex ?: Int.MAX_VALUE },
                        { it.publishedYear ?: Int.MAX_VALUE },
                        { it.title.lowercase() }
                    )
                )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val universeBooks: StateFlow<List<Book>> = combine(book, bookRepository.observeBooks()) { current, all ->
        if (current == null || current.universeName.isBlank()) {
            emptyList()
        } else {
            all.filter {
                it.id != current.id &&
                    it.universeName.equals(current.universeName, ignoreCase = true)
            }.sortedWith(
                compareBy(
                    { it.seriesName.ifBlank { "zzz" }.lowercase() },
                    { it.seriesIndex ?: Int.MAX_VALUE },
                    { it.publishedYear ?: Int.MAX_VALUE },
                    { it.title.lowercase() }
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _coverCandidates = MutableStateFlow<List<BookMetadata>>(emptyList())
    val coverCandidates: StateFlow<List<BookMetadata>> = _coverCandidates.asStateFlow()

    private val _coverCandidatesLoading = MutableStateFlow(false)
    val coverCandidatesLoading: StateFlow<Boolean> = _coverCandidatesLoading.asStateFlow()

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
        // Automatische Anreicherung beim Öffnen, falls Cover, Reihe/Universum, Beschreibung oder Tropes fehlen
        viewModelScope.launch {
            val initial = book.filterNotNull().first()
            val needsCover = !CoverCache.isValidCoverFile(initial.coverPath)
            val needsDetails = initial.description.isBlank() ||
                initial.tropes.isEmpty() ||
                (initial.seriesName.isBlank() && initial.universeName.isBlank())
            if (needsCover || needsDetails) {
                _refreshing.value = true
                try {
                    enrichmentService.refreshBook(initial.id, forceCoverRefresh = needsCover)
                } finally {
                    _refreshing.value = false
                }
            }
        }
    }

    fun refreshCoverAndDetails(onDone: (String) -> Unit = {}) {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            try {
                val result = enrichmentService.refreshBook(bookId, forceCoverRefresh = true)
                result.fold(
                    onSuccess = { updated ->
                        val hasCover = CoverCache.isValidCoverFile(updated.coverPath) || !updated.coverUrl.isNullOrBlank()
                        onDone(
                            if (hasCover) "Cover & Buchdetails wurden aktualisiert."
                            else "Buchdetails aktualisiert (kein Online-Cover gefunden)."
                        )
                    },
                    onFailure = { onDone("Aktualisierung fehlgeschlagen: ${it.message ?: "Unbekannter Fehler"}") }
                )
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun loadCoverCandidates() {
        val current = book.value ?: return
        viewModelScope.launch {
            _coverCandidatesLoading.value = true
            try {
                _coverCandidates.value = metadataFetcher.findCoverCandidates(
                    title = current.title,
                    author = current.author,
                    isbn = current.isbn
                )
            } finally {
                _coverCandidatesLoading.value = false
            }
        }
    }

    fun selectCoverCandidate(coverUrl: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            _refreshing.value = true
            try {
                val res = enrichmentService.applyCoverFromUrl(bookId, coverUrl)
                res.fold(
                    onSuccess = { onDone("Neues Cover gespeichert.") },
                    onFailure = { onDone("Dieses Cover konnte nicht geladen werden.") }
                )
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun selectLocalCover(contentResolver: ContentResolver, uri: Uri, onDone: (String) -> Unit) {
        val current = book.value ?: return
        viewModelScope.launch {
            _refreshing.value = true
            try {
                val key = "${CoverCache.keyFor(current.isbn, current.title, current.author)}_${current.id}"
                val path = coverCache.savePicked(contentResolver, uri, key)
                if (path != null) {
                    val updated = current.copy(
                        coverPath = path,
                        updatedAt = System.currentTimeMillis()
                    )
                    bookRepository.update(updated, current.tropes.map { it.id }.toSet())
                    onDone("Eigenes Cover gespeichert.")
                } else {
                    onDone("Bild konnte nicht geladen werden.")
                }
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun setSeriesAndUniverse(seriesName: String, seriesIndex: Int?, universeName: String) {
        viewModelScope.launch {
            val current = book.value ?: return@launch
            val updated = current.copy(
                seriesName = seriesName.trim(),
                seriesIndex = seriesIndex,
                universeName = universeName.trim(),
                updatedAt = System.currentTimeMillis()
            )
            bookRepository.update(updated, current.tropes.map { it.id }.toSet())
        }
    }

    fun applyDetectedSeriesUniverse() {
        val current = book.value ?: return
        val detected = characteristics.value?.detectedSeriesUniverse ?: return
        if (!detected.hasAny) return
        setSeriesAndUniverse(
            seriesName = current.seriesName.ifBlank { detected.seriesName },
            seriesIndex = current.seriesIndex ?: detected.seriesIndex,
            universeName = current.universeName.ifBlank { detected.universeName }
        )
    }

    fun addSuggestedTrope(tropeName: String) {
        viewModelScope.launch {
            val current = book.value ?: return@launch
            val currentNames = current.tropes.map { it.name }.toSet()
            val newIds = tropeRepository.resolveIds(currentNames + tropeName)
            bookRepository.setTropes(current.id, newIds)
        }
    }

    fun applyAllSuggestedTropes(tropeNames: List<String>) {
        viewModelScope.launch {
            val current = book.value ?: return@launch
            val currentNames = current.tropes.map { it.name }.toSet()
            val newIds = tropeRepository.resolveIds(currentNames + tropeNames)
            bookRepository.setTropes(current.id, newIds)
        }
    }

    fun updateTropes(selectedNames: Set<String>) {
        viewModelScope.launch {
            val current = book.value ?: return@launch
            val newIds = tropeRepository.resolveIds(selectedNames)
            bookRepository.setTropes(current.id, newIds)
        }
    }

    fun setStatus(newStatus: ReadingStatus) {
        viewModelScope.launch {
            val current = book.value ?: return@launch
            if (current.status == newStatus) return@launch
            val updated = current.copy(
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )
            bookRepository.update(updated, current.tropes.map { it.id }.toSet())
        }
    }

    fun setRating(newRating: Int) {
        viewModelScope.launch {
            val current = book.value ?: return@launch
            val normalized = if (newRating <= 0) null else newRating.coerceIn(1, 5)
            val updated = current.copy(
                rating = normalized,
                updatedAt = System.currentTimeMillis()
            )
            bookRepository.update(updated, current.tropes.map { it.id }.toSet())
        }
    }

    fun setSpice(newSpice: Int) {
        viewModelScope.launch {
            val current = book.value ?: return@launch
            val normalized = newSpice.coerceIn(0, 5)
            val updated = current.copy(
                spiceLevel = normalized,
                updatedAt = System.currentTimeMillis()
            )
            bookRepository.update(updated, current.tropes.map { it.id }.toSet())
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
                    .mapNotNull { b ->
                        RecommendationEngine.keyOf(
                            BookMetadata(isbn = b.isbn, title = b.title, author = b.author)
                        )
                    }
                    .toSet() + RecommendationEngine.keyOf(
                    BookMetadata(isbn = current.isbn, title = current.title, author = current.author)
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
