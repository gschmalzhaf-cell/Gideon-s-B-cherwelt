package com.gideon.buecherwelt.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gideon.buecherwelt.data.local.ReadingStatus
import com.gideon.buecherwelt.data.settings.LlmConfig
import com.gideon.buecherwelt.data.settings.SettingsRepository
import com.gideon.buecherwelt.domain.llm.LlmService
import com.gideon.buecherwelt.domain.model.Book
import com.gideon.buecherwelt.domain.repository.BookRepository
import com.gideon.buecherwelt.domain.repository.TropeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val query: String = "",
    val genre: String? = null,
    val status: ReadingStatus? = null,
    val minRating: Int = 0,
    val trope: String? = null,
    val genres: List<String> = emptyList(),
    val tropes: List<String> = emptyList(),
    val semanticQuery: String = "",
    val semanticActive: Boolean = false,
    val semanticLoading: Boolean = false,
    val semanticIds: Set<Long>? = null,
    val semanticExplanation: String? = null,
    val semanticError: String? = null,
    val llmConfigured: Boolean = false
) {
    val filtered: List<Book>
        get() {
            val ids = if (semanticActive) semanticIds else null
            return books.filter { book ->
                (ids == null || book.id in ids) &&
                    (query.isBlank() ||
                        book.title.contains(query, ignoreCase = true) ||
                        book.author.contains(query, ignoreCase = true) ||
                        isbnComparable(book.isbn ?: "").contains(isbnComparable(query))) &&
                    (genre == null || book.genre == genre) &&
                    (status == null || book.status == status) &&
                    (minRating == 0 || (book.rating ?: 0) >= minRating) &&
                    (trope == null || book.tropes.any { it.name == trope })
            }
        }
}

class LibraryViewModel(
    private val bookRepository: BookRepository,
    tropeRepository: TropeRepository,
    private val llmService: LlmService,
    settingsRepository: SettingsRepository
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
                _state.update {
                    it.copy(
                        books = books,
                        genres = books.map { b -> b.genre }
                            .filter { g -> g.isNotBlank() }
                            .distinct()
                            .sorted()
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
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }
    fun setGenre(genre: String?) = _state.update { it.copy(genre = genre) }
    fun setStatus(status: ReadingStatus?) = _state.update { it.copy(status = status) }
    fun setMinRating(rating: Int) = _state.update { it.copy(minRating = rating) }
    fun setTrope(trope: String?) = _state.update { it.copy(trope = trope) }

    fun setSemanticQuery(query: String) = _state.update { it.copy(semanticQuery = query) }

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
