# ScamGuardian (Stage 1)

Privacy-first, on-device Android scam detector for SMS / WhatsApp / Telegram (KZ + CIS).

**Source of truth:** `context/PRD.md`, `DESIGN.md`, `ARCHITECTURE.md`, `SCHEMA.md`, `RULES.md`.

## Stack

- Kotlin, Jetpack Compose, Room
- minSdk 29 / targetSdk 35
- Package: `kz.qorgau.scamguardian`
- Layers: UI → ViewModel/UseCase → Domain → Data

## Project layout

```
app/src/main/java/kz/qorgau/scamguardian/
  di/                 # AppContainer (manual DI)
  domain/
    model/            # RiskLevel, AnalysisRecord, AppSettings, ScamRule…
    repository/       # Interfaces
    rules/            # RuleEngine contract
    classifier/       # ScamClassifier contract
  data/
    local/db/         # Room entities, DAOs, mappers, database
    repository/       # Room implementations
  ui/theme/           # DESIGN.md palette + typography
  notification/       # (next) NotificationListenerService
```

## Room schema (v1)

| Table | Purpose |
|-------|---------|
| `analysis_log` | Local analysis history |
| `app_settings` | Single-row user preferences |

No internet permission. Message text never leaves the device. Cloud backup of DB excluded.

## Build

```bash
./gradlew :app:assembleDebug
```

JDK 17+ required (Android Studio JBR works).

## Stage 1 implementation order

1. ✅ Project structure + Room schema
2. Rule Engine (JSON rules + matching)
3. NotificationListenerService
4. Pipeline: notify → analyze → store → alert
5. Screens (History, Settings, Manual check)
6. i18n + capability detection + model fallback
