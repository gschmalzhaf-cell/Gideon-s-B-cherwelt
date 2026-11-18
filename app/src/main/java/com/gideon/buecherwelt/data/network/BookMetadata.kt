package com.gideon.buecherwelt.data.network

import com.gideon.buecherwelt.data.local.ReadingStatus
import com.gideon.buecherwelt.domain.model.Book

data class BookMetadata(
    val isbn: String? = null,
    val title: String = "",
    val subtitle: String = "",
    val author: String = "",
    val publisher: String = "",
    val publishedYear: Int? = null,
    val description: String = "",
    val categories: List<String> = emptyList(),
    val coverUrl: String? = null,
    val pageCount: Int? = null,
    val language: String = "",
    val seriesHints: List<String> = emptyList()
)

fun BookMetadata.toBook(coverPath: String? = null): Book {
    val detected = com.gideon.buecherwelt.domain.analysis.SeriesUniverseDetector.detect(
        title = title,
        subtitle = subtitle,
        author = author,
        description = description,
        apiSeries = seriesHints
    )
    return Book(
        id = 0,
        isbn = isbn?.ifBlank { null },
        title = title,
        subtitle = subtitle,
        author = author,
        publisher = publisher,
        publishedYear = publishedYear,
        description = description,
        coverUrl = coverUrl,
        coverPath = coverPath,
        genre = categories.firstOrNull() ?: "",
        subgenre = categories.getOrNull(1) ?: "",
        spiceLevel = null,
        rating = null,
        status = ReadingStatus.PLANNED,
        notes = "",
        seriesName = detected.seriesName,
        seriesIndex = detected.seriesIndex,
        universeName = detected.universeName,
        pageCount = pageCount,
        language = language,
        tropes = emptyList(),
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

sealed interface FetchResult {
    data class Success(val metadata: BookMetadata) : FetchResult
    data object NotFound : FetchResult
    data object NoNetwork : FetchResult
    data class Error(val message: String) : FetchResult
}

class NoNetworkException : Exception("Keine Netzwerkverbindung")
