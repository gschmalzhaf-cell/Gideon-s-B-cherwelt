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
            .map { b -> b to computeLocalFit(b, baseProfile, books) }
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
     * samt deutscher Begründung anhand des Leseprofils (inkl. Reihen-Kontinuität & Universum).
     */
    fun computeLocalFit(
        book: Book,
        profile: ReadingProfile,
        allBooks: List<Book> = emptyList()
    ): Pair<Int, String> {
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

        // Reihen-Kontinuität: direkter Folgeband einer gelesenen Reihe bekommt starken Boost
        var seriesBoostApplied = false
        if (book.seriesName.isNotBlank() && book.seriesIndex != null && allBooks.isNotEmpty()) {
            val prevVol = allBooks.firstOrNull {
                it.id != book.id &&
                    it.seriesName.equals(book.seriesName, ignoreCase = true) &&
                    it.seriesIndex == book.seriesIndex - 1 &&
                    it.status == ReadingStatus.READ
            }
            if (prevVol != null) {
                val prevRating = prevVol.rating ?: 4
                if (prevRating >= 3) {
                    score += if (prevRating >= 4) 18 else 10
                    reasons += "Direkte Fortsetzung nach Band ${book.seriesIndex - 1} von „${book.seriesName}“"
                    seriesBoostApplied = true
                }
            } else if (book.seriesIndex > 1 && book.status == ReadingStatus.PLANNED) {
                val unreadPredecessor = allBooks.any {
                    it.id != book.id &&
                        it.seriesName.equals(book.seriesName, ignoreCase = true) &&
                        (it.seriesIndex ?: Int.MAX_VALUE) < book.seriesIndex &&
                        it.status == ReadingStatus.PLANNED
                }
                if (unreadPredecessor) {
                    score -= 10
                }
            }
        }

        if (!seriesBoostApplied && book.seriesName.isNotBlank() &&
            profile.favorites.any { it.seriesName.equals(book.seriesName, ignoreCase = true) }
        ) {
            score += 12
            reasons += "Fortsetzung deiner Lieblingsreihe „${book.seriesName}“"
        }

        if (book.universeName.isNotBlank() &&
            profile.favorites.any { it.universeName.equals(book.universeName, ignoreCase = true) } &&
            reasons.none { it.contains(book.universeName, ignoreCase = true) }
        ) {
            score += 9
            reasons += "Lieblings-Universum „${book.universeName}“"
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

    data class BookCharacteristics(
        val moods: List<String> = emptyList(),
        val pacing: String = "Ausgewogen",
        val settingHints: List<String> = emptyList(),
        val spiceLabel: String = "",
        val estimatedHours: Double? = null,
        val suggestedTropes: List<String> = emptyList(),
        val detectedSeriesUniverse: SeriesUniverseInfo = SeriesUniverseInfo()
    )

    fun analyzeBookCharacteristics(
        book: Book,
        catalog: List<Trope> = emptyList(),
        existingSeries: List<String> = emptyList(),
        existingUniverses: List<String> = emptyList()
    ): BookCharacteristics {
        val text = "${book.title} ${book.subtitle} ${book.description} ${book.genre} ${book.subgenre} ${book.seriesName} ${book.universeName}".lowercase()
        val assignedTropeNames = book.tropes.map { it.name }.toSet()

        val inferred = inferBookIntelligence(
            title = "${book.title} ${book.subtitle}".trim(),
            description = "${book.description} ${book.author} ${book.seriesName} ${book.universeName}",
            categories = listOfNotNull(
                book.genre.takeIf { it.isNotBlank() },
                book.subgenre.takeIf { it.isNotBlank() }
            ),
            catalog = catalog
        )
        val allTropeNames = (assignedTropeNames + inferred.tropes).map { it.lowercase() }

        // Moods / Atmosphäre
        val moods = mutableListOf<String>()
        if (allTropeNames.any { it in listOf("dragons", "chosen one", "hidden heir", "magic school", "portal fantasy", "fated mates") } ||
            listOf("magie", "drache", "königreich", "fantasy", "fae", "zauber").any { text.contains(it) }
        ) {
            moods += "Magisch & Episch"
        }
        if (allTropeNames.any { it in listOf("dark academia", "morally grey protagonist", "antihero", "revenge", "dystopie", "postapokalypse") } ||
            listOf("düster", "dunkel", "schatten", "geheimnis", "blut", "fluch", "abgrund", "mafia", "dark").any { text.contains(it) }
        ) {
            moods += "Düster & Geheimnisvoll"
        }
        if (allTropeNames.any { it in listOf("enemies to lovers", "friends to lovers", "slow burn", "forbidden love", "second chance", "grumpy x sunshine", "soulmates") } ||
            listOf("liebe", "herz", "romantisch", "gefühle", "leidenschaft", "kuss", "romance").any { text.contains(it) }
        ) {
            moods += "Emotional & Romantisch"
        }
        if (allTropeNames.any { it in listOf("cold case", "katzen-und-maus", "großer plot-twist am ende", "unzuverlässiger erzähler", "heist", "survival") } ||
            listOf("thriller", "krimi", "mord", "gefahr", "jagd", "überleben", "spannung", "verschwörung").any { text.contains(it) }
        ) {
            moods += "Spannend & Nervenaufreibend"
        }
        if (allTropeNames.any { it in listOf("found family", "childhood friends", "coming of age", "fake relationship") } ||
            listOf("humor", "warmherzig", "freundschaft", "familie", "sommer", "wohlfühl", "cozy").any { text.contains(it) }
        ) {
            moods += "Warmherzig & Charakterstark"
        }
        if (moods.isEmpty()) {
            moods += "Atmosphärisch & Fesselnd"
        }

        // Pacing
        val pacing = when {
            allTropeNames.contains("slow burn") || text.contains("slow burn") -> "Slow Burn (knisternder Aufbau)"
            listOf("thriller", "heist", "survival", "jagd", "atemlos", "pageturner").any { text.contains(it) } -> "Rasanter Pageturner"
            (book.pageCount ?: 0) >= 550 || text.contains("episch") || text.contains("chronik") -> "Episch & Weltbildend"
            else -> "Ausgewogenes Erzähltempo"
        }

        // Setting / Schauplatz-Hinweise
        val settings = mutableListOf<String>()
        val settingRules = listOf(
            "Akademie / Campus" to listOf("akademie", "academy", "college", "universität", "internat", "schule", "campus"),
            "Königreich / Hof" to listOf("königreich", "thron", "hof", "palast", "krone", "imperium"),
            "Kleinstadt" to listOf("kleinstadt", "dorf", "küstenstadt", "small town"),
            "Großstadt / Metropole" to listOf("new york", "london", "chicago", "berlin", "paris", "metropole"),
            "Zukunft / Weltraum" to listOf("weltraum", "planet", "galaxis", "raumschiff", "zukunft", "dystopie")
        )
        for ((label, kws) in settingRules) {
            if (kws.any { text.contains(it) }) settings += label
        }

        val effectiveSpice = book.spiceLevel ?: inferred.spice ?: 0
        val spiceLabel = TropesCatalog.spiceLevels.firstOrNull { it.first == effectiveSpice }?.second
            ?: TropesCatalog.spiceLevels.first().second

        val hours = book.pageCount?.takeIf { it > 0 }?.let { pages ->
            (pages / 50.0 * 10.0).toInt() / 10.0
        }

        val detectedSU = SeriesUniverseDetector.detect(
            title = book.title,
            subtitle = book.subtitle,
            author = book.author,
            description = book.description,
            existingSeries = existingSeries,
            existingUniverses = existingUniverses
        )

        val additionalTropes = inferred.tropes.filter { t ->
            assignedTropeNames.none { it.equals(t, ignoreCase = true) }
        }

        return BookCharacteristics(
            moods = moods.take(4),
            pacing = pacing,
            settingHints = settings.take(3),
            spiceLabel = spiceLabel,
            estimatedHours = hours,
            suggestedTropes = additionalTropes,
            detectedSeriesUniverse = detectedSU
        )
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
            "Enemies to Lovers" to listOf("feind", "erzfeind", "enemies", "hasst ihn", "hasst sie", "rival", "gegner", "widersacher", "fourth wing", "twisted hate", "cruel prince", "elfenkrone", "powerless"),
            "Friends to Lovers" to listOf("beste freunde", "besten freund", "friends to lovers", "kindheitsfreund", "freundschaft"),
            "Childhood Friends" to listOf("kindheit", "sandkasten", "seit sie kinder", "childhood"),
            "Forbidden Love" to listOf("verbotene liebe", "forbidden", "nicht lieben darf", "tabu", "unerlaubt", "verboten"),
            "Slow Burn" to listOf("slow burn", "langsam näher", "knisternde spannung", "allmählich"),
            "Forced Proximity" to listOf("gezwungen zusammen", "unter einem dach", "gefangen", "eingesperrt", "forced proximity", "zusammenarbeiten", "leibwächter", "bodyguard"),
            "Fake Relationship" to listOf("scheinbeziehung", "fake dating", "verlobung zum schein", "so tun als ob", "the deal"),
            "Marriage of Convenience" to listOf("zweckehe", "arrangierte ehe", "zwangsheirat", "marriage of convenience", "king of wrath", "brutal prince"),
            "Second Chance" to listOf("zweite chance", "second chance", "ex-freund", "jahre später wieder", "alte liebe", "wiedersehen", "begin again"),
            "Grumpy x Sunshine" to listOf("mürrisch", "grumpy", "sonnenschein", "sunshine", "verschlossen", "eiskalt", "the fine print", "icebreaker"),
            "Brother's Best Friend" to listOf("bester freund ihres bruders", "freund meines bruders", "brother's best friend", "twisted love"),
            "Boss-Employee" to listOf("chef", "praktikantin", "assistentin", "boss", "büro", "milliardär", "ceo"),
            "Secret Billionaire" to listOf("milliardär", "billionaire", "reich", "erbe eines imperiums"),
            "Secret Royal" to listOf("prinz", "prinzessin", "royal", "adel", "königsfamilie", "thronfolger", "twisted games"),
            "Chosen One" to listOf("auserwählt", "chosen one", "prophezeiung", "schicksal der welt", "harry potter", "shadow and bone", "goldene flammen"),
            "Magic School" to listOf("akademie", "zauberschule", "magische schule", "academy", "internat", "college", "basgiath", "hogwarts", "kingmakers", "maxton hall"),
            "Dark Academia" to listOf("dark academia", "geheimbund", "alte bibliothek", "elitäre universität", "kingmakers"),
            "Dragons" to listOf("drache", "drachen", "drachenreiter", "dragon", "fourth wing", "iron flame", "onyx storm", "flammengeküsst"),
            "Fated Mates" to listOf("seelenverwandt", "fated mates", "gefährte", "gefährtin", "mate", "reich der sieben höfe", "court of"),
            "Soulmates" to listOf("seelenverwandt", "soulmate", "füreinander bestimmt"),
            "Found Family" to listOf("wahlfamilie", "found family", "außenseiter-gruppe", "crew", "gefährten", "lied der krähen", "six of crows"),
            "Hidden Heir" to listOf("verlorene erbin", "thronfolger", "geheime prinzessin", "krone", "königreich", "erbe"),
            "Morally Grey Protagonist" to listOf("moralisch fragwürdig", "morally grey", "dunkle vergangenheit", "skrupellos", "mafia", "auftragskiller", "dieb"),
            "Antihero" to listOf("antiheld", "antihero", "verbrecher", "kriminell", "rücksichtslos"),
            "Ancient Evil erwacht" to listOf("uralte macht", "uraltes böses", "dunkle bedrohung", "dämonen", "schatten"),
            "Quest im Fantasy-Setting" to listOf("reise", "quest", "suche nach", "artefakt", "gefährliche mission"),
            "Heist im Fantasy-Setting" to listOf("unmöglicher einbruch", "raubzug", "krähen", "six of crows"),
            "Dystopie" to listOf("dystopie", "dystopian", "totalitär", "überwachungsstaat", "regime", "panem", "hunger games"),
            "Untergrund-Widerstand" to listOf("rebellion", "rebellen", "widerstand", "aufstand", "revolution"),
            "Postapokalypse" to listOf("apokalypse", "nach dem untergang", "endzeit", "überlebende"),
            "Zeitreise" to listOf("zeitreise", "time travel", "vergangenheit reisen", "jahrhundert", "rubinrot", "saphirblau", "smaragdgrün"),
            "KI wird selbstbewusst" to listOf("künstliche intelligenz", "android", "roboter", "ki "),
            "Unzuverlässiger Erzähler" to listOf("wahrheit oder lüge", "erinnerungslücken", "unreliable", "traue niemandem"),
            "Ermittler mit dunkler Vergangenheit" to listOf("kommissar", "detektiv", "ermittler", "polizist", "dämonen der vergangenheit"),
            "Cold Case" to listOf("cold case", "ungelöster fall", "vor jahren verschwunden", "alter mordfall"),
            "Kleine Stadt mit Geheimnissen" to listOf("kleinstadt", "dorf", "idyllisch", "dunkles geheimnis"),
            "Katzen-und-Maus" to listOf("katz-und-maus", "jagd", "verfolgung", "killer", "stalker"),
            "Großer Plot-Twist am Ende" to listOf("nichts ist wie es scheint", "wendepunkt", "plot twist", "abgrund"),
            "Coming of Age" to listOf("erwachsenwerden", "coming of age", "selbstfindung", "jugend", "erster sommer", "studium"),
            "Revenge" to listOf("rache", "vergeltung", "revenge", "rächen"),
            "Heist" to listOf("coup", "einbruch", "heist", "diebstahl", "meisterdieb"),
            "Survival" to listOf("überleben", "wildnis", "survival", "kampf ums überleben", "tödliche prüfung"),
            "Multiple POV" to listOf("perspektiven", "beide sichten", "wechselnde sicht"),
            "Cliffhanger am Ende" to listOf("cliffhanger", "auftakt", "erster band")
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
            "Romantasy" to listOf("romantasy", "drachenreiter", "fae", "magie und leidenschaft", "fourth wing", "iron flame", "onyx storm", "sarah j. maas", "reich der sieben höfe", "blood and ash", "powerless", "caraval"),
            "Fantasy" to listOf("fantasy", "magie", "zauber", "drache", "königreich", "elfen", "dämon", "schattenjäger", "grisha", "tolkien", "harry potter"),
            "Dark Romance" to listOf("dark romance", "mafia", "kingmakers", "brutal birthright", "sophie lark", "rina kent", "haunting adeline"),
            "Romance" to listOf("romance", "liebesroman", "herzklopfen", "verliebt", "liebe", "new adult", "college", "elle kennedy", "ana huang", "mona kasten", "lauren asher", "hannah grace"),
            "Krimi & Thriller" to listOf("thriller", "krimi", "kommissar", "ermittler", "mord", "verbrechen", "psychothriller"),
            "Sci-Fi" to listOf("science fiction", "sci-fi", "weltraum", "galaxis", "zukunft", "planet", "raumschiff", "dune", "wüstenplanet"),
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
            listOf("erotica", "dark romance", "explizit", "open door", "smut", "brutal birthright", "kingmakers", "twisted", "kings of sin", "haunting adeline").any { text.contains(it) } -> 4
            listOf("leidenschaft", "sinnlich", "verführung", "knistern", "begehren", "romantasy", "fourth wing", "iron flame", "onyx storm", "silbernes feuer", "off-campus", "briar u", "icebreaker", "dreamland").any { text.contains(it) } -> 3
            listOf("romantik", "liebesgeschichte", "kuss", "verliebt", "romance", "new adult", "maxton hall", "again-reihe", "powerless").any { text.contains(it) } -> 2
            listOf("freundschaft", "gefühle", "jugendbuch", "rubinrot", "caraval", "elfenkrone").any { text.contains(it) } -> 1
            else -> 0
        }

        return TropeSuggestion(
            tropes = matchedTropes.take(8).toList(),
            genres = inferredGenres.distinct().take(3),
            spice = inferredSpice
        )
    }

    /**
     * Erkennt aus einem Buchtitel (und optionalen Metadaten) automatisch Serienname + Bandnummer.
     */
    fun detectSeriesFromTitle(rawTitle: String, existingSeries: List<String> = emptyList()): Pair<String, Int?>? {
        val info = SeriesUniverseDetector.detect(
            title = rawTitle,
            existingSeries = existingSeries
        )
        return if (info.seriesName.isNotBlank()) info.seriesName to info.seriesIndex else null
    }
}

