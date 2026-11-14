package com.gideon.buecherwelt.domain.llm

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
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

    private fun JsonPrimitive.stringContentOrNull(): String? = if (isString) content else null

    fun parseTropeSuggestion(content: String): TropeSuggestion? {
        val raw = jsonObjectRegex.find(content)?.value ?: return null
        return try {
            val obj = Json.parseToJsonElement(raw).jsonObject
            TropeSuggestion(
                tropes = obj["tropes"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.stringContentOrNull() }?.take(10) ?: emptyList(),
                genres = obj["genres"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.stringContentOrNull() }?.take(4) ?: emptyList(),
                spice = obj["spice"]?.jsonPrimitive?.content?.toIntOrNull()?.coerceIn(0, 5)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun parseBookRecommendations(content: String): List<LlmBookRecommendation> {
        val raw = jsonObjectRegex.find(content)?.value ?: return emptyList()
        return try {
            val obj = Json.parseToJsonElement(raw).jsonObject
            val books = obj["books"]?.jsonArray ?: return emptyList()
            books.mapNotNull { element ->
                val book = element.jsonObject
                val title = book["title"]?.jsonPrimitive?.stringContentOrNull() ?: return@mapNotNull null
                LlmBookRecommendation(
                    title = title,
                    author = book["author"]?.jsonPrimitive?.stringContentOrNull().orEmpty(),
                    reason = book["reason"]?.jsonPrimitive?.stringContentOrNull().orEmpty()
                )
            }.take(8)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
