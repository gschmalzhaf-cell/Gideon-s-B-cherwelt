package com.gideon.buecherwelt.data.network

import com.gideon.buecherwelt.data.local.ReadingStatus
import com.gideon.buecherwelt.domain.model.Book

data class BookMetadata(
    val isbn: String? = null,
    val title: String = "",
    val author: String = "",
    val publisher: String = "",
    val publishedYear: Int? = null,
    val description: String = "",
    val categories: List<String> = emptyList(),
    val coverUrl: String? = null
)

fun BookMetadata.toBook(coverPath: String? = null): Book = Book(
    id = 0,
    isbn = isbn?.ifBlank { null },
    title = title,
    author = author,
    publisher = publisher,
    publishedYear = publishedYear,
    description = description,
    coverUrl = coverUrl,
    coverPath = coverPath,
    genre = categories.firstOrNull() ?: "",
    subgenre = "",
    spiceLevel = null,
    rating = null,
    status = ReadingStatus.PLANNED,
    notes = "",
    tropes = emptyList(),
    createdAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis()
)

sealed interface FetchResult {
    data class Success(val metadata: BookMetadata) : FetchResult
    data object NotFound : FetchResult
    data object NoNetwork : FetchResult
    data class Error(val message: String) : FetchResult
}

class NoNetworkException : Exception("Keine Netzwerkverbindung")
