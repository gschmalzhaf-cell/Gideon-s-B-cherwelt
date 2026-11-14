# Google Stitch – Design-Projekt „Gideon's Bücherwelt"

Designgebende Quelle für die App-UI ist das Google-Stitch-Projekt (ersetzt Pixso):

- **Projekt:** `projects/2017226052745578868` – Titel: „Gideon's Bücherwelt – Android App (Bibliothek, Tropes, Empfehlungen)" (MOBILE, PRIVATE, Owner: g.schmalzhaf@gmail.com)
- **Design-System:** `assets/3041670234388923945` – „Bücherwelt – Warmes Bibliotheks-Theme (Light + Dark)"
  - Seed/Primary `#7A4E2D`, Secondary `#5D6B3F`, Tertiary `#C89B3C`, Neutral `#FAF5EF`, FIDELITY, ROUND_TWELVE/EIGHT
  - Font: Plus Jakarta Sans (body/headline/label); Spacing 4dp-Raster (space-xs…space-xl, gutter, margin)
  - designMd enthält die komplette Spezifikation (Farben Light+Dark, Komponenten, Screens) – siehe auch `../DESIGN.md`

## Generierte Screens (alle MOBILE, 780px breit, screenType DESIGN mit HTML-Code + Screenshot)

| Screen | Resource name |
|---|---|
| Bibliothek (Grid, Filter, FAB, BottomNav) | `projects/2017226052745578868/screens/c26319a9b0854f159dddcc03a9757ca7` (Varianten: `8c0ee47dac6c400abd39e5221607f5f6`, `a348c594a7f24658be5417134a2f313d`, `fb8981c924d344228ddb15e2f1b23f31`) |
| Buchdetail – Fourth Wing (Cover, Sterne, Spice, Tropes, Details, Notizen, Ähnliche Bücher) | `projects/2017226052745578868/screens/492b11c12db147f682a1db24154d3013` |
| Buch erfassen (ISBN/Scan, Cover-Vorschau, Formular, LLM-Chips, Trope-Picker, Speichern) | `projects/2017226052745578868/screens/1a854d6e50a8458ea9deb042d7642e41` |
| Entdecken (Profil-Banner, LLM-Insight, Empfehlungen, Merkliste) | `projects/2017226052745578868/screens/f7b32ecbf1f6430d9447d9f4e5579256` |
| Statistiken (KPIs, Monatsverlauf, Genre-Verteilung, Top-Tropes, Autor:innen, Verlage, Spice-Verteilung) | `projects/2017226052745578868/screens/9693c2ebc8c544efa59652d67e1dc415` |
| Einstellungen (Darstellung System/Hell/Dunkel, LLM-Key/Provider/Modell, Import, Tropes verwalten) | `projects/2017226052745578868/screens/79099f635f634adfa14404d2a5b31f5e` (Variante: `5166fd9d998047958aa3f32435fc90de`) |
| Bonus: Kuratierte Listen & Jahres-Highlights | `projects/2017226052745578868/screens/f6f8936953ee46528b686f4ef01f1502` |

HTML/Screenshots im Stitch-Projekt unter „Code" exportieren bzw. per MCP `download_file` mit der jeweiligen `html_code.name`/`screenshot.name` laden.

## Umsetzungstreue

Die Compose-Umsetzung folgt den Stitch-Tokens 1:1: Farben siehe
`app/src/main/java/com/gideon/buecherwelt/ui/theme/Color.kt`, Komponenten siehe
`ui/components/` (BookCard, BookCover, StarRating, TropePicker, SuggestionChips, FilterBar).
Dark Mode: in Stitch via Tailwind `dark:`-Klasse vorbereitet, in der App über
Einstellungen (System/Hell/Dunkel) umschaltbar.

Statische HTML-Referenz-Mockups (vor Stitch erstellt) liegen in `../mockups/`.
