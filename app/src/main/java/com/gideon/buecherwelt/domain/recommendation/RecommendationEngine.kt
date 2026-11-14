package com.gideon.buecherwelt.domain.recommendation

import com.gideon.buecherwelt.data.network.BookMetadata
import com.gideon.buecherwelt.data.network.BookMetadataFetcher
import com.gideon.buecherwelt.domain.analysis.ReadingProfile
import com.gideon.buecherwelt.domain.model.Book

data class ScoredCandidate(
    val metadata: BookMetadata,
    val score: Int,
    val reasons: List<String>
)

class RecommendationEngine(
    private val fetcher: BookMetadataFetcher
) {

    suspend fun candidatesForProfile(
        profile: ReadingProfile,
        excludeKeys: Set<String>,
        limit: Int = 24
    ): List<ScoredCandidate> {
        val queries = buildList {
            profile.topGenres.take(3).forEach { add("subject:${it.first}") }
            profile.topAuthors.take(3).forEach { add("inauthor:${it.first}") }
            profile.topTropes.take(4).forEach { add("\"${it.first}\"") }
            if (isEmpty()) add("subject:Fiction")
        }
        val seen = mutableSetOf<String>()
        val out = mutableListOf<ScoredCandidate>()
        for (query in queries) {
            for (metadata in fetcher.search(query, 10)) {
                val key = keyOf(metadata)
                if (key in excludeKeys || !seen.add(key)) continue
                val (score, reasons) = scoreAgainstProfile(metadata, profile)
                if (score > 0) out.add(ScoredCandidate(metadata, score, reasons))
            }
        }
        return out.sortedByDescending { it.score }.take(limit)
    }

    suspend fun similarTo(
        book: Book,
        excludeKeys: Set<String>,
        limit: Int = 12
    ): List<ScoredCandidate> {
        val queries = buildList {
            if (book.author.isNotBlank()) add("inauthor:${book.author}")
            if (book.genre.isNotBlank()) add("subject:${book.genre}")
            book.tropes.take(3).forEach { add("\"${it.name}\"") }
            if (book.title.isNotBlank()) add("intitle:${book.title}")
        }
        val seen = mutableSetOf<String>()
        val out = mutableListOf<ScoredCandidate>()
        for (query in queries) {
            for (metadata in fetcher.search(query, 10)) {
                val key = keyOf(metadata)
                if (key in excludeKeys || !seen.add(key)) continue
                val (score, reasons) = scoreAgainstBook(metadata, book)
                if (score > 0) out.add(ScoredCandidate(metadata, score, reasons))
            }
        }
        return out.sortedByDescending { it.score }.take(limit)
    }

    private fun scoreAgainstProfile(metadata: BookMetadata, profile: ReadingProfile): Pair<Int, List<String>> {
        var score = 0
        val reasons = mutableListOf<String>()
        val categories = metadata.categories.map { it.lowercase() }
        profile.topAuthors.forEach { (author, _) ->
            if (author.equals(metadata.author, ignoreCase = true) ||
                metadata.author.contains(author, ignoreCase = true)
            ) {
                score += 3
                reasons.add("Autor:in aus deinen Favoriten: $author")
            }
        }
        profile.topGenres.forEach { (genre, _) ->
            if (categories.any { it.contains(genre.lowercase()) } ||
                genre.equals(metadata.genreHint(), ignoreCase = true)
            ) {
                score += 2
                reasons.add("Genre passt zu deinen Top-Genres: $genre")
            }
        }
        val overlap = profile.topTropes.count { (trope, _) ->
            metadata.title.contains(trope, ignoreCase = true) ||
                categories.any { it.contains(trope.lowercase()) }
        }
        if (overlap > 0) {
            score += overlap
            reasons.add("Themen aus deinen Top-Tropes")
        }
        profile.topPublishers.forEach { (publisher, _) ->
            if (publisher.equals(metadata.publisher, ignoreCase = true)) {
                score += 1
                reasons.add("Verlag aus deinen Favoriten: $publisher")
            }
        }
        return score to reasons.distinct()
    }

    private fun scoreAgainstBook(metadata: BookMetadata, book: Book): Pair<Int, List<String>> {
        var score = 0
        val reasons = mutableListOf<String>()
        if (book.author.isNotBlank() && metadata.author.contains(book.author, ignoreCase = true)) {
            score += 4
            reasons.add("Gleiche Autor:in: ${book.author}")
        }
        val categories = metadata.categories.map { it.lowercase() }
        if (book.genre.isNotBlank() && categories.any { it.contains(book.genre.lowercase()) }) {
            score += 3
            reasons.add("Genre: ${book.genre}")
        }
        val tropeHits = book.tropes.count { trope ->
            metadata.title.contains(trope.name, ignoreCase = true) ||
                categories.any { it.contains(trope.name.lowercase()) }
        }
        if (tropeHits > 0) {
            score += 2 * tropeHits
            reasons.add("Ähnliche Tropes/Themen")
        }
        if (book.publisher.isNotBlank() && book.publisher.equals(metadata.publisher, ignoreCase = true)) {
            score += 2
            reasons.add("Gleicher Verlag: ${book.publisher}")
        }
        if (book.publishedYear != null && metadata.publishedYear != null &&
            kotlin.math.abs(book.publishedYear - metadata.publishedYear) <= 5
        ) {
            score += 1
        }
        return score to reasons.distinct()
    }

    companion object {
        fun keyOf(metadata: BookMetadata): String =
            (metadata.isbn ?: "${metadata.title.lowercase()}|${metadata.author.lowercase()}")
    }
}

private fun BookMetadata.genreHint(): String = categories.firstOrNull().orEmpty()
