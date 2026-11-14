# App-Icon „Gideon's Bücherwelt"

Entworfen in **Figma** (File „Gideon's Bücherwelt – App Icon":
https://www.figma.com/design/EXpFAAzNmXzDb6I4xp3dJy – zwei AI-generierte Varianten:
Full-Bleed + transparentes Adaptive-Foreground, Previews: `figma-ai-*-preview.jpg`).

Umgesetzt als **vektorbasiertes adaptives Icon** im Stitch-Farbschema:
- Hintergrund: `@color/ic_launcher_background` = `#7A4E2D` (primary)
- Foreground: `res/drawable/ic_launcher_foreground.xml` – aufgeschlagenes Buch in Creme
  `#FAF5EF` mit goldenem Lesezeichen `#C89B3C` und olivenem Akzent `#5D6B3F`, im
  108dp-Viewport auf die adaptive Safe-Zone (Mittelkreis ~66dp) skaliert
- PNG-Mipmaps (48–192dp) in `res/mipmap-*dpi/`, 512px für den Play-Store-Eintrag:
  `app-icon-512.png`; transparentes Foreground als Referenz: `app-icon-foreground-1024.png`
