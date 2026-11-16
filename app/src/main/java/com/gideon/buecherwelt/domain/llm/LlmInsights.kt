package com.gideon.buecherwelt.domain.llm

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class BookInsights(
    val summary: String,
    val fitScore: Int,
    val fitReason: String
)

data class SemanticSearchResult(
    val ids: List<Long>,
    val explanation: String
)

data class RerankEntry(
    val key: String,
    val score: Int,
    val reason: String
)

object LlmInsightsParser {

    private val jsonObjectRegex = Regex("\\{[\\s\\S]*\\}")

    private fun JsonPrimitive.stringContentOrNull(): String? = if (isString) content else null

    fun parseRerank(content: String): List<RerankEntry> {
        val raw = jsonObjectRegex.find(content)?.value ?: return emptyList()
        return try {
            val obj = Json.parseToJsonElement(raw).jsonObject
            obj["ranked"]?.jsonArray?.mapNotNull { element ->
                val entry = element.jsonObject
                val key = entry["key"]?.jsonPrimitive?.stringContentOrNull() ?: return@mapNotNull null
                RerankEntry(
                    key = key,
                    score = entry["score"]?.jsonPrimitive?.content?.toIntOrNull()?.coerceIn(0, 100) ?: 0,
                    reason = entry["reason"]?.jsonPrimitive?.stringContentOrNull().orEmpty()
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseSemanticSearch(content: String): SemanticSearchResult? {
        val raw = jsonObjectRegex.find(content)?.value ?: return null
        return try {
            val obj = Json.parseToJsonElement(raw).jsonObject
            SemanticSearchResult(
                ids = obj["ids"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.content.toLongOrNull() }
                    ?: emptyList(),
                explanation = obj["explanation"]?.jsonPrimitive?.stringContentOrNull().orEmpty()
            )
        } catch (e: Exception) {
            null
        }
    }

    fun parseBookInsights(content: String): BookInsights? {
        val raw = jsonObjectRegex.find(content)?.value ?: return null
        return try {
            val obj = Json.parseToJsonElement(raw).jsonObject
            val summary = obj["summary"]?.jsonPrimitive?.stringContentOrNull() ?: return null
            BookInsights(
                summary = summary,
                fitScore = obj["fitScore"]?.jsonPrimitive?.content?.toIntOrNull()?.coerceIn(0, 100) ?: 0,
                fitReason = obj["fitReason"]?.jsonPrimitive?.stringContentOrNull().orEmpty()
            )
        } catch (e: Exception) {
            null
        }
    }
}
