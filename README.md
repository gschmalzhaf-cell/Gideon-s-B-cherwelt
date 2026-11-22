# 📚 Gideon's Bücherwelt

**Deine intelligente, 100 % lokale Android-Bibliothek** – zum Verwalten deiner persönlichen Büchersammlung, Erkennen von Buchreihen & Universen, Analysieren deiner Lese-DNA und Entdecken neuer Lieblingsbücher.

- **Privacy First & Offline-fähig**: Alle Daten liegen lokal auf deinem Gerät (Room/SQLite & DataStore). Es gibt keinen Account-Zwang und keine Cloud-Pflicht.
- **Netzwerk nur bei Bedarf**: Ausschließlich für Buchdaten-/Cover-Abfragen (ISBN, ASIN, Amazon-Kurzlinks, Titel-Suche) sowie optionale KI-/LLM-Aufrufe mit deinem eigenen API-Key (z. B. Mistral, OpenAI-kompatible Endpunkte).

---

## ✨ Funktionen

### 1. Schnelles Erfassen & automatische Anreicherung
- **ISBN-/Barcode-Scan** per Kamera (ML Kit + CameraX) oder manuelle Eingabe von **ISBN, ASIN & Amazon-Kurzlinks** (`amzn.eu`, `amzn.to`, `a.co` inkl. automatischer Weiterleitungs-Auflösung).
- **Cover-Scanner (OCR + KI)**: Buchcover fotografieren und automatisch erkennen lassen.
- **Multi-Quellen-Suche**: Kombiniert Google Books, Open Library, Audnex, Apple Books, Goodreads und öffentliche Produktseiten-Metadaten – ideal auch für deutsche Indie-, Dark-Romance- und Kindle-Unlimited-Titel.
- **Duplikat-Erkennung**: Erkennt doppelte Bücher (ISBN oder Titel + Autor:in) und bietet intelligentes Zusammenführen an.
- **StoryGraph-CSV-Import**: Bestehende Leselisten importieren und fehlende Metadaten/Cover automatisch nachladen.
- **Hintergrund-Anreicherung**: Ergänzt beim Start oder per Klick fehlende Cover, Klappentexte, Seitenzahlen, Buchreihen, Universen und Tropes.

### 2. Bibliotheks-Intelligenz (100 % offline + optionales LLM)
- **Smart-Suche & Stimmungs-Chips**: Versteht natürliche Anfragen wie *„ungelesen & düster“*, *„kurz & romantisch“*, *„ohne spice“* oder *„nächstes highlight“* komplett offline – mit optionaler LLM-Erweiterung und automatischem Offline-Fallback.
- **Reihen- & Universums-Erkennung**: Erkennt automatisch Buchreihen, Bandnummern und übergeordnete Buch-Universen (z. B. *Maasverse*, *Grishaverse*, *Empyrean-Universum*, *Cosmere*, *Schattenjäger-Universum*).
- **Reihen-Wächter & Lücken-Check**: Zeigt lesebereite Folgebände in deinem Regal und warnt bei fehlenden Bandnummern innerhalb einer Reihe.
- **Lokale Buch-Analyse**: Leitet aus Klappentext & Metadaten automatisch Stimmungen, Erzähltempo (Pacing), Schauplätze, geschätzte Lesezeit, Tropes und Spice-Level ab.
- **Offline „Ähnliche Bücher im eigenen Regal“**: Findet sofort passende Titel aus deiner Sammlung mit Match-Prozentwert und Begründung.

### 3. Metadaten, Tropes & Buchdetailseite
- **Editierbarer Tropes-Katalog**: Über 70 kuratierte Tropes nach Kategorien (Romance, Fantasy, Sci-Fi, Krimi/Thriller, Allgemein), Autovervollständigung, eigene Tropes und 1-Klick-Übernahme erkannter Vorschläge.
- **Bewertung & Status**: 1–5 Sterne, Spice-Level 0–5, Lesestatus (*Geplant / Lesend / Gelesen / Abgebrochen*), persönliche Notizen und direktes Cover-Ersetzen (Online-Auswahl oder eigene Galerie).
- **Lesepult**: Hebt aktuell gelesene Bücher direkt oben in der Bibliothek hervor.
- **Serien- & Universums-Verknüpfung**: Klickbare Übersicht aller Bände einer Reihe und verbundener Reihen desselben Universums direkt auf der Buchdetailseite.

### 4. Entdecken, SuB-Berater & Merkliste
- **Stimmungsbasierter SuB-Berater**: Sortiert deine ungelesenen Bücher nach Profil-Passgenauigkeit (inkl. direktem Folgeband-Boost) und filtert nach aktueller Leselaune (*Alle*, *Fortsetzung*, *Düster*, *Romantisch*, *Magisch*, *Spannend*, *Wohlfühl*, *Kurz*).
- **Regelbasierte & KI-Empfehlungen**: Analysiert Genres, Autor:innen, Tropes, Klappentexte und negative Signale (Abbrüche, 1–2 ★). Mit LLM-Key zusätzlich **KI-Re-Ranking** und kuratierte Vorschläge ohne Duplikate aus deinem Regal.
- **Merkliste**: Spannende Funde merken und per Klick in die Bibliothek übernehmen.

### 5. Lese-Chronik, Exlibris & Lese-DNA
- **Lese-Archetyp (Exlibris)**: Automatisch abgeleitete Lese-Persönlichkeit samt Badge.
- **Lese-DNA & Zeit-Prognose**: Geschätzte gelesene Seiten & Lesestunden, SuB-Lesezeit, durchschnittliche Buchlänge und Top-Stimmungen.
- **Reihen-Fortschritt & Dashboards**: Fortschrittsbalken pro Reihe, Genre-Verteilung, Top-Tropes, Lieblingsautor:innen/-verlage, Ø Bewertung, Ø Spice, „Weniger beliebt“-Signale und klickbares Ehrenregal (4–5 ★).

### 6. Diagnose & Datenschutz
- **App-Report** (*Einstellungen → Über*): Erzeugt lokal einen Markdown- & JSON-Bericht (Geräte-/DB-Infos, Qualitätsstatistiken, Leseprofil) zum Teilen bei Fehlersuche – **API-Keys werden automatisch geschwärzt (redacted)**.

---

## 🛠️ Tech-Stack

| Bereich | Technologien |
|---|---|
| **Sprache & UI** | Kotlin 2.4, Jetpack Compose (Material 3, Compose BOM 2026), Navigation-Compose, MVVM |
| **Persistenz** | Room (SQLite, Schema v4 mit Migrationen 1→4), Jetpack DataStore (Theme & LLM-Config) |
| **Netzwerk & Bilder** | Retrofit 3, OkHttp 5, kotlinx.serialization, Coil 3 (+ lokaler Offline-Cover-Cache in `filesDir/covers`) |
| **Kamera & On-Device ML** | CameraX, Google ML Kit (Barcode-Scanning & Text-Recognition) |
| **KI (optional)** | OpenAI-kompatibler `/chat/completions`-Client mit konfigurierbarer Basis-URL & Modellwahl |
| **Design** | Warmes Bibliotheks-Theme (Light & Dark), Playfair Display + Plus Jakarta Sans, Google-Stitch-Designsystem (`design/`) |

---

## 📂 Projektstruktur

```text
app/src/main/java/com/gideon/buecherwelt/
├── di/AppContainer.kt              # Manuelles Dependency Injection
├── data/
│   ├── local/                      # Room (AppDatabase, Entities, DAOs), TropesCatalog
│   ├── settings/                   # DataStore SettingsRepository (Theme, LLM)
│   ├── network/                    # BookMetadataFetcher (ISBN/ASIN/URLs), APIs, CoverCache
│   └── csv/                        # StoryGraphCsvImporter
├── domain/
│   ├── analysis/                   # ReadingProfile, SeriesUniverseDetector, SmartLibraryEngine
│   ├── llm/                        # LlmClient, LlmService, LlmPrompts, CoverIdentifier
│   ├── model/                      # Domain-Modelle (Book, Trope, WishlistItem)
│   ├── recommendation/             # RecommendationEngine (Profil- & Ähnlichkeits-Scoring)
│   ├── report/                     # AppReportGenerator (Markdown + JSON, redacted)
│   ├── repository/                 # BookRepository, TropeRepository, WishlistRepository
│   └── usecase/                    # AddToLibraryUseCase, BookEnrichmentService
└── ui/
    ├── theme/                      # Color, Type, Shape, Theme
    ├── navigation/                 # AppNavHost & Routen
    ├── components/                 # BookCard, BookCover, FilterBar, TropePicker, StarRating
    └── screens/                    # library, detail, add, discover, stats, settings, tropes, import, scanner
design/                             # Verbindliche Design-Tokens (DESIGN.md) & Mockups
```

---

## 🚀 Build & Release

### Voraussetzungen
- Android Studio (aktuell) mit **Android SDK 37**, **JDK 17** und **Gradle 9.9.1**.

### Lokal bauen
```bash
# Gradle-Wrapper erzeugen (falls noch nicht vorhanden):
gradle wrapper --gradle-version 9.9.1 --distribution-type bin

# Debug-APK bauen:
./gradlew :app:assembleDebug
```
Die fertige APK liegt unter `app/build/outputs/apk/debug/app-debug.apk`.  
Berechtigungen: **Kamera** (nur für ISBN-/Cover-Scan) und **Internet** (nur für Buchdaten, Cover & optionales LLM).

### CI/CD & Signiertes Release
- **CI (`.github/workflows/build.yml`)**: Baut bei jedem Push/PR auf `main` eine Debug-APK.
- **Release (`.github/workflows/release.yml`)**: Baut bei jedem Git-Tag `v*` signierte `app-release.apk` + `app-release.aab` und veröffentlicht ein GitHub-Release mit dem Inhalt aus [RELEASE_NOTES.md](RELEASE_NOTES.md). Details zur Keystore-Einrichtung siehe [RELEASE.md](RELEASE.md).

---

## 🗺️ Roadmap & Optimierungsvorschläge

Nachfolgend konkrete Ideen für zukünftige Releases, priorisiert nach Nutzen für Leser:innen und technischer Qualität:

### 🎯 Hoher Nutzwert (Features)
1. **Lese-Fortschritt & Lese-Sessions (Seitenzähler / % + Leseziele)**
   - Aktuelle Seite oder Prozentstand für Bücher auf dem *Lesepult* speichern, inkl. Jahres-Leseziel (*„18 / 30 Bücher in 2026“*) und monatlicher Lese-Timeline in der Statistik.
2. **Lokales Backup & Restore (JSON / ZIP inkl. Cover)**
   - Ein-Klick-Export der gesamten Bibliothek (Bücher, Tropes, Notizen, Bewertungen, Merkliste) als `.json` oder `.zip` zum Gerätewechsel – und kompatibler Import für Goodreads-CSVs zusätzlich zu StoryGraph.
3. **Verleih- & Besitz-Tracker (Print / E-Book / Hörbuch / Verliehen)**
   - Format-Badge (Hardcover, Taschenbuch, Kindle, Hörbuch), Regal-Standort sowie „Verliehen an … seit …“ inkl. Erinnerung.
4. **Zitat- & Highlight-Sammlung pro Buch**
   - Lieblingszitate (optional per Kamera-Texterkennung mit dem bereits vorhandenen ML Kit Text Recognition!) direkt am Buch speichern und teilen.
5. **Reihen-Lücken per 1-Klick auf die Merkliste / Suchen**
   - Im *Reihen-Wächter* direkt auf einen fehlenden Band (z. B. *„Band 2 fehlt“*) tippen, um automatisch die Titel-Suche vorzubefüllen oder ihn auf die Merkliste zu setzen.
6. **Smart-Regale / Dynamische Sammlungen**
   - Eigene benannte Regale oder gespeicherte Smart-Suche-Filter (z. B. *„Herbst-Vibes“*, *„Urlaubs-SuB unter 350 Seiten“*, *„5-Sterne-Highlights“*).

### ⚡ Technische & UX-Optimierungen
1. **Automatisierte Unit- & Instrumentation-Tests**
   - JUnit-Testsuite für `SmartLibraryEngine`, `SeriesUniverseDetector`, `ReadingStatsCalculator`, `BookMetadataFetcher` (Parser) und Room-Migrationen (1→4) in `.github/workflows/build.yml` integrieren.
2. **Performance-Caching für Buch-Charakteristiken**
   - `analyzeBookCharacteristics` und `computeLocalFit` für große Bibliotheken (> 300 Bücher) memoizen bzw. auf `Dispatchers.Default` berechnen, damit Filter- und Such-Interaktionen dauerhaft mit 60/120 fps laufen.
3. **Android Share-Target („Teilen an Gideon's Bücherwelt“)**
   - Amazon-, Thalia-, Goodreads- oder StoryGraph-Links direkt aus dem Browser oder der Amazon-App per Android-Teilen-Menü an die App senden und sofort im *Buch hinzufügen*-Screen öffnen.
4. **Home-Screen-Widget (Glance)**
   - Kompaktes Android-Widget für das *aktuelle Lesepult* oder den *SuB-Tipp des Tages*.
5. **R8 / Code-Shrinking für Release-Builds**
   - `isMinifyEnabled = true` & `isShrinkResources = true` mit sauber getesteten ProGuard-Regeln für `kotlinx.serialization`, Room und Retrofit aktivieren, um die APK-Größe deutlich zu reduzieren.
6. **Barrierefreiheit & Tablet-/Foldable-Layout**
   - Adaptive Master-Detail-Ansicht (Bibliothek + Buchdetail nebeneinander) auf großen Displays sowie durchgängige `contentDescription`- und TalkBack-Optimierungen.

---

## 🛡️ Fehlerbehandlung & Offline-Verhalten

- **Keine Netzwerkverbindung**: Bibliothek, Smart-Suche, Reihen-Wächter, Lese-DNA, Ähnliche Bücher im Regal, SuB-Berater und manuelle Erfassung funktionieren vollständig offline.
- **ISBN/ASIN nicht gefunden**: Klarer Hinweis mit direkter Möglichkeit zur Titel-Suche, KI-Vervollständigung oder manuellen Erfassung.
- **Duplikate**: Zusammenführen-Dialog ergänzt leere Felder und vereint Tropes ohne Datenverlust.
- **LLM nicht konfiguriert oder offline**: Alle Kern- und Intelligenz-Features bleiben nutzbar; die Smart-Suche schaltet automatisch auf die lokale Offline-Intelligenz um.
