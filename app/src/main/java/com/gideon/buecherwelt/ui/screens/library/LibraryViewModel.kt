package com.gideon.buecherwelt.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gideon.buecherwelt.data.local.ReadingStatus
import com.gideon.buecherwelt.data.settings.LlmConfig
import com.gideon.buecherwelt.data.settings.SettingsRepository
import com.gideon.buecherwelt.domain.analysis.ReadingProfile
import com.gideon.buecherwelt.domain.analysis.ReadingStatsCalculator
import com.gideon.buecherwelt.domain.llm.LlmService
import com.gideon.buecherwelt.domain.model.Book
import com.gideon.buecherwelt.domain.repository.BookRepository
import com.gideon.buecherwelt.domain.repository.TropeRepository
import com.gideon.buecherwelt.domain.usecase.BookEnrichmentService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibrarySortOrder(val label: String) {
    RECENT("Zuletzt"),
    TITLE("Titel A–Z"),
    AUTHOR("Autor:in"),
    RATING("Bewertung ★"),
    SERIES("Reihe & Band"),
    FIT_SCORE("Profil-Match")
}

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val profile: ReadingProfile = ReadingProfile(),
    val query: String = "",
    val genre: String? = null,
    val status: ReadingStatus? = null,
    val minRating: Int = 0,
    val trope: String? = null,
    val series: String? = null,
    val universe: String? = null,
    val sortOrder: LibrarySortOrder = LibrarySortOrder.RECENT,
    val genres: List<String> = emptyList(),
    val tropes: List<String> = emptyList(),
    val seriesNames: List<String> = emptyList(),
    val universeNames: List<String> = emptyList(),
    val semanticQuery: String = "",
    val semanticActive: Boolean = false,
    val semanticLoading: Boolean = false,
    val semanticIds: Set<Long>? = null,
    val semanticExplanation: String? = null,
    val semanticError: String? = null,
    val llmConfigured: Boolean = false,
    val enriching: Boolean = false,
    val enrichProgress: Pair<Int, Int>? = null,
    val enrichMessage: String? = null
) {
    val readingBooks: List<Book>
        get() = books.filter { it.status == ReadingStatus.READING }

    val filtered: List<Book>
        get() {
            val ids = if (semanticActive) semanticIds else null
            val q = query.trim()
            val qIsbn = isbnComparable(q)
            val matched = books.filter { book ->
                val matchesQuery = q.isBlank() ||
                    book.title.contains(q, ignoreCase = true) ||
                    book.subtitle.contains(q, ignoreCase = true) ||
                    book.author.contains(q, ignoreCase = true) ||
                    book.seriesName.contains(q, ignoreCase = true) ||
                    book.universeName.contains(q, ignoreCase = true) ||
                    book.genre.contains(q, ignoreCase = true) ||
                    book.subgenre.contains(q, ignoreCase = true) ||
                    book.publisher.contains(q, ignoreCase = true) ||
                    book.notes.contains(q, ignoreCase = true) ||
                    book.description.contains(q, ignoreCase = true) ||
                    book.tropes.any { it.name.contains(q, ignoreCase = true) } ||
                    (qIsbn.isNotEmpty() && isbnComparable(book.isbn ?: "").contains(qIsbn))

                (ids == null || book.id in ids) &&
                    matchesQuery &&
                    (genre == null || book.genre == genre) &&
                    (status == null || book.status == status) &&
                    (minRating == 0 || (book.rating ?: 0) >= minRating) &&
                    (trope == null || book.tropes.any { it.name == trope }) &&
                    (series == null || book.seriesName.equals(series, ignoreCase = true)) &&
                    (universe == null || book.universeName.equals(universe, ignoreCase = true))
            }

            if (semanticActive && semanticIds != null) {
                val orderMap = semanticIds.withIndex().associate { it.value to it.index }
                return matched.sortedBy { orderMap[it.id] ?: Int.MAX_VALUE }
            }

            return when (sortOrder) {
                LibrarySortOrder.RECENT -> matched.sortedByDescending { maxOf(it.updatedAt, it.createdAt, it.id) }
                LibrarySortOrder.TITLE -> matched.sortedBy { it.title.lowercase() }
                LibrarySortOrder.AUTHOR -> matched.sortedWith(
                    compareBy<Book> { it.author.ifBlank { "zzz" }.lowercase() }
                        .thenBy { it.seriesName.lowercase() }
                        .thenBy { it.seriesIndex ?: Int.MAX_VALUE }
                        .thenBy { it.title.lowercase() }
                )
                LibrarySortOrder.RATING -> matched.sortedWith(
                    compareByDescending<Book> { it.rating ?: 0 }
                        .thenByDescending { it.aiFitScore ?: ReadingStatsCalculator.computeLocalFit(it, profile).first }
                        .thenBy { it.title.lowercase() }
                )
                LibrarySortOrder.SERIES -> matched.sortedWith(
                    compareBy<Book> {
                        it.seriesName.ifBlank { it.universeName.ifBlank { "zzz" } }.lowercase()
                    }
                        .thenBy { it.seriesIndex ?: Int.MAX_VALUE }
                        .thenBy { it.title.lowercase() }
                )
                LibrarySortOrder.FIT_SCORE -> matched.sortedWith(
                    compareByDescending<Book> {
                        it.aiFitScore ?: ReadingStatsCalculator.computeLocalFit(it, profile).first
                    }.thenByDescending { it.rating ?: 0 }
                )
            }
        }
}

class LibraryViewModel(
    private val bookRepository: BookRepository,
    tropeRepository: TropeRepository,
    private val llmService: LlmService,
    settingsRepository: SettingsRepository,
    private val bookEnrichmentService: BookEnrichmentService
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

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
                val profile = ReadingStatsCalculator.compute(books)
                _state.update {
                    it.copy(
                        books = books,
                        profile = profile,
                        genres = books.map { b -> b.genre }
                            .filter { g -> g.isNotBlank() }
                            .distinct()
                            .sorted(),
                        seriesNames = books.map { b -> b.seriesName }
                            .filter { s -> s.isNotBlank() }
                            .distinct()
                            .sortedBy { s -> s.lowercase() },
                        universeNames = books.map { b -> b.universeName }
                            .filter { u -> u.isNotBlank() }
                            .distinct()
                            .sortedBy { u -> u.lowercase() }
                    )
                }
            }
        }
        viewModelScope.launch {
            tropeRepository.observeAll().collect { all ->
                _state.update {
                    val fromBooks = it.books.flatMap { b -> b.tropes.map { t -> t.name } }
                    it.copy(tropes = (fromBooks + all.map { t -> t.name }).distinct().sorted())
                }
            }
        }
        viewModelScope.launch {
            bookEnrichmentService.ensureLibraryEnrichedInBackground()
        }
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }
    fun setGenre(genre: String?) = _state.update { it.copy(genre = genre) }
    fun setStatus(status: ReadingStatus?) = _state.update { it.copy(status = status) }
    fun setMinRating(rating: Int) = _state.update { it.copy(minRating = rating) }
    fun setTrope(trope: String?) = _state.update { it.copy(trope = trope) }
    fun setSeries(series: String?) = _state.update { it.copy(series = series) }
    fun setUniverse(universe: String?) = _state.update { it.copy(universe = universe) }
    fun setSortOrder(order: LibrarySortOrder) = _state.update { it.copy(sortOrder = order) }

    fun setSemanticQuery(query: String) = _state.update { it.copy(semanticQuery = query) }

    fun refreshLibraryCoversAndDetails() {
        if (_state.value.enriching) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    enriching = true,
                    enrichProgress = 0 to it.books.size,
                    enrichMessage = "Prüfe Cover, Reihen, Universen & Buchdetails…"
                )
            }
            val summary = bookEnrichmentService.refreshAllBooks(forceCovers = true) { done, total, _ ->
                _state.update { it.copy(enrichProgress = done to total) }
            }
            val msg = buildString {
                append("${summary.total} Bücher geprüft")
                if (summary.coversUpdated > 0) append(" · ${summary.coversUpdated} Cover geladen")
                if (summary.seriesOrUniverseDetected > 0) append(" · ${summary.seriesOrUniverseDetected} Reihen/Universen erkannt")
                if (summary.detailsUpdated > 0) append(" · ${summary.detailsUpdated} Details ergänzt")
            }
            _state.update {
                it.copy(
                    enriching = false,
                    enrichProgress = null,
                    enrichMessage = msg
                )
            }
        }
    }

    fun dismissEnrichMessage() = _state.update { it.copy(enrichMessage = null) }

    fun runSemanticSearch() {
        val query = state.value.semanticQuery.trim()
        if (query.isEmpty()) return
        if (!llmConfig.isConfigured) {
            _state.update {
                it.copy(semanticError = "Kein LLM-API-Key konfiguriert (Einstellungen).")
            }
            return
        }
        viewModelScope.launch {
            _state.update {
                it.copy(semanticLoading = true, semanticError = null, semanticActive = false)
            }
            val books = bookRepository.getAllBooks()
            val result = llmService.semanticSearch(llmConfig, query, books)
            _state.update {
                if (result == null) {
                    it.copy(
                        semanticLoading = false,
                        semanticError = "KI-Suche fehlgeschlagen (offline oder API-Fehler)."
                    )
                } else {
                    it.copy(
                        semanticLoading = false,
                        semanticActive = true,
                        semanticIds = result.ids.toSet(),
                        semanticExplanation = result.explanation.ifBlank { null },
                        semanticError = null
                    )
                }
            }
        }
    }

    fun clearSemantic() = _state.update {
        it.copy(
            semanticQuery = "",
            semanticActive = false,
            semanticLoading = false,
            semanticIds = null,
            semanticExplanation = null,
            semanticError = null
        )
    }
}

private fun isbnComparable(s: String): String =
    s.uppercase().filter { it != '-' && !it.isWhitespace() }


