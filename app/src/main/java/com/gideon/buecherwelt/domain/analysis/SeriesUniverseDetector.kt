package com.gideon.buecherwelt.domain.analysis

data class SeriesUniverseInfo(
    val seriesName: String = "",
    val seriesIndex: Int? = null,
    val universeName: String = ""
) {
    val hasAny: Boolean
        get() = seriesName.isNotBlank() || seriesIndex != null || universeName.isNotBlank()
}

object SeriesUniverseDetector {

    private data class KnownBookEntry(
        val titleKeywords: List<String>,
        val authorKeyword: String = "",
        val seriesName: String,
        val seriesIndex: Int?,
        val universeName: String
    )

    private data class KnownSeriesRule(
        val seriesKeywords: List<String>,
        val authorKeyword: String = "",
        val canonicalSeries: String,
        val universeName: String
    )

    private val knownBooks: List<KnownBookEntry> = listOf(
        // Empyrean-Universum (Rebecca Yarros)
        KnownBookEntry(listOf("fourth wing", "flammengeküsst"), "yarros", "Flammengeküsst (Empyrean)", 1, "Empyrean-Universum"),
        KnownBookEntry(listOf("iron flame"), "yarros", "Flammengeküsst (Empyrean)", 2, "Empyrean-Universum"),
        KnownBookEntry(listOf("onyx storm"), "yarros", "Flammengeküsst (Empyrean)", 3, "Empyrean-Universum"),

        // Maasverse – Das Reich der sieben Höfe / ACOTAR (Sarah J. Maas)
        KnownBookEntry(listOf("dornen und rosen", "court of thorns and roses"), "maas", "Das Reich der sieben Höfe", 1, "Maasverse"),
        KnownBookEntry(listOf("flammen und finsternis", "court of mist and fury"), "maas", "Das Reich der sieben Höfe", 2, "Maasverse"),
        KnownBookEntry(listOf("sterne und schwerter", "court of wings and ruin"), "maas", "Das Reich der sieben Höfe", 3, "Maasverse"),
        KnownBookEntry(listOf("frost und mondlicht", "court of frost and starlight"), "maas", "Das Reich der sieben Höfe", 4, "Maasverse"),
        KnownBookEntry(listOf("silbernes feuer", "court of silver flames"), "maas", "Das Reich der sieben Höfe", 5, "Maasverse"),

        // Maasverse – Throne of Glass (Sarah J. Maas)
        KnownBookEntry(listOf("die erwählte", "throne of glass"), "maas", "Throne of Glass", 1, "Maasverse"),
        KnownBookEntry(listOf("kriegerin im schatten", "crown of midnight"), "maas", "Throne of Glass", 2, "Maasverse"),
        KnownBookEntry(listOf("erbin des feuers", "heir of fire"), "maas", "Throne of Glass", 3, "Maasverse"),
        KnownBookEntry(listOf("königin der finsternis", "queen of shadows"), "maas", "Throne of Glass", 4, "Maasverse"),
        KnownBookEntry(listOf("die sturmbezwingerin", "empire of storms"), "maas", "Throne of Glass", 5, "Maasverse"),
        KnownBookEntry(listOf("der verwundete krieger", "tower of dawn"), "maas", "Throne of Glass", 6, "Maasverse"),
        KnownBookEntry(listOf("herrscherin über asche und zorn", "kingdom of ash"), "maas", "Throne of Glass", 7, "Maasverse"),

        // Maasverse – Crescent City (Sarah J. Maas)
        KnownBookEntry(listOf("wenn das dunkel erwacht", "house of earth and blood"), "maas", "Crescent City", 1, "Maasverse"),
        KnownBookEntry(listOf("wenn ein stern erstrahlt", "house of sky and breath"), "maas", "Crescent City", 2, "Maasverse"),
        KnownBookEntry(listOf("wenn die schatten sich erheben", "house of flame and shadow"), "maas", "Crescent City", 3, "Maasverse"),

        // Grishaverse (Leigh Bardugo)
        KnownBookEntry(listOf("goldene flammen", "shadow and bone"), "bardugo", "Grisha-Trilogie", 1, "Grishaverse"),
        KnownBookEntry(listOf("eisige wellen", "siege and storm"), "bardugo", "Grisha-Trilogie", 2, "Grishaverse"),
        KnownBookEntry(listOf("lodernde schwingen", "ruin and rising"), "bardugo", "Grisha-Trilogie", 3, "Grishaverse"),
        KnownBookEntry(listOf("das lied der krähen", "six of crows"), "bardugo", "Krähen-Dilogie", 1, "Grishaverse"),
        KnownBookEntry(listOf("das gold der krähen", "crooked kingdom"), "bardugo", "Krähen-Dilogie", 2, "Grishaverse"),
        KnownBookEntry(listOf("thron aus gold und asche", "king of scars"), "bardugo", "King of Scars", 1, "Grishaverse"),
        KnownBookEntry(listOf("herrschaft der wölfe", "rule of wolves"), "bardugo", "King of Scars", 2, "Grishaverse"),

        // Schattenjäger-Universum (Cassandra Clare)
        KnownBookEntry(listOf("city of bones"), "clare", "Chroniken der Unterwelt", 1, "Schattenjäger-Universum"),
        KnownBookEntry(listOf("city of ashes"), "clare", "Chroniken der Unterwelt", 2, "Schattenjäger-Universum"),
        KnownBookEntry(listOf("city of glass"), "clare", "Chroniken der Unterwelt", 3, "Schattenjäger-Universum"),
        KnownBookEntry(listOf("city of fallen angels"), "clare", "Chroniken der Unterwelt", 4, "Schattenjäger-Universum"),
        KnownBookEntry(listOf("city of lost souls"), "clare", "Chroniken der Unterwelt", 5, "Schattenjäger-Universum"),
        KnownBookEntry(listOf("city of heavenly fire"), "clare", "Chroniken der Unterwelt", 6, "Schattenjäger-Universum"),
        KnownBookEntry(listOf("clockwork angel"), "clare", "Chroniken der Schattenjäger", 1, "Schattenjäger-Universum"),
        KnownBookEntry(listOf("clockwork prince"), "clare", "Chroniken der Schattenjäger", 2, "Schattenjäger-Universum"),
        KnownBookEntry(listOf("clockwork princess"), "clare", "Chroniken der Schattenjäger", 3, "Schattenjäger-Universum"),
        KnownBookEntry(listOf("lady midnight"), "clare", "Die dunklen Mächte", 1, "Schattenjäger-Universum"),
        KnownBookEntry(listOf("lord of shadows"), "clare", "Die dunklen Mächte", 2, "Schattenjäger-Universum"),
        KnownBookEntry(listOf("queen of air and darkness"), "clare", "Die dunklen Mächte", 3, "Schattenjäger-Universum"),
        KnownBookEntry(listOf("chain of gold"), "clare", "Die letzten Stunden", 1, "Schattenjäger-Universum"),
        KnownBookEntry(listOf("chain of iron"), "clare", "Die letzten Stunden", 2, "Schattenjäger-Universum"),
        KnownBookEntry(listOf("chain of thorns"), "clare", "Die letzten Stunden", 3, "Schattenjäger-Universum"),

        // Sophie Lark Mafia-Universum (Brutal Birthright & Kingmakers)
        KnownBookEntry(listOf("brutal prince"), "lark", "Brutal Birthright", 1, "Sophie-Lark-Mafia-Universum"),
        KnownBookEntry(listOf("stolen heir"), "lark", "Brutal Birthright", 2, "Sophie-Lark-Mafia-Universum"),
        KnownBookEntry(listOf("savage lover"), "lark", "Brutal Birthright", 3, "Sophie-Lark-Mafia-Universum"),
        KnownBookEntry(listOf("bloody heart"), "lark", "Brutal Birthright", 4, "Sophie-Lark-Mafia-Universum"),
        KnownBookEntry(listOf("broken vow"), "lark", "Brutal Birthright", 5, "Sophie-Lark-Mafia-Universum"),
        KnownBookEntry(listOf("heavy crown"), "lark", "Brutal Birthright", 6, "Sophie-Lark-Mafia-Universum"),
        KnownBookEntry(listOf("the heir", "kingmakers – jahr 1", "kingmakers - jahr 1"), "lark", "Kingmakers", 1, "Sophie-Lark-Mafia-Universum"),
        KnownBookEntry(listOf("the rebel", "kingmakers – jahr 2", "kingmakers - jahr 2"), "lark", "Kingmakers", 2, "Sophie-Lark-Mafia-Universum"),
        KnownBookEntry(listOf("the bully", "kingmakers – jahr 3", "kingmakers - jahr 3"), "lark", "Kingmakers", 3, "Sophie-Lark-Mafia-Universum"),
        KnownBookEntry(listOf("the spy", "kingmakers – jahr 4", "kingmakers - jahr 4"), "lark", "Kingmakers", 4, "Sophie-Lark-Mafia-Universum"),
        KnownBookEntry(listOf("the savage", "kingmakers – jahr 5", "kingmakers - jahr 5"), "lark", "Kingmakers", 5, "Sophie-Lark-Mafia-Universum"),

        // Ana Huang Universum (Twisted & Kings of Sin)
        KnownBookEntry(listOf("twisted love"), "huang", "Twisted-Reihe", 1, "Ana-Huang-Universum"),
        KnownBookEntry(listOf("twisted games"), "huang", "Twisted-Reihe", 2, "Ana-Huang-Universum"),
        KnownBookEntry(listOf("twisted hate"), "huang", "Twisted-Reihe", 3, "Ana-Huang-Universum"),
        KnownBookEntry(listOf("twisted lies"), "huang", "Twisted-Reihe", 4, "Ana-Huang-Universum"),
        KnownBookEntry(listOf("king of wrath"), "huang", "Kings of Sin", 1, "Ana-Huang-Universum"),
        KnownBookEntry(listOf("king of pride"), "huang", "Kings of Sin", 2, "Ana-Huang-Universum"),
        KnownBookEntry(listOf("king of greed"), "huang", "Kings of Sin", 3, "Ana-Huang-Universum"),
        KnownBookEntry(listOf("king of sloth"), "huang", "Kings of Sin", 4, "Ana-Huang-Universum"),
        KnownBookEntry(listOf("king of envy"), "huang", "Kings of Sin", 5, "Ana-Huang-Universum"),

        // Briar U / Off-Campus Universum (Elle Kennedy)
        KnownBookEntry(listOf("the deal"), "kennedy", "Off-Campus-Reihe", 1, "Briar-U-Universum"),
        KnownBookEntry(listOf("the mistake"), "kennedy", "Off-Campus-Reihe", 2, "Briar-U-Universum"),
        KnownBookEntry(listOf("the score"), "kennedy", "Off-Campus-Reihe", 3, "Briar-U-Universum"),
        KnownBookEntry(listOf("the goal"), "kennedy", "Off-Campus-Reihe", 4, "Briar-U-Universum"),
        KnownBookEntry(listOf("the legacy"), "kennedy", "Off-Campus-Reihe", 5, "Briar-U-Universum"),
        KnownBookEntry(listOf("the chase"), "kennedy", "Briar U", 1, "Briar-U-Universum"),
        KnownBookEntry(listOf("the risk"), "kennedy", "Briar U", 2, "Briar-U-Universum"),
        KnownBookEntry(listOf("the play"), "kennedy", "Briar U", 3, "Briar-U-Universum"),
        KnownBookEntry(listOf("the dare"), "kennedy", "Briar U", 4, "Briar-U-Universum"),
        KnownBookEntry(listOf("the graham effect"), "kennedy", "Campus Diaries", 1, "Briar-U-Universum"),
        KnownBookEntry(listOf("the dixon rule"), "kennedy", "Campus Diaries", 2, "Briar-U-Universum"),

        // Lauren Asher Universum
        KnownBookEntry(listOf("the fine print"), "asher", "Dreamland Billionaires", 1, "Dreamland-Universum"),
        KnownBookEntry(listOf("terms and conditions"), "asher", "Dreamland Billionaires", 2, "Dreamland-Universum"),
        KnownBookEntry(listOf("final offer"), "asher", "Dreamland Billionaires", 3, "Dreamland-Universum"),
        KnownBookEntry(listOf("love redesigned"), "asher", "Lakefront Billionaires", 1, "Dreamland-Universum"),
        KnownBookEntry(listOf("love unwritten"), "asher", "Lakefront Billionaires", 2, "Dreamland-Universum"),

        // Mona Kasten (Maxton Hall & Again)
        KnownBookEntry(listOf("save me"), "kasten", "Maxton Hall", 1, "Maxton-Hall-Welt"),
        KnownBookEntry(listOf("save you"), "kasten", "Maxton Hall", 2, "Maxton-Hall-Welt"),
        KnownBookEntry(listOf("save us"), "kasten", "Maxton Hall", 3, "Maxton-Hall-Welt"),
        KnownBookEntry(listOf("begin again"), "kasten", "Again-Reihe", 1, "Woodshill-Universum"),
        KnownBookEntry(listOf("trust again"), "kasten", "Again-Reihe", 2, "Woodshill-Universum"),
        KnownBookEntry(listOf("feel again"), "kasten", "Again-Reihe", 3, "Woodshill-Universum"),
        KnownBookEntry(listOf("hope again"), "kasten", "Again-Reihe", 4, "Woodshill-Universum"),
        KnownBookEntry(listOf("dream again"), "kasten", "Again-Reihe", 5, "Woodshill-Universum"),

        // Elfenkrone-Universum (Holly Black)
        KnownBookEntry(listOf("elfenkrone", "the cruel prince"), "black", "Elfenkrone-Trilogie", 1, "Elfenreich-Universum"),
        KnownBookEntry(listOf("elfenkönig", "the wicked king"), "black", "Elfenkrone-Trilogie", 2, "Elfenreich-Universum"),
        KnownBookEntry(listOf("elfenthron", "the queen of nothing"), "black", "Elfenkrone-Trilogie", 3, "Elfenreich-Universum"),
        KnownBookEntry(listOf("elfenerbe", "the stolen heir"), "black", "Elfen-Dilogie", 1, "Elfenreich-Universum"),
        KnownBookEntry(listOf("elfenmacht", "the prisoner's throne"), "black", "Elfen-Dilogie", 2, "Elfenreich-Universum"),

        // Caraval / Once Upon a Broken Heart (Stephanie Garber)
        KnownBookEntry(listOf("caraval"), "garber", "Caraval-Trilogie", 1, "Caraval-Universum"),
        KnownBookEntry(listOf("legendary"), "garber", "Caraval-Trilogie", 2, "Caraval-Universum"),
        KnownBookEntry(listOf("finale"), "garber", "Caraval-Trilogie", 3, "Caraval-Universum"),
        KnownBookEntry(listOf("once upon a broken heart"), "garber", "Once Upon a Broken Heart", 1, "Caraval-Universum"),
        KnownBookEntry(listOf("the ballad of never after"), "garber", "Once Upon a Broken Heart", 2, "Caraval-Universum"),
        KnownBookEntry(listOf("a curse for true love"), "garber", "Once Upon a Broken Heart", 3, "Caraval-Universum"),

        // Powerless (Lauren Roberts)
        KnownBookEntry(listOf("powerless"), "roberts", "Powerless-Trilogie", 1, "Ilya-Universum"),
        KnownBookEntry(listOf("reckless"), "roberts", "Powerless-Trilogie", 2, "Ilya-Universum"),
        KnownBookEntry(listOf("fearless"), "roberts", "Powerless-Trilogie", 3, "Ilya-Universum"),

        // Blood and Ash (Jennifer L. Armentrout)
        KnownBookEntry(listOf("liebe kennt keine grenzen", "from blood and ash"), "armentrout", "Blood and Ash", 1, "Blood-and-Ash-Universum"),
        KnownBookEntry(listOf("rache und liebe", "a kingdom of flesh and fire"), "armentrout", "Blood and Ash", 2, "Blood-and-Ash-Universum"),
        KnownBookEntry(listOf("krone aus blut und asche", "the crown of gilded bones"), "armentrout", "Blood and Ash", 3, "Blood-and-Ash-Universum"),
        KnownBookEntry(listOf("krieg der zwei königinnen", "the war of two queens"), "armentrout", "Blood and Ash", 4, "Blood-and-Ash-Universum"),

        // Maple Hills (Hannah Grace)
        KnownBookEntry(listOf("icebreaker"), "grace", "Maple Hills", 1, "Maple-Hills-Universum"),
        KnownBookEntry(listOf("wildfire"), "grace", "Maple Hills", 2, "Maple-Hills-Universum"),
        KnownBookEntry(listOf("daydream"), "grace", "Maple Hills", 3, "Maple-Hills-Universum"),

        // Kerstin Gier (Edelstein & Silber)
        KnownBookEntry(listOf("rubinrot"), "gier", "Edelstein-Trilogie", 1, "Zeitreise-Universum"),
        KnownBookEntry(listOf("saphirblau"), "gier", "Edelstein-Trilogie", 2, "Zeitreise-Universum"),
        KnownBookEntry(listOf("smaragdgrün"), "gier", "Edelstein-Trilogie", 3, "Zeitreise-Universum"),
        KnownBookEntry(listOf("das erste buch der träume"), "gier", "Silber-Trilogie", 1, "Silber-Traumwelt"),
        KnownBookEntry(listOf("das zweite buch der träume"), "gier", "Silber-Trilogie", 2, "Silber-Traumwelt"),
        KnownBookEntry(listOf("das dritte buch der träume"), "gier", "Silber-Trilogie", 3, "Silber-Traumwelt"),

        // Wizarding World / Harry Potter (J.K. Rowling)
        KnownBookEntry(listOf("stein der weisen", "philosopher's stone", "sorcerer's stone"), "rowling", "Harry Potter", 1, "Wizarding World"),
        KnownBookEntry(listOf("kammer des schreckens", "chamber of secrets"), "rowling", "Harry Potter", 2, "Wizarding World"),
        KnownBookEntry(listOf("gefangene von askaban", "prisoner of azkaban"), "rowling", "Harry Potter", 3, "Wizarding World"),
        KnownBookEntry(listOf("feuerkelch", "goblet of fire"), "rowling", "Harry Potter", 4, "Wizarding World"),
        KnownBookEntry(listOf("orden des phönix", "order of the phoenix"), "rowling", "Harry Potter", 5, "Wizarding World"),
        KnownBookEntry(listOf("halbblutprinz", "half-blood prince"), "rowling", "Harry Potter", 6, "Wizarding World"),
        KnownBookEntry(listOf("heiligtümer des todes", "deathly hallows"), "rowling", "Harry Potter", 7, "Wizarding World"),

        // Mittelerde (J.R.R. Tolkien)
        KnownBookEntry(listOf("die gefährten", "fellowship of the ring"), "tolkien", "Der Herr der Ringe", 1, "Mittelerde"),
        KnownBookEntry(listOf("die zwei türme", "the two towers"), "tolkien", "Der Herr der Ringe", 2, "Mittelerde"),
        KnownBookEntry(listOf("die rückkehr des königs", "return of the king"), "tolkien", "Der Herr der Ringe", 3, "Mittelerde"),
        KnownBookEntry(listOf("der hobbit", "the hobbit"), "tolkien", "Mittelerde-Klassiker", 1, "Mittelerde"),
        KnownBookEntry(listOf("das silmarillion", "the silmarillion"), "tolkien", "Mittelerde-Klassiker", null, "Mittelerde"),

        // Panem-Universum (Suzanne Collins)
        KnownBookEntry(listOf("tödliche spiele", "the hunger games"), "collins", "Die Tribute von Panem", 1, "Panem-Universum"),
        KnownBookEntry(listOf("gefährliche liebe", "catching fire"), "collins", "Die Tribute von Panem", 2, "Panem-Universum"),
        KnownBookEntry(listOf("flammender zorn", "mockingjay"), "collins", "Die Tribute von Panem", 3, "Panem-Universum"),
        KnownBookEntry(listOf("lied von vogel und schlange", "ballad of songbirds and snakes"), "collins", "Die Tribute von Panem (Prequel)", null, "Panem-Universum"),

        // Cosmere (Brandon Sanderson)
        KnownBookEntry(listOf("der weg der könige", "the way of kings"), "sanderson", "Sturmlicht-Chroniken", 1, "Cosmere"),
        KnownBookEntry(listOf("die worte des lichts", "words of radiance"), "sanderson", "Sturmlicht-Chroniken", 2, "Cosmere"),
        KnownBookEntry(listOf("die klinge des lichts", "oathbringer"), "sanderson", "Sturmlicht-Chroniken", 3, "Cosmere"),
        KnownBookEntry(listOf("der rhythmus des krieges", "rhythm of war"), "sanderson", "Sturmlicht-Chroniken", 4, "Cosmere"),
        KnownBookEntry(listOf("kinder des nebels", "the final empire", "mistborn"), "sanderson", "Mistborn / Kinder des Nebels", 1, "Cosmere"),

        // Percy-Jackson-Universum (Rick Riordan)
        KnownBookEntry(listOf("diebe im olymp", "lightning thief"), "riordan", "Percy Jackson", 1, "Percy-Jackson-Universum"),
        KnownBookEntry(listOf("im bann des zyklopen", "sea of monsters"), "riordan", "Percy Jackson", 2, "Percy-Jackson-Universum"),
        KnownBookEntry(listOf("der fluch des titanen", "titan's curse"), "riordan", "Percy Jackson", 3, "Percy-Jackson-Universum"),
        KnownBookEntry(listOf("die schlacht um das labyrinth", "battle of the labyrinth"), "riordan", "Percy Jackson", 4, "Percy-Jackson-Universum"),
        KnownBookEntry(listOf("die letzte göttin", "last olympian"), "riordan", "Percy Jackson", 5, "Percy-Jackson-Universum"),

        // Dune-Universum (Frank Herbert)
        KnownBookEntry(listOf("der wüstenplanet", "dune"), "herbert", "Dune – Der Wüstenplanet", 1, "Dune-Universum"),
        KnownBookEntry(listOf("der herr des wüstenplaneten", "dune messiah"), "herbert", "Dune – Der Wüstenplanet", 2, "Dune-Universum"),
        KnownBookEntry(listOf("die kinder des wüstenplaneten", "children of dune"), "herbert", "Dune – Der Wüstenplanet", 3, "Dune-Universum")
    )

    private val seriesToUniverseRules: List<KnownSeriesRule> = listOf(
        KnownSeriesRule(listOf("flammengeküsst", "empyrean", "fourth wing"), "yarros", "Flammengeküsst (Empyrean)", "Empyrean-Universum"),
        KnownSeriesRule(listOf("reich der sieben höfe", "court of thorns", "acotar"), "maas", "Das Reich der sieben Höfe", "Maasverse"),
        KnownSeriesRule(listOf("throne of glass"), "maas", "Throne of Glass", "Maasverse"),
        KnownSeriesRule(listOf("crescent city"), "maas", "Crescent City", "Maasverse"),
        KnownSeriesRule(listOf("grisha", "shadow and bone"), "bardugo", "Grisha-Trilogie", "Grishaverse"),
        KnownSeriesRule(listOf("krähen", "six of crows"), "bardugo", "Krähen-Dilogie", "Grishaverse"),
        KnownSeriesRule(listOf("king of scars"), "bardugo", "King of Scars", "Grishaverse"),
        KnownSeriesRule(listOf("chroniken der unterwelt", "mortal instruments"), "clare", "Chroniken der Unterwelt", "Schattenjäger-Universum"),
        KnownSeriesRule(listOf("chroniken der schattenjäger", "infernal devices"), "clare", "Chroniken der Schattenjäger", "Schattenjäger-Universum"),
        KnownSeriesRule(listOf("dunklen mächte", "dark artifices"), "clare", "Die dunklen Mächte", "Schattenjäger-Universum"),
        KnownSeriesRule(listOf("kingmakers"), "lark", "Kingmakers", "Sophie-Lark-Mafia-Universum"),
        KnownSeriesRule(listOf("brutal birthright"), "lark", "Brutal Birthright", "Sophie-Lark-Mafia-Universum"),
        KnownSeriesRule(listOf("twisted"), "huang", "Twisted-Reihe", "Ana-Huang-Universum"),
        KnownSeriesRule(listOf("kings of sin"), "huang", "Kings of Sin", "Ana-Huang-Universum"),
        KnownSeriesRule(listOf("off-campus", "off campus"), "kennedy", "Off-Campus-Reihe", "Briar-U-Universum"),
        KnownSeriesRule(listOf("briar u"), "kennedy", "Briar U", "Briar-U-Universum"),
        KnownSeriesRule(listOf("campus diaries"), "kennedy", "Campus Diaries", "Briar-U-Universum"),
        KnownSeriesRule(listOf("dreamland billionaires"), "asher", "Dreamland Billionaires", "Dreamland-Universum"),
        KnownSeriesRule(listOf("lakefront billionaires"), "asher", "Lakefront Billionaires", "Dreamland-Universum"),
        KnownSeriesRule(listOf("dirty air"), "asher", "Dirty Air", "Dreamland-Universum"),
        KnownSeriesRule(listOf("maxton hall"), "kasten", "Maxton Hall", "Maxton-Hall-Welt"),
        KnownSeriesRule(listOf("again-reihe", "begin again"), "kasten", "Again-Reihe", "Woodshill-Universum"),
        KnownSeriesRule(listOf("elfenkrone", "folk of the air"), "black", "Elfenkrone-Trilogie", "Elfenreich-Universum"),
        KnownSeriesRule(listOf("caraval"), "garber", "Caraval-Trilogie", "Caraval-Universum"),
        KnownSeriesRule(listOf("once upon a broken heart"), "garber", "Once Upon a Broken Heart", "Caraval-Universum"),
        KnownSeriesRule(listOf("blood and ash"), "armentrout", "Blood and Ash", "Blood-and-Ash-Universum"),
        KnownSeriesRule(listOf("flesh and fire"), "armentrout", "Flesh and Fire", "Blood-and-Ash-Universum"),
        KnownSeriesRule(listOf("harry potter"), "rowling", "Harry Potter", "Wizarding World"),
        KnownSeriesRule(listOf("herr der ringe", "lord of the rings"), "tolkien", "Der Herr der Ringe", "Mittelerde"),
        KnownSeriesRule(listOf("sturmlicht", "stormlight", "mistborn", "kinder des nebels"), "sanderson", "Sturmlicht-Chroniken", "Cosmere"),
        KnownSeriesRule(listOf("percy jackson", "helden des olymp"), "riordan", "Percy Jackson", "Percy-Jackson-Universum"),
        KnownSeriesRule(listOf("tribute von panem", "hunger games"), "collins", "Die Tribute von Panem", "Panem-Universum"),
        KnownSeriesRule(listOf("hexer", "witcher"), "sapkowski", "Hexer-Saga", "Hexer-Universum"),
        KnownSeriesRule(listOf("lied von eis und feuer", "song of ice and fire", "game of thrones"), "martin", "Das Lied von Eis und Feuer", "Westeros"),
        KnownSeriesRule(listOf("wüstenplanet", "dune"), "herbert", "Dune – Der Wüstenplanet", "Dune-Universum"),
        KnownSeriesRule(listOf("scheibenwelt", "discworld"), "pratchett", "Scheibenwelt-Romane", "Scheibenwelt")
    )

    fun detect(
        title: String,
        subtitle: String = "",
        author: String = "",
        description: String = "",
        apiSeries: List<String> = emptyList(),
        existingSeries: List<String> = emptyList(),
        existingUniverses: List<String> = emptyList()
    ): SeriesUniverseInfo {
        val cleanTitle = title.trim()
        val cleanSubtitle = subtitle.trim()
        val cleanAuthor = author.trim().lowercase()
        val combinedHeader = "$cleanTitle $cleanSubtitle".trim()
        val fullText = "$cleanTitle $cleanSubtitle $description".trim()
        val lowerHeader = combinedHeader.lowercase()
        val lowerFull = fullText.lowercase()

        var detectedSeries = ""
        var detectedIndex: Int? = null
        var detectedUniverse = ""

        // 1. Check Known Books Catalog (High Precision)
        for (entry in knownBooks) {
            val authorMatches = entry.authorKeyword.isBlank() ||
                cleanAuthor.isBlank() ||
                cleanAuthor.contains(entry.authorKeyword)
            if (authorMatches && entry.titleKeywords.any { kw -> lowerHeader.contains(kw) }) {
                detectedSeries = entry.seriesName
                detectedIndex = entry.seriesIndex
                detectedUniverse = entry.universeName
                break
            }
        }

        // 2. Check OpenLibrary / API Series metadata strings (e.g., "Kingmakers (1)", "The Empyrean #2")
        if (detectedSeries.isBlank() && apiSeries.isNotEmpty()) {
            for (raw in apiSeries) {
                val parsed = parseSeriesString(raw)
                if (parsed != null && parsed.first.isNotBlank()) {
                    detectedSeries = parsed.first
                    if (detectedIndex == null) detectedIndex = parsed.second
                    break
                }
            }
        }

        // 3. Extract from Title & Subtitle patterns
        if (detectedSeries.isBlank()) {
            extractFromTitleOrSubtitle(cleanTitle, existingSeries)?.let { (s, idx) ->
                detectedSeries = s
                if (detectedIndex == null) detectedIndex = idx
            }
        }
        if (detectedSeries.isBlank() && cleanSubtitle.isNotBlank()) {
            extractFromTitleOrSubtitle(cleanSubtitle, existingSeries)?.let { (s, idx) ->
                detectedSeries = s
                if (detectedIndex == null) detectedIndex = idx
            }
        }

        // 4. Extract from Description / Klappentext
        if (detectedSeries.isBlank() || detectedIndex == null) {
            val fromDesc = extractSeriesFromDescription(description, existingSeries)
            if (fromDesc != null) {
                if (detectedSeries.isBlank() && fromDesc.first.isNotBlank()) {
                    detectedSeries = fromDesc.first
                }
                if (detectedIndex == null && fromDesc.second != null) {
                    detectedIndex = fromDesc.second
                }
            }
        }

        // 5. Fallback volume number detection if we have a series (or title volume marker)
        if (detectedIndex == null) {
            detectedIndex = extractVolumeNumber(combinedHeader)
                ?: if (detectedSeries.isNotBlank()) extractVolumeNumber(description) else null
        }

        // 6. Universe Detection: Known Series -> Universe rules
        if (detectedUniverse.isBlank()) {
            for (rule in seriesToUniverseRules) {
                val authorOk = rule.authorKeyword.isBlank() ||
                    cleanAuthor.isBlank() ||
                    cleanAuthor.contains(rule.authorKeyword)
                val matchesSeriesOrText = rule.seriesKeywords.any { kw ->
                    detectedSeries.contains(kw, ignoreCase = true) || lowerFull.contains(kw)
                }
                if (authorOk && matchesSeriesOrText) {
                    if (detectedSeries.isBlank()) detectedSeries = rule.canonicalSeries
                    detectedUniverse = rule.universeName
                    break
                }
            }
        }

        // 7. Universe Detection: Explicit Universe mentions in Title, Subtitle, or Description
        if (detectedUniverse.isBlank()) {
            detectedUniverse = extractUniverseFromText(fullText, existingUniverses)
        }

        return SeriesUniverseInfo(
            seriesName = cleanSeriesName(detectedSeries),
            seriesIndex = detectedIndex,
            universeName = cleanUniverseName(detectedUniverse)
        )
    }

    private fun parseSeriesString(raw: String): Pair<String, Int?>? {
        val trimmed = raw.trim()
        if (trimmed.length < 3) return null
        val regex = Regex(
            "^(.+?)(?:[,\\s–—:-]+(?:Band|Bd\\.?|Teil|Vol\\.?|Volume|Book|Buch|Jahr|Nr\\.?|#)?\\s*(\\d{1,3})|\\s*\\((?:Band|Bd\\.?|Teil|Vol\\.?|Book|#)?\\s*(\\d{1,3})\\))?$",
            RegexOption.IGNORE_CASE
        )
        val m = regex.find(trimmed) ?: return trimmed to null
        val name = m.groupValues[1].trim().trimEnd(',', '-', '–', ':')
        val idx = m.groupValues[2].toIntOrNull() ?: m.groupValues[3].toIntOrNull()
        return if (name.length >= 3) name to idx else null
    }

    fun extractFromTitleOrSubtitle(
        text: String,
        existingSeries: List<String> = emptyList()
    ): Pair<String, Int?>? {
        val t = text.trim()
        if (t.isBlank()) return null

        // Check existing series from library first
        for (known in existingSeries) {
            if (known.length >= 3 && t.contains(known, ignoreCase = true)) {
                val idx = extractVolumeNumber(t)
                return known to idx
            }
        }

        // Pattern 1: "Titel (Serienname, Band 2)" or "Titel (Serienname #2)" or "Titel (Serienname 2)"
        val parenRegex = Regex(
            "\\(([^()]+?)[,\\s–—:-]+(?:Band|Bd\\.?|Teil|Vol\\.?|Book|Buch|Jahr|Nr\\.?|#)?\\s*(\\d{1,3})\\)",
            RegexOption.IGNORE_CASE
        )
        parenRegex.find(t)?.let { m ->
            val series = m.groupValues[1].trim()
            val index = m.groupValues[2].toIntOrNull()
            if (isValidSeriesCandidate(series)) return series to index
        }

        // Pattern 2: "Band 1 der XYZ-Reihe" / "Teil 2 der XYZ-Trilogie" / "Band 3 der XYZ-Saga"
        val derReiheRegex = Regex(
            "(?:Band|Bd\\.?|Teil|Buch)\\s*(\\d{1,3})\\s+der\\s+(?:großen\\s+|neuen\\s+|beliebten\\s+|epischen\\s+)?[„\"»]?([A-ZÄÖÜa-zäöüß0-9\\s-]{3,45}?)[“\"«]?(?:-(?:Reihe|Serie|Trilogie|Dilogie|Saga|Chroniken))?\\b",
            RegexOption.IGNORE_CASE
        )
        derReiheRegex.find(t)?.let { m ->
            val index = m.groupValues[1].toIntOrNull()
            val series = m.groupValues[2].trim()
            if (isValidSeriesCandidate(series)) return series to index
        }

        // Pattern 3: "Serienname – Band 1" / "Serienname: Jahr 1" / "Serienname - Teil 2" / "Serienname #3"
        val dashRegex = Regex(
            "^([^:–—()-]{3,50}?)(?:[\\s]*[–—:-][\\s]*|\\s+)(?:Band|Bd\\.?|Teil|Vol\\.?|Book|Buch|Jahr|#)\\s*(\\d{1,3})\\b",
            RegexOption.IGNORE_CASE
        )
        dashRegex.find(t)?.let { m ->
            val series = m.groupValues[1].trim()
            val index = m.groupValues[2].toIntOrNull()
            if (isValidSeriesCandidate(series)) return series to index
        }

        // Pattern 4: "Titel: A [Series Name] Novel (Book 1)"
        val novelRegex = Regex(
            "A\\s+([A-Za-z0-9\\s'-]{3,40}?)\\s+Novel(?:[,\\s(]+(?:Book|#)\\s*(\\d{1,3}))?",
            RegexOption.IGNORE_CASE
        )
        novelRegex.find(t)?.let { m ->
            val series = m.groupValues[1].trim()
            val index = m.groupValues[2].toIntOrNull()
            if (isValidSeriesCandidate(series)) return series to index
        }

        return null
    }

    private fun extractSeriesFromDescription(
        description: String,
        existingSeries: List<String>
    ): Pair<String, Int?>? {
        if (description.isBlank()) return null

        // Check existing series names in description
        for (known in existingSeries) {
            if (known.length >= 4 && description.contains(known, ignoreCase = true)) {
                return known to extractVolumeNumber(description)
            }
        }

        // Quoted series name with suffix: „Flammengeküsst“-Reihe, "Kingmakers"-Serie, »Maxton Hall«-Trilogie
        val quotedSeriesRegex = Regex(
            "[„\"»]([A-ZÄÖÜa-zäöüß0-9\\s'&-]{3,40}?)[“\"«][\\s-]*(?:Reihe|Serie|Trilogie|Dilogie|Saga|Chroniken)",
            RegexOption.IGNORE_CASE
        )
        quotedSeriesRegex.find(description)?.let { m ->
            val series = m.groupValues[1].trim()
            if (isValidSeriesCandidate(series)) {
                return series to extractVolumeNumber(description)
            }
        }

        // Hyphenated series name: "der großen Empyrean-Reihe", "der neuen Woodshill-Serie", "der Schattenjäger-Chroniken"
        val hyphenSeriesRegex = Regex(
            "\\bder\\s+(?:großen\\s+|neuen\\s+|beliebten\\s+|epischen\\s+|erfolgreichen\\s+|Bestseller-)?([A-ZÄÖÜ][A-Za-zÄÖÜäöüß0-9-]{2,35})-(?:Reihe|Serie|Trilogie|Dilogie|Saga|Chroniken)\\b"
        )
        hyphenSeriesRegex.find(description)?.let { m ->
            val series = m.groupValues[1].trim()
            if (isValidSeriesCandidate(series)) {
                return series to extractVolumeNumber(description)
            }
        }

        return null
    }

    fun extractVolumeNumber(text: String): Int? {
        if (text.isBlank()) return null

        // 1. Explicit number: "Band 2", "Bd. 3", "Teil 1", "Jahr 4", "Book 2", "Vol. 1", "#3"
        val explicitRegex = Regex(
            "(?:\\b(?:Band|Bd\\.?|Teil|Vol\\.?|Volume|Book|Buch|Jahr)\\s*(\\d{1,3})\\b|#(\\d{1,3})\\b)",
            RegexOption.IGNORE_CASE
        )
        explicitRegex.find(text)?.let { m ->
            val n = m.groupValues[1].toIntOrNull() ?: m.groupValues[2].toIntOrNull()
            if (n != null && n in 1..99) return n
        }

        // 2. Numeric ordinal: "1. Band", "2. Teil", "3. Buch"
        val numOrdinalRegex = Regex("\\b(\\d{1,2})\\.\\s*(?:Band|Teil|Buch)\\b", RegexOption.IGNORE_CASE)
        numOrdinalRegex.find(text)?.let { m ->
            val n = m.groupValues[1].toIntOrNull()
            if (n != null && n in 1..50) return n
        }

        // 3. Roman numerals: "Band I", "Band II", "Band III", "Band IV", "Band V", "Teil II"
        val romanRegex = Regex("\\b(?:Band|Teil|Buch|Vol\\.?)\\s+(I|II|III|IV|V|VI|VII|VIII|IX|X)\\b")
        romanRegex.find(text)?.let { m ->
            val roman = when (m.groupValues[1]) {
                "I" -> 1
                "II" -> 2
                "III" -> 3
                "IV" -> 4
                "V" -> 5
                "VI" -> 6
                "VII" -> 7
                "VIII" -> 8
                "IX" -> 9
                "X" -> 10
                else -> null
            }
            if (roman != null) return roman
        }

        // 4. German & English word ordinals
        val lower = text.lowercase()
        val wordRules: List<Pair<Int, List<String>>> = listOf(
            1 to listOf(
                "erste band", "erster band", "erstes buch", "erste buch", "erster teil", "erste teil",
                "auftakt der", "auftakt zur", "auftaktband", "reihenauftakt", "serienauftakt",
                "book one", "first book in", "volume one", "part one"
            ),
            2 to listOf(
                "zweite band", "zweiter band", "zweites buch", "zweite buch", "zweiter teil", "zweite teil",
                "book two", "second book in", "volume two", "part two", "finale der dilogie"
            ),
            3 to listOf(
                "dritte band", "dritter band", "drittes buch", "dritte buch", "dritter teil", "dritte teil",
                "book three", "third book in", "volume three", "part three", "finale der trilogie", "abschluss der trilogie"
            ),
            4 to listOf(
                "vierte band", "vierter band", "viertes buch", "vierter teil",
                "book four", "fourth book in"
            ),
            5 to listOf(
                "fünfte band", "fünfter band", "fünftes buch", "fünfter teil",
                "book five", "fifth book in"
            )
        )
        for ((num, phrases) in wordRules) {
            if (phrases.any { lower.contains(it) }) return num
        }

        return null
    }

    private fun extractUniverseFromText(text: String, existingUniverses: List<String>): String {
        if (text.isBlank()) return ""

        for (known in existingUniverses) {
            if (known.length >= 4 && text.contains(known, ignoreCase = true)) {
                return known
            }
        }

        // Pattern 1: "im [Name]-Universum" / "aus dem [Name]-Universum"
        val deUniRegex = Regex(
            "\\b(?:im|aus\\s+dem|das)\\s+[„\"»]?([A-ZÄÖÜ][A-Za-zÄÖÜäöüß0-9\\s-]{2,35}?)[“\"«]?-Universum\\b"
        )
        deUniRegex.find(text)?.let { m ->
            val name = m.groupValues[1].trim()
            if (isValidSeriesCandidate(name)) return "$name-Universum"
        }

        // Pattern 2: "in der Welt von [Name]"
        val weltRegex = Regex(
            "\\bin\\s+der\\s+Welt\\s+von\\s+[„\"»]?([A-ZÄÖÜ][A-Za-zÄÖÜäöüß0-9\\s-]{2,30}?)[“\"«]?(?:[.,;\\s]|$)"
        )
        weltRegex.find(text)?.let { m ->
            val name = m.groupValues[1].trim()
            if (isValidSeriesCandidate(name)) return "$name-Welt"
        }

        // Pattern 3: "set in the [Name] universe" / "A [Name] Universe Novel"
        val enUniRegex = Regex(
            "\\b(?:in\\s+the|A)\\s+([A-Z][A-Za-z0-9\\s'-]{2,30}?)\\s+Universe\\b"
        )
        enUniRegex.find(text)?.let { m ->
            val name = m.groupValues[1].trim()
            if (isValidSeriesCandidate(name)) return "$name-Universum"
        }

        return ""
    }

    private fun isValidSeriesCandidate(candidate: String): Boolean {
        val cleaned = candidate.trim()
        if (cleaned.length < 3 || cleaned.length > 55) return false
        val blacklist = setOf(
            "roman", "thriller", "fantasy", "romantasy", "krimi", "sachbuch", "bestseller",
            "taschenbuch", "hardcover", "ausgabe", "sonderausgabe", "neuausgabe", "edition",
            "deutsch", "german", "english", "novel", "story", "geschichten", "buch", "band"
        )
        return cleaned.lowercase() !in blacklist
    }

    private fun cleanSeriesName(raw: String): String =
        raw.trim().trim('"', '„', '“', '»', '«', '-', '–', '—', ':', ',')

    private fun cleanUniverseName(raw: String): String =
        raw.trim().trim('"', '„', '“', '»', '«', '-', '–', '—', ':', ',')
}
