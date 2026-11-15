# Gideon's Bücherwelt – v1.0.6

Cover-Scan als Recherche-Alternative, wenn die ISBN nichts bringt.

## Neu
- **„Cover scannen"** im Erfassungs-Screen (Foto aufnehmen per Kamera oder aus der Galerie): Die App erkennt das Buch per **LLM-Vision** (konfigurierter API-Key, Bild als base64 an das Modell) oder per **OCR (ML Kit Texterkennung)** und lädt die Metadaten dann über die ISBN-/Titel-Suche (Google Books / Open Library / DNB)
- Neue Kamera-Ansicht „CoverScannerScreen" (CameraX ImageCapture)
- Versionsnummer jetzt in den Einstellungen („Über") sichtbar

## Fixes
- Fehlermeldung bei erfolgloser Kennung nennt jetzt die geprüften Quellen (Google Books, Open Library, DNB)

**Full Changelog**: https://github.com/gschmalzhaf-cell/Gideon-s-B-cherwelt/commits/v1.0.6

---

# Gideon's Bücherwelt – v1.0.5

ISBN-Lookup deutlich robuster (z. B. gescannte ISBN 9783453292819, die weder Google Books noch Open Library direkt lieferten).

## Fixes
- **Fallback-Kette pro ISBN**: ISBN-13 ↔ ISBN-10 (Prüfziffer wird konvertiert), Google Books `isbn:`, Open Library `/isbn/…` **und** OL-Suche `isbn:`, als letzte Quelle **Deutsche Nationalbibliothek (SRU/oai_dc)** — besonders für deutschsprachige Bücher zuverlässig
- DNB-Metadaten (Titel, Autor:in, Verlag, Jahr, DDC-Sachgruppen) werden geparst und als Fallback gemerged
- Google Books ohne API-Key (429/Rate-Limit) wird sauber abgefangen und fällt nicht mehr auf „nichts gefunden" zurück

**Full Changelog**: https://github.com/gschmalzhaf-cell/Gideon-s-B-cherwelt/commits/v1.0.5

---

# Gideon's Bücherwelt – v1.0.4

Weitere Design-Nachbesserung Richtung Google Stitch (alle Screens).

## Design
- **Alle Eingabefelder** jetzt im Stitch-Stil: gefüllt (`surfaceContainerLow`), 12dp Radius, ohne Outline-Box (statt OutlinedTextField) — über gemeinsame `AppTextField`-Komponente in Bibliothek, Erfassen, Einstellungen, Tropes
- **Karten** überall konsistent: `surfaceContainerLow`, 16dp Radius — Entdecken, Statistik, Buchdetail (Hero als 24dp-Container mit Border, Details als Card), „Ähnliche Bücher"
- **Buchdetail-Hero** wie Stitch: Cover 115dp + Status-Chip + Titel in **Playfair Display** (Serif, gebundelt) + Autor:in in primary semibold
- **Buch erfassen**: ISBN/ASIN-Bereich und Bewertung/Status/Spice als Section-Cards (surface, 12dp, dezenter Border) wie im Stitch-Screen
- Trope-Chips auf dem Detail-Screen als Pills in primaryContainer

**Full Changelog**: https://github.com/gschmalzhaf-cell/Gideon-s-B-cherwelt/commits/v1.0.4

---

# Gideon's Bücherwelt – v1.0.3

Design-Treue zum Google-Stitch-Design-System erhöht.

## Design
- **Schriftart Plus Jakarta Sans** (Stitch-Font) jetzt in der ganzen App gebundelt (Gewichte 400–800 + kursiv) statt Systemschrift; Typografie-Skala gemäß Stitch (title-lg 22/600, title-md 16/600, body-md 14/400, body-sm 12/400, label-md 12/500)
- **Shapes**: Karten 16dp, Suchfeld 12dp, Chips/Status-Chips pill (voll gerundet)
- **Surface-Tonal-Layering** wie in Stitch: Buchkarten und Suchfeld in `surfaceContainerLow`, Filter-Chips in `surfaceContainer`
- **Top-App-Bar** wie Stitch: Logo-Kreis (primaryContainer) + Titel in primary, bold
- **Bottom-Navigation**: aktiver Tab als Pill in `primaryContainer`; **Filter-Chips** als Pills, aktiver Filter in `primary`
- Buchkarten-Titel semibold; Doku (`design/DESIGN.md`, `design/stitch/SCREENS.md`) aktualisiert

**Full Changelog**: https://github.com/gschmalzhaf-cell/Gideon-s-B-cherwelt/commits/v1.0.3

---

# Gideon's Bücherwelt – v1.0.2

Verbesserungen bei der Buch-Suche.

## Neu
- **Suche per ASIN und Goodreads-ID** zusätzlich zur ISBN: Im „Buch erfassen"-Feld (ISBN / ASIN / Goodreads-ID) werden ASINs im Format `B0…` über Open Library (`id_amazon`) aufgelöst und numerische Goodreads-IDs über `id_goodreads` – jeweils angereichert mit Metadaten/Cover von Google Books + Open Library
- **ISBN-Suche toleriert Bindestriche und Leerzeichen** (beidseitig): `978-3-16-148410-0` und `9783161484100` finden dasselbe Buch – sowohl in der Bibliotheks-Suche als auch bei den API-Abfragen

**Full Changelog**: https://github.com/gschmalzhaf-cell/Gideon-s-B-cherwelt/commits/v1.0.2

---

# Gideon's Bücherwelt – v1.0.1 (Hotfix)

Behebt einen Crash direkt beim App-Start.

## Fixes
- Absturz beim Start behoben (`IllegalArgumentException: long does not allow nullable values`): das optionale `editBookId`-Navigationsargument der „Buch erfassen/bearbeiten“-Route nutzte in Navigation 2.10.x nicht mehr erlaubte nullable `LongType`-Werte. Jetzt mit `defaultValue = -1L` als Sentinel, der intern wieder zu „neues Buch“ (`null`) gemappt wird.

**Full Changelog**: https://github.com/gschmalzhaf-cell/Gideon-s-B-cherwelt/commits/v1.0.1

---

# Gideon's Bücherwelt – v1.0.0 (erster Release)

Erste veröffentlichte Version der lokalen Bibliothek-App.

## Highlights
- Bücher per ISBN-Scan (Kamera/Barcode) oder manuell erfassen; Metadaten automatisch aus Google Books + Open Library (Fallback), Cover-Cache lokal – Bibliothek auch offline nutzbar
- Eigene Tags/Tropes mit Autovervollständigung, Bewertung (1–5 Sterne), Lesestatus, Spice-Level (0–5), Genre/Subgenre, Freitextnotizen
- Optionale LLM-gestützte Trope-/Genre-/Spice-Vorschläge und personalisierte Empfehlungen (konfigurierbarer API-Key, nur lokal gespeichert)
- StoryGraph-CSV-Import inkl. automatischem Metadaten-Nachladen per ISBN
- Statistik-Dashboard (Top-Genre/-Tropes/-Autor:innen/-Verlage, Ø Spice-Level) und regelbasierte „Ähnliche Bücher“-Empfehlungen im Entdecken-Bereich mit Merkliste
- Dark Mode; rein lokal (Room + DataStore), kein Login, keine Cloud

**Full Changelog**: https://github.com/gschmalzhaf-cell/Gideon-s-B-cherwelt/commits/v1.0.0
