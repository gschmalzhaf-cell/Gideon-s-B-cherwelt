## Gideon's Bücherwelt v1.5.0 – Zuverlässige Buchcover, Reihen-/Band- & Universum-Automatik, Tiefgehende Buchdetails & Neues Bibliotheks-App-Icon

### 🖼️ Zuverlässiges Laden & Aktualisieren der richtigen Buchcover
- **Verbesserte Cover-Auflösung (Google Books & Open Library)**:
  - Open Library `/isbn/{isbn}.json` liest nun das `covers`-Feld korrekt aus und nutzt zusätzlich hochauflösende ISBN-Cover-Fallbacks.
  - Wenn ein ISBN-Eintrag kein Cover oder keinen Klappentext liefert, sucht die App automatisch per Titel + Autor:in nach passenden Editionen und ergänzt das richtige Cover.
  - **Intelligentes Editions-Scoring**: Filtert „Study Guides / Summaries“ heraus, gleicht Autor:in & Titel ab und bevorzugt Ausgaben mit echtem hochauflösenden Cover (`extraLarge` / `large` / `zoom=2`).
  - **Cover-Validierung**: Erkennt und verwirft leere 1×1-Platzhalterbilder (< 1 KB), sodass ungültige Dateien nie im lokalen Cache landen.
- **Automatische & Manuelle Cover-Aktualisierung**:
  - Beim App-Start sowie beim Öffnen einer Buch-Detailseite werden fehlende Cover und Details automatisch im Hintergrund nachgeladen.
  - **Editionen-Cover-Auswahl**: Direkt auf der Buch-Detailseite können per *„Andere Ausgabe“* bis zu 8 alternative Buchcover-Editionen visuell verglichen und per Fingertipp ausgewählt werden.
  - **Bibliotheks-Sync**: Neuer Aktualisierungs-Button oben im Bücherregal sowie in den Einstellungen (*„Alle Cover, Reihen & Buchdetails neu laden“*).

### 📚 Automatische Erkennung von Buchreihe, Band (Teil) & Buch-Universum (separat erfasst)
- **Getrennte Erfassung von Reihe (`seriesName`), Band (`seriesIndex`) & Buch-Universum (`universeName`)**:
  - Bücher können gleichzeitig einer **Buchreihe** (inkl. Bandnummer, z. B. *Band 1*) **und/oder** einem übergeordneten **Buch-Universum** (z. B. *Grishaverse*, *Maasverse*, *Empyrean-Universum*, *Schattenjäger-Universum*, *Briar-U-Universum*, *Dreamland-Universum*, *Caraval-Universum*, *Elfenreich-Universum*, *Cosmere*, *Mittelerde*, *Wizarding World* u. v. m.) angehören.
- **Mehrstufige automatische Erkennung (`SeriesUniverseDetector`)**:
  - Erkennt Reihe, Bandnummer und Universum automatisch aus Titel, Untertitel, Klappentext (inkl. deutscher/englischer Zahlwörter wie *„Auftakt der Trilogie“*, *„erster/zweiter/dritter Band“*, römischer Zahlen `Band I–X`), OpenLibrary-Serienmetadaten sowie einem kuratierten Reihen- & Universum-Katalog.
  - Auf der Buch-Detailseite zeigt die Sektion **„Buchreihe, Band & Buch-Universum“** sowohl weitere Bände derselben Reihe (nach Bandnummer sortiert) als auch andere Werke aus demselben Buch-Universum separat an.
  - Im Bücherregal kann nun separat nach **Reihe** und nach **Universum** gefiltert werden.

### ✨ Mehr Details auf den Buch-Detailseiten (`BookDetailScreen`)
- **Reichhaltige Sektion „Tropes, Motive & Buch-Charakteristik“**:
  - **Buch-Charakteristik**: Automatisch erkannte **Stimmung & Vibe** (z. B. *Düster & Geheimnisvoll*, *Emotional & Herzschmerz*, *Cozy & Wohlfühlbuch*), **Erzähltempo** (*Pageturner*, *Slow-Burn*, *Epischer Aufbau*), **Setting-Merkmale** (*Akademie / Campus*, *Hofstaat & Königreiche*, *Kleinstadt-Idylle* usw.) und **Spice-Einschätzung**.
  - **Tropes nach Kategorien gruppiert** (*Romantik & Dynamik*, *Fantasy & Magie*, *Handlung & Spannung*, *Charaktere & Gefühle*, *Setting & Atmosphäre*) mit Direkt-Editor zum Anpassen der Tropes direkt auf der Detailseite.
  - **1-Tipp Trope-Erkennung**: Automatisch im Buch erkannte Tropes werden als Schnellauswahl-Chips vorgeschlagen und können einzeln oder gesammelt übernommen werden.
  - **Erweiterte Buchdaten**: Untertitel, Seitenzahl inkl. geschätzter Lesezeit (in Stunden), Sprache, Verlag, Erscheinungsjahr, ISBN sowie interaktive Spice-Auswahl.

### 🎨 Neues App-Icon im edlen Bibliotheks- & Lederband-Design
- Komplett neu gestaltetes adaptives Launcher-Icon (`ic_launcher_foreground.xml`, `ic_launcher_background.xml` sowie alle `mipmap-*`-Auflösungen): Aufgeschlagenes, goldgeprägtes Lederbuch auf einem Holz-Lesepult mit geschwungenen Pergamentseiten, goldener Initiale, karminrotem Lesezeichen-Bändchen und funkelnden goldenen Sternen vor warmem Mahagoni-Hintergrund.
