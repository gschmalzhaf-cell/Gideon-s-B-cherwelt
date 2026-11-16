package com.gideon.buecherwelt.domain.report

import com.gideon.buecherwelt.data.local.ReadingStatus
import com.gideon.buecherwelt.domain.analysis.ReadingProfile
import com.gideon.buecherwelt.domain.model.Book
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppReportGenerator {

    data class Env(
        val appVersionName: String,
        val appVersionCode: Int,
        val androidRelease: String,
        val androidSdk: Int,
        val deviceManufacturer: String,
        val deviceModel: String,
        val locale: String,
        val dbVersion: Int,
        val themeMode: String,
        val llmConfigured: Boolean,
        val llmModel: String,
        val llmBaseUrl: String
    )

    fun generate(
        env: Env,
        books: List<Book>,
        tropeCount: Int,
        customTropeCount: Int,
        wishlistCount: Int,
        profile: ReadingProfile
    ): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())

        val statusCounts = ReadingStatus.entries.associateWith { s -> books.count { it.status == s } }
        val seriesCount = books.map { it.seriesName }.filter { it.isNotBlank() }.distinct().size
        val noCover = books.count { it.coverUrl == null && it.coverPath == null }
        val noDescription = books.count { it.description.isBlank() }
        val noGenre = books.count { it.genre.isBlank() }
        val noTropes = books.count { it.tropes.isEmpty() }
        val readUnrated = books.count { it.status == ReadingStatus.READ && it.rating == null }
        val withInsights = books.count { it.aiInsightsAt > 0 }

        val md = buildString {
            appendLine("# Bücherwelt – App-Report")
            appendLine("Erstellt: $timestamp")
            appendLine()
            appendLine("## App & Gerät")
            appendLine("- App-Version: ${env.appVersionName} (versionCode ${env.appVersionCode})")
            appendLine("- Android: ${env.androidRelease} (SDK ${env.androidSdk})")
            appendLine("- Gerät: ${env.deviceManufacturer} ${env.deviceModel}")
            appendLine("- Locale: ${env.locale}")
            appendLine("- Room-DB-Version: ${env.dbVersion}")
            appendLine()
            appendLine("## Einstellungen (ohne Secrets)")
            appendLine("- Theme: ${env.themeMode}")
            appendLine("- LLM konfiguriert: ${if (env.llmConfigured) "ja" else "nein"}")
            if (env.llmConfigured) {
                appendLine("- LLM-Modell: ${env.llmModel}")
                appendLine("- LLM-Basis-URL: ${env.llmBaseUrl}")
            }
            appendLine("- LLM-API-Key: **redacted** (wird nie in Reports aufgenommen)")
            appendLine()
            appendLine("## Bibliothek")
            appendLine("- Bücher gesamt: ${books.size}")
            ReadingStatus.entries.forEach { s ->
                appendLine("  - ${s.label}: ${statusCounts[s] ?: 0}")
            }
            appendLine("- Serien: $seriesCount")
            appendLine("- Tropes im Katalog: $tropeCount (davon eigene: $customTropeCount)")
            appendLine("- Merkliste: $wishlistCount")
            appendLine()
            appendLine("## Datenqualität")
            appendLine("- ohne Cover: $noCover")
            appendLine("- ohne Beschreibung: $noDescription")
            appendLine("- ohne Genre: $noGenre")
            appendLine("- ohne Tropes: $noTropes")
            appendLine("- gelesen ohne Bewertung: $readUnrated")
            appendLine("- mit KI-Einblicken: $withInsights")
            appendLine()
            appendLine("## Leseprofil (Auszug)")
            appendLine("- Top-Genres: ${profile.topGenres.joinToString { "${it.first} (${it.second})" }.ifBlank { "–" }}")
            appendLine("- Top-Tropes: ${profile.topTropes.take(5).joinToString { "${it.first} (${it.second})" }.ifBlank { "–" }}")
            appendLine("- Lieblingsautor:innen: ${profile.topAuthors.joinToString { it.first }.ifBlank { "–" }}")
            appendLine("- Unbeliebt (Abbrüche/1-2★): Genres ${profile.dislikedGenres.joinToString { it.first }.ifBlank { "–" }}; Autor:innen ${profile.dislikedAuthors.joinToString { it.first }.ifBlank { "–" }}; Tropes ${profile.dislikedTropes.joinToString { it.first }.ifBlank { "–" }}")
            appendLine("- Ø Bewertung: ${profile.avgRating?.let { "%.2f".format(it) } ?: "–"}, Ø Spice: ${profile.avgSpice?.let { "%.2f".format(it) } ?: "–"}, Abbrüche: ${profile.abandonedCount}")
            appendLine("- Lieblingsbücher (Top 5): ${profile.favorites.take(5).joinToString { it.title }.ifBlank { "–" }}")
            appendLine()
            appendLine("## JSON (maschinenlesbar)")
            appendLine("```json")
        }

        val json = buildJsonObject {
            put("reportType", "buecherwelt-app-report")
            put("generatedAt", timestamp)
            put("app", buildJsonObject {
                put("versionName", env.appVersionName)
                put("versionCode", env.appVersionCode)
                put("androidRelease", env.androidRelease)
                put("androidSdk", env.androidSdk)
                put("device", "${env.deviceManufacturer} ${env.deviceModel}")
                put("locale", env.locale)
                put("roomDbVersion", env.dbVersion)
            })
            put("settings", buildJsonObject {
                put("themeMode", env.themeMode)
                put("llmConfigured", env.llmConfigured)
                put("llmModel", env.llmModel)
                put("llmBaseUrl", env.llmBaseUrl)
                put("llmApiKey", "REDACTED")
            })
            put("library", buildJsonObject {
                put("totalBooks", books.size)
                put("byStatus", buildJsonObject {
                    ReadingStatus.entries.forEach { s -> put(s.name, statusCounts[s] ?: 0) }
                })
                put("seriesCount", seriesCount)
                put("tropeCount", tropeCount)
                put("customTropeCount", customTropeCount)
                put("wishlistCount", wishlistCount)
            })
            put("quality", buildJsonObject {
                put("noCover", noCover)
                put("noDescription", noDescription)
                put("noGenre", noGenre)
                put("noTropes", noTropes)
                put("readUnrated", readUnrated)
                put("withAiInsights", withInsights)
            })
            put("profile", buildJsonObject {
                put("topGenres", buildJsonArray {
                    profile.topGenres.forEach { (name, count) ->
                        add(buildJsonObject { put("name", name); put("count", count) })
                    }
                })
                put("topTropes", buildJsonArray {
                    profile.topTropes.take(10).forEach { (name, count) ->
                        add(buildJsonObject { put("name", name); put("count", count) })
                    }
                })
                put("topAuthors", buildJsonArray {
                    profile.topAuthors.forEach { (name, count) ->
                        add(buildJsonObject { put("name", name); put("count", count) })
                    }
                })
                put("dislikedGenres", buildJsonArray { profile.dislikedGenres.forEach { add(it.first) } })
                put("dislikedAuthors", buildJsonArray { profile.dislikedAuthors.forEach { add(it.first) } })
                put("dislikedTropes", buildJsonArray { profile.dislikedTropes.forEach { add(it.first) } })
                put("abandonedCount", profile.abandonedCount)
                put("avgRating", profile.avgRating ?: 0.0)
                put("avgSpice", profile.avgSpice ?: 0.0)
                put("favoriteTitles", buildJsonArray {
                    profile.favorites.take(10).forEach { add(it.title) }
                })
            })
            put("books", buildJsonArray {
                books.take(500).forEach { b ->
                    add(buildJsonObject {
                        put("id", b.id)
                        put("title", b.title)
                        put("author", b.author)
                        put("isbn", b.isbn ?: "")
                        put("status", b.status.name)
                        put("rating", b.rating ?: 0)
                        put("spice", b.spiceLevel ?: 0)
                        put("genre", b.genre)
                        put("series", b.seriesName)
                        put("seriesIndex", b.seriesIndex ?: 0)
                        put("tropeCount", b.tropes.size)
                        put("hasCover", b.coverUrl != null || b.coverPath != null)
                        put("hasDescription", b.description.isNotBlank())
                        put("hasAiInsights", b.aiInsightsAt > 0)
                    })
                }
            })
        }

        return md + json.toString() + "\n```\n"
    }
}
