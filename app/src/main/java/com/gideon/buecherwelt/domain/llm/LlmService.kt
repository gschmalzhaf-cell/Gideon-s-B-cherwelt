package com.gideon.buecherwelt.domain.llm

import com.gideon.buecherwelt.data.settings.LlmConfig
import com.gideon.buecherwelt.domain.analysis.ReadingProfile
import com.gideon.buecherwelt.domain.model.Trope
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

    suspend fun recommendBooks(config: LlmConfig, profile: ReadingProfile): List<LlmBookRecommendation> {
        if (!config.isConfigured) return emptyList()
        val (system, user) = LlmPrompts.bookRecommendations(profile)
        val content = client.complete(config, system, user) ?: return emptyList()
        return LlmResponseParser.parseBookRecommendations(content)
    }
}
