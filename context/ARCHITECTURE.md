# Architecture Document
**Project:** ScamGuardian (Stage 1)  
**Version:** 1.0  
**Date:** 2026-07-28  

## 1. High-Level Overview

ScamGuardian Stage 1 is a single-process Android application that runs entirely on the device.  
There is **no backend**, no account system, and no network dependency for the core detection loop after the optional model has been downloaded once.

```
┌─────────────────────────────────────────────────────┐
│                   Android App                       │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │ Notification │  │   Rule       │  │  On-device │ │
│  │   Listener   │→ │   Engine     │→ │  Classifier│ │
│  └──────────────┘  └──────────────┘  └───────────┘ │
│           │                │               │        │
│           └────────────────┼───────────────┘        │
│                            ▼                        │
│                   ┌─────────────────┐               │
│                   │  Alert Manager  │               │
│                   │  + Local Store  │               │
│                   └─────────────────┘               │
└─────────────────────────────────────────────────────┘
```

## 2. Design Principles

- **Privacy by architecture** — analysis never leaves the device.
- **Progressive enhancement** — pure rules work on the weakest phones; model is optional.
- **Fail safe** — if the model fails to load or is too slow, fall back to rules only.
- **Auditable** — core detection logic and pattern lists are open and readable.
- **Battery conscious** — rules run first; model is invoked only on ambiguous cases.

## 3. Major Components

### 3.1 Notification Capture Layer
- Implements `NotificationListenerService`.
- Filters packages: `com.android.mms` / Google Messages, `com.whatsapp`, `org.telegram.messenger` (and Business variants if needed).
- Extracts title, text, bigText, and timestamp.
- Ignores group chats or very long messages above a configurable threshold (to save resources).

### 3.2 Rule Engine
- Fast, deterministic pattern matching.
- Categories of rules:
  - Urgency + financial request
  - Impersonation of banks (Kaspi, Halyk, Freedom, etc.)
  - Requests for codes / AnyDesk / TeamViewer
  - Known phishing phrases in Russian and Kazakh
  - Suspicious short links
- Rules are stored as structured data (JSON or Kotlin objects) so they can be updated without code changes.
- Each matched rule produces a human-readable reason.

### 3.3 On-Device Classifier (Optional)
- Small quantized language model or specialized text classifier.
- Invoked only when the rule engine returns “uncertain”.
- Input: cleaned message text + optional metadata (source app, time of day).
- Output: risk score + short explanation.
- Preferred runtimes: MediaPipe LLM Inference, ONNX Runtime, or llama.cpp Android bindings.
- Model is downloaded once (or shipped with the app) and stored in app-private storage.

### 3.4 Alert & History Manager
- Creates high-priority notification when risk is high.
- Stores analysis results in local Room / SQLite database.
- Provides the History screen and “Mark as false positive” action (local only).

### 3.5 Settings & Capability Detection
- Detects device RAM and decides default mode (Rules-only / Rules + Light Model / Full).
- User can override.
- Language preference (Russian / Kazakh).
- Per-app monitoring toggles.

## 4. Data Flow (Happy Path)

1. New notification arrives → NotificationListenerService.
2. Text is extracted and normalized (lowercase, remove extra whitespace, basic cleaning).
3. Rule Engine evaluates →  
   - High confidence scam → create alert + store.  
   - Clearly safe → store as safe (optional) or ignore.  
   - Uncertain → pass to Classifier.
4. Classifier returns score + explanation.
5. Final decision is made → Alert Manager shows notification and writes to local DB.
6. User can open the app to see full history and details.

## 5. Technology Choices (Recommended)

| Layer              | Choice                          | Reason |
|--------------------|----------------------------------|--------|
| Language           | Kotlin                           | Native Android, best performance & permission handling |
| UI                 | Jetpack Compose                  | Modern, fast iteration, good accessibility |
| Local DB           | Room                             | Simple, type-safe, perfect for history |
| Background         | Foreground Service (when needed) + WorkManager for maintenance | Survive battery optimizations |
| Model Runtime      | MediaPipe or ONNX Runtime        | Good Android support, relatively easy quantization |
| Pattern Storage    | JSON assets + optional Room table | Easy to update and open-source |

Alternative: Flutter if the developer prefers faster cross-platform UI, but native Kotlin is preferred for deep NotificationListener and future CallScreening work.

## 6. Security & Privacy Notes

- `NotificationListenerService` requires explicit user enablement in system settings.
- No `INTERNET` permission is required for core functionality (optional only for first model download).
- All message content stays in app-private storage.
- No third-party analytics SDKs that could leak content.
- Open-source the rule set and the decision logic so the community can audit it.

## 7. Extensibility Points (for later stages)

- CallScreeningService can be added later without changing the core text pipeline.
- New messaging apps can be added by simply extending the package filter list.
- New languages = new rule packs + prompt templates.
- Community rule contributions can be imported as versioned JSON.

## 8. Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| NotificationListener killed by aggressive OEMs | Foreground service + battery optimization exemption request + clear user instructions |
| Model too slow on low-end devices | Strict capability detection + pure-rules fallback |
| High false positives | Conservative thresholds + easy “Mark as safe” + continuous local pattern tuning |
| Google Play policy on NotificationListener | Clear privacy policy, open-source code, no data collection |

---

**Architecture owner:** Sayan  
**Last updated:** 2026-07-28