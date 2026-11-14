package com.gideon.buecherwelt.domain.llm

import com.gideon.buecherwelt.domain.analysis.ReadingProfile
import com.gideon.buecherwelt.domain.model.Trope

object LlmPrompts {

    fun tropeSuggestion(
        catalog: List<Trope>,
        title: String,
        author: String,
        description: String,
        categories: List<String>
    ): Pair<String, String> {
        val system = """
            Du analysierst Bücher und schlägst passende Book-Tropes vor.
            Antworte AUSSCHLIESSLICH mit einem JSON-Objekt, ohne erklärenden Text:
            {"tropes": ["..."], "genres": ["..."], "spice": 0}
            - "tropes": maximal 8 Namen, exakt so geschrieben wie im Katalog.
            - "genres": 1-3 passende Genre-Bezeichnungen.
            - "spice": ganze Zahl 0-5 (0 = keine Liebesszenen, 5 = sehr explizit / Open Door).
        """.trimIndent()
        val catalogText = catalog.joinToString("\n") { "- ${it.name} (${it.category})" }
        val user = """
            Trope-Katalog:
            $catalogText

            Buch:
            Titel: $title
            Autor:in: $author
            Kategorien: ${categories.joinToString(", ").ifBlank { "keine" }}
            Klappentext: ${description.ifBlank { "keiner vorhanden" }.take(2500)}
        """.trimIndent()
        return system to user
    }

    fun bookRecommendations(profile: ReadingProfile): Pair<String, String> {
        val system = """
            Du empfiehlst Bücher anhand eines Leseprofils.
            Antworte AUSSCHLIESSLICH mit einem JSON-Objekt, ohne erklärenden Text:
            {"books": [{"title": "...", "author": "...", "reason": "..."}]}
            - maximal 8 reale, veröffentlichte Bücher.
            - "reason": ein kurzer Satz auf Deutsch, warum das Buch zum Profil passt.
        """.trimIndent()
        val user = buildString {
            appendLine("Mein Leseprofil:")
            appendLine("Anzahl Bücher: ${profile.totalBooks}")
            appendLine("Top-Genres: ${profile.topGenres.joinToString { it.first }.ifBlank { "unbekannt" }}")
            appendLine("Top-Tropes: ${profile.topTropes.joinToString { it.first }.ifBlank { "unbekannt" }}")
            appendLine("Lieblingsautor:innen: ${profile.topAuthors.joinToString { it.first }.ifBlank { "unbekannt" }}")
            appendLine("Lieblingsverlage: ${profile.topPublishers.joinToString { it.first }.ifBlank { "unbekannt" }}")
            appendLine("Durchschnittlicher Spice-Level: ${profile.avgSpice?.let { "%.1f".format(it) } ?: "unbekannt"}")
            appendLine("Lieblingsbücher (gelesen, 4-5 Sterne):")
            if (profile.favorites.isEmpty()) appendLine("- (noch keine)")
            profile.favorites.take(10).forEach {
                appendLine("- ${it.title} von ${it.author} (${it.rating}/5, ${it.tropes.joinToString { t -> t.name }})")
            }
        }
        return system to user
    }
}
