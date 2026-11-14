# Gideon's Bücherwelt – Design-Spezifikation (verbindlich für die Umsetzung)

Designgebende Quelle ist das **Google-Stitch-Projekt** „Gideon's Bücherwelt – Android App
(Bibliothek, Tropes, Empfehlungen)" (`projects/2017226052745578868`) mit dem Design-System
`assets/3041670234388923945` – Details und Screen-IDs siehe `design/stitch/SCREENS.md`.
Diese Spezifikation hier beschreibt die verbindlichen Tokens; die sechs Bildschirm-Mockups
liegen als statische HTML-Referenz in `design/mockups/`. Die Compose-Umsetzung folgt exakt
diesen Tokens (`ui/theme/Color.kt`, `Theme.kt`, `ui/components/*`).

## 1. Farbschema

„Bibliotheks-Wärme": dunkles Holzbraun als Primärfarbe, warmes Creme-Weiß im Light Mode,
tiefes Braun-Schwarz im Dark Mode, Gold als Akzent (Sterne, Chips-Auswahl).

### Light Mode
| Token | Hex | Verwendung |
|---|---|---|
| primary | #7A4E2D | Buttons, aktive Bottom-Nav, primäre Aktionen |
| onPrimary | #FFFFFF | Text auf primary |
| primaryContainer | #F1DEC9 | Status-Chip „Lesend", Filter-Chips selected |
| secondary | #5D6B3F | Status-Chip „Gelesen" |
| secondaryContainer | #E2E8C8 | |
| tertiary | #C89B3C | Status-Chip „Geplant", Sterne, Akzente |
| tertiaryContainer | #F6E7C1 | |
| background | #FAF5EF | App-Hintergrund |
| surface | #FFFBF6 | Karten, Bottom-Nav, Dialoge |
| surfaceVariant | #EDE0D2 | Cover-Platzhalter, Eingabefelder |
| onSurface | #211A14 | Haupttext |
| onSurfaceVariant | #51453A | Nebentext |
| error / errorContainer | #B3261E / #F9DEDC | Fehler, Status „Abgebrochen" |
| spice | #E25822 | Flammen-Icons (Spice-Level) |

### Dark Mode
| Token | Hex |
|---|---|
| primary | #E3B98F |
| onPrimary | #442A14 |
| primaryContainer | #5F3D22 |
| onPrimaryContainer | #F6DFC6 |
| secondary | #C6CF9F |
| secondaryContainer | #414A28 |
| tertiary | #DDB968 |
| tertiaryContainer | #6A4E0C |
| background | #171009 |
| surface | #211710 |
| surfaceVariant | #362A1F |
| onSurface | #EFE3D8 |
| onSurfaceVariant | #CBBBA9 |
| error / errorContainer | #F2B8B5 / #8C1D18 |

Dark Mode ist in den Einstellungen umschaltbar (System / Hell / Dunkel), Default: System.

## 2. Typografie

Material-3-Default-Typografie (sans-serif, system font):
- Display/Top-App-Bar-Titel: `titleLarge` (22sp)
- Screen-Titel in TopAppBar: `titleLarge`
- Abschnitts-Überschriften (SectionHeader): `titleMedium` (16sp, semibold)
- Buchkarten-Titel: `bodyMedium` (14sp, max 2 Zeilen, Ellipsis)
- Nebentext (Autor, Metadaten): `bodySmall` (12sp, onSurfaceVariant)
- Chips/Labels: `labelLarge` / `labelSmall`

## 3. Spacing-System

4dp-Baseline: 4 / 8 / 12 / 16 / 24 / 32. Screen-Padding: 16dp. Karten-Innenabstand: 12dp.
Grid-Gap Bibliothek: 12dp. Bottom-Nav-Höhe: Standard (80dp). FAB: Standard, rechts unten.

## 4. Komponentenbibliothek

| Komponente | Spezifikation |
|---|---|
| Primary Button | filled, primary, height 48dp, radius 24dp (pill), Icon + Label |
| Outlined Button | outlined, surface, pill |
| Buchkarte (BookCard) | Card radius 16dp; Cover 2:3 (aspectRatio), ContentScale.Crop; Titel 2 Zeilen; Autor 1 Zeile; unten: Sterne (links) + Status-Chip (rechts) |
| Cover-Platzhalter | surfaceVariant mit `MenuBook`-Icon, zentriert |
| Trope-Chip | FilterChip, pill, selected = primaryContainer; mit ×-Trailing-Icon wenn abwählbar |
| LLM-Vorschlags-Chips | FilterChip in einem surface-Container mit Label „LLM-Vorschläge – antippen zum Übernehmen" |
| Sterne-Bewertung | 5 Sterne, gefüllt = #C89B3C (Star), leer = StarBorder in onSurfaceVariant; klickbar (erneuter Klick auf akt. Stern = 0) |
| Status-Chip | pill 8dp radius, Farbe je Status (s. Tabelle) |
| Spice-Level | Reihe aus bis zu 5 Flammen-Icons (#E25822) + „x/5" |
| Filterleiste | Suchfeld (volle Breite, Search-Icon) + horizontal scrollbare Reihe aus Dropdown-Buttons (Genre / Status / Bewertung / Trope) |
| Dropdown-Filter | OutlinedButton mit Label + ArrowDropDown; Menü: „Alle" + Optionen |
| Bottom Navigation | 4 Items: Bibliothek (Book), Entdecken (Explore), Statistik (BarChart), Einstellungen (Settings); nur auf Haupt-Screens sichtbar |
| TopAppBar | titleLarge links, Actions rechts (Settings/Edit/Delete) |
| FAB | „+“ (Add), primary |
| Scanner-Overlay | Vollbild-Kamera, oben rechts Close-Button (kreisförmig, halbtransparent), unten Hinweis-Text |
| Leer-Zustand | zentriert: Titel (titleMedium) + Untertitel (bodyMedium, onSurfaceVariant) |
| Fehler-Banner | errorContainer, abgerundet 12dp, Text + OK-Button |
| Fortschritt | LinearProgressIndicator bei Import/Loading; CircularProgressIndicator (18dp) in Buttons |

## 5. Bildschirme (Mockups in `design/mockups/`)

1. `01-library.html` – Bibliothek: TopAppBar „Gideon's Bücherwelt" + Settings-Icon; Filterleiste; 3-Spalten-Grid (adaptiv, min 110dp) aus Buchkarten; FAB „+".
2. `02-book-detail.html` – Buchdetail: Cover links (120dp), Titel/Autor/Status/Sterne/Spice rechts; Abschnitte Details, Tropes-Chips, Beschreibung, Notizen, „Ähnliche Bücher" mit Button + Liste; TopAppBar mit Edit/Delete.
3. `03-add-scan.html` – Erfassung: ISBN-Feld + Buttons „Daten laden"/„ISBN scannen"; Cover-Vorschau; alle editierbaren Felder; LLM-Chips; Trope-Picker; Speichern-Button.
4. `04-discover.html` – Entdecken: Profil-Zeile; Buttons „Empfehlungen laden" / „Per LLM empfehlen"; Empfehlungskarten (Cover, Titel, Autor, Gründe, Add + Merkliste); Merkliste.
5. `05-stats.html` – Statistik: Karten für Bibliothek/Status, Top-Genres-Chips, Genre-Verteilung (Balken), Top-Tropes, Lieblingsautor:innen/-verlage, Ø Spice-Level, Lieblingsbücher.
6. `06-settings.html` – Einstellungen: Segmented Control Darstellung (System/Hell/Dunkel), LLM-Felder (Key als Passwort), Daten (CSV-Import, Tropes verwalten), Über.

## 6. Umsetzungstreue

- Compose-Tokens siehe `app/src/main/java/com/gideon/buecherwelt/ui/theme/Color.kt` (exakt die Hex-Werte oben).
- Komponenten siehe `ui/components/` (BookCard, BookCover, StarRating, TropePicker, SuggestionChips, FilterBar, Common.kt).
- Prüfung: Screenshots der App mit den Pixso-Mockups vergleichen (Layout, Farben, Abstände).
