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
