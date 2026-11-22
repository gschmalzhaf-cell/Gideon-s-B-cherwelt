package com.gideon.buecherwelt.domain.llm

import com.gideon.buecherwelt.domain.analysis.ReadingProfile
import com.gideon.buecherwelt.domain.model.Book
import com.gideon.buecherwelt.domain.model.Trope
import com.gideon.buecherwelt.domain.recommendation.RecommendationEngine
import com.gideon.buecherwelt.domain.recommendation.ScoredCandidate
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object LlmPrompts {

    fun tropeSuggestion(
        catalog: List<Trope>,
        title: String,
        author: String,
        description: String,
        categories: List<String>
    ): Pair<String, String> {
        val system = """
            Du analysierst Bücher und schlägst passende Book-Tropes sowie Buchreihe, Bandnummer und Buch-Universum vor.
            Antworte AUSSCHLIESSLICH mit einem JSON-Objekt, ohne erklärenden Text:
            {"tropes": ["..."], "genres": ["..."], "spice": 0, "seriesName": "", "seriesIndex": null, "universeName": ""}
            - "tropes": maximal 8 Namen, exakt so geschrieben wie im Katalog.
            - "genres": 1-3 passende Genre-Bezeichnungen.
            - "spice": ganze Zahl 0-5 (0 = keine Liebesszenen, 5 = sehr explizit / Open Door).
            - "seriesName": Name der Buchreihe (leer lassen, wenn Einzelband ohne Reihe).
            - "seriesIndex": Bandnummer als ganze Zahl (z. B. 1, 2, 3) oder null.
            - "universeName": Name des übergeordneten Buch-Universums / der Buchwelt (z. B. "Grishaverse", "Maasverse", "Schattenjäger-Universum", "Empyrean-Universum"), sonst leer.
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

    fun completeBook(
        catalog: List<Trope>,
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
    ): Pair<String, String> {
        val system = """
            Du bist ein präziser Buch-Assistent für eine persönliche Bibliothek.
            Deine Aufgabe: Identifiziere das gesuchte Buch anhand von Kennung (ISBN/ASIN/Goodreads-ID), Titel oder Autor:in und vervollständige fehlende Buchdaten.
            Antworte AUSSCHLIESSLICH mit einem JSON-Objekt ohne erklärenden Text:
            {
              "found": true,
              "title": "",
              "subtitle": "",
              "author": "",
              "isbn": "",
              "publisher": "",
              "publishedYear": null,
              "pageCount": null,
              "language": "de",
              "description": "",
              "genre": "",
              "subgenre": "",
              "seriesName": "",
              "seriesIndex": null,
              "universeName": "",
              "spice": 0,
              "tropes": []
            }
            Regeln:
            - Wenn du die Kennung (z. B. eine unbekannte ASIN) oder das Buch NICHT sicher kennst, setze "found": false und erfinde KEINE Fakten!
            - "description": spoilerfreier, aussagekräftiger deutscher Klappentext (3-5 Sätze), falls noch keiner vorhanden ist.
            - "seriesName" / "seriesIndex": Name der Buchreihe und Bandnummer (ganze Zahl), falls Teil einer Reihe.
            - "universeName": Name des übergeordneten Buch-Universums (z. B. "Maasverse", "Grishaverse", "Empyrean-Universum", "Schattenjäger-Universum"), sonst leer.
            - "spice": ganze Zahl 0-5.
            - "tropes": maximal 8 passende Namen exakt wie im Katalog.
        """.trimIndent()
        val catalogText = catalog.joinToString(", ") { it.name }
        val user = buildString {
            appendLine("Vorhandene Angaben:")
            if (identifier.isNotBlank()) appendLine("- Kennung (ISBN/ASIN/ID): $identifier")
            if (title.isNotBlank()) appendLine("- Titel: $title")
            if (subtitle.isNotBlank()) appendLine("- Untertitel: $subtitle")
            if (author.isNotBlank()) appendLine("- Autor:in: $author")
            if (publisher.isNotBlank()) appendLine("- Verlag: $publisher")
            if (publishedYear != null) appendLine("- Erscheinungsjahr: $publishedYear")
            if (pageCount != null) appendLine("- Seitenzahl: $pageCount")
            if (genre.isNotBlank()) appendLine("- Genre: $genre")
            if (seriesName.isNotBlank()) appendLine("- Buchreihe: $seriesName")
            if (universeName.isNotBlank()) appendLine("- Buch-Universum: $universeName")
            if (description.isNotBlank()) appendLine("- Vorhandener Klappentext: ${description.take(1500)}")
            appendLine()
            appendLine("Verfügbare Tropes: $catalogText")
        }
        return system to user
    }

    fun bookRecommendations(
        profile: ReadingProfile,
        ownedTitles: List<String> = emptyList()
    ): Pair<String, String> {
        val system = """
            Du empfiehlst Bücher anhand eines Leseprofils.
            Antworte AUSSCHLIESSLICH mit einem JSON-Objekt, ohne erklärenden Text:
            {"books": [{"title": "...", "author": "...", "reason": "..."}]}
            - maximal 8 reale, veröffentlichte Bücher.
            - "reason": ein kurzer Satz auf Deutsch, warum das Buch zum Profil passt.
            - Berücksichtige auch die unbeliebten Genres/Tropes/Autor:innen (Abbrüche, niedrige Bewertungen) und meide diese.
            - Empfiehl KEINE Bücher, die bereits in der Liste der vorhandenen Titel stehen.
        """.trimIndent()
        val ownedBlock = if (ownedTitles.isNotEmpty()) {
            "\n\nBereits in meiner Bibliothek (nicht erneut empfehlen):\n" +
                ownedTitles.take(80).joinToString(", ")
        } else ""
        val user = "Mein Leseprofil:\n${profileText(profile)}$ownedBlock"
        return system to user
    }

    fun rerankCandidates(profile: ReadingProfile, candidates: List<ScoredCandidate>): Pair<String, String> {
        val system = """
            Du ordnest Buch-Empfehlungen nach einem Leseprofil neu (bestes zuerst).
            Antworte AUSSCHLIESSLICH mit einem JSON-Objekt, ohne erklärenden Text:
            {"ranked": [{"key": "...", "score": 0-100, "reason": "..."}]}
            - "ranked" enthält ALLE übergebenen keys, bester zuerst.
            - "score": ganze Zahl 0-100, wie gut das Buch zum Profil passt (unbeliebte Genres/Tropes/Autor:innen senken den Score).
            - "reason": ein kurzer Satz auf Deutsch, begründet am Profil (z. B. "weil du X magst").
        """.trimIndent()
        val candidatesJson = buildJsonArray {
            candidates.forEach { candidate ->
                add(buildJsonObject {
                    put("key", RecommendationEngine.keyOf(candidate.metadata))
                    put("title", candidate.metadata.title)
                    put("author", candidate.metadata.author)
                    put("categories", buildJsonArray {
                        candidate.metadata.categories.forEach { add(it) }
                    })
                    put("description", candidate.metadata.description.take(300))
                    put("ruleReasons", buildJsonArray {
                        candidate.reasons.forEach { add(it) }
                    })
                })
            }
        }
        val user = "Leseprofil:\n${profileText(profile)}\n\nKandidaten:\n$candidatesJson"
        return system to user
    }

    fun semanticSearch(query: String, books: List<Book>): Pair<String, String> {
        val system = """
            Du erhältst eine Freitext-Frage und die Bücher einer Bibliothek.
            Antworte AUSSCHLIESSLICH mit einem JSON-Objekt, ohne erklärenden Text:
            {"ids": [1, 2, 3], "explanation": "..."}
            - "ids": IDs der passenden Bücher (nur IDs aus der Liste, beste zuerst, maximal 30).
            - "explanation": ein kurzer Satz auf Deutsch, warum diese Bücher zur Frage passen.
            - Bei keinem Treffer: {"ids": [], "explanation": "..."}.
        """.trimIndent()
        val capped = books.take(150)
        val booksJson = buildJsonArray {
            capped.forEach { book ->
                add(buildJsonObject {
                    put("id", book.id)
                    put("title", book.title)
                    put("author", book.author)
                    put("genre", book.genre)
                    if (book.seriesName.isNotBlank()) put("series", book.seriesName)
                    if (book.seriesIndex != null) put("band", book.seriesIndex)
                    if (book.universeName.isNotBlank()) put("universe", book.universeName)
                    if (book.pageCount != null) put("pages", book.pageCount)
                    put("tropes", buildJsonArray {
                        book.tropes.forEach { add(it.name) }
                    })
                    put("status", book.status.label)
                    put("rating", book.rating ?: 0)
                    put("spice", book.spiceLevel ?: 0)
                    put("notes", book.notes.take(200))
                    put("description", book.description.take(300))
                })
            }
        }
        val truncation = if (books.size > capped.size) {
            "\n(Hinweis: Bibliothek auf ${capped.size} von ${books.size} Büchern gekürzt)"
        } else ""
        val user = "Frage: $query\n\nBibliothek:$truncation\n$booksJson"
        return system to user
    }

    fun bookInsights(book: Book, profile: ReadingProfile): Pair<String, String> {
        val system = """
            Du analysierst ein Buch und schätzt ein, wie gut es zu einem Leseprofil passt.
            Antworte AUSSCHLIESSLICH mit einem JSON-Objekt, ohne erklärenden Text:
            {"summary": "...", "fitScore": 0-100, "fitReason": "..."}
            - "summary": spoilerfreie Zusammenfassung in 2-3 Sätzen auf Deutsch (Handlung + Stimmung, keine Auflösung).
            - "fitScore": ganze Zahl 0-100, Passgenauigkeit zum Profil (Abbrüche und 1-2-Sterne-Bewertungen berücksichtigen!).
            - "fitReason": ein Satz auf Deutsch mit der Begründung für den Score.
        """.trimIndent()
        val user = buildString {
            appendLine("Leseprofil:")
            appendLine(profileText(profile))
            appendLine()
            appendLine("Buch:")
            appendLine("Titel: ${book.title}")
            appendLine("Autor:in: ${book.author}")
            appendLine("Genre: ${book.genre.ifBlank { "unbekannt" }}")
            if (book.seriesName.isNotBlank()) {
                appendLine("Buchreihe: ${book.seriesName}${book.seriesIndex?.let { " (Band $it)" } ?: ""}")
            }
            if (book.universeName.isNotBlank()) {
                appendLine("Buch-Universum: ${book.universeName}")
            }
            if (book.pageCount != null) {
                appendLine("Seiten: ${book.pageCount}")
            }
            appendLine("Tropes: ${book.tropes.joinToString { it.name }.ifBlank { "keine" }}")
            appendLine("Klappentext: ${book.description.ifBlank { "keiner vorhanden" }.take(2500)}")
            appendLine("Eigenes Rating: ${book.rating ?: "-"}/5, Status: ${book.status.label}")
            appendLine("Eigene Notizen: ${book.notes.ifBlank { "-" }.take(500)}")
        }
        return system to user
    }

    private fun profileText(profile: ReadingProfile): String = buildString {
        appendLine("Anzahl Bücher: ${profile.totalBooks}")
        appendLine("Top-Genres: ${profile.topGenres.joinToString { it.first }.ifBlank { "unbekannt" }}")
        appendLine("Top-Tropes: ${profile.topTropes.joinToString { it.first }.ifBlank { "unbekannt" }}")
        appendLine("Lieblingsautor:innen: ${profile.topAuthors.joinToString { it.first }.ifBlank { "unbekannt" }}")
        appendLine("Lieblingsverlage: ${profile.topPublishers.joinToString { it.first }.ifBlank { "unbekannt" }}")
        appendLine("Durchschnittliche Bewertung: ${profile.avgRating?.let { "%.1f".format(it) } ?: "unbekannt"} / 5")
        appendLine("Durchschnittlicher Spice-Level: ${profile.avgSpice?.let { "%.1f".format(it) } ?: "unbekannt"}")
        appendLine("Abgebrochene Bücher: ${profile.abandonedCount}")
        if (profile.dislikedGenres.isNotEmpty()) {
            appendLine("Unbeliebte Genres (Abbrüche / 1-2 Sterne): ${profile.dislikedGenres.joinToString { it.first }}")
        }
        if (profile.dislikedAuthors.isNotEmpty()) {
            appendLine("Unbeliebte Autor:innen: ${profile.dislikedAuthors.joinToString { it.first }}")
        }
        if (profile.dislikedTropes.isNotEmpty()) {
            appendLine("Unbeliebte Tropes: ${profile.dislikedTropes.joinToString { it.first }}")
        }
        appendLine("Lieblingsbücher (gelesen, 4-5 Sterne):")
        if (profile.favorites.isEmpty()) appendLine("- (noch keine)")
        profile.favorites.take(10).forEach {
            appendLine("- ${it.title} von ${it.author} (${it.rating}/5, ${it.tropes.joinToString { t -> t.name }})")
        }
    }
}
