package com.gideon.buecherwelt.domain.usecase

import com.gideon.buecherwelt.data.network.BookMetadataFetcher
import com.gideon.buecherwelt.data.network.CoverCache
import com.gideon.buecherwelt.data.network.FetchResult
import com.gideon.buecherwelt.data.network.toBook
import com.gideon.buecherwelt.domain.recommendation.ScoredCandidate
import com.gideon.buecherwelt.domain.repository.BookRepository

class DuplicateBookException(val title: String) : Exception("Buch bereits vorhanden: $title")

class AddToLibraryUseCase(
    private val bookRepository: BookRepository,
    private val tropeRepository: com.gideon.buecherwelt.domain.repository.TropeRepository,
    private val metadataFetcher: BookMetadataFetcher,
    private val coverCache: CoverCache
) {

    suspend fun addFromCandidate(candidate: ScoredCandidate): Result<Long> {
        val fetched = candidate.metadata.isbn?.let { isbn ->
            when (val result = metadataFetcher.fetchByIsbn(isbn)) {
                is FetchResult.Success -> result.metadata
                else -> candidate.metadata
            }
        } ?: candidate.metadata
        return addMetadata(fetched)
    }

    suspend fun addMetadata(metadata: com.gideon.buecherwelt.data.network.BookMetadata): Result<Long> {
        val duplicate = bookRepository.findDuplicate(metadata.title, metadata.author, metadata.isbn)
        if (duplicate != null) {
            return Result.failure(DuplicateBookException(duplicate.title))
        }
        val coverPath = coverCache.downloadWithFallback(
            primaryUrl = metadata.coverUrl,
            isbn = metadata.isbn,
            key = CoverCache.keyFor(metadata.isbn, metadata.title, metadata.author)
        )
        val catalog = tropeRepository.getAll()
        val inferred = com.gideon.buecherwelt.domain.analysis.ReadingStatsCalculator.inferBookIntelligence(
            title = "${metadata.title} ${metadata.subtitle}".trim(),
            description = "${metadata.description} ${metadata.author}",
            categories = metadata.categories,
            catalog = catalog
        )
        val baseBook = metadata.toBook(coverPath)
        val book = baseBook.copy(
            genre = baseBook.genre.ifBlank { inferred.genres.firstOrNull().orEmpty() },
            subgenre = baseBook.subgenre.ifBlank { inferred.genres.getOrNull(1).orEmpty() },
            spiceLevel = inferred.spice
        )
        val tropeIds = tropeRepository.resolveIds(inferred.tropes.take(6))
        val id = bookRepository.insert(book, tropeIds)
        return Result.success(id)
    }
}
