# Design System
**Project:** ScamGuardian (Stage 1)  
**Version:** 1.0  
**Date:** 2026-07-28  

## 1. Design Philosophy

ScamGuardian must feel **calm, trustworthy, and protective** — never aggressive or panic-inducing.  
The visual language should communicate “quiet guardian” rather than “alarm system”.

Core principles:
- Clarity over decoration
- High legibility (many users are older)
- Instant understanding of risk level
- Strong privacy signal (local-only badges)
- Bilingual by default (Russian primary, Kazakh secondary)

## 2. Brand & Tone

- **Name:** Qorgau (product name; internal codename was ScamGuardian)  
- **Tagline options:**  
  - “Защита, которая остаётся с тобой”  
  - “Скам не пройдёт. Данные тоже.”  
  - “Локальная защита от мошенников”  
  - “Қорғау — сенімен бірге”
- **Personality:** Calm, competent, respectful, slightly serious but not cold.
- **Voice in UI copy:** Direct, plain language, no jargon, no fear-mongering.

## 3. Color Palette

### Primary
| Name | Hex | Usage |
|------|-----|-------|
| Guardian Blue | `#1B3A4B` | Primary actions, headers, trust |
| Soft Teal | `#2A9D8F` | Success / safe state, accents |
| Alert Coral | `#E76F51` | High-risk scam alerts |
| Warning Amber | `#F4A261` | Suspicious / medium risk |
| Safe Green | `#2A9D8F` | Confirmed safe |

### Neutrals
| Name | Hex | Usage |
|------|-----|-------|
| Background | `#F8F9FA` | Main background (light mode) |
| Surface | `#FFFFFF` | Cards, sheets |
| Text Primary | `#1A1A1A` | Body text |
| Text Secondary | `#5C6B73` | Secondary text, hints |
| Border | `#E2E8F0` | Dividers, card borders |

### Dark Mode (required)
- Background: `#0F172A`
- Surface: `#1E293B`
- Text Primary: `#F1F5F9`
- Keep risk colors (Coral / Amber / Teal) almost unchanged for recognition.

## 4. Typography

- **Primary font:** Inter or SF Pro (system) for excellent Cyrillic + Kazakh support.
- **Fallback:** Roboto / system sans-serif.

| Style | Size / Weight | Usage |
|-------|---------------|-------|
| Display | 28–32 / SemiBold | Screen titles |
| Title | 20–22 / SemiBold | Section headers |
| Body | 16–17 / Regular | Main content |
| Caption | 13–14 / Regular | Timestamps, secondary info |
| Button | 16 / Medium | All buttons |

Minimum body text size: 16 sp.  
Support dynamic type / system font scaling.

## 5. Spacing & Layout

- Base unit: 4 dp
- Standard padding: 16 dp
- Card internal padding: 16–20 dp
- Section gaps: 24 dp
- Bottom navigation / floating action safe area respected

Use generous whitespace. Avoid dense screens.

## 6. Core Components

### Risk Badge
- **High risk (Scam):** Coral background, white text, icon “warning”
- **Suspicious:** Amber background, dark text
- **Safe:** Teal/green subtle background

### Alert Card
- Clear hierarchy: Risk level → Short verdict → “Why?” expandable section → Timestamp + source app icon
- One primary action: “Got it” / “Mark as safe” / “Report false positive” (local only)

### History List Item
- App icon + sender/title
- One-line preview of message
- Risk badge on the right
- Time ago

### Empty States
- Friendly illustration or simple icon
- Short helpful text (“Пока всё чисто” / “Пока ешқандай күдікті хабарлама жоқ”)

### Settings Toggles
- Large touch targets
- Clear labels + short descriptions under each toggle

## 7. Iconography

- Prefer outlined icons (Material Symbols or Lucide style)
- Consistent 24 dp size
- Risk icons must be instantly recognizable even in monochrome

## 8. Motion & Feedback

- Subtle, fast transitions (200–300 ms)
- Alert appearance: gentle slide + fade, not aggressive bounce
- Haptic feedback on high-risk detection (light impact)
- No continuous animations that drain battery

## 9. Accessibility

- Minimum contrast ratio 4.5:1 (text)
- Support TalkBack / screen readers with meaningful labels
- Large text mode must not break layout
- Color is never the only indicator of risk (always combine with icon + text)

## 10. Localization

- All UI strings externalized
- Russian as primary development language (default `values/`)
- Kazakh (`values-kk/`) and English (`values-en/`) translations required for MVP
- Rule-based explanations and templates must support RU / KK / EN cleanly

## 11. Privacy Visual Language

Always show a small persistent indicator:
- “Только на устройстве” / “Тек құрылғыда”
- Lock or shield icon next to analysis results

This reinforces the core promise of the product.

---

**Design owner:** Sayan  
**Last updated:** 2026-07-28