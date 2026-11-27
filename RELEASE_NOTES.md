## Gideon's Bücherwelt v1.7.4 – Cover-Scan mit Internet-Recherche als Fallback

### 🌐 Neu: Websuche, wenn das Cover nicht erkannt wird
- Wird ein Buch beim Cover-Scan weder per **KI (LLM)** noch per **OCR + Katalog** (Google Books / Open Library / DNB) gefunden, startet die App automatisch eine **Internet-Recherche**: Die auf dem Cover erkannten Texte werden über die offene Websuche (DuckDuckGo/Bing, ohne API-Key) gesucht und die Top-Treffer – z. B. **Autor:innen-Seite, Verlag oder Buchhandlung** – werden analysiert. Titel, Autor:in, ISBN, Cover und Erscheinungsjahr werden aus den Seiten-Metadaten übernommen und anschließend gegen die Kataloge aufgelöst. Jedes Buch ist irgendwo auffindbar.
- Im Dialog „Cover scannen“ wird der Ablauf erklärt, und während der Erkennung zeigt die App den aktuellen Schritt an (KI-Analyse → OCR → Kataloge → Internet-Recherche).

---

## Gideon's Bücherwelt v1.7.3 – Bibliothek: Intelligentere Suche & intuitiveres Filtern

### 🔎 Mehr-Begriff-Suche (UND-Verknüpfung)
- Die normale Suche im Bibliothek-Reiter versteht jetzt **mehrere Begriffe** (per Leerzeichen getrennt, z. B. `sanderson stormlight`): Ein Buch erscheint nur, wenn **jeder** Begriff in Titel, Autor:in, Reihe, Universum, Genre, Trope, Notizen, Beschreibung oder ISBN vorkommt. ISBN-Suche funktioniert weiterhin – auch pro Begriff und als komplette ISBN mit Leerzeichen.

### ↩️ Filter schneller zurücksetzen
- Sobald Suche oder ein Filter aktiv ist, erscheint in der Filter-Leiste ein **„Zurücksetzen“-Chip** – ein Tipp genügt, um Suche, alle Filter und die Sortierung zurückzusetzen.
- Auch im „Keine Bücher gefunden“-Hinweis gibt es jetzt einen Button **„Suche & Filter zurücksetzen“** (setzt zusätzlich eine aktive Smart-Suche zurück).

### ⌨️ Smart-Suche per Tastatur starten
- Im Smart-&-KI-Suchfeld startet die **Suchtaste (IME „Suchen“)** der Tastatur die Suche direkt – der Griff zum Lupen-Button ist optional.

### 🧠 Neuer Smart-Suche-Intent „unbewertet“
- Fragen wie „unbewertet“, „ohne Bewertung“ oder „noch nicht bewertet“ finden jetzt gezielt Bücher **ohne Sterne-Bewertung** – inkl. neuem Preset-Chip „❓ Unbewertet“.

---

## Gideon's Bücherwelt v1.7.2 – Bibliothek: Suchfelder scrollen mit dem Regal

### ✨ Verbesserung
- **Suchfelder mitscrollen lassen**: Auf dem Reiter *Bibliothek* werden die beiden Suchfelder oben (Titel/Autor:in/ISBN-Suche und Smart- & KI-Suche inklusive Filter-Chips, Smart-Presets und Fortschrittsanzeige) nicht mehr fest am oberen Rand gehalten. Beim Hochscrollen der Bücher schieben sie sich jetzt gemeinsam mit dem Bücherregal nach oben – so bleibt mehr Platz für die Bücher im Blick.

---

## Gideon's Bücherwelt v1.7.1 – Zuverlässiger ISBN-Scan & ISBN-Suche (Bugfix-Release)

### 🔧 Behobene Ursachen bei ISBN-Scan & manueller ISBN-Eingabe
- **Kein Abbruch mehr bei einzelnen API-Timeouts**: Bisher brach die gesamte ISBN-Suche mit einem Fehler ab, sobald *eine einzelne* Quelle (z. B. Open Library oder DNB) kurzzeitig nicht erreichbar war. Jetzt werden alle Quellen unabhängig voneinander abgefragt – nur wenn wirklich keine Verbindung besteht, erscheint ein Netzwerkfehler.
- **Korrekte ISBN-10 ↔ ISBN-13 Umrechnung** (`isbnVariants`): Fehlerhafte Prüfziffern-Berechnung bei der Umwandlung von ISBN-10 in ISBN-13 behoben.
- **Bindestriche, Präfixe & Barcode-Zusätze** (`normalizeIsbn` & `BarcodeScannerScreen`):
  - Erkennt nun zuverlässig ISBNs mit Bindestrichen/Gedankenstrichen, vorangestellten Präfixen wie `ISBN-13: 978-…` / `ISBN-10: …` sowie **EAN-13-Barcodes mit 2- oder 5-stelligem Preis-Zusatz**.
  - Der Kamera-Barcode-Scanner filtert nun gezielt nach gültigen ISBN-Barcodes (`978`/`979` bzw. ISBN-10), damit kleine Neben-Barcodes (z. B. Preis- oder Verlags-Codes auf der Rückseite) den Scan nicht mehr kapern.
- **Erweiterte Katalog-Fallbacks für ISBNs**:
  - Ergänzt Google Books (`isbn:` + Volltext-Fallback), Open Library und Deutsche Nationalbibliothek (DNB) nun zusätzlich um **Apple Books (iTunes ISBN Lookup)**, **Goodreads**, **Amazon-Metadaten (ISBN-10, CAPTCHA-frei)** sowie optionalen **KI-Fallback** bei hinterlegtem LLM-Key.
