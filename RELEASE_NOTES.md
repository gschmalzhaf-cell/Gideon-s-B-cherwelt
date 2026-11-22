## Gideon's Bücherwelt v1.7.1 – Zuverlässiger ISBN-Scan & ISBN-Suche (Bugfix-Release)

### 🔧 Behobene Ursachen bei ISBN-Scan & manueller ISBN-Eingabe
- **Kein Abbruch mehr bei einzelnen API-Timeouts**: Bisher brach die gesamte ISBN-Suche mit einem Fehler ab, sobald *eine einzelne* Quelle (z. B. Open Library oder DNB) kurzzeitig nicht erreichbar war. Jetzt werden alle Quellen unabhängig voneinander abgefragt – nur wenn wirklich keine Verbindung besteht, erscheint ein Netzwerkfehler.
- **Korrekte ISBN-10 ↔ ISBN-13 Umrechnung** (`isbnVariants`): Fehlerhafte Prüfziffern-Berechnung bei der Umwandlung von ISBN-10 in ISBN-13 behoben.
- **Bindestriche, Präfixe & Barcode-Zusätze** (`normalizeIsbn` & `BarcodeScannerScreen`):
  - Erkennt nun zuverlässig ISBNs mit Bindestrichen/Gedankenstrichen, vorangestellten Präfixen wie `ISBN-13: 978-…` / `ISBN-10: …` sowie **EAN-13-Barcodes mit 2- oder 5-stelligem Preis-Zusatz**.
  - Der Kamera-Barcode-Scanner filtert nun gezielt nach gültigen ISBN-Barcodes (`978`/`979` bzw. ISBN-10), damit kleine Neben-Barcodes (z. B. Preis- oder Verlags-Codes auf der Rückseite) den Scan nicht mehr kapern.
- **Erweiterte Katalog-Fallbacks für ISBNs**:
  - Ergänzt Google Books (`isbn:` + Volltext-Fallback), Open Library und Deutsche Nationalbibliothek (DNB) nun zusätzlich um **Apple Books (iTunes ISBN Lookup)**, **Goodreads**, **Amazon-Metadaten (ISBN-10, CAPTCHA-frei)** sowie optionalen **KI-Fallback** bei hinterlegtem LLM-Key.
