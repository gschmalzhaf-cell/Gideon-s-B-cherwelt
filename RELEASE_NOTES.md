## Gideon's Bücherwelt v1.7.0 – Bibliotheks-Intelligenz 2.0 (Offline Smart-Suche, Reihen-Wächter, Stimmungs-SuB & Lese-DNA)

### 🧠 100 % Offline Smart-Suche & Stimmungs-Chips in der Bibliothek
- **Natürliche Freitext- & Intent-Suche für alle** (`SmartLibraryEngine.localSmartSearch`):
  - Funktioniert jetzt **komplett offline und ohne LLM-API-Key** – und dient bei konfiguriertem LLM zusätzlich als nahtloser Offline-Fallback.
  - Versteht natürliche Suchanfragen wie *„ungelesen & düster“*, *„kurz & romantisch“*, *„ohne spice“*, *„5 sterne fantasy“* oder *„nächstes highlight“* (inkl. Status-, Spice-, Seitenlängen-, Bewertungs-, Stimmungs- und Pacing-Erkennung).
- **Quick-Tap Stimmungs-Chips**: Mit einem Tipp direkt nach *✨ Highlight*, *🌙 Düster*, *🔥 Spicy*, *☕ Cozy & Kurz*, *🐉 Magisch* oder *⭐ Favoriten* filtern.

### 📚 Reihen-Wächter (Weiterlesen & Lücken-Check)
- **Automatische Reihen-Analyse** (`SmartLibraryEngine.analyzeSeriesProgress`):
  - Erkennt begonnene Reihen mit einem direkt lesebereiten Folgeband im Regal (*„Bereit zum Weiterlesen“*).
  - **Lücken-Detektor**: Warnt automatisch, wenn zwischen deinen Bänden einzelne Nummern fehlen (z. B. *„Band 2 fehlt“*).
  - Direkt sichtbar im **Entdecken**-Tab sowie als vollständige Fortschritts-Übersicht in der **Lese-Chronik (Statistik)**.

### 🎭 Stimmungsbasierter SuB-Berater & smarterer Profil-Match
- **Leselaune-Filter im Entdecken-Tab**: Filtert und priorisiert deinen SuB passend zur heutigen Stimmung (*Alle*, *Fortsetzung*, *Düster*, *Romantisch*, *Magisch*, *Spannend*, *Wohlfühl*, *Kurz*).
- **Reihen-Kontinuität & Universum-Boost**: Direkte Folgebände deiner gelesenen Lieblingsreihen sowie Bücher aus deinen Lieblings-Universen werden im lokalen Profil-Match intelligent höher gewichtet.

### 🔍 Offline „Ähnliche Bücher im eigenen Regal“ & Lese-DNA
- **Sofortige Ähnlichkeitssuche im Regal**: Auf der Buchdetailseite werden ab sofort sofort und offline ähnliche Bücher aus deiner eigenen Sammlung inkl. Match-Prozentwert und Begründung angezeigt.
- **Lese-DNA & Zeit-Prognose** in der Statistik: Geschätzte gelesene Seiten & Lesestunden, SuB-Lesezeit, durchschnittliche Buchlänge und deine bevorzugten Lese-Stimmungen – plus klickbare Favoriten und Reihen-Folgebände.

### ✨ Verbesserte Online- & KI-Empfehlungen
- `RecommendationEngine` analysiert nun auch **Klappentexte** und abgeleitete Tropes/Genres/Spice-Werte externer Kandidaten.
- LLM-Empfehlungen erhalten eine Liste bereits vorhandener Titel, um doppelte Vorschläge zu vermeiden, sowie Reihen-, Universums- und Seitenangaben für präzisere KI-Einblicke.
