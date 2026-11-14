# Erster Release – Checklist

## Was bereits im Repo ist
- Vollständiger App-Code (4 Schritte, Commits siehe `git log`), lauffähig ab Schritt 1
- Design in Google Stitch (`projects/2017226052745578868`, Design-System `assets/3041670234388923945`), Doku in `design/`
- Release-Signing in `app/build.gradle.kts` liest Keystore **nur aus Umgebungsvariablen** (keine Secrets im Repo)
- CI: `.github/workflows/build.yml` baut bei jedem Push/PR eine Debug-APK (assembleDebug)
- CI: `.github/workflows/release.yml` baut bei Tag `v*` signierte APK + AAB und legt ein GitHub-Release an

## Einmalig lokal: Keystore erzeugen (Key bleibt auf deinem Gerät!)
```bash
keytool -genkeypair -v -keystore ~/buecherwelt-release.jks \
  -alias buecherwelt -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass <STORE_PASSWORD> -keypass <KEY_PASSWORD> \
  -dname "CN=Gideon Schmalzhaf, O=Privat, C=DE"
```

## GitHub-Secrets einrichten (Repo → Settings → Secrets and variables → Actions)
| Secret | Inhalt |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w 0 ~/buecherwelt-release.jks` |
| `KEYSTORE_PASSWORD` | Store-Passwort |
| `KEY_ALIAS` | `buecherwelt` |
| `KEY_PASSWORD` | Key-Passwort |

## Release auslösen
```bash
git tag v1.0.0
git push origin v1.0.0
```
Der Workflow baut `app-release.apk` (Sideload/ direkt installierbar) und `app-release.aab`
(Google Play) und hängt beide an ein GitHub-Release.

## Lokal signiert bauen (ohne CI)
```bash
export KEYSTORE_PATH=~/buecherwelt-release.jks
export KEYSTORE_PASSWORD=... KEY_ALIAS=buecherwelt KEY_PASSWORD=...
gradle wrapper && ./gradlew :app:assembleRelease :app:bundleRelease
# APK: app/build/outputs/apk/release/app-release.apk
# AAB: app/build/outputs/bundle/release/app-release.aab
```

## Vor dem Release prüfen
- `versionCode`/`versionName` in `app/build.gradle.kts` passend (aktuell 1 / 1.0)
- In der App: Einstellungen → LLM-Key nur lokal (DataStore), nichts wird committed
- Smoke-Test auf Gerät: ISBN-Scan, manuelle Erfassung, CSV-Import, Statistik, Entdecken, Dark Mode
- Hinweis: In der Sandbox war kein Android-SDK verfügbar – der erste CI-Lauf (build.yml)
  kompiliert erstmals; bei Fehlern melden, ich fixe sie.

## Google Play (optional)
- AAB aus dem Release-Artefakt hochladen, Store-Eintrag anlegen, App-Signing von Google aktivieren.
- Datenschutzerklärung erforderlich (App liest nur lokal; Netzwerk nur für ISBN/LLM/Cover – in der Erklärung vermerken).
