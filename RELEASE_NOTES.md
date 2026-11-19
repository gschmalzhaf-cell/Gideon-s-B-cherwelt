## Gideon's Bücherwelt v1.6.0 – Zuverlässige ASIN-Erkennung, Titel-Suche mit Trefferliste & KI-Buchvervollständigung

### 🔎 Verbesserte ASIN- & Kennungs-Erfassung („Buch erfassen“)
- **Erweiterte ASIN-Auflösung** (`BookMetadataFetcher`):
  - Unterstützt nun neben klassischen ISBN-10/13 und Goodreads-IDs auch **Amazon ASINs** (z. B. `B0…` oder `A…`) sowie direkt eingefügte **Amazon-Produktlinks** (`/dp/…`, `/gp/product/…`).
  - Kombiniert Abfragen über **Open Library** (`id_amazon:` & Volltext), **Google Books**, die **Audnex-API** (für Audible-/Amazon-ASINs) und reichert gefundene Treffer automatisch per ISBN sowie Titel + Autor:in mit Cover und Klappentext an.
  - Falls ein ASIN-Eintrag in keinem Online-Katalog gelistet ist, springt bei konfiguriertem LLM-Key automatisch die **KI-Identifikation** ein.

### 📚 Neue Titel-Suche beim Buch-Erfassen (`AddBookScreen`)
- **Zwei Modi in der Erfassungskarte**: Umschaltbar zwischen **`ISBN / ASIN`** und **`Titel-Suche`**.
- **Titel-Suche mit optionaler Autor:innen-Angabe**: Durchsucht Google Books, Open Library und DNB gleichzeitig und zeigt eine übersichtliche **Trefferliste** mit Cover-Vorschau, Titel, Autor:in, Verlag, Erscheinungsjahr und Seitenzahl. Per Fingertipp wird das gewählte Buch inkl. Cover, Reihe/Universum und Tropes direkt ins Formular übernommen.

### ✨ Suche & Buchdaten per KI vervollständigen
- **KI-Buchvervollständigung** (`LlmService.completeBook` / `AddBookViewModel.completeWithAi`):
  - Sowohl in der **`ISBN / ASIN`**-Suche, in der **`Titel-Suche`** (inkl. *„Nicht dabei? Mit KI vervollständigen“*) als auch direkt im Formular (*„Fehlende Buchdetails & Cover mit KI vervollständigen“*).
  - Identifiziert das Buch anhand von Kennung, Titel oder Teilangaben, ergänzt fehlenden Klappentext, Untertitel, Verlag, Jahr, Seitenzahl, Genre, Buchreihe, Bandnummer, Buch-Universum, Spice-Level sowie passende Tropes – und lädt automatisch das passende Buchcover nach.
