## Gideon's Bücherwelt v1.6.1 – Amazon-Kurzlinks (`amzn.eu`), Indie-/Kindle-ASINs & Erweiterte Titel-Suche (Apple Books & Goodreads)

### 🔗 Amazon-Kurzlinks (`amzn.eu`, `amzn.to`, `a.co`) & Kindle-ASINs
- **Automatische Kurzlink-Auflösung** (`BookMetadataFetcher.resolveRedirectUrl`):
  - Löst nun auch verkürzte Amazon-Links wie `https://amzn.eu/d/…` automatisch über ihre Weiterleitung auf, extrahiert die dahinterliegende ASIN **sowie** den Titel-Slug aus der Ziel-URL.
- **Erweiterte ASIN- & Indie-/Self-Publisher-Quellen**:
  - Ergänzt Open Library, Google Books und Audnex um **Goodreads** sowie eine sichere, CAPTCHA-freie Auswertung öffentlicher Amazon-Produktseiten-Metadaten (inkl. Titel, Autor:in, Klappentext und Cover).
  - Alle OkHttp-Netzwerkaufrufe laufen nun konsequent auf `Dispatchers.IO`.

### 📚 Verbesserte Titel-Suche für Indie-, Dark-Romance- & Kindle-Bücher
- **Apple Books (iTunes eBook API)** & **Goodreads** direkt in der Titel-Suche (`searchByTitle` / `searchAndRankByTitleAuthor`) integriert – damit werden auch deutsche Self-Publisher- und Kindle-Unlimited-Titel gefunden, die nicht in Google Books oder Open Library gelistet sind.
