# Data Schema
**Project:** ScamGuardian (Stage 1)  
**Version:** 1.0  
**Date:** 2026-07-28  

## 1. Overview

ScamGuardian Stage 1 uses only **local on-device storage**.  
There is no remote database and no user accounts.

Primary storage: **Room (SQLite)** for structured history and settings.  
Secondary: SharedPreferences / DataStore for simple flags and user preferences.  
Model files and rule packs live in app-private internal storage.

## 2. Entity Relationship

```
┌──────────────────┐       ┌─────────────────────┐
│   AnalysisLog    │       │    AppSettings      │
│──────────────────│       │─────────────────────│
│ id (PK)          │       │ id (PK)             │
│ source_app       │       │ language            │
│ sender           │       │ sensitivity         │
│ message_text     │       │ rules_only_mode     │
│ risk_level       │       │ monitor_sms         │
│ risk_score       │       │ monitor_whatsapp    │
│ explanation      │       │ monitor_telegram    │
│ matched_rules    │       │ model_enabled       │
│ created_at       │       │ last_model_check    │
│ user_feedback    │       └─────────────────────┘
│ is_read          │
└──────────────────┘
```

## 3. Tables

### 3.1 AnalysisLog

Stores every analyzed message (or at least every alert).

| Column          | Type          | Constraints          | Description |
|-----------------|---------------|----------------------|-------------|
| id              | INTEGER       | PRIMARY KEY, AUTOINCREMENT | Unique ID |
| source_app      | TEXT          | NOT NULL             | Package name or friendly name (sms, whatsapp, telegram) |
| sender          | TEXT          | NULLABLE             | Phone number or chat title |
| message_text    | TEXT          | NOT NULL             | Original or cleaned text that was analyzed |
| risk_level      | TEXT          | NOT NULL             | `high` / `suspicious` / `safe` |
| risk_score      | REAL          | NULLABLE             | 0.0–1.0 from classifier (if used) |
| explanation     | TEXT          | NOT NULL             | Human-readable reason (RU or KK) |
| matched_rules   | TEXT          | NULLABLE             | JSON array of rule IDs that fired |
| created_at      | INTEGER       | NOT NULL             | Unix timestamp |
| user_feedback   | TEXT          | NULLABLE             | `false_positive` / `confirmed` / null |
| is_read         | INTEGER       | NOT NULL, DEFAULT 0  | 0 = unread, 1 = read |

**Indexes:**
- `created_at DESC` (for history screen)
- `risk_level` (for filtering)
- `source_app`

### 3.2 AppSettings

Simple key-value style table or single-row settings.

| Column             | Type    | Default     | Description |
|--------------------|---------|-------------|-------------|
| id                 | INTEGER | 1           | Always single row |
| language           | TEXT    | `ru`        | `ru` or `kk` |
| sensitivity        | TEXT    | `medium`    | `low` / `medium` / `high` |
| rules_only_mode    | INTEGER | 0           | 1 = never use model |
| monitor_sms        | INTEGER | 1           | |
| monitor_whatsapp   | INTEGER | 1           | |
| monitor_telegram   | INTEGER | 1           | |
| model_enabled      | INTEGER | 1           | |
| last_model_check   | INTEGER | 0           | Timestamp of last model integrity check |

## 4. Local Files (not in SQLite)

- `/data/data/<package>/files/models/` — quantized model files
- `/data/data/<package>/files/rules/` — versioned JSON rule packs
- `/data/data/<package>/files/patterns/` — optional compiled pattern cache

## 5. Data Retention Policy

- Default: keep last 90 days of AnalysisLog.
- User can clear history at any time.
- High-risk entries can be kept longer if desired (configurable later).
- No automatic upload or backup of message content.

## 6. Migration Strategy

- Room versioning from the first release.
- Rule packs are versioned independently (semantic version in JSON).
- Model updates are treated as file replacements with checksum verification.

## 7. Privacy Notes

- `message_text` never leaves the device.
- When the user marks something as false positive, only the local record is updated.
- No analytics events contain message content.

## 8. Future Schema Extensions (not in Stage 1)

- `KnownScamNumbers` table (for Stage 2 Call Screening).
- `RuleContribution` local queue (if community contribution is added later).
- `ModelPerformanceLog` for on-device evaluation metrics.

---

**Schema owner:** Sayan  
**Last updated:** 2026-07-28