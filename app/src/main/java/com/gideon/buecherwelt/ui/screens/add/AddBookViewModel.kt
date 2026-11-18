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
    val subtitle: String = "",
    val author: String = "",
    val publisher: String = "",
    val publishedYear: Int? = null,
    val pageCount: Int? = null,
    val language: String = "",
    val description: String = "",
    val genre: String = "",
    val subgenre: String = "",
    val spice: Int = 0,
    val rating: Int? = null,
    val status: ReadingStatus = ReadingStatus.PLANNED,
    val notes: String = "",
    val seriesName: String = "",
    val seriesIndex: Int? = null,
    val universeName: String = "",
    val tropes: Set<String> = emptySet(),
    val coverUrl: String? = null,
    val coverPath: String? = null
)

data class BookForm(
    val title: String,
    val subtitle: String = "",
    val author: String,
    val publisher: String,
    val publishedYear: Int?,
    val pageCount: Int? = null,
    val language: String = "",
    val description: String,
    val genre: String,
    val subgenre: String,
    val spice: Int,
    val rating: Int?,
    val status: ReadingStatus,
    val notes: String,
    val seriesName: String,
    val seriesIndex: Int?,
    val universeName: String = "",
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
    val seriesNames: List<String> = emptyList(),
    val universeNames: List<String> = emptyList()
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
        viewModelScope.launch {
            bookRepository.observeUniverseNames().collect { names ->
                _state.update { it.copy(universeNames = names) }
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
                    subtitle = book.subtitle,
                    author = book.author,
                    publisher = book.publisher,
                    publishedYear = book.publishedYear,
                    pageCount = book.pageCount,
                    language = book.language,
                    description = book.description,
                    genre = book.genre,
                    subgenre = book.subgenre,
                    spice = book.spiceLevel ?: 0,
                    rating = book.rating,
                    status = book.status,
                    notes = book.notes,
                    seriesName = book.seriesName,
                    seriesIndex = book.seriesIndex,
                    universeName = book.universeName,
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
                        error = "Zu dieser Kennung wurden keine Daten gefunden (Google Books, Open Library und DNB geprüft). Du kannst das Buch unten manuell anlegen."
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
                        error = "Buch auf dem Cover nicht erkannt (LLM/OCR + Google Books/Open Library/DNB). Bitte manuell ausfüllen."
                    )
                }
                is FetchResult.NoNetwork -> _state.update {
                    it.copy(loading = false, error = "Keine Netzwerkverbindung. Manuelle Erfassung ist trotzdem möglich.")
                }
                is FetchResult.Error -> _state.update {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    private suspend fun applyMetadata(meta: BookMetadata, rawIsbn: String?) {
        val effectiveIsbn = meta.isbn ?: rawIsbn?.let { r -> BookMetadataFetcher.normalizeIsbn(r) }
        val coverPath = coverCache.downloadWithFallback(
            primaryUrl = meta.coverUrl,
            isbn = effectiveIsbn,
            key = CoverCache.keyFor(effectiveIsbn, meta.title, meta.author),
            forceRefresh = true
        )
        val detectedSU = com.gideon.buecherwelt.domain.analysis.SeriesUniverseDetector.detect(
            title = meta.title,
            subtitle = meta.subtitle,
            author = meta.author,
            description = meta.description,
            apiSeries = meta.seriesHints,
            existingSeries = _state.value.seriesNames,
            existingUniverses = _state.value.universeNames
        )
        val localSuggestions = com.gideon.buecherwelt.domain.analysis.ReadingStatsCalculator
            .inferBookIntelligence(
                title = "${meta.title} ${meta.subtitle}".trim(),
                description = "${meta.description} ${meta.author} ${detectedSU.seriesName} ${detectedSU.universeName}",
                categories = meta.categories,
                catalog = _state.value.allTropes
            ).copy(
                seriesName = detectedSU.seriesName,
                seriesIndex = detectedSU.seriesIndex,
                universeName = detectedSU.universeName
            )
        _state.update {
            it.copy(
                loading = false,
                suggestions = if (
                    localSuggestions.tropes.isNotEmpty() ||
                    localSuggestions.genres.isNotEmpty() ||
                    (localSuggestions.spice ?: 0) > 0
                ) localSuggestions else null,
                formInit = FormInit(
                    isbn = effectiveIsbn,
                    title = meta.title,
                    subtitle = meta.subtitle,
                    author = meta.author,
                    publisher = meta.publisher,
                    publishedYear = meta.publishedYear,
                    pageCount = meta.pageCount,
                    language = meta.language,
                    description = meta.description,
                    genre = meta.categories.firstOrNull()
                        ?: localSuggestions.genres.firstOrNull().orEmpty(),
                    subgenre = meta.categories.getOrNull(1)
                        ?: localSuggestions.genres.getOrNull(1).orEmpty(),
                    spice = localSuggestions.spice ?: 0,
                    seriesName = detectedSU.seriesName,
                    seriesIndex = detectedSU.seriesIndex,
                    universeName = detectedSU.universeName,
                    tropes = localSuggestions.tropes.take(5).toSet(),
                    coverUrl = meta.coverUrl,
                    coverPath = coverPath
                )
            )
        }
    }

    fun suggestTropes(
        currentTitle: String? = null,
        currentSubtitle: String? = null,
        currentAuthor: String? = null,
        currentDescription: String? = null,
        currentGenre: String? = null,
        currentSubgenre: String? = null
    ) {
        val form = _state.value.formInit
        val title = currentTitle ?: form?.title.orEmpty()
        val subtitle = currentSubtitle ?: form?.subtitle.orEmpty()
        val author = currentAuthor ?: form?.author.orEmpty()
        val description = currentDescription ?: form?.description.orEmpty()
        val categories = listOfNotNull(
            (currentGenre ?: form?.genre)?.takeIf { it.isNotBlank() },
            (currentSubgenre ?: form?.subgenre)?.takeIf { it.isNotBlank() }
        )

        val detectedSU = com.gideon.buecherwelt.domain.analysis.SeriesUniverseDetector.detect(
            title = title,
            subtitle = subtitle,
            author = author,
            description = description,
            existingSeries = _state.value.seriesNames,
            existingUniverses = _state.value.universeNames
        )

        val localFallback = com.gideon.buecherwelt.domain.analysis.ReadingStatsCalculator
            .inferBookIntelligence(
                title = "$title $subtitle".trim(),
                description = "$description $author ${detectedSU.seriesName} ${detectedSU.universeName}",
                categories = categories,
                catalog = _state.value.allTropes
            ).copy(
                seriesName = detectedSU.seriesName,
                seriesIndex = detectedSU.seriesIndex,
                universeName = detectedSU.universeName
            )

        if (!llmConfig.isConfigured) {
            _state.update {
                it.copy(
                    suggestions = localFallback,
                    error = if (localFallback.tropes.isEmpty() && localFallback.genres.isEmpty()) {
                        "Für mehr Vorschläge Klappentext eingeben oder einen LLM-Key in den Einstellungen hinterlegen."
                    } else null
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(suggestionLoading = true, error = null) }
            val suggestion = llmService.suggestTropes(
                config = llmConfig,
                title = "$title $subtitle".trim(),
                author = author,
                description = description,
                categories = categories
            )?.let { llmSug ->
                llmSug.copy(
                    seriesName = llmSug.seriesName.ifBlank { detectedSU.seriesName },
                    seriesIndex = llmSug.seriesIndex ?: detectedSU.seriesIndex,
                    universeName = llmSug.universeName.ifBlank { detectedSU.universeName }
                )
            }
            _state.update {
                it.copy(
                    suggestionLoading = false,
                    suggestions = suggestion ?: localFallback,
                    error = if (suggestion == null) {
                        "LLM-Abfrage nicht erreichbar – intelligente lokale Analyse aus dem Klappentext angezeigt."
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
            val autoSU = if (form.seriesName.isBlank() || form.seriesIndex == null || form.universeName.isBlank()) {
                com.gideon.buecherwelt.domain.analysis.SeriesUniverseDetector.detect(
                    title = form.title,
                    subtitle = form.subtitle,
                    author = form.author,
                    description = form.description,
                    existingSeries = _state.value.seriesNames,
                    existingUniverses = _state.value.universeNames
                )
            } else null

            val incoming = Book(
                id = _state.value.editBookId ?: 0L,
                isbn = isbn?.ifBlank { null },
                title = form.title.trim(),
                subtitle = form.subtitle.trim(),
                author = form.author.trim(),
                publisher = form.publisher.trim(),
                publishedYear = form.publishedYear,
                pageCount = form.pageCount,
                language = form.language.trim(),
                description = form.description.trim(),
                coverUrl = coverUrl,
                coverPath = coverPath,
                genre = form.genre.trim(),
                subgenre = form.subgenre.trim(),
                spiceLevel = form.spice,
                rating = form.rating,
                status = form.status,
                notes = form.notes.trim(),
                seriesName = form.seriesName.trim().ifBlank { autoSU?.seriesName.orEmpty() },
                seriesIndex = form.seriesIndex ?: autoSU?.seriesIndex,
                universeName = form.universeName.trim().ifBlank { autoSU?.universeName.orEmpty() },
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
                subtitle = form.subtitle.trim(),
                author = form.author.trim(),
                publisher = form.publisher.trim(),
                publishedYear = form.publishedYear,
                pageCount = form.pageCount,
                language = form.language.trim(),
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
                universeName = form.universeName.trim()
            )
            val tropeIds = tropeRepository.resolveIds(form.selectedTropes)
            val merged = bookRepository.merge(existing, incoming, tropeIds)
            _state.update { it.copy(saving = false) }
            onResult(SaveResult.Saved(merged.id))
        }
    }
}
