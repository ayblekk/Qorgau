# Product Requirements Document (PRD)
**Project:** ScamGuardian (Stage 1)  
**Version:** 1.0  
**Date:** 2026-07-28  
**Status:** Draft for MVP  

## 1. Overview

ScamGuardian is a privacy-first, fully on-device Android application that protects users in Kazakhstan and the broader CIS region from text-based scams arriving via SMS, WhatsApp, and Telegram.

Stage 1 focuses exclusively on **text analysis**. The app reads incoming message notifications, applies a combination of local rule-based pattern matching and a small on-device language model, and alerts the user when a message looks like a scam — with a clear explanation in Russian or Kazakh.

No data ever leaves the device.

## 2. Problem Statement

- Phone and messaging scams are extremely widespread in Kazakhstan (nearly every second user encounters phone fraud; high volume of Kaspi, bank, and police impersonation schemes).
- Existing global anti-scam tools are either cloud-based (privacy risk), English-centric, or weak on local Russian/Kazakh scam patterns.
- Users need protection that works offline, on mid-range and even low-end Android phones, without selling their data.

## 3. Goals

### Primary Goal
Deliver a reliable, privacy-respecting text scam detector tailored to Kazakhstan that can be demonstrated as a working open-source public good.

### Success Metrics (MVP)
- Correctly flags ≥ 85% of common local scam patterns on a curated test set of 200+ real examples.
- False positive rate < 8% on normal everyday messages.
- Average analysis time < 1.5 seconds on a mid-range Android device (4–6 GB RAM).
- Works completely offline after initial model download.
- Clear, non-alarmist explanations in Russian and Kazakh.

## 4. Target Users

- Ordinary Android users in Kazakhstan (primary).
- Russian-speaking users across CIS.
- Especially vulnerable groups: older adults, people less familiar with digital threats.
- Open-source contributors and researchers interested in regional AI safety.

## 5. MVP Scope (Stage 1)

### In Scope
- NotificationListenerService for SMS, WhatsApp, and Telegram.
- Local rule engine with Kazakhstan/CIS-specific scam patterns.
- Optional small on-device text classifier / LLM (1–3B quantized).
- Manual paste analysis.
- Bilingual UI and explanations (Russian + Kazakh).
- Local history of analyzed messages (on-device only).
- Simple settings: language, sensitivity, enable/disable per app.
- Completely offline operation after first setup.
- Open-source release (core logic + model adapters).

### Explicitly Out of Scope (Stage 1)
- Live call audio interception or real-time voice analysis.
- iOS version.
- Cloud backend or account system.
- Automatic blocking of numbers (only alerts).
- Deepfake voice detection.
- Browser/extension version.

## 6. Core User Flows

1. **Automatic protection**  
   User grants Notification Access → app silently analyzes new SMS / WhatsApp / Telegram notifications → shows high-priority alert if scam detected.

2. **Manual check**  
   User pastes suspicious text → taps “Check” → receives verdict + explanation.

3. **Review history**  
   User opens History tab → sees past alerts with timestamps and explanations.

## 7. Functional Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| F1 | Capture text from SMS, WhatsApp, Telegram via NotificationListener | Must |
| F2 | Run local rule-based pattern matching | Must |
| F3 | Run optional small on-device model for ambiguous cases | Should |
| F4 | Display clear verdict + human-readable explanation (RU/KK) | Must |
| F5 | Store analysis history only on device | Must |
| F6 | Support Russian and Kazakh interface + explanations | Must |
| F7 | Work without internet after model download | Must |
| F8 | Allow user to enable/disable monitoring per messaging app | Should |
| F9 | Provide “Why is this a scam?” expandable details | Must |

## 8. Non-Functional Requirements

- **Privacy:** Zero network calls for analysis. No analytics, no crash reporting that sends content.
- **Performance:** Analysis must remain usable on devices with 3–4 GB RAM.
- **Battery:** Background monitoring should be lightweight (rules first, model only when needed).
- **Accessibility:** Large text option, high contrast, screen-reader friendly.
- **Openness:** Core detection logic and pattern lists must be open source and auditable.

## 9. Technical Constraints

- Android 10+ (API 29) minimum because of CallScreening and modern NotificationListener behavior (even if Stage 1 does not yet use CallScreening).
- Prefer Kotlin + Jetpack Compose or Flutter for faster UI iteration.
- Model size target: preferably under 1.5–2 GB on disk for the heaviest option; provide a pure-rules fallback.

## 10. Future Stages (Out of current scope)

- Stage 2: Call Screening + basic number reputation.
- Stage 3: Optional on-device STT + voice analysis.
- Multi-language expansion beyond RU/KK.
- Public pattern contribution system.

## 11. Open Questions

- Preferred UI framework (native Kotlin/Compose vs Flutter)?
- Exact small model to ship first (Gemma-2B, Phi-3.5-mini, Qwen2.5-1.5B, or pure TFLite classifier)?
- How aggressively should we collect and anonymize local scam examples for improving the rule set? (Must stay privacy-safe.)

---

**Document owner:** Sayan  
**Last updated:** 2026-07-28