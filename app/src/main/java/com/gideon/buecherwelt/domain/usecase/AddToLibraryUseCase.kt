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
        val coverPath = metadata.coverUrl?.let {
            coverCache.download(it, CoverCache.keyFor(metadata.isbn, metadata.title, metadata.author))
        }
        val id = bookRepository.insert(metadata.toBook(coverPath), emptySet())
        return Result.success(id)
    }
}
