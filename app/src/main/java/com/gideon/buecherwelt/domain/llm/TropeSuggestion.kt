package com.gideon.buecherwelt.domain.llm

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class TropeSuggestion(
    val tropes: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val spice: Int? = null,
    val seriesName: String = "",
    val seriesIndex: Int? = null,
    val universeName: String = ""
)

data class LlmBookRecommendation(
    val title: String,
    val author: String,
    val reason: String
)

data class AiBookCompletion(
    val found: Boolean = true,
    val title: String = "",
    val subtitle: String = "",
    val author: String = "",
    val isbn: String = "",
    val publisher: String = "",
    val publishedYear: Int? = null,
    val pageCount: Int? = null,
    val language: String = "",
    val description: String = "",
    val genre: String = "",
    val subgenre: String = "",
    val seriesName: String = "",
    val seriesIndex: Int? = null,
    val universeName: String = "",
    val spice: Int? = null,
    val tropes: List<String> = emptyList()
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
                spice = obj["spice"]?.jsonPrimitive?.content?.toIntOrNull()?.coerceIn(0, 5),
                seriesName = obj["seriesName"]?.jsonPrimitive?.stringContentOrNull()?.trim().orEmpty(),
                seriesIndex = obj["seriesIndex"]?.jsonPrimitive?.content?.toIntOrNull(),
                universeName = obj["universeName"]?.jsonPrimitive?.stringContentOrNull()?.trim().orEmpty()
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

    fun parseBookCompletion(content: String): AiBookCompletion? {
        val raw = jsonObjectRegex.find(content)?.value ?: return null
        return try {
            val obj = Json.parseToJsonElement(raw).jsonObject
            val found = obj["found"]?.jsonPrimitive?.content?.lowercase() != "false"
            AiBookCompletion(
                found = found,
                title = obj["title"]?.jsonPrimitive?.stringContentOrNull()?.trim().orEmpty(),
                subtitle = obj["subtitle"]?.jsonPrimitive?.stringContentOrNull()?.trim().orEmpty(),
                author = obj["author"]?.jsonPrimitive?.stringContentOrNull()?.trim().orEmpty(),
                isbn = obj["isbn"]?.jsonPrimitive?.stringContentOrNull()?.trim().orEmpty(),
                publisher = obj["publisher"]?.jsonPrimitive?.stringContentOrNull()?.trim().orEmpty(),
                publishedYear = obj["publishedYear"]?.jsonPrimitive?.content?.toIntOrNull()?.takeIf { it in 1400..2100 },
                pageCount = obj["pageCount"]?.jsonPrimitive?.content?.toIntOrNull()?.takeIf { it in 10..10000 },
                language = obj["language"]?.jsonPrimitive?.stringContentOrNull()?.trim().orEmpty(),
                description = obj["description"]?.jsonPrimitive?.stringContentOrNull()?.trim().orEmpty(),
                genre = obj["genre"]?.jsonPrimitive?.stringContentOrNull()?.trim().orEmpty(),
                subgenre = obj["subgenre"]?.jsonPrimitive?.stringContentOrNull()?.trim().orEmpty(),
                seriesName = obj["seriesName"]?.jsonPrimitive?.stringContentOrNull()?.trim().orEmpty(),
                seriesIndex = obj["seriesIndex"]?.jsonPrimitive?.content?.toIntOrNull()?.takeIf { it > 0 },
                universeName = obj["universeName"]?.jsonPrimitive?.stringContentOrNull()?.trim().orEmpty(),
                spice = obj["spice"]?.jsonPrimitive?.content?.toIntOrNull()?.coerceIn(0, 5),
                tropes = obj["tropes"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.stringContentOrNull()?.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.take(10)
                    ?: emptyList()
            )
        } catch (e: Exception) {
            null
        }
    }
}
