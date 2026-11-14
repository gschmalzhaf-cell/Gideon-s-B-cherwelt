package com.gideon.buecherwelt.domain.llm

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class TropeSuggestion(
    val tropes: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val spice: Int? = null
)

data class LlmBookRecommendation(
    val title: String,
    val author: String,
    val reason: String
)

object LlmResponseParser {

    private val jsonObjectRegex = Regex("\\{[\\s\\S]*\\}")

    fun parseTropeSuggestion(content: String): TropeSuggestion? {
        val raw = jsonObjectRegex.find(content)?.value ?: return null
        return try {
            val obj = Json.parseToJsonObject(raw)
            TropeSuggestion(
                tropes = obj["tropes"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull }?.take(10) ?: emptyList(),
                genres = obj["genres"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull }?.take(4) ?: emptyList(),
                spice = obj["spice"]?.jsonPrimitive?.intOrNull?.coerceIn(0, 5)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun parseBookRecommendations(content: String): List<LlmBookRecommendation> {
        val raw = jsonObjectRegex.find(content)?.value ?: return emptyList()
        return try {
            val obj = Json.parseToJsonObject(raw)
            val books = obj["books"]?.jsonArray ?: return emptyList()
            books.mapNotNull { element ->
                val book = element.jsonObject
                val title = book["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                LlmBookRecommendation(
                    title = title,
                    author = book["author"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    reason = book["reason"]?.jsonPrimitive?.contentOrNull.orEmpty()
                )
            }.take(8)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
