# Gideon's Bücherwelt

Android-App (Kotlin + Jetpack Compose, MVVM) zum Verwalten einer persönlichen Bibliothek und
zum Entdecken neuer Bücher. **100 % lokal** (Room-Datenbank, DataStore), offline-fähig;
Netzwerk wird nur für ISBN-Abfragen (Google Books + Open Library), Cover-Downloads und
optionale LLM-Aufrufe (z. B. Mistral) genutzt.

## Tech-Stack

- Kotlin 2.0, Jetpack Compose (Material 3), Navigation-Compose, MVVM
- Room (SQLite) – Bücher, Tropes, Buch↔Trope, Merkliste
- DataStore (Einstellungen: Theme, LLM-Key/-URL/-Modell)
- Retrofit + OkHttp + kotlinx.serialization – Google Books API, Open Library API
- ML Kit Barcode-Scanning + CameraX – ISBN-Scan per Kamera
- Coil – Cover-Anzeige; Cover werden zusätzlich lokal in `filesDir/covers` gecacht (offline)
- LLM-Client (OpenAI-kompatibles `/chat/completions`, konfigurierbare Basis-URL)

## Funktionen

1. **Bücher erfassen** – ISBN per Kamera-Scan (EAN/ISBN) oder manuell; Buchdaten werden
   automatisch geladen (Google Books, Open Library als Fallback für fehlende Felder);
   manuelle Nachbearbeitung aller Felder; komplett manuelle Erfassung ohne ISBN möglich.
   Duplikate (ISBN oder Titel+Autor) werden erkannt und können zusammengeführt werden.
2. **Metadaten & Tags** – editierbarer Tropes-Katalog (Autovervollständigung, Mehrfachauswahl,
   eigene Tropes), Bewertung 1–5, Lesestatus (geplant/lesend/gelesen/abgebrochen),
   Spice-Level 0–5, Genre/Subgenre, Notizen. **LLM-Trope-Vorschläge** als anklickbare Chips
   (nur nach Bestätigung übernommen). **StoryGraph-CSV-Import** (Titel, Autor:in, Status,
   Tags; Buchdaten per ISBN nachladen).
3. **Statistik** – Dashboard mit Top-Genres, Top-Tropes, Lieblingsautor:innen/-verlagen,
   Ø Spice-Level, Ø Bewertung, Genre-Verteilung, Status-Übersicht, Lieblingsbüchern **und
   „Weniger beliebt"** (Abbrüche + 1-2-Sterne-Bewertungen als negative Signale). Wird
   automatisch aus den erfassten Büchern abgeleitet (gelesene + hoch bewertete Bücher zählen stärker).
4. **Entdecken** – regelbasierte Empfehlungen (gleiche Tropes/Genre/Autor:in/Verlag, diversifiziert,
   negative Signale abgewertet) und optionale LLM-Empfehlungen mit Begründung; bei konfiguriertem
   LLM-Key zusätzlich **KI-Re-Ranking** der regelbasierten Kandidaten mit Begründung pro Buch;
   „Ähnliche Bücher" pro Buch; Merkliste; per Klick (ISBN) zur Sammlung hinzufügen.
5. **KI-Features** (optional, LLM-Key in den Einstellungen) – **semantische Suche** in der
   Bibliothek per Freitext (z. B. „düstere Bücher mit Happy End") und **KI-Einblicke** in der
   Buchdetailansicht (spoilerfreie Zusammenfassung + „Passt zu deinem Profil"-Score 0-100,
   lokal in der Datenbank gecacht).

## Projektstruktur

```
app/src/main/java/com/gideon/buecherwelt/
  di/AppContainer.kt            manuelles DI
  data/local/                   Room (Entities, DAOs, AppDatabase), TropesCatalog
  data/settings/                DataStore SettingsRepository (Theme, LLM)
  data/network/                 GoogleBooksApi, OpenLibraryApi, BookMetadataFetcher, CoverCache
  data/csv/                     StoryGraphCsvImporter
  domain/                       Modelle, Repositories, Analyse, Empfehlungen, LLM, UseCases
  ui/                           Theme, Navigation, Komponenten, Screens (library, add, scanner,
                                detail, discover, stats, settings, tropes, import)
design/DESIGN.md                verbindliche Design-Spezifikation (Farben, Typo, Komponenten)
design/mockups/*.html           6 Bildschirm-Mockups (Pixso-importierbar, 412x915)
```

## Build & Start

Voraussetzung: Android Studio (neueste Version) mit Android SDK 35, JDK 17.

```bash
# Gradle-Wrapper erzeugen (einmalig, falls gradle-wrapper.jar fehlt):
gradle wrapper            # oder in Android Studio: Rechtsklick gradle-wrapper.properties

./gradlew assembleDebug
```

App auf dem Smartphone installieren (`app-debug.apk`) und starten.
Berechtigungen: Kamera (nur für den ISBN-Scan), Internet (nur für API-Abfragen).

## Release

Siehe [RELEASE.md](RELEASE.md): Keystore lokal erzeugen, 4 GitHub-Secrets setzen
(`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), dann `git tag v1.0.0 && git push origin v1.0.0` –
der Workflow `.github/workflows/release.yml` baut signierte APK + AAB und legt ein GitHub-Release an.
Bei jedem Push/PR baut `.github/workflows/build.yml` eine Debug-APK (CI-Compile-Check).

## Design (Google Stitch)

Die GUI ist im Google-Stitch-Projekt **„Gideon's Bücherwelt – Android App (Bibliothek, Tropes,
Empfehlungen)"** (`projects/2017226052745578868`) entworfen – inkl. Design-System
(`assets/3041670234388923945`, warmes Bibliotheks-Theme mit Light+Dark) und allen sechs
Hauptbildschirmen (Bibliothek, Buchdetail, Buch erfassen/Scan, Entdecken, Statistiken,
Einstellungen; Screen-IDs siehe [design/stitch/SCREENS.md](design/stitch/SCREENS.md)).
Die verbindliche Token-Spezifikation liegt in [design/DESIGN.md](design/DESIGN.md), statische
Referenz-Mockups in [design/mockups/](design/mockups/). Die Compose-Umsetzung implementiert
dieselben Tokens (Farben siehe `ui/theme/Color.kt`, Komponenten siehe `ui/components/`).

## Entwicklungs-Schritte (jeweils lauffähige Version, eigene Commits)

1. Grundgerüst + Erfassung (Scan/ISBN/manuell) + Bibliothek-Grid + Dark Mode
2. Tropes/Tags + LLM-Vorschläge + StoryGraph-CSV-Import + Tropes-Verwaltung
3. Statistik-Dashboard (Lesevorlieben-Analyse)
4. Entdecken (regelbasiert + LLM + Merkliste + ähnliche Bücher)

## Fehlerbehandlung

- Keine Netzwerkverbindung → Hinweis, manuelle Erfassung weiterhin möglich, alles andere offline nutzbar.
- ISBN nicht gefunden → Meldung, manuelle Erfassung möglich.
- Duplikate → Dialog „bereits vorhanden – zusammenführen?" (leere Felder ergänzen, Tropes vereinen).
- LLM nicht konfiguriert/offline → nur manueller Tropes-Katalog bzw. nur regelbasierte Empfehlungen.
