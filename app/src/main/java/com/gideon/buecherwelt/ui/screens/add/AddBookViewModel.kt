package com.gideon.buecherwelt.ui.screens.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gideon.buecherwelt.data.local.ReadingStatus
import com.gideon.buecherwelt.data.network.BookMetadataFetcher
import com.gideon.buecherwelt.data.network.CoverCache
import com.gideon.buecherwelt.data.network.FetchResult
import com.gideon.buecherwelt.data.settings.LlmConfig
import com.gideon.buecherwelt.data.settings.SettingsRepository
import com.gideon.buecherwelt.domain.llm.CoverIdentifier
import com.gideon.buecherwelt.domain.llm.LlmService
import com.gideon.buecherwelt.domain.llm.TropeSuggestion
import com.gideon.buecherwelt.data.network.BookMetadata
import com.gideon.buecherwelt.domain.model.Book
import com.gideon.buecherwelt.domain.model.Trope
import com.gideon.buecherwelt.domain.repository.BookRepository
import com.gideon.buecherwelt.domain.repository.TropeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FormInit(
    val isbn: String? = null,
    val title: String = "",
    val author: String = "",
    val publisher: String = "",
    val publishedYear: Int? = null,
    val description: String = "",
    val genre: String = "",
    val subgenre: String = "",
    val spice: Int = 0,
    val rating: Int? = null,
    val status: ReadingStatus = ReadingStatus.PLANNED,
    val notes: String = "",
    val seriesName: String = "",
    val seriesIndex: Int? = null,
    val tropes: Set<String> = emptySet(),
    val coverUrl: String? = null,
    val coverPath: String? = null
)

data class BookForm(
    val title: String,
    val author: String,
    val publisher: String,
    val publishedYear: Int?,
    val description: String,
    val genre: String,
    val subgenre: String,
    val spice: Int,
    val rating: Int?,
    val status: ReadingStatus,
    val notes: String,
    val seriesName: String,
    val seriesIndex: Int?,
    val selectedTropes: Set<String>
)

sealed interface SaveResult {
    data class Saved(val bookId: Long) : SaveResult
    data class Duplicate(val existing: Book) : SaveResult
    data class Error(val message: String) : SaveResult
}

data class AddBookUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val formInit: FormInit? = null,
    val allTropes: List<Trope> = emptyList(),
    val suggestions: TropeSuggestion? = null,
    val suggestionLoading: Boolean = false,
    val llmConfigured: Boolean = false,
    val saving: Boolean = false,
    val editBookId: Long? = null,
    val seriesNames: List<String> = emptyList()
)

class AddBookViewModel(
    private val bookRepository: BookRepository,
    private val tropeRepository: TropeRepository,
    private val metadataFetcher: BookMetadataFetcher,
    private val coverCache: CoverCache,
    settingsRepository: SettingsRepository,
    private val llmService: LlmService,
    private val coverIdentifier: CoverIdentifier,
    private val editBookId: Long?
) : ViewModel() {

    private val _state = MutableStateFlow(AddBookUiState(editBookId = editBookId))
    val state: StateFlow<AddBookUiState> = _state.asStateFlow()

    private var llmConfig: LlmConfig = LlmConfig()

    init {
        viewModelScope.launch {
            settingsRepository.llmConfig.collect { config ->
                llmConfig = config
                _state.update { it.copy(llmConfigured = config.isConfigured) }
            }
        }
        viewModelScope.launch {
            tropeRepository.observeAll().collect { tropes ->
                _state.update { it.copy(allTropes = tropes) }
            }
        }
        viewModelScope.launch {
            bookRepository.observeSeriesNames().collect { names ->
                _state.update { it.copy(seriesNames = names) }
            }
        }
        if (editBookId != null) {
            viewModelScope.launch { loadForEdit(editBookId) }
        }
    }

    private suspend fun loadForEdit(id: Long) {
        val book = bookRepository.getBook(id) ?: return
        _state.update {
            it.copy(
                formInit = FormInit(
                    isbn = book.isbn,
                    title = book.title,
                    author = book.author,
                    publisher = book.publisher,
                    publishedYear = book.publishedYear,
                    description = book.description,
                    genre = book.genre,
                    subgenre = book.subgenre,
                    spice = book.spiceLevel ?: 0,
                    rating = book.rating,
                    status = book.status,
                    notes = book.notes,
                    seriesName = book.seriesName,
                    seriesIndex = book.seriesIndex,
                    tropes = book.tropes.map { t -> t.name }.toSet(),
                    coverUrl = book.coverUrl,
                    coverPath = book.coverPath
                )
            )
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun onIsbnSubmitted(rawIsbn: String) {
        if (rawIsbn.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, suggestions = null) }
            when (val result = metadataFetcher.fetchByIsbn(rawIsbn)) {
                is FetchResult.Success -> applyMetadata(result.metadata, rawIsbn)
                is FetchResult.NotFound -> _state.update {
                    it.copy(
                        loading = false,
                        error = "Zu dieser Kennung wurden keine Daten gefunden (Google Books, Open Library und DNB gepr\u00fcft). Du kannst das Buch unten manuell anlegen."
                    )
                }
                is FetchResult.NoNetwork -> _state.update {
                    it.copy(
                        loading = false,
                        error = "Keine Netzwerkverbindung. Manuelle Erfassung ist trotzdem möglich."
                    )
                }
                is FetchResult.Error -> _state.update {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun onCoverImageFailed() {
        _state.update { it.copy(loading = false, error = "Bild konnte nicht geladen werden.") }
    }

    fun onCoverImage(path: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, suggestions = null) }
            when (val result = coverIdentifier.identify(path, llmConfig)) {
                is FetchResult.Success -> applyMetadata(result.metadata, result.metadata.isbn)
                is FetchResult.NotFound -> _state.update {
                    it.copy(
                        loading = false,
                        error = "Buch auf dem Cover nicht erkannt (LLM/OCR + Google Books/Open Library/DNB). Bitte manuell ausf\u00fcllen."
                    )
                }
                is FetchResult.NoNetwork -> _state.update {
                    it.copy(loading = false, error = "Keine Netzwerkverbindung. Manuelle Erfassung ist trotzdem m\u00f6glich.")
                }
                is FetchResult.Error -> _state.update {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    private suspend fun applyMetadata(meta: BookMetadata, rawIsbn: String?) {
        val coverPath = meta.coverUrl?.let {
            coverCache.download(it, CoverCache.keyFor(meta.isbn, meta.title, meta.author))
        }
        _state.update {
            it.copy(
                loading = false,
                formInit = FormInit(
                    isbn = meta.isbn ?: rawIsbn?.let { r -> BookMetadataFetcher.normalizeIsbn(r) },
                    title = meta.title,
                    author = meta.author,
                    publisher = meta.publisher,
                    publishedYear = meta.publishedYear,
                    description = meta.description,
                    genre = meta.categories.firstOrNull().orEmpty(),
                    subgenre = meta.categories.getOrNull(1).orEmpty(),
                    coverUrl = meta.coverUrl,
                    coverPath = coverPath
                )
            )
        }
    }

    fun suggestTropes() {
        val form = _state.value.formInit ?: return
        if (!llmConfig.isConfigured) {
            _state.update { it.copy(error = "Kein LLM-API-Key hinterlegt – bitte in den Einstellungen konfigurieren.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(suggestionLoading = true, error = null) }
            val suggestion = llmService.suggestTropes(
                config = llmConfig,
                title = form.title,
                author = form.author,
                description = form.description,
                categories = listOfNotNull(form.genre, form.subgenre)
            )
            _state.update {
                it.copy(
                    suggestionLoading = false,
                    suggestions = suggestion,
                    error = if (suggestion == null) {
                        "LLM-Vorschläge nicht verfügbar (offline oder API-Fehler). Tropes können manuell gewählt werden."
                    } else it.error
                )
            }
        }
    }

    fun save(
        form: BookForm,
        isbn: String?,
        coverUrl: String?,
        coverPath: String?,
        onResult: (SaveResult) -> Unit
    ) {
        if (form.title.isBlank()) {
            onResult(SaveResult.Error("Bitte mindestens einen Titel angeben."))
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            val now = System.currentTimeMillis()
            val incoming = Book(
                id = _state.value.editBookId ?: 0L,
                isbn = isbn?.ifBlank { null },
                title = form.title.trim(),
                author = form.author.trim(),
                publisher = form.publisher.trim(),
                publishedYear = form.publishedYear,
                description = form.description.trim(),
                coverUrl = coverUrl,
                coverPath = coverPath,
                genre = form.genre.trim(),
                subgenre = form.subgenre.trim(),
                spiceLevel = form.spice,
                rating = form.rating,
                status = form.status,
                notes = form.notes.trim(),
                seriesName = form.seriesName.trim(),
                seriesIndex = form.seriesIndex,
                tropes = emptyList(),
                createdAt = now,
                updatedAt = now
            )
            val tropeIds = tropeRepository.resolveIds(form.selectedTropes)
            if (_state.value.editBookId != null) {
                val existing = bookRepository.getBook(_state.value.editBookId!!)
                val preserved = existing?.let {
                    incoming.copy(
                        aiSummary = it.aiSummary,
                        aiFitScore = it.aiFitScore,
                        aiFitReason = it.aiFitReason,
                        aiInsightsAt = it.aiInsightsAt
                    )
                } ?: incoming
                bookRepository.update(preserved, tropeIds)
                _state.update { it.copy(saving = false) }
                onResult(SaveResult.Saved(_state.value.editBookId!!))
                return@launch
            }
            val duplicate = bookRepository.findDuplicate(incoming.title, incoming.author, incoming.isbn)
            if (duplicate != null) {
                _state.update { it.copy(saving = false) }
                onResult(SaveResult.Duplicate(duplicate))
                return@launch
            }
            val id = bookRepository.insert(incoming, tropeIds)
            _state.update { it.copy(saving = false) }
            onResult(SaveResult.Saved(id))
        }
    }

    fun mergeDuplicate(
        existing: Book,
        form: BookForm,
        isbn: String?,
        coverUrl: String?,
        coverPath: String?,
        onResult: (SaveResult) -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            val incoming = Book(
                id = 0,
                isbn = isbn?.ifBlank { null },
                title = form.title.trim(),
                author = form.author.trim(),
                publisher = form.publisher.trim(),
                publishedYear = form.publishedYear,
                description = form.description.trim(),
                coverUrl = coverUrl,
                coverPath = coverPath,
                genre = form.genre.trim(),
                subgenre = form.subgenre.trim(),
                spiceLevel = form.spice,
                rating = form.rating,
                status = form.status,
                notes = form.notes.trim(),
                seriesName = form.seriesName.trim(),
                seriesIndex = form.seriesIndex
            )
            val tropeIds = tropeRepository.resolveIds(form.selectedTropes)
            val merged = bookRepository.merge(existing, incoming, tropeIds)
            _state.update { it.copy(saving = false) }
            onResult(SaveResult.Saved(merged.id))
        }
    }
}
