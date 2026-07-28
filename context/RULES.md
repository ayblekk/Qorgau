# Coding & Collaboration Rules
**Project:** ScamGuardian (Stage 1)  
**Version:** 1.1  
**Date:** 2026-07-28  

## 1. Core Principles

1. **Privacy is non-negotiable**  
   Any code that could send message content off-device is forbidden unless it is an explicit, user-triggered, and clearly labelled action (there should be none in Stage 1).

2. **KISS for new features**  
   Prefer the simplest solution that works correctly. Complexity is added only when measurements or real user needs prove it is required.

3. **DRY — but only after the third or fourth repetition**  
   Do not abstract too early. Copy-paste is acceptable for the first 2–3 similar blocks. Extract when the pattern is stable.

4. **SOLID where it helps readability**  
   Especially Single Responsibility for the Rule Engine and Notification processing.

5. **Open and auditable**  
   Detection logic and rule definitions must remain readable by a security-conscious outsider.

## 2. Language & Style

- **Primary language:** Kotlin
- Follow official Kotlin coding conventions.
- Prefer `val` over `var`.
- Use data classes for simple domain types.
- Explicit visibility modifiers.
- Meaningful names (no `tmp`, `data2`, `process()`).

### Naming
- Classes: `PascalCase`
- Functions & variables: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Resource IDs: `snake_case`

## 3. Architecture Rules

- **Layering**  
  UI → ViewModel / UseCase → Domain (RuleEngine) → Data (Room, File storage).  
  Do not let UI talk directly to Room.

- **Dependency direction**  
  High-level modules must not depend on low-level details. Inject rule sources.

- **No God classes**  
  NotificationListenerService should be thin — it extracts data and hands it to a dedicated analyzer.

## 4. Rule Engine Specific Rules

- Rules are data, not hard-coded `if` statements scattered across the codebase.
- Every rule must have:
  - Unique stable ID
  - Human-readable description (used in explanations)
  - Language tags (ru / kk / both)
  - Severity weight
- Rule evaluation must be pure (no side effects).
- Keep the rule evaluation path extremely fast (target < 20–30 ms).

## 5. Testing Expectations

- Unit tests for Rule Engine (high coverage on pattern matching).
- Unit tests for text cleaning / normalization.
- Instrumentation tests for NotificationListener extraction (where practical).
- Manual test set of at least 50 real local scam examples + 50 clean messages before any release.

## 6. Git & Commit Rules

- Meaningful commit messages (what + why).
- Small, focused commits.
- `main` / `master` must always build and pass basic checks.

## 7. Documentation Rules

- Public functions that implement detection logic need KDoc.
- Complex decisions (why a certain threshold was chosen, why a rule exists) should be commented.
- Keep the five context documents (`PRD`, `DESIGN`, `ARCHITECTURE`, `SCHEMA`, `RULES`) up to date when scope changes.

## 8. Performance & Battery Rules

- Detection path is rules-only; keep it cheap.
- Cache compiled patterns.
- Respect Doze and App Standby. Use appropriate foreground service type only when necessary and explain it to the user.
- Measure on a real mid-range device (not only emulators or flagships).

## 9. Security Checklist (before every release)

- [ ] No `INTERNET` permission required for core path (or clearly optional).
- [ ] Message content never written to logs in release builds.
- [ ] No third-party SDK that can read notifications or clipboard.
- [ ] Privacy policy and open-source license are present.

## 10. What We Explicitly Avoid in Stage 1

- Account systems or cloud sync of messages.
- Automatic call blocking or number blacklisting (comes later).
- On-device ML / LLM runtimes or model downloads.
- Dark patterns or aggressive permission requests.

---

**These rules exist to keep the project simple, trustworthy, and shippable.**  
When in doubt, choose the option that is easier to audit and harder to accidentally leak user data.

**Rules owner:** Sayan  
**Last updated:** 2026-07-28
