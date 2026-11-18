package com.gideon.buecherwelt.domain.analysis

import com.gideon.buecherwelt.data.local.ReadingStatus
import com.gideon.buecherwelt.data.local.TropesCatalog
import com.gideon.buecherwelt.domain.llm.TropeSuggestion
import com.gideon.buecherwelt.domain.model.Book
import com.gideon.buecherwelt.domain.model.Trope

data class ReaderArchetype(
    val title: String = "Literatur-Entdecker:in",
    val subtitle: String = "Deine Bücherwelt wächst – jedes gelesene Buch schärft dein persönliches Profil.",
    val badge: String = "Bibliophil"
)

data class ReadingProfile(
    val totalBooks: Int = 0,
    val statusCounts: Map<ReadingStatus, Int> = emptyMap(),
    val topGenres: List<Pair<String, Int>> = emptyList(),
    val genreDistribution: List<Pair<String, Int>> = emptyList(),
    val topTropes: List<Pair<String, Int>> = emptyList(),
    val topAuthors: List<Pair<String, Int>> = emptyList(),
    val topPublishers: List<Pair<String, Int>> = emptyList(),
    val dislikedGenres: List<Pair<String, Int>> = emptyList(),
    val dislikedTropes: List<Pair<String, Int>> = emptyList(),
    val dislikedAuthors: List<Pair<String, Int>> = emptyList(),
    val abandonedCount: Int = 0,
    val avgRating: Double? = null,
    val avgSpice: Double? = null,
    val favorites: List<Book> = emptyList(),
    val archetype: ReaderArchetype = ReaderArchetype(),
    val subRecommendations: List<Pair<Book, Pair<Int, String>>> = emptyList()
)

object ReadingStatsCalculator {

    private fun weight(book: Book): Int = when {
        book.status == ReadingStatus.READ && (book.rating ?: 0) >= 4 -> 3
        book.status == ReadingStatus.READ || book.status == ReadingStatus.READING -> 2
        else -> 1
    }

    private fun isNegative(book: Book): Boolean =
        book.status == ReadingStatus.ABANDONED ||
            (book.status == ReadingStatus.READ && book.rating != null && book.rating <= 2)

    fun compute(books: List<Book>): ReadingProfile {
        if (books.isEmpty()) return ReadingProfile()
        val genres = mutableMapOf<String, Int>()
        val tropes = mutableMapOf<String, Int>()
        val authors = mutableMapOf<String, Int>()
        val publishers = mutableMapOf<String, Int>()
        val dislikedGenres = mutableMapOf<String, Int>()
        val dislikedTropes = mutableMapOf<String, Int>()
        val dislikedAuthors = mutableMapOf<String, Int>()
        val statusCounts = ReadingStatus.entries.associateWith { 0 }.toMutableMap()
        var spiceSum = 0
        var spiceCount = 0
        var ratingSum = 0
        var ratingCount = 0
        var abandoned = 0
        for (book in books) {
            val w = weight(book)
            statusCounts[book.status] = (statusCounts[book.status] ?: 0) + 1
            if (book.status == ReadingStatus.ABANDONED) abandoned++
            if (isNegative(book)) {
                if (book.genre.isNotBlank()) dislikedGenres[book.genre] = (dislikedGenres[book.genre] ?: 0) + 2
                if (book.author.isNotBlank()) dislikedAuthors[book.author] = (dislikedAuthors[book.author] ?: 0) + 2
                book.tropes.forEach { trope -> dislikedTropes[trope.name] = (dislikedTropes[trope.name] ?: 0) + 2 }
            }
            if (book.genre.isNotBlank()) genres[book.genre] = (genres[book.genre] ?: 0) + w
            if (book.author.isNotBlank()) authors[book.author] = (authors[book.author] ?: 0) + w
            if (book.publisher.isNotBlank()) publishers[book.publisher] = (publishers[book.publisher] ?: 0) + w
            book.tropes.forEach { trope -> tropes[trope.name] = (tropes[trope.name] ?: 0) + w }
            if (book.spiceLevel != null) {
                spiceSum += book.spiceLevel!!
                spiceCount++
            }
            if (book.rating != null) {
                ratingSum += book.rating!!
                ratingCount++
            }
        }
        val sortedGenres = genres.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
        fun Map<String, Int>.top(n: Int) =
            entries.sortedByDescending { it.value }.map { it.key to it.value }.take(n)
        val favorites = books
            .filter { it.status == ReadingStatus.READ && (it.rating ?: 0) >= 4 }
            .sortedWith(compareByDescending<Book> { it.rating ?: 0 }.thenBy { it.title.lowercase() })

        val avgRating = if (ratingCount > 0) ratingSum.toDouble() / ratingCount else null
        val avgSpice = if (spiceCount > 0) spiceSum.toDouble() / spiceCount else null
        val topGenres = sortedGenres.take(5)
        val topTropes = tropes.top(10)
        val topAuthors = authors.top(5)
        val topPublishers = publishers.top(5)

        val archetype = buildArchetype(
            totalBooks = books.size,
            readCount = statusCounts[ReadingStatus.READ] ?: 0,
            topGenre = topGenres.firstOrNull()?.first,
            topTrope = topTropes.firstOrNull()?.first,
            avgSpice = avgSpice,
            avgRating = avgRating
        )

        val baseProfile = ReadingProfile(
            totalBooks = books.size,
            statusCounts = statusCounts,
            topGenres = topGenres,
            genreDistribution = sortedGenres,
            topTropes = topTropes,
            topAuthors = topAuthors,
            topPublishers = topPublishers,
            dislikedGenres = dislikedGenres.top(5),
            dislikedTropes = dislikedTropes.top(5),
            dislikedAuthors = dislikedAuthors.top(5),
            abandonedCount = abandoned,
            avgRating = avgRating,
            avgSpice = avgSpice,
            favorites = favorites,
            archetype = archetype
        )

        val subRecs = books
            .filter { it.status == ReadingStatus.PLANNED }
            .map { b -> b to computeLocalFit(b, baseProfile) }
            .sortedByDescending { it.second.first }
            .take(5)

        return baseProfile.copy(subRecommendations = subRecs)
    }

    private fun buildArchetype(
        totalBooks: Int,
        readCount: Int,
        topGenre: String?,
        topTrope: String?,
        avgSpice: Double?,
        avgRating: Double?
    ): ReaderArchetype {
        val g = topGenre?.lowercase().orEmpty()
        val t = topTrope.orEmpty()
        val spice = avgSpice ?: 0.0
        val title = when {
            ("fantasy" in g || "romantasy" in g) && spice >= 2.5 -> "Romantasy- & Drachen-Chronist:in"
            "fantasy" in g -> "Weltenwanderer & Fantasy-Hüter:in"
            "romance" in g || "liebe" in g -> if (spice >= 3.0) "Leidenschaftliche:r Romance-Kenner:in" else "Herzblut-Romantiker:in"
            "thriller" in g || "krimi" in g -> "Spurensucher:in im Schatten"
            "sci" in g || "science" in g -> "Sternenreisende:r Visionär:in"
            totalBooks >= 25 -> "Meisterhafte:r Privatbibliothekar:in"
            else -> "Kurator:in der eigenen Bücherwelt"
        }
        val badge = when {
            readCount >= 20 -> "Meisterleser:in"
            (avgRating ?: 0.0) >= 4.3 -> "Feinschmecker:in"
            spice >= 3.2 -> "Spice-Liebhaber:in"
            topTrope != null -> "$topTrope-Fan"
            else -> "Bibliophil"
        }
        val subtitle = buildString {
            if (topGenre != null) {
                append("Dein Herz schlägt besonders für $topGenre")
                if (t.isNotBlank()) append(" mit „$t“")
                append(". ")
            }
            if (readCount > 0) {
                append("$readCount ${if (readCount == 1) "Buch" else "Bücher"} bereits gelesen")
                if (avgRating != null) append(" (Ø ${"%.1f".format(avgRating)} ★)")
                append(".")
            } else {
                append("$totalBooks ${if (totalBooks == 1) "Werk" else "Werke"} warten in deinen Regalen.")
            }
        }
        return ReaderArchetype(title = title, subtitle = subtitle, badge = badge)
    }

    /**
     * Berechnet lokal (100 % offline) einen Profil-Passgenauigkeits-Score (0–100)
     * samt deutscher Begründung anhand des Leseprofils.
     */
    fun computeLocalFit(book: Book, profile: ReadingProfile): Pair<Int, String> {
        if (profile.totalBooks == 0) {
            return 75 to "Neues Werk für deine Bibliothek – sammle weitere Bücher für ein präzises Leseprofil."
        }
        var score = 58
        val reasons = mutableListOf<String>()
        val dislikedHits = mutableListOf<String>()

        if (book.author.isNotBlank()) {
            val topAuthorIdx = profile.topAuthors.indexOfFirst { it.first.equals(book.author, ignoreCase = true) }
            if (topAuthorIdx >= 0) {
                score += if (topAuthorIdx == 0) 20 else 15
                reasons += "Lieblingsautor:in (${book.author})"
            }
            if (profile.dislikedAuthors.any { it.first.equals(book.author, ignoreCase = true) }) {
                score -= 22
                dislikedHits += "Autor:in (${book.author})"
            }
        }

        if (book.genre.isNotBlank()) {
            val topGenreIdx = profile.topGenres.indexOfFirst {
                it.first.equals(book.genre, ignoreCase = true) ||
                    book.genre.contains(it.first, ignoreCase = true) ||
                    it.first.contains(book.genre, ignoreCase = true)
            }
            if (topGenreIdx >= 0) {
                score += if (topGenreIdx == 0) 16 else 11
                reasons += "Top-Genre (${book.genre})"
            }
            if (profile.dislikedGenres.any { it.first.equals(book.genre, ignoreCase = true) }) {
                score -= 15
                dislikedHits += "Genre (${book.genre})"
            }
        }

        val matchingTropes = book.tropes.map { it.name }.filter { tName ->
            profile.topTropes.any { it.first.equals(tName, ignoreCase = true) }
        }
        if (matchingTropes.isNotEmpty()) {
            score += (matchingTropes.size * 7).coerceAtMost(21)
            reasons += "Deine Top-Tropes (${matchingTropes.take(2).joinToString(", ")})"
        }

        val dislikedTropes = book.tropes.map { it.name }.filter { tName ->
            profile.dislikedTropes.any { it.first.equals(tName, ignoreCase = true) }
        }
        if (dislikedTropes.isNotEmpty()) {
            score -= (dislikedTropes.size * 10).coerceAtMost(25)
            dislikedHits += "eher gemiedene Tropes (${dislikedTropes.first()})"
        }

        if (book.seriesName.isNotBlank() && profile.favorites.any { it.seriesName.equals(book.seriesName, ignoreCase = true) }) {
            score += 12
            reasons += "Fortsetzung deiner Lieblingsreihe „${book.seriesName}“"
        }

        if (profile.avgSpice != null && book.spiceLevel != null) {
            val diff = kotlin.math.abs(profile.avgSpice - book.spiceLevel)
            if (diff <= 1.0) {
                score += 6
            } else if (diff >= 2.8) {
                score -= 6
            }
        }

        val finalScore = score.coerceIn(15, 99)
        val explanation = when {
            reasons.isNotEmpty() && dislikedHits.isEmpty() ->
                "Passt hervorragend wegen: ${reasons.joinToString(" · ")}."
            reasons.isNotEmpty() && dislikedHits.isNotEmpty() ->
                "Pluspunkte: ${reasons.joinToString(", ")} – aber Achtung bei: ${dislikedHits.joinToString(", ")}."
            dislikedHits.isNotEmpty() ->
                "Vorsicht: Überschneidung mit bisherigen Abbrüchen/Kritikpunkten (${dislikedHits.joinToString(", ")})."
            else ->
                "Frischer Impuls abseits deiner bisherigen Schwerpunkte – ideal zum Entdecken neuer Facetten."
        }
        return finalScore to explanation
    }

    /**
     * Lokale Buch-Intelligenz: Erkennt automatisch passende Tropes, Genre und Spice-Level
     * aus Klappentext, Titel und Kategorien – komplett offline ohne API-Key!
     */
    fun inferBookIntelligence(
        title: String,
        description: String,
        categories: List<String>,
        catalog: List<Trope>
    ): TropeSuggestion {
        val text = "$title $description ${categories.joinToString(" ")}".lowercase()
        val matchedTropes = mutableSetOf<String>()

        // 1. Direkter Treffer aus Katalog
        catalog.forEach { trope ->
            if (trope.name.length >= 4 && text.contains(trope.name.lowercase())) {
                matchedTropes += trope.name
            }
        }

        // 2. Semantische Schlüsselwort-Regeln (Deutsch & Englisch)
        val rules: List<Pair<String, List<String>>> = listOf(
            "Enemies to Lovers" to listOf("feind", "erzfeind", "enemies", "hasst ihn", "hasst sie", "rival", "gegner"),
            "Friends to Lovers" to listOf("beste freunde", "besten freund", "friends to lovers", "kindheitsfreund", "freundschaft"),
            "Forbidden Love" to listOf("verbotene liebe", "forbidden", "nicht lieben darf", "tabu", "unerlaubt"),
            "Slow Burn" to listOf("slow burn", "langsam näher", "knisternde spannung"),
            "Forced Proximity" to listOf("gezwungen zusammen", "unter einem dach", "gefangen", "eingesperrt", "forced proximity"),
            "Fake Relationship" to listOf("scheinbeziehung", "fake dating", "verlobung zum schein", "so tun als ob"),
            "Second Chance" to listOf("zweite chance", "second chance", "ex-freund", "jahre später wieder", "alte liebe"),
            "Grumpy x Sunshine" to listOf("mürrisch", "grumpy", "sonnenschein", "sunshine", "verschlossen"),
            "Chosen One" to listOf("auserwählt", "chosen one", "prophezeiung", "schicksal der welt"),
            "Magic School" to listOf("akademie", "zauberschule", "magische schule", "academy", "internat", "college"),
            "Dark Academia" to listOf("dark academia", "geheimbund", "alte bibliothek", "elitäre universität"),
            "Dragons" to listOf("drache", "drachen", "drachenreiter", "dragon"),
            "Fated Mates" to listOf("seelenverwandt", "fated mates", "gefährte", "gefährtin", "mate"),
            "Found Family" to listOf("wahlfamilie", "found family", "außenseiter-gruppe", "crew"),
            "Hidden Heir" to listOf("verlorene erbin", "thronfolger", "geheime prinzessin", "krone", "königreich"),
            "Morally Grey Protagonist" to listOf("moralisch fragwürdig", "morally grey", "dunkle vergangenheit", "skrupellos"),
            "Dystopie" to listOf("dystopie", "dystopian", "totalitär", "überwachungsstaat", "regime"),
            "Postapokalypse" to listOf("apokalypse", "nach dem untergang", "endzeit", "überlebende"),
            "Zeitreise" to listOf("zeitreise", "time travel", "vergangenheit reisen", "jahrhundert"),
            "KI wird selbstbewusst" to listOf("künstliche intelligenz", "android", "roboter", "ki "),
            "Unzuverlässiger Erzähler" to listOf("wahrheit oder lüge", "erinnerungslücken", "unreliable", "traue niemandem"),
            "Cold Case" to listOf("cold case", "ungelöster fall", "vor jahren verschwunden", "alter mordfall"),
            "Kleine Stadt mit Geheimnissen" to listOf("kleinstadt", "dorf", "idyllisch", "dunkles geheimnis"),
            "Großer Plot-Twist am Ende" to listOf("nichts ist wie es scheint", "wendepunkt", "plot twist", "abgrund"),
            "Coming of Age" to listOf("erwachsenwerden", "coming of age", "selbstfindung", "jugend"),
            "Revenge" to listOf("rache", "vergeltung", "revenge", "rächen"),
            "Heist" to listOf("coup", "einbruch", "heist", "diebstahl", "meisterdieb"),
            "Survival" to listOf("überleben", "wildnis", "survival", "kampf ums überleben")
        )

        val catalogNames = catalog.map { it.name }.toSet().ifEmpty {
            TropesCatalog.entries.map { it.first }.toSet()
        }
        for ((tropeName, keywords) in rules) {
            if (tropeName in catalogNames && keywords.any { kw -> text.contains(kw) }) {
                matchedTropes += tropeName
            }
        }

        // 3. Genre-Erkennung
        val inferredGenres = mutableListOf<String>()
        val genreRules: List<Pair<String, List<String>>> = listOf(
            "Romantasy" to listOf("romantasy", "drachenreiter", "fae", "magie und leidenschaft"),
            "Fantasy" to listOf("fantasy", "magie", "zauber", "drache", "königreich", "elfen", "dämon"),
            "Romance" to listOf("romance", "liebesroman", "herzklopfen", "verliebt", "liebe"),
            "Krimi & Thriller" to listOf("thriller", "krimi", "kommissar", "ermittler", "mord", "verbrechen", "psychothriller"),
            "Sci-Fi" to listOf("science fiction", "sci-fi", "weltraum", "galaxis", "zukunft", "planet", "raumschiff"),
            "Historischer Roman" to listOf("historisch", "jahrhundert", "mittelalter", "viktorianisch"),
            "Sachbuch" to listOf("sachbuch", "ratgeber", "biografie", "geschichte", "wissenschaft")
        )
        for ((genreName, keywords) in genreRules) {
            if (keywords.any { kw -> text.contains(kw) }) {
                inferredGenres += genreName
            }
        }
        categories.forEach { cat ->
            val cleaned = cat.trim()
            if (cleaned.isNotBlank() && cleaned !in inferredGenres && inferredGenres.size < 3) {
                inferredGenres += cleaned
            }
        }

        // 4. Spice-Schätzung
        val inferredSpice: Int = when {
            listOf("erotica", "dark romance", "explizit", "open door", "smut").any { text.contains(it) } -> 4
            listOf("leidenschaft", "sinnlich", "verführung", "knistern", "begehren", "romantasy").any { text.contains(it) } -> 3
            listOf("romantik", "liebesgeschichte", "kuss", "verliebt", "romance").any { text.contains(it) } -> 2
            listOf("freundschaft", "gefühle").any { text.contains(it) } -> 1
            else -> 0
        }

        return TropeSuggestion(
            tropes = matchedTropes.take(8).toList(),
            genres = inferredGenres.distinct().take(3),
            spice = inferredSpice
        )
    }

    /**
     * Erkennt aus einem Buchtitel automatisch Serienname + Bandnummer, z. B.:
     * "Kingmakers – Jahr 1" -> ("Kingmakers", 1)
     * "Fourth Wing (Flammengeküsst, Band 1)" -> ("Flammengeküsst", 1)
     * "Die Erben der Nacht - Teil 3: ..." -> ("Die Erben der Nacht", 3)
     */
    fun detectSeriesFromTitle(rawTitle: String, existingSeries: List<String> = emptyList()): Pair<String, Int?>? {
        val title = rawTitle.trim()
        if (title.isBlank()) return null

        // Prüfe zunächst, ob eine bekannte Serie im Titel vorkommt
        for (known in existingSeries) {
            if (known.length >= 3 && title.contains(known, ignoreCase = true)) {
                val numMatch = Regex("(?:Band|Bd\\.?|Teil|Vol\\.?|Buch|Jahr|#)\\s*(\\d{1,3})", RegexOption.IGNORE_CASE)
                    .find(title)
                return known to numMatch?.groupValues?.get(1)?.toIntOrNull()
            }
        }

        // Muster 1: "Titel (Serienname, Band 2)" oder "Titel (Serienname 2)"
        val parenRegex = Regex("\\(([^()]+?)(?:[,\\s–-]+(?:Band|Bd\\.?|Teil|Vol\\.?|Buch|Jahr|#)?\\s*(\\d{1,3}))\\)$", RegexOption.IGNORE_CASE)
        parenRegex.find(title)?.let { m ->
            val series = m.groupValues[1].trim()
            val index = m.groupValues[2].toIntOrNull()
            if (series.length >= 3 && !series.equals("Roman", ignoreCase = true)) {
                return series to index
            }
        }

        // Muster 2: "Serienname – Band 1" / "Serienname: Jahr 1" / "Serienname - Teil 2"
        val dashRegex = Regex("^(.+?)[\\s]*[–—:-][\\s]*(?:Band|Bd\\.?|Teil|Vol\\.?|Buch|Jahr|#)\\s*(\\d{1,3})\\b", RegexOption.IGNORE_CASE)
        dashRegex.find(title)?.let { m ->
            val series = m.groupValues[1].trim()
            val index = m.groupValues[2].toIntOrNull()
            if (series.length >= 3) {
                return series to index
            }
        }

        return null
    }
}

