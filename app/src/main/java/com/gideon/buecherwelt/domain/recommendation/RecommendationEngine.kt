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
        return applyDiversity(out, limit)
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
        val descLower = metadata.description.take(1200).lowercase()
        val inferred = com.gideon.buecherwelt.domain.analysis.ReadingStatsCalculator.inferBookIntelligence(
            title = metadata.title,
            description = metadata.description.take(1200),
            categories = metadata.categories,
            catalog = emptyList()
        )
        val inferredGenresLower = inferred.genres.map { it.lowercase() }
        val inferredTropesLower = inferred.tropes.map { it.lowercase() }

        profile.topAuthors.forEach { (author, _) ->
            if (author.equals(metadata.author, ignoreCase = true) ||
                metadata.author.contains(author, ignoreCase = true)
            ) {
                score += 4
                reasons.add("Autor:in aus deinen Favoriten: $author")
            }
        }
        profile.topGenres.forEach { (genre, _) ->
            val gLower = genre.lowercase()
            if (categories.any { it.contains(gLower) } ||
                inferredGenresLower.any { it.contains(gLower) || gLower.contains(it) } ||
                genre.equals(metadata.genreHint(), ignoreCase = true)
            ) {
                score += 3
                reasons.add("Genre passt zu deinen Top-Genres: $genre")
            }
        }
        val matchedTropes = profile.topTropes.map { it.first }.filter { trope ->
            val tLower = trope.lowercase()
            metadata.title.contains(trope, ignoreCase = true) ||
                categories.any { it.contains(tLower) } ||
                inferredTropesLower.any { it == tLower } ||
                descLower.contains(tLower)
        }
        if (matchedTropes.isNotEmpty()) {
            score += (matchedTropes.size * 2).coerceAtMost(6)
            reasons.add("Passt zu deinen Top-Tropes: ${matchedTropes.take(2).joinToString(", ")}")
        }
        profile.topPublishers.forEach { (publisher, _) ->
            if (publisher.equals(metadata.publisher, ignoreCase = true)) {
                score += 1
                reasons.add("Verlag aus deinen Favoriten: $publisher")
            }
        }
        if (profile.avgSpice != null && inferred.spice != null && inferred.spice > 0) {
            val diff = kotlin.math.abs(profile.avgSpice - inferred.spice)
            if (diff <= 1.0) score += 1
            else if (diff >= 2.8) score -= 1
        }
        profile.dislikedAuthors.forEach { (author, _) ->
            if (author.equals(metadata.author, ignoreCase = true) ||
                metadata.author.contains(author, ignoreCase = true)
            ) {
                score -= 4
            }
        }
        profile.dislikedGenres.forEach { (genre, _) ->
            val gLower = genre.lowercase()
            if (categories.any { it.contains(gLower) } ||
                inferredGenresLower.any { it.contains(gLower) } ||
                genre.equals(metadata.genreHint(), ignoreCase = true)
            ) {
                score -= 2
            }
        }
        val dislikedOverlap = profile.dislikedTropes.count { (trope, _) ->
            val tLower = trope.lowercase()
            metadata.title.contains(trope, ignoreCase = true) ||
                categories.any { it.contains(tLower) } ||
                inferredTropesLower.any { it == tLower } ||
                descLower.contains(tLower)
        }
        score -= 2 * dislikedOverlap
        return score to reasons.distinct()
    }

    private fun scoreAgainstBook(metadata: BookMetadata, book: Book): Pair<Int, List<String>> {
        var score = 0
        val reasons = mutableListOf<String>()
        val descLower = metadata.description.take(1200).lowercase()
        val inferred = com.gideon.buecherwelt.domain.analysis.ReadingStatsCalculator.inferBookIntelligence(
            title = metadata.title,
            description = metadata.description.take(1200),
            categories = metadata.categories,
            catalog = emptyList()
        )
        val inferredGenresLower = inferred.genres.map { it.lowercase() }
        val inferredTropesLower = inferred.tropes.map { it.lowercase() }

        if (book.author.isNotBlank() && metadata.author.contains(book.author, ignoreCase = true)) {
            score += 4
            reasons.add("Gleiche Autor:in: ${book.author}")
        }
        val categories = metadata.categories.map { it.lowercase() }
        if (book.genre.isNotBlank()) {
            val gLower = book.genre.lowercase()
            if (categories.any { it.contains(gLower) } || inferredGenresLower.any { it.contains(gLower) }) {
                score += 3
                reasons.add("Genre: ${book.genre}")
            }
        }
        val matchedTropes = book.tropes.map { it.name }.filter { tName ->
            val tLower = tName.lowercase()
            metadata.title.contains(tName, ignoreCase = true) ||
                categories.any { it.contains(tLower) } ||
                inferredTropesLower.any { it == tLower } ||
                descLower.contains(tLower)
        }
        if (matchedTropes.isNotEmpty()) {
            score += (2 * matchedTropes.size).coerceAtMost(8)
            reasons.add("Ähnliche Tropes: ${matchedTropes.take(2).joinToString(", ")}")
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

    private fun applyDiversity(candidates: List<ScoredCandidate>, limit: Int): List<ScoredCandidate> {
        val perAuthor = mutableMapOf<String, Int>()
        val perGenre = mutableMapOf<String, Int>()
        val out = mutableListOf<ScoredCandidate>()
        for (candidate in candidates.sortedByDescending { it.score }) {
            if (out.size >= limit) break
            val authorKey = candidate.metadata.author.lowercase()
            val genreKey = candidate.metadata.genreHint().lowercase()
            if (authorKey.isNotBlank() && (perAuthor[authorKey] ?: 0) >= 3) continue
            if (genreKey.isNotBlank() && (perGenre[genreKey] ?: 0) >= 4) continue
            if (authorKey.isNotBlank()) perAuthor[authorKey] = (perAuthor[authorKey] ?: 0) + 1
            if (genreKey.isNotBlank()) perGenre[genreKey] = (perGenre[genreKey] ?: 0) + 1
            out.add(candidate)
        }
        return out
    }

    companion object {
        fun keyOf(metadata: BookMetadata): String =
            (metadata.isbn ?: "${metadata.title.lowercase()}|${metadata.author.lowercase()}")
    }
}

private fun BookMetadata.genreHint(): String = categories.firstOrNull().orEmpty()
