"""Generate Qorgau 2-page PDF for Sentient Open Source AGI Grant upload."""
from pathlib import Path

from reportlab.lib.colors import HexColor, white
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.lib.utils import ImageReader
from reportlab.pdfgen import canvas

OUT = Path(__file__).resolve().parent / "Qorgau-Sentient-Grant.pdf"
ROOT = Path(__file__).resolve().parents[1]
SCREENS = ROOT / "branding" / "grant-screens"

W, H = A4
guardian = HexColor("#1B3A4B")
teal = HexColor("#2A9D8F")
bg = HexColor("#F8F9FA")
text = HexColor("#1A1A1A")
muted = HexColor("#5C6B73")
border = HexColor("#E2E8F0")
card = white
soft = HexColor("#A8C5C0")
L = 14 * mm
R = W - 14 * mm


def draw_header(c: canvas.Canvas, subtitle: str = "On-device Scam Guardian  ·  Kazakhstan / CIS") -> None:
    c.setFillColor(bg)
    c.rect(0, 0, W, H, fill=1, stroke=0)

    c.setFillColor(guardian)
    c.rect(0, H - 28 * mm, W, 28 * mm, fill=1, stroke=0)
    c.setFillColor(teal)
    c.rect(0, H - 29.5 * mm, W, 1.5 * mm, fill=1, stroke=0)

    logo_candidates = [
        ROOT / "branding" / "logo-rounded-no-outer.png",
        ROOT / "branding" / "logo-preview-256.png",
        ROOT / "branding" / "logo-appicon-512.png",
    ]
    for logo_path in logo_candidates:
        if logo_path.exists():
            c.drawImage(
                str(logo_path),
                14 * mm,
                H - 24 * mm,
                width=14 * mm,
                height=14 * mm,
                mask="auto",
                preserveAspectRatio=True,
                anchor="c",
            )
            break

    c.setFillColor(white)
    c.setFont("Helvetica-Bold", 18)
    c.drawString(32 * mm, H - 14 * mm, "Qorgau")
    c.setFont("Helvetica", 9)
    c.setFillColor(soft)
    c.drawString(32 * mm, H - 19.5 * mm, subtitle)

    c.setFont("Helvetica-Bold", 9)
    c.setFillColor(teal)
    c.drawRightString(W - 14 * mm, H - 12 * mm, "Sentient Open Source AGI Grant")
    c.setFont("Helvetica", 8)
    c.setFillColor(soft)
    c.drawRightString(W - 14 * mm, H - 17 * mm, "RFP #02 · The Scam Guardian")
    c.drawRightString(W - 14 * mm, H - 21.5 * mm, "Ask: $10,000  ·  Solo builder")


def draw_footer(c: canvas.Canvas) -> None:
    c.setFillColor(guardian)
    c.rect(0, 0, W, 16 * mm, fill=1, stroke=0)
    c.setFillColor(teal)
    c.rect(0, 16 * mm, W, 1.2 * mm, fill=1, stroke=0)

    c.setFillColor(white)
    c.setFont("Helvetica-Bold", 8.5)
    c.drawString(L, 9 * mm, "github.com/ayblekk/Qorgau")
    c.setFont("Helvetica", 8)
    c.setFillColor(soft)
    c.drawString(
        L,
        4.5 * mm,
        "Privacy-first · fully on-device · open & auditable · good for the people cloud tools skip",
    )

    c.setFillColor(white)
    c.setFont("Helvetica", 8)
    c.drawRightString(R, 9 * mm, "Builder: Ayblek")
    c.setFillColor(soft)
    c.drawRightString(R, 4.5 * mm, "Grant track · public good · no equity")


def section_title(c: canvas.Canvas, label: str, y_pos: float) -> float:
    c.setFillColor(guardian)
    c.setFont("Helvetica-Bold", 10)
    c.drawString(L, y_pos, label.upper())
    c.setStrokeColor(teal)
    c.setLineWidth(1.2)
    c.line(L, y_pos - 2 * mm, L + 28 * mm, y_pos - 2 * mm)
    return y_pos - 7 * mm


def wrap_paragraph(
    c: canvas.Canvas,
    content: str,
    y_pos: float,
    size: float = 8.5,
    color=text,
    leading: float = 3.7 * mm,
    max_width: float | None = None,
) -> float:
    if max_width is None:
        max_width = R - L
    words = content.split()
    lines: list[str] = []
    cur = ""
    for w in words:
        trial = (cur + " " + w).strip()
        if c.stringWidth(trial, "Helvetica", size) <= max_width:
            cur = trial
        else:
            if cur:
                lines.append(cur)
            cur = w
    if cur:
        lines.append(cur)
    c.setFillColor(color)
    c.setFont("Helvetica", size)
    for line in lines:
        c.drawString(L, y_pos, line)
        y_pos -= leading
    return y_pos


def draw_page1(c: canvas.Canvas) -> None:
    draw_header(c)
    y = H - 38 * mm

    # One-liner
    c.setFillColor(card)
    c.setStrokeColor(border)
    c.setLineWidth(0.8)
    c.roundRect(L, y - 12 * mm, R - L, 14 * mm, 3 * mm, fill=1, stroke=1)
    c.setFillColor(teal)
    c.circle(L + 4 * mm, y - 5 * mm, 1.3 * mm, fill=1, stroke=0)
    c.setFillColor(text)
    c.setFont("Helvetica-Bold", 9)
    c.drawString(L + 8 * mm, y - 3.5 * mm, "One line")
    c.setFont("Helvetica", 8.5)
    c.setFillColor(muted)
    one = (
        "Open, fully on-device Android scam guardian for KZ/CIS — "
        "SMS, WhatsApp, Telegram. Private by design. RU / KK / EN."
    )
    line1, line2 = "", ""
    for w in one.split():
        t = (line1 + " " + w).strip()
        if c.stringWidth(t, "Helvetica", 8.5) <= (R - L - 10 * mm):
            line1 = t
        else:
            line2 = (line2 + " " + w).strip()
    c.drawString(L + 8 * mm, y - 8 * mm, line1)
    if line2:
        c.drawString(L + 8 * mm, y - 11.5 * mm, line2)
    y -= 18 * mm

    y = section_title(c, "Problem & why now", y)
    y = wrap_paragraph(
        c,
        "Messaging fraud in Kazakhstan and the CIS is mass-scale: bank impersonation "
        "(Kaspi, Halyk…), OTP theft, fake police/courier scripts, remote-access scams. "
        "Global tools fail — cloud-first (privacy risk), English-centric, weak offline. "
        "AI made scams cheaper; victims still only have a mid-range Android phone.",
        y,
    )
    y -= 2 * mm
    y = wrap_paragraph(
        c,
        "Qorgau (protection) detects scam texts fully on-device. No INTERNET for analysis. "
        "Message text never leaves the phone. Built for the market English cloud tools skip — "
        "a direct take on Sentient Product Request #02: The Scam Guardian.",
        y,
    )
    y -= 4 * mm

    card_h = 28 * mm
    card_w = (R - L - 4 * mm) / 2
    c.setFillColor(card)
    c.setStrokeColor(border)
    c.roundRect(L, y - card_h, card_w, card_h, 2.5 * mm, fill=1, stroke=1)
    c.roundRect(L + card_w + 4 * mm, y - card_h, card_w, card_h, 2.5 * mm, fill=1, stroke=1)

    c.setFillColor(guardian)
    c.setFont("Helvetica-Bold", 9)
    c.drawString(L + 3 * mm, y - 5 * mm, "WHO IT HELPS")
    c.drawString(L + card_w + 7 * mm, y - 5 * mm, "WHO IS BUILDING")
    c.setStrokeColor(teal)
    c.setLineWidth(1)
    c.line(L + 3 * mm, y - 6.5 * mm, L + 18 * mm, y - 6.5 * mm)
    c.line(L + card_w + 7 * mm, y - 6.5 * mm, L + card_w + 28 * mm, y - 6.5 * mm)

    c.setFillColor(muted)
    c.setFont("Helvetica", 8)
    who_lines = [
        "Everyday Android users in Kazakhstan,",
        "especially elders and less technical people.",
        "RU/KK speakers across CIS. Families who",
        "want protection without cloud spyware.",
        "One panicked SMS away from lost savings.",
    ]
    for i, line in enumerate(who_lines):
        c.drawString(L + 3 * mm, y - 11 * mm - i * 3.4 * mm, line)

    team_lines = [
        "I build this alone — Ayblek (@ayblekk),",
        "from Kazakhstan. Working Stage-1 MVP:",
        "Kotlin/Compose, pure rule engine, local",
        "history, RU/KK/EN UI, auditable JSON",
        "rules for real local scam patterns.",
    ]
    for i, line in enumerate(team_lines):
        c.drawString(L + card_w + 7 * mm, y - 11 * mm - i * 3.4 * mm, line)

    y -= card_h + 5 * mm

    y = section_title(c, "What exists now (Stage 1)", y)
    bullets = [
        "Notification capture: SMS · WhatsApp · Telegram (NotificationListenerService)",
        "On-device rule engine + auditable JSON pack (Kaspi/Halyk, OTP, AnyDesk, urgency+money…)",
        "Local alerts + history · settings · sensitivity · no cloud · no analytics on message content",
        "Stack: Kotlin, Jetpack Compose, Room · minSdk 29 · package kz.qorgau.scamguardian",
    ]
    for b in bullets:
        c.setFillColor(teal)
        c.circle(L + 1.5 * mm, y + 1 * mm, 1 * mm, fill=1, stroke=0)
        c.setFillColor(text)
        c.setFont("Helvetica", 8.2)
        c.drawString(L + 5 * mm, y, b)
        y -= 4 * mm
    y -= 2 * mm

    y = section_title(c, "What's open — if it closed tomorrow", y)
    y = wrap_paragraph(
        c,
        "Open: full source + versioned JSON rule packs at github.com/ayblekk/Qorgau — "
        "anyone can audit, fork, and localize. If it dies: KZ/CIS users lose rare offline, "
        "in-language defense that never phones home. Only revocable cloud scanners remain — "
        "not trustworthy for every private message.",
        y,
    )
    y -= 4 * mm

    y = section_title(c, "What $10k unlocks (~3 months)", y)
    unlock = [
        ("1", "Public release", "License, docs, privacy policy, signed APK, install path users can follow"),
        ("2", "Rule corpus 2×", "More real KZ/CIS patterns + false-positive tuning on everyday messages"),
        ("3", "Low-end QA", "Test on 3–4 GB RAM phones — the hardware most people actually own"),
        ("4", "Contributors", "Demo video + guide so others can fork rules for new locales"),
    ]
    row_h = 8.5 * mm
    for num, title, desc in unlock:
        c.setFillColor(guardian)
        c.roundRect(L, y - 5.5 * mm, 6 * mm, 6 * mm, 1.2 * mm, fill=1, stroke=0)
        c.setFillColor(white)
        c.setFont("Helvetica-Bold", 8)
        c.drawCentredString(L + 3 * mm, y - 3.6 * mm, num)
        c.setFillColor(text)
        c.setFont("Helvetica-Bold", 8.5)
        c.drawString(L + 9 * mm, y - 2 * mm, title)
        c.setFillColor(muted)
        c.setFont("Helvetica", 8)
        c.drawString(L + 9 * mm, y - 5.5 * mm, desc)
        y -= row_h

    draw_footer(c)


def draw_page2(c: canvas.Canvas) -> None:
    draw_header(c, subtitle="Working product screens  ·  real device")
    y = H - 38 * mm

    y = section_title(c, "Product screenshots — Stage 1 MVP", y)
    y = wrap_paragraph(
        c,
        "Live Android UI. Protection runs only on-device (badge: device-only). "
        "Per-app monitoring for SMS / WhatsApp / Telegram, RU·KK·EN language, "
        "sensitivity control, local analysis history — no cloud.",
        y,
        size=8.5,
    )
    y -= 5 * mm

    shots = [
        (SCREENS / "01-settings.jpg", "Settings", "On-device protection · SMS / WA / TG toggles"),
        (SCREENS / "02-analysis.jpg", "Analysis", "Language RU / KK / EN · sensitivity"),
        (SCREENS / "03-history.jpg", "History", "Local log · Safe / risk badges"),
    ]
    missing = [p for p, _, _ in shots if not p.exists()]
    if missing:
        raise FileNotFoundError(f"Missing screenshots: {missing}")

    gap = 4 * mm
    col_w = (R - L - 2 * gap) / 3
    # Phone aspect ~9:19.5 → tall
    img_w = col_w - 3 * mm
    img_h = img_w * (19.5 / 9)
    # Cap height so footer fits
    max_h = y - 28 * mm
    if img_h > max_h:
        img_h = max_h
        img_w = img_h * (9 / 19.5)

    for i, (path, title, caption) in enumerate(shots):
        x = L + i * (col_w + gap)
        # Card background
        card_top = y + 2 * mm
        card_h = img_h + 18 * mm
        c.setFillColor(card)
        c.setStrokeColor(border)
        c.setLineWidth(0.7)
        c.roundRect(x, card_top - card_h, col_w, card_h, 3 * mm, fill=1, stroke=1)

        # Title
        c.setFillColor(guardian)
        c.setFont("Helvetica-Bold", 9)
        c.drawCentredString(x + col_w / 2, card_top - 5 * mm, title)

        # Image (phone screenshot)
        img_x = x + (col_w - img_w) / 2
        img_y = card_top - 7 * mm - img_h
        # subtle phone frame
        c.setStrokeColor(guardian)
        c.setLineWidth(1.5)
        c.roundRect(img_x - 1.2 * mm, img_y - 1.2 * mm, img_w + 2.4 * mm, img_h + 2.4 * mm, 2 * mm, fill=0, stroke=1)
        c.drawImage(
            str(path),
            img_x,
            img_y,
            width=img_w,
            height=img_h,
            preserveAspectRatio=True,
            anchor="c",
            mask="auto",
        )

        # Caption under image
        c.setFillColor(muted)
        c.setFont("Helvetica", 7)
        # wrap caption
        words = caption.split()
        lines: list[str] = []
        cur = ""
        max_cw = col_w - 4 * mm
        for w in words:
            trial = (cur + " " + w).strip()
            if c.stringWidth(trial, "Helvetica", 7) <= max_cw:
                cur = trial
            else:
                if cur:
                    lines.append(cur)
                cur = w
        if cur:
            lines.append(cur)
        cy = img_y - 4 * mm
        for line in lines[:2]:
            c.drawCentredString(x + col_w / 2, cy, line)
            cy -= 3 * mm

    # Note strip
    note_y = 22 * mm
    c.setFillColor(card)
    c.setStrokeColor(border)
    c.roundRect(L, note_y, R - L, 12 * mm, 2.5 * mm, fill=1, stroke=1)
    c.setFillColor(teal)
    c.circle(L + 4 * mm, note_y + 6 * mm, 1.3 * mm, fill=1, stroke=0)
    c.setFillColor(text)
    c.setFont("Helvetica", 8)
    c.drawString(
        L + 8 * mm,
        note_y + 7 * mm,
        "Privacy badge on every screen: analysis never leaves the device.",
    )
    c.setFillColor(muted)
    c.setFont("Helvetica", 7.5)
    c.drawString(
        L + 8 * mm,
        note_y + 3 * mm,
        "Repo + source: github.com/ayblekk/Qorgau  ·  Stage 1 rules engine, no cloud runtime",
    )

    draw_footer(c)


def main() -> None:
    c = canvas.Canvas(str(OUT), pagesize=A4)
    c.setTitle("Qorgau — Sentient Open Source AGI Grant")
    c.setAuthor("Ayblek")
    c.setSubject("On-device scam guardian for Kazakhstan/CIS")

    draw_page1(c)
    c.showPage()
    draw_page2(c)
    c.save()
    print(f"Wrote {OUT} ({OUT.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
