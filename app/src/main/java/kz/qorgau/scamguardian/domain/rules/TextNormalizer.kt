package kz.qorgau.scamguardian.domain.rules

/**
 * Lightweight text cleaning before rule matching (ARCHITECTURE.md §4).
 * Pure function — no I/O, safe for any thread.
 */
object TextNormalizer {

    private val multiWhitespace = Regex("\\s+")
    private val zeroWidth = Regex("[\\u200B-\\u200D\\uFEFF]")

    /**
     * Lowercases, strips zero-width chars, collapses whitespace,
     * normalizes a few common phishing lookalikes.
     */
    fun normalize(raw: String): String {
        if (raw.isBlank()) return ""
        var text = raw.lowercase()
        text = zeroWidth.replace(text, "")
        text = text.replace('ё', 'е')
        // Latin/Cyrillic mix often used in brand names (kаspi, кaspi).
        text = text
            .replace("kаspi", "kaspi")
            .replace("кaspi", "kaspi")
            .replace("cаspi", "kaspi")
            .replace("каspi", "kaspi")
        text = multiWhitespace.replace(text, " ").trim()
        return text
    }
}
