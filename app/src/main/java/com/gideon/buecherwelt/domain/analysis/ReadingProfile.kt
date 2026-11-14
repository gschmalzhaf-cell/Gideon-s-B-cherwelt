package com.gideon.buecherwelt.domain.analysis

import com.gideon.buecherwelt.data.local.ReadingStatus
import com.gideon.buecherwelt.domain.model.Book

data class ReadingProfile(
    val totalBooks: Int = 0,
    val statusCounts: Map<ReadingStatus, Int> = emptyMap(),
    val topGenres: List<Pair<String, Int>> = emptyList(),
    val genreDistribution: List<Pair<String, Int>> = emptyList(),
    val topTropes: List<Pair<String, Int>> = emptyList(),
    val topAuthors: List<Pair<String, Int>> = emptyList(),
    val topPublishers: List<Pair<String, Int>> = emptyList(),
    val avgSpice: Double? = null,
    val favorites: List<Book> = emptyList()
)

object ReadingStatsCalculator {

    private fun weight(book: Book): Int = when {
        book.status == ReadingStatus.READ && (book.rating ?: 0) >= 4 -> 3
        book.status == ReadingStatus.READ || book.status == ReadingStatus.READING -> 2
        else -> 1
    }

    fun compute(books: List<Book>): ReadingProfile {
        if (books.isEmpty()) return ReadingProfile()
        val genres = mutableMapOf<String, Int>()
        val tropes = mutableMapOf<String, Int>()
        val authors = mutableMapOf<String, Int>()
        val publishers = mutableMapOf<String, Int>()
        val statusCounts = ReadingStatus.entries.associateWith { 0 }.toMutableMap()
        var spiceSum = 0
        var spiceCount = 0
        for (book in books) {
            val w = weight(book)
            statusCounts[book.status] = (statusCounts[book.status] ?: 0) + 1
            if (book.genre.isNotBlank()) genres[book.genre] = (genres[book.genre] ?: 0) + w
            if (book.author.isNotBlank()) authors[book.author] = (authors[book.author] ?: 0) + w
            if (book.publisher.isNotBlank()) publishers[book.publisher] = (publishers[book.publisher] ?: 0) + w
            book.tropes.forEach { trope -> tropes[trope.name] = (tropes[trope.name] ?: 0) + w }
            if (book.spiceLevel != null) {
                spiceSum += book.spiceLevel!!
                spiceCount++
            }
        }
        val sortedGenres = genres.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
        fun Map<String, Int>.top(n: Int) =
            entries.sortedByDescending { it.value }.map { it.key to it.value }.take(n)
        val favorites = books
            .filter { it.status == ReadingStatus.READ && (it.rating ?: 0) >= 4 }
            .sortedWith(compareByDescending<Book> { it.rating ?: 0 }.thenBy { it.title.lowercase() })
        return ReadingProfile(
            totalBooks = books.size,
            statusCounts = statusCounts,
            topGenres = sortedGenres.take(5),
            genreDistribution = sortedGenres,
            topTropes = tropes.top(10),
            topAuthors = authors.top(5),
            topPublishers = publishers.top(5),
            avgSpice = if (spiceCount > 0) spiceSum.toDouble() / spiceCount else null,
            favorites = favorites
        )
    }
}
