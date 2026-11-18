package com.gideon.buecherwelt.domain.usecase

import com.gideon.buecherwelt.data.network.BookMetadata
import com.gideon.buecherwelt.data.network.BookMetadataFetcher
import com.gideon.buecherwelt.data.network.CoverCache
import com.gideon.buecherwelt.data.network.FetchResult
import com.gideon.buecherwelt.domain.analysis.ReadingStatsCalculator
import com.gideon.buecherwelt.domain.analysis.SeriesUniverseDetector
import com.gideon.buecherwelt.domain.model.Book
import com.gideon.buecherwelt.domain.repository.BookRepository
import com.gideon.buecherwelt.domain.repository.TropeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

data class EnrichmentSummary(
    val total: Int,
    val coversUpdated: Int,
    val detailsUpdated: Int,
    val seriesOrUniverseDetected: Int
)

class BookEnrichmentService(
    private val bookRepository: BookRepository,
    private val tropeRepository: TropeRepository,
    private val metadataFetcher: BookMetadataFetcher,
    private val coverCache: CoverCache
) {
    private val backgroundMutex = Mutex()
    @Volatile
    private var initialPassDone = false

    /**
     * Führt beim App-Start einmalig im Hintergrund eine automatische Anreicherung durch:
     * 1) Offline-Erkennung von Buchreihen, Bandnummern, Universen und Tropes für alle Bücher.
     * 2) Online-Nachladen fehlender oder ungültiger Buchcover sowie fehlender Klappentexte/Seitenzahlen.
     */
    suspend fun ensureLibraryEnrichedInBackground() = withContext(Dispatchers.IO) {
        if (initialPassDone || !backgroundMutex.tryLock()) return@withContext
        try {
            val allBooks = bookRepository.getAllBooks()
            if (allBooks.isEmpty()) {
                initialPassDone = true
                return@withContext
            }
            val catalog = tropeRepository.getAll()
            val existingSeries = allBooks.map { it.seriesName }.filter { it.isNotBlank() }.distinct().toMutableList()
            val existingUniverses = allBooks.map { it.universeName }.filter { it.isNotBlank() }.distinct().toMutableList()

            // Pass 1: Schnelle Offline-Erkennung (Reihe, Band, Universum, Tropes)
            for (book in allBooks) {
                val updatedOffline = enrichOfflineFields(book, catalog, existingSeries, existingUniverses)
                if (updatedOffline != null) {
                    val (newBook, newTropeIds) = updatedOffline
                    bookRepository.update(newBook, newTropeIds)
                    if (newBook.seriesName.isNotBlank() && newBook.seriesName !in existingSeries) {
                        existingSeries += newBook.seriesName
                    }
                    if (newBook.universeName.isNotBlank() && newBook.universeName !in existingUniverses) {
                        existingUniverses += newBook.universeName
                    }
                }
            }

            // Pass 2: Online-Nachladen für Bücher ohne gültiges Cover oder ohne Beschreibung
            val booksNeedingNetwork = bookRepository.getAllBooks().filter { b ->
                !CoverCache.isValidCoverFile(b.coverPath) ||
                    b.description.isBlank() ||
                    b.pageCount == null
            }
            for (book in booksNeedingNetwork) {
                try {
                    refreshBookInternal(
                        book = book,
                        forceCoverRefresh = !CoverCache.isValidCoverFile(book.coverPath),
                        catalog = catalog,
                        existingSeries = existingSeries,
                        existingUniverses = existingUniverses
                    )
                } catch (_: Exception) {
                    // Offline oder Einzelfehler ignorieren
                }
            }
            initialPassDone = true
        } finally {
            backgroundMutex.unlock()
        }
    }

    /**
     * Aktualisiert ein einzelnes Buch (Cover + Metadaten + Reihe/Universum + Tropes).
     */
    suspend fun refreshBook(
        bookId: Long,
        forceCoverRefresh: Boolean = true
    ): Result<Book> = withContext(Dispatchers.IO) {
        try {
            val current = bookRepository.getBook(bookId)
                ?: return@withContext Result.failure(IllegalArgumentException("Buch nicht gefunden."))
            val allBooks = bookRepository.getAllBooks()
            val catalog = tropeRepository.getAll()
            val existingSeries = allBooks.map { it.seriesName }.filter { it.isNotBlank() }.distinct()
            val existingUniverses = allBooks.map { it.universeName }.filter { it.isNotBlank() }.distinct()

            val updated = refreshBookInternal(
                book = current,
                forceCoverRefresh = forceCoverRefresh,
                catalog = catalog,
                existingSeries = existingSeries,
                existingUniverses = existingUniverses
            )
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ersetzt das Cover eines Buches gezielt durch eine ausgewählte Cover-URL.
     */
    suspend fun applyCoverFromUrl(bookId: Long, newCoverUrl: String): Result<Book> =
        withContext(Dispatchers.IO) {
            try {
                val current = bookRepository.getBook(bookId)
                    ?: return@withContext Result.failure(IllegalArgumentException("Buch nicht gefunden."))
                val key = "${CoverCache.keyFor(current.isbn, current.title, current.author)}_${current.id}"
                val newPath = coverCache.download(newCoverUrl, key, forceRefresh = true)
                    ?: return@withContext Result.failure(IllegalStateException("Cover konnte nicht heruntergeladen werden."))
                val updated = current.copy(
                    coverUrl = newCoverUrl,
                    coverPath = newPath,
                    updatedAt = System.currentTimeMillis()
                )
                bookRepository.update(updated, current.tropes.map { it.id }.toSet())
                Result.success(updated)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Aktualisiert alle Bücher in der Bibliothek (Cover & Details & Reihen/Universen) mit Fortschrittsanzeige.
     */
    suspend fun refreshAllBooks(
        forceCovers: Boolean = true,
        onProgress: (done: Int, total: Int, currentTitle: String) -> Unit = { _, _, _ -> }
    ): EnrichmentSummary = withContext(Dispatchers.IO) {
        val books = bookRepository.getAllBooks()
        if (books.isEmpty()) return@withContext EnrichmentSummary(0, 0, 0, 0)

        val catalog = tropeRepository.getAll()
        val existingSeries = books.map { it.seriesName }.filter { it.isNotBlank() }.distinct().toMutableList()
        val existingUniverses = books.map { it.universeName }.filter { it.isNotBlank() }.distinct().toMutableList()

        var coversUpdated = 0
        var detailsUpdated = 0
        var seriesUpdated = 0

        books.forEachIndexed { idx, book ->
            onProgress(idx + 1, books.size, book.title)
            try {
                val hadValidCover = CoverCache.isValidCoverFile(book.coverPath)
                val hadSeriesOrUni = book.seriesName.isNotBlank() || book.universeName.isNotBlank()
                val updated = refreshBookInternal(
                    book = book,
                    forceCoverRefresh = forceCovers || !hadValidCover,
                    catalog = catalog,
                    existingSeries = existingSeries,
                    existingUniverses = existingUniverses
                )
                if (CoverCache.isValidCoverFile(updated.coverPath) && (!hadValidCover || updated.coverPath != book.coverPath || forceCovers)) {
                    coversUpdated++
                }
                if (updated.description != book.description ||
                    updated.pageCount != book.pageCount ||
                    updated.tropes.size > book.tropes.size
                ) {
                    detailsUpdated++
                }
                if ((!hadSeriesOrUni && (updated.seriesName.isNotBlank() || updated.universeName.isNotBlank())) ||
                    updated.seriesIndex != book.seriesIndex
                ) {
                    seriesUpdated++
                }
                if (updated.seriesName.isNotBlank() && updated.seriesName !in existingSeries) {
                    existingSeries += updated.seriesName
                }
                if (updated.universeName.isNotBlank() && updated.universeName !in existingUniverses) {
                    existingUniverses += updated.universeName
                }
            } catch (_: Exception) {
                // Nächstes Buch fortsetzen
            }
        }
        EnrichmentSummary(
            total = books.size,
            coversUpdated = coversUpdated,
            detailsUpdated = detailsUpdated,
            seriesOrUniverseDetected = seriesUpdated
        )
    }

    private suspend fun enrichOfflineFields(
        book: Book,
        catalog: List<com.gideon.buecherwelt.domain.model.Trope>,
        existingSeries: List<String>,
        existingUniverses: List<String>
    ): Pair<Book, Set<Long>>? {
        val detectedSU = SeriesUniverseDetector.detect(
            title = book.title,
            subtitle = book.subtitle,
            author = book.author,
            description = book.description,
            existingSeries = existingSeries,
            existingUniverses = existingUniverses
        )
        val inferred = ReadingStatsCalculator.inferBookIntelligence(
            title = "${book.title} ${book.subtitle}".trim(),
            description = "${book.description} ${book.author} ${detectedSU.seriesName} ${detectedSU.universeName}",
            categories = listOfNotNull(
                book.genre.takeIf { it.isNotBlank() },
                book.subgenre.takeIf { it.isNotBlank() }
            ),
            catalog = catalog
        )

        val newSeries = book.seriesName.ifBlank { detectedSU.seriesName }
        val newIndex = book.seriesIndex ?: detectedSU.seriesIndex
        val newUniverse = book.universeName.ifBlank { detectedSU.universeName }
        val newGenre = book.genre.ifBlank { inferred.genres.firstOrNull().orEmpty() }
        val newSubgenre = book.subgenre.ifBlank { inferred.genres.getOrNull(1).orEmpty() }
        val newSpice = book.spiceLevel ?: inferred.spice
        val validCoverPath = book.coverPath?.takeIf { CoverCache.isValidCoverFile(it) }

        val existingTropeIds = book.tropes.map { it.id }.toSet()
        val finalTropeIds = if (existingTropeIds.isEmpty() && inferred.tropes.isNotEmpty()) {
            tropeRepository.resolveIds(inferred.tropes.take(6))
        } else {
            existingTropeIds
        }

        val changed = newSeries != book.seriesName ||
            newIndex != book.seriesIndex ||
            newUniverse != book.universeName ||
            newGenre != book.genre ||
            newSubgenre != book.subgenre ||
            newSpice != book.spiceLevel ||
            validCoverPath != book.coverPath ||
            finalTropeIds != existingTropeIds

        if (!changed) return null

        val updatedBook = book.copy(
            seriesName = newSeries,
            seriesIndex = newIndex,
            universeName = newUniverse,
            genre = newGenre,
            subgenre = newSubgenre,
            spiceLevel = newSpice,
            coverPath = validCoverPath
        )
        return updatedBook to finalTropeIds
    }

    private suspend fun refreshBookInternal(
        book: Book,
        forceCoverRefresh: Boolean,
        catalog: List<com.gideon.buecherwelt.domain.model.Trope>,
        existingSeries: List<String>,
        existingUniverses: List<String>
    ): Book {
        var meta: BookMetadata? = null
        if (!book.isbn.isNullOrBlank()) {
            when (val r = metadataFetcher.fetchByIsbn(book.isbn)) {
                is FetchResult.Success -> meta = r.metadata
                else -> Unit
            }
        }
        if (meta == null || meta.coverUrl.isNullOrBlank() || meta.description.isBlank()) {
            when (val byTitle = metadataFetcher.findByTitleAuthor(book.title, book.author)) {
                is FetchResult.Success -> {
                    val found = byTitle.metadata
                    meta = if (meta == null) {
                        found
                    } else {
                        meta.copy(
                            subtitle = meta.subtitle.ifBlank { found.subtitle },
                            publisher = meta.publisher.ifBlank { found.publisher },
                            publishedYear = meta.publishedYear ?: found.publishedYear,
                            description = meta.description.ifBlank { found.description },
                            categories = meta.categories.ifEmpty { found.categories },
                            coverUrl = meta.coverUrl ?: found.coverUrl,
                            pageCount = meta.pageCount ?: found.pageCount,
                            language = meta.language.ifBlank { found.language },
                            seriesHints = (meta.seriesHints + found.seriesHints).distinct()
                        )
                    }
                }
                else -> Unit
            }
        }

        val effectiveIsbn = book.isbn?.ifBlank { null } ?: meta?.isbn?.ifBlank { null }
        val effectiveCoverUrl = meta?.coverUrl ?: book.coverUrl
        val cacheKey = "${CoverCache.keyFor(effectiveIsbn, book.title, book.author)}_${book.id}"

        val newCoverPath = if (forceCoverRefresh || !CoverCache.isValidCoverFile(book.coverPath)) {
            coverCache.downloadWithFallback(
                primaryUrl = effectiveCoverUrl,
                isbn = effectiveIsbn,
                key = cacheKey,
                forceRefresh = forceCoverRefresh
            ) ?: book.coverPath?.takeIf { CoverCache.isValidCoverFile(it) }
        } else {
            book.coverPath
        }

        val effectiveSubtitle = book.subtitle.ifBlank { meta?.subtitle.orEmpty() }
        val effectiveDescription = book.description.ifBlank { meta?.description.orEmpty() }
        val effectivePublisher = book.publisher.ifBlank { meta?.publisher.orEmpty() }
        val effectiveYear = book.publishedYear ?: meta?.publishedYear
        val effectivePages = book.pageCount ?: meta?.pageCount
        val effectiveLanguage = book.language.ifBlank { meta?.language.orEmpty() }
        val categories = (listOfNotNull(
            book.genre.takeIf { it.isNotBlank() },
            book.subgenre.takeIf { it.isNotBlank() }
        ) + meta?.categories.orEmpty()).distinct()

        val detectedSU = SeriesUniverseDetector.detect(
            title = book.title,
            subtitle = effectiveSubtitle,
            author = book.author.ifBlank { meta?.author.orEmpty() },
            description = effectiveDescription,
            apiSeries = meta?.seriesHints.orEmpty(),
            existingSeries = existingSeries,
            existingUniverses = existingUniverses
        )

        val inferred = ReadingStatsCalculator.inferBookIntelligence(
            title = "${book.title} $effectiveSubtitle".trim(),
            description = "$effectiveDescription ${book.author} ${detectedSU.seriesName} ${detectedSU.universeName}",
            categories = categories,
            catalog = catalog
        )

        val effectiveGenre = book.genre.ifBlank {
            meta?.categories?.firstOrNull() ?: inferred.genres.firstOrNull().orEmpty()
        }
        val effectiveSubgenre = book.subgenre.ifBlank {
            meta?.categories?.getOrNull(1) ?: inferred.genres.getOrNull(1).orEmpty()
        }
        val effectiveSpice = if ((book.spiceLevel ?: 0) > 0) book.spiceLevel else inferred.spice

        val existingTropeIds = book.tropes.map { it.id }.toSet()
        val inferredTropeIds = if (inferred.tropes.isNotEmpty()) {
            tropeRepository.resolveIds(inferred.tropes.take(6))
        } else emptySet()
        val finalTropeIds = if (existingTropeIds.isEmpty()) inferredTropeIds else existingTropeIds

        val updated = book.copy(
            isbn = effectiveIsbn,
            subtitle = effectiveSubtitle,
            author = book.author.ifBlank { meta?.author.orEmpty() },
            publisher = effectivePublisher,
            publishedYear = effectiveYear,
            description = effectiveDescription,
            coverUrl = effectiveCoverUrl,
            coverPath = newCoverPath,
            genre = effectiveGenre,
            subgenre = effectiveSubgenre,
            spiceLevel = effectiveSpice,
            seriesName = book.seriesName.ifBlank { detectedSU.seriesName },
            seriesIndex = book.seriesIndex ?: detectedSU.seriesIndex,
            universeName = book.universeName.ifBlank { detectedSU.universeName },
            pageCount = effectivePages,
            language = effectiveLanguage,
            updatedAt = System.currentTimeMillis()
        )
        bookRepository.update(updated, finalTropeIds)
        return bookRepository.getBook(book.id) ?: updated
    }
}
