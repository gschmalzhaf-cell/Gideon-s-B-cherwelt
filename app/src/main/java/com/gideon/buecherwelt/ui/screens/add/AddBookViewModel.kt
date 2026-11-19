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
import com.gideon.buecherwelt.data.network.SearchListResult
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
    val statusInfo: String? = null,
    val formInit: FormInit? = null,
    val searchResults: List<BookMetadata> = emptyList(),
    val searchResultsEmpty: Boolean = false,
    val aiCompletionLoading: Boolean = false,
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
    fun dismissStatusInfo() = _state.update { it.copy(statusInfo = null) }
    fun clearSearchResults() = _state.update { it.copy(searchResults = emptyList(), searchResultsEmpty = false) }

    fun onIsbnSubmitted(rawIsbn: String) {
        if (rawIsbn.isBlank()) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    error = null,
                    statusInfo = null,
                    suggestions = null,
                    searchResults = emptyList(),
                    searchResultsEmpty = false
                )
            }
            val kinds = BookMetadataFetcher.classifyIdentifier(rawIsbn)
            val isAsin = kinds.any { it.first == BookMetadataFetcher.Companion.IdentifierKind.ASIN }
            when (val result = metadataFetcher.fetchByIsbn(rawIsbn)) {
                is FetchResult.Success -> applyMetadata(result.metadata, rawIsbn)
                is FetchResult.NotFound -> {
                    if (isAsin && llmConfig.isConfigured) {
                        val aiResolved = runAiCompletionInternal(identifier = rawIsbn.trim())
                        if (aiResolved) return@launch
                    }
                    _state.update {
                        it.copy(
                            loading = false,
                            error = if (isAsin) {
                                "Zu dieser ASIN wurden keine Katalogdaten gefunden (Open Library, Google Books & Audnex geprüft). Tipp: Nutze die Titel-Suche oder fülle Titel + Autor:in mit KI aus."
                            } else {
                                "Zu dieser Kennung wurden keine Daten gefunden (Google Books, Open Library und DNB geprüft). Tipp: Nutze die Titel-Suche oder lege das Buch unten manuell an."
                            }
                        )
                    }
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

    fun searchByTitle(titleQuery: String, authorQuery: String = "") {
        if (titleQuery.isBlank()) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    error = null,
                    statusInfo = null,
                    searchResults = emptyList(),
                    searchResultsEmpty = false
                )
            }
            when (val res = metadataFetcher.searchByTitle(titleQuery, authorQuery)) {
                is SearchListResult.Success -> _state.update {
                    it.copy(loading = false, searchResults = res.items, searchResultsEmpty = false)
                }
                is SearchListResult.Empty -> _state.update {
                    it.copy(loading = false, searchResults = emptyList(), searchResultsEmpty = true)
                }
                is SearchListResult.NoNetwork -> _state.update {
                    it.copy(loading = false, error = "Keine Netzwerkverbindung für die Titelsuche.")
                }
                is SearchListResult.Error -> _state.update {
                    it.copy(loading = false, error = res.message)
                }
            }
        }
    }

    fun selectSearchResult(meta: BookMetadata) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    error = null,
                    searchResults = emptyList(),
                    searchResultsEmpty = false
                )
            }
            val enriched = if (meta.isbn != null && (meta.description.isBlank() || meta.coverUrl.isNullOrBlank())) {
                when (val r = metadataFetcher.fetchByIsbn(meta.isbn)) {
                    is FetchResult.Success -> r.metadata.copy(
                        coverUrl = r.metadata.coverUrl ?: meta.coverUrl,
                        description = r.metadata.description.ifBlank { meta.description }
                    )
                    else -> meta
                }
            } else meta
            applyMetadata(enriched, enriched.isbn)
        }
    }

    fun completeWithAi(
        identifier: String = "",
        title: String = "",
        subtitle: String = "",
        author: String = "",
        publisher: String = "",
        publishedYear: Int? = null,
        pageCount: Int? = null,
        language: String = "",
        description: String = "",
        genre: String = "",
        subgenre: String = "",
        seriesName: String = "",
        seriesIndex: Int? = null,
        universeName: String = "",
        spice: Int = 0,
        selectedTropes: Set<String> = emptySet(),
        coverUrl: String? = null,
        coverPath: String? = null
    ) {
        if (!llmConfig.isConfigured) {
            _state.update { it.copy(error = "Für die KI-Vervollständigung bitte zuerst einen LLM-API-Key in den Einstellungen hinterlegen.") }
            return
        }
        if (identifier.isBlank() && title.isBlank()) {
            _state.update { it.copy(error = "Bitte mindestens einen Buchtitel oder eine Kennung (ISBN/ASIN) eingeben.") }
            return
        }
        viewModelScope.launch {
            runAiCompletionInternal(
                identifier = identifier,
                title = title,
                subtitle = subtitle,
                author = author,
                publisher = publisher,
                publishedYear = publishedYear,
                pageCount = pageCount,
                language = language,
                description = description,
                genre = genre,
                subgenre = subgenre,
                seriesName = seriesName,
                seriesIndex = seriesIndex,
                universeName = universeName,
                spice = spice,
                selectedTropes = selectedTropes,
                coverUrl = coverUrl,
                coverPath = coverPath
            )
        }
    }

    private suspend fun runAiCompletionInternal(
        identifier: String = "",
        title: String = "",
        subtitle: String = "",
        author: String = "",
        publisher: String = "",
        publishedYear: Int? = null,
        pageCount: Int? = null,
        language: String = "",
        description: String = "",
        genre: String = "",
        subgenre: String = "",
        seriesName: String = "",
        seriesIndex: Int? = null,
        universeName: String = "",
        spice: Int = 0,
        selectedTropes: Set<String> = emptySet(),
        coverUrl: String? = null,
        coverPath: String? = null
    ): Boolean {
        _state.update { it.copy(aiCompletionLoading = true, error = null, statusInfo = null) }
        val completion = llmService.completeBook(
            config = llmConfig,
            identifier = identifier.trim(),
            title = title.trim(),
            subtitle = subtitle.trim(),
            author = author.trim(),
            publisher = publisher.trim(),
            publishedYear = publishedYear,
            pageCount = pageCount,
            description = description.trim(),
            genre = genre.trim(),
            seriesName = seriesName.trim(),
            universeName = universeName.trim()
        )
        if (completion == null || !completion.found || (completion.title.isBlank() && title.isBlank())) {
            _state.update {
                it.copy(
                    loading = false,
                    aiCompletionLoading = false,
                    error = "Die KI konnte das Buch anhand dieser Angaben nicht sicher identifizieren."
                )
            }
            return false
        }

        val resolvedTitle = title.ifBlank { completion.title }
        val resolvedAuthor = author.ifBlank { completion.author }
        val resolvedIsbn = BookMetadataFetcher.normalizeIsbn(identifier)
            ?: BookMetadataFetcher.normalizeIsbn(completion.isbn)
            ?: _state.value.formInit?.isbn

        // Online-Nachladen für Cover & exakte Verlagsdaten falls noch fehlend
        var onlineMeta: BookMetadata? = null
        if (coverPath == null || coverUrl == null || resolvedIsbn != null) {
            val lookup = if (resolvedIsbn != null) {
                metadataFetcher.fetchByIsbn(resolvedIsbn)
            } else {
                metadataFetcher.findByTitleAuthor(resolvedTitle, resolvedAuthor)
            }
            if (lookup is FetchResult.Success) {
                onlineMeta = lookup.metadata
            }
        }

        val finalCoverUrl = coverUrl ?: onlineMeta?.coverUrl
        val finalCoverPath = coverPath ?: coverCache.downloadWithFallback(
            primaryUrl = finalCoverUrl,
            isbn = resolvedIsbn ?: onlineMeta?.isbn,
            key = CoverCache.keyFor(resolvedIsbn ?: onlineMeta?.isbn, resolvedTitle, resolvedAuthor),
            forceRefresh = false
        )

        val finalTitle = resolvedTitle
        val finalSubtitle = subtitle.ifBlank { onlineMeta?.subtitle.orEmpty() }.ifBlank { completion.subtitle }
        val finalAuthor = resolvedAuthor.ifBlank { onlineMeta?.author.orEmpty() }
        val finalDesc = description.ifBlank { completion.description }.ifBlank { onlineMeta?.description.orEmpty() }

        val detectedSU = com.gideon.buecherwelt.domain.analysis.SeriesUniverseDetector.detect(
            title = finalTitle,
            subtitle = finalSubtitle,
            author = finalAuthor,
            description = finalDesc,
            apiSeries = onlineMeta?.seriesHints.orEmpty(),
            existingSeries = _state.value.seriesNames,
            existingUniverses = _state.value.universeNames
        )

        val finalSeries = seriesName.ifBlank { completion.seriesName }.ifBlank { detectedSU.seriesName }
        val finalSeriesIdx = seriesIndex ?: completion.seriesIndex ?: detectedSU.seriesIndex
        val finalUniverse = universeName.ifBlank { completion.universeName }.ifBlank { detectedSU.universeName }
        val finalTropes = (selectedTropes + completion.tropes).filter { it.isNotBlank() }.toSet()

        _state.update {
            it.copy(
                loading = false,
                aiCompletionLoading = false,
                searchResults = emptyList(),
                searchResultsEmpty = false,
                statusInfo = "Buchdaten & Cover durch KI und Katalogsuche vervollständigt.",
                formInit = FormInit(
                    isbn = resolvedIsbn ?: onlineMeta?.isbn ?: identifier.trim().takeIf { id -> id.isNotBlank() },
                    title = finalTitle,
                    subtitle = finalSubtitle,
                    author = finalAuthor,
                    publisher = publisher.ifBlank { onlineMeta?.publisher.orEmpty() }.ifBlank { completion.publisher },
                    publishedYear = publishedYear ?: onlineMeta?.publishedYear ?: completion.publishedYear,
                    pageCount = pageCount ?: onlineMeta?.pageCount ?: completion.pageCount,
                    language = language.ifBlank { onlineMeta?.language.orEmpty() }.ifBlank { completion.language },
                    description = finalDesc,
                    genre = genre.ifBlank { completion.genre }.ifBlank { onlineMeta?.categories?.firstOrNull().orEmpty() },
                    subgenre = subgenre.ifBlank { completion.subgenre }.ifBlank { onlineMeta?.categories?.getOrNull(1).orEmpty() },
                    spice = if (spice > 0) spice else (completion.spice ?: 0),
                    rating = _state.value.formInit?.rating,
                    status = _state.value.formInit?.status ?: ReadingStatus.PLANNED,
                    notes = _state.value.formInit?.notes.orEmpty(),
                    seriesName = finalSeries,
                    seriesIndex = finalSeriesIdx,
                    universeName = finalUniverse,
                    tropes = finalTropes,
                    coverUrl = finalCoverUrl,
                    coverPath = finalCoverPath
                )
            )
        }
        return true
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
