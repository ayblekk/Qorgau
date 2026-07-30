package kz.qorgau.scamguardian.domain.rules

import kz.qorgau.scamguardian.domain.model.AppLanguage
import kz.qorgau.scamguardian.domain.model.RiskLevel
import kz.qorgau.scamguardian.domain.model.SourceApp

/**
 * Reduces false positives for official Kaspi transactional SMS when the sender
 * alpha-tag matches known brands and the body has no active scam signals.
 *
 * Pure / side-effect free. SMS-only for Stage 1.
 *
 * Security notes:
 * - Bare "kaspi" / "каспи" are NOT trusted — SMS alpha-tags are spoofable.
 * - Only full official tags (kaspi.kz, KaspiGold, …) can dampen.
 * - Hard scam rules (OTP request, remote access, card data, …) never dampen.
 */
object OfficialSenderPolicy {

    /** Full alpha-tags only — never bare "kaspi" (easy spoof). */
    private val officialKaspiSenders: Set<String> = setOf(
        "kaspi.kz",
        "kaspigold",
        "kaspibank",
        "kaspikredit",
        "kaspishop",
        "kaspi gold",
        "kaspi bank",
        "kaspi kredit",
        "kaspi shop",
    )

    private val officialKaspiCompact: Set<String> = setOf(
        "kaspikz",
        "kaspigold",
        "kaspibank",
        "kaspikredit",
        "kaspishop",
    )

    /**
     * Rule IDs that always mean real scam pressure — never silence them even
     * when the alpha-tag looks official.
     */
    private val neverDampenRuleIds: Set<String> = setOf(
        "otp_code_request",
        "remote_access_tools",
        "card_data_request",
        "police_prosecutor_impersonation",
        "bank_security_impersonation",
        "sim_swap_scare",
        "relative_emergency_money",
        "secret_keep_quiet",
        "tech_support_impersonation",
        "tax_service_impersonation",
        "court_bailiff_impersonation",
        "traffic_police_fine_scam",
        "military_summons_scam",
        "fake_credit_alert",
        "debt_collection_scare",
        "investment_crypto_scam",
        "urgency_finance",
        "account_blocked_threat",
    )

    /**
     * Phrases real bank OTP/ops SMS almost never use to *solicit* action from the user.
     * If any appear, do not dampen.
     */
    private val neverDampenSubstrings: List<String> = listOf(
        "anydesk",
        "teamviewer",
        "rustdesk",
        "ultraviewer",
        "удаленный доступ",
        "удалённый доступ",
        "пришлите код",
        "пришли код",
        "отправьте код",
        "скинь код",
        "скажите код",
        "сообщите код",
        "назовите код",
        "перешлите код",
        "кодты жіберіңіз",
        "кодты айтыңыз",
        "короткий опрос",
        "пройдите опрос",
        "пройти опрос",
        "получите бонус",
        "заполните анкету",
        "на ваше имя",
        "оформить кредит",
        "оформить займ",
        "микрозайм",
        "bit.ly",
        "tinyurl",
        "установите программу",
        "скачайте программу",
        // Active money solicit — real Kaspi SMS does not ask the user to transfer out.
        "срочно перев",
        "переведите",
        "переведи на",
        "аударыңыз",
        "номер карты",
        "данные карты",
        "cvv",
        // Phishing / account takeover common in spoofed "bank" SMS.
        "личный кабинет",
        "подтвердите данные",
        "обновите данные",
        "перейдите по ссылке",
        "перейди по ссылке",
        "нажмите на ссылку",
        "откройте ссылку",
        "подтвердите по ссылке",
        "http://",
        "https://",
        "www.",
        "служба безопасности",
        "ваш аккаунт взломан",
        "подозрительный вход",
    )

    fun normalizeSender(sender: String?): String {
        if (sender.isNullOrBlank()) return ""
        return sender
            .lowercase()
            .replace("ё", "е")
            .replace("™", "")
            .replace("®", "")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    fun isOfficialKaspiSender(sender: String?): Boolean {
        val n = normalizeSender(sender)
        if (n.isEmpty()) return false
        if (n in officialKaspiSenders) return true
        val compact = n.replace(" ", "").replace(".", "")
        return compact in officialKaspiCompact
    }

    fun hasActiveScamOverride(bodyText: String): Boolean {
        val normalized = TextNormalizer.normalize(bodyText)
        if (normalized.isEmpty()) return false
        return neverDampenSubstrings.any { it in normalized }
    }

    /**
     * If SMS from an official Kaspi tag without active scam solicits → force SAFE.
     * Otherwise returns [result] unchanged.
     */
    fun maybeDampen(
        result: RuleEvaluationResult,
        sourceApp: SourceApp,
        sender: String?,
        bodyText: String,
        language: AppLanguage,
    ): RuleEvaluationResult {
        if (result.riskLevel == RiskLevel.SAFE) return result
        if (sourceApp != SourceApp.SMS) return result
        if (!isOfficialKaspiSender(sender)) return result
        // Hard rules always win over alpha-tag trust.
        if (result.matchedRuleIds.any { it in neverDampenRuleIds }) return result
        if (hasActiveScamOverride(bodyText)) return result

        return RuleEvaluationResult(
            riskLevel = RiskLevel.SAFE,
            matchedRuleIds = emptyList(),
            explanation = safeExplanation(language),
            confidence = 0.7f,
            isUncertain = false,
        )
    }

    private fun safeExplanation(language: AppLanguage): String =
        when (language) {
            AppLanguage.RUSSIAN -> "Явных признаков скама не найдено."
            AppLanguage.KAZAKH -> "Айқын алаяқтық белгілері табылмады."
            AppLanguage.ENGLISH -> "No clear scam signs found."
        }
}
