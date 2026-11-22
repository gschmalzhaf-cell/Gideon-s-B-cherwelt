package com.gideon.buecherwelt.domain.llm

import com.gideon.buecherwelt.data.settings.LlmConfig
import com.gideon.buecherwelt.domain.analysis.ReadingProfile
import com.gideon.buecherwelt.domain.model.Book
import com.gideon.buecherwelt.domain.model.Trope
import com.gideon.buecherwelt.domain.recommendation.RecommendationEngine
import com.gideon.buecherwelt.domain.recommendation.ScoredCandidate
import com.gideon.buecherwelt.domain.repository.TropeRepository

class LlmService(
    private val client: LlmClient,
    private val tropeRepository: TropeRepository
) {

    suspend fun suggestTropes(
        config: LlmConfig,
        title: String,
        author: String,
        description: String,
        categories: List<String>
    ): TropeSuggestion? {
        if (!config.isConfigured) return null
        val catalog = tropeRepository.getAll()
        val (system, user) = LlmPrompts.tropeSuggestion(catalog, title, author, description, categories)
        val content = client.complete(config, system, user) ?: return null
        return LlmResponseParser.parseTropeSuggestion(content)
    }

    suspend fun completeBook(
        config: LlmConfig,
        identifier: String = "",
        title: String = "",
        subtitle: String = "",
        author: String = "",
        publisher: String = "",
        publishedYear: Int? = null,
        pageCount: Int? = null,
        description: String = "",
        genre: String = "",
        seriesName: String = "",
        universeName: String = ""
    ): AiBookCompletion? {
        if (!config.isConfigured) return null
        val catalog = tropeRepository.getAll()
        val (system, user) = LlmPrompts.completeBook(
            catalog = catalog,
            identifier = identifier,
            title = title,
            subtitle = subtitle,
            author = author,
            publisher = publisher,
            publishedYear = publishedYear,
            pageCount = pageCount,
            description = description,
            genre = genre,
            seriesName = seriesName,
            universeName = universeName
        )
        val content = client.complete(config, system, user) ?: return null
        return LlmResponseParser.parseBookCompletion(content)
    }

    suspend fun recommendBooks(
        config: LlmConfig,
        profile: ReadingProfile,
        ownedTitles: List<String> = emptyList()
    ): List<LlmBookRecommendation> {
        if (!config.isConfigured) return emptyList()
        val (system, user) = LlmPrompts.bookRecommendations(profile, ownedTitles)
        val content = client.complete(config, system, user) ?: return emptyList()
        return LlmResponseParser.parseBookRecommendations(content)
    }

    suspend fun rerankCandidates(
        config: LlmConfig,
        profile: ReadingProfile,
        candidates: List<ScoredCandidate>
    ): List<ScoredCandidate>? {
        if (!config.isConfigured || candidates.isEmpty()) return null
        val (system, user) = LlmPrompts.rerankCandidates(profile, candidates)
        val content = client.complete(config, system, user) ?: return null
        val ranked = LlmInsightsParser.parseRerank(content)
        if (ranked.isEmpty()) return null
        val byKey = candidates.associateBy { RecommendationEngine.keyOf(it.metadata) }
        val seen = mutableSetOf<String>()
        val out = mutableListOf<ScoredCandidate>()
        ranked.forEach { entry ->
            val candidate = byKey[entry.key] ?: return@forEach
            if (!seen.add(entry.key)) return@forEach
            val reasons = buildList {
                if (entry.reason.isNotBlank()) add(entry.reason)
                candidate.reasons.forEach { if (it !in this) add(it) }
            }
            out.add(candidate.copy(score = entry.score, reasons = reasons))
        }
        candidates.forEach { candidate ->
            if (RecommendationEngine.keyOf(candidate.metadata) !in seen) out.add(candidate)
        }
        return out
    }

    suspend fun semanticSearch(
        config: LlmConfig,
        query: String,
        books: List<Book>
    ): SemanticSearchResult? {
        if (!config.isConfigured || query.isBlank() || books.isEmpty()) return null
        val (system, user) = LlmPrompts.semanticSearch(query, books)
        val content = client.complete(config, system, user) ?: return null
        val result = LlmInsightsParser.parseSemanticSearch(content) ?: return null
        val validIds = books.map { it.id }.toSet()
        return result.copy(ids = result.ids.filter { it in validIds }.take(30))
    }

    suspend fun bookInsights(
        config: LlmConfig,
        book: Book,
        profile: ReadingProfile
    ): BookInsights? {
        if (!config.isConfigured) return null
        val (system, user) = LlmPrompts.bookInsights(book, profile)
        val content = client.complete(config, system, user) ?: return null
        return LlmInsightsParser.parseBookInsights(content)
    }
}
