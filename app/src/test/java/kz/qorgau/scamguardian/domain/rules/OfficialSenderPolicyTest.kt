package kz.qorgau.scamguardian.domain.rules

import kz.qorgau.scamguardian.domain.model.AppLanguage
import kz.qorgau.scamguardian.domain.model.RiskLevel
import kz.qorgau.scamguardian.domain.model.SourceApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialSenderPolicyTest {

    @Test
    fun `recognizes official kaspi alpha tags`() {
        assertTrue(OfficialSenderPolicy.isOfficialKaspiSender("kaspi.kz"))
        assertTrue(OfficialSenderPolicy.isOfficialKaspiSender("KaspiGold"))
        assertTrue(OfficialSenderPolicy.isOfficialKaspiSender("KaspiBank"))
        assertTrue(OfficialSenderPolicy.isOfficialKaspiSender("KaspiKredit"))
        assertTrue(OfficialSenderPolicy.isOfficialKaspiSender("KaspiShop"))
        assertTrue(OfficialSenderPolicy.isOfficialKaspiSender("Kaspi Gold"))
        assertFalse(OfficialSenderPolicy.isOfficialKaspiSender("Keruen"))
        assertFalse(OfficialSenderPolicy.isOfficialKaspiSender(null))
    }

    @Test
    fun `dampens official otp style sms`() {
        val raw = RuleEvaluationResult(
            riskLevel = RiskLevel.HIGH,
            matchedRuleIds = listOf("bank_kaspi_impersonation", "combo_bank_and_code"),
            explanation = "scam",
            confidence = 0.9f,
            isUncertain = false,
        )
        val out = OfficialSenderPolicy.maybeDampen(
            result = raw,
            sourceApp = SourceApp.SMS,
            sender = "kaspi.kz",
            bodyText = "Kod: 482915. Nikomu ne soobshchayte.",
            language = AppLanguage.ENGLISH,
        )
        assertEquals(RiskLevel.SAFE, out.riskLevel)
        assertTrue(out.matchedRuleIds.isEmpty())
    }

    @Test
    fun `does not dampen when body asks user to send code`() {
        val raw = RuleEvaluationResult(
            riskLevel = RiskLevel.HIGH,
            matchedRuleIds = listOf("otp_code_request"),
            explanation = "scam",
            confidence = 0.9f,
            isUncertain = false,
        )
        val out = OfficialSenderPolicy.maybeDampen(
            result = raw,
            sourceApp = SourceApp.SMS,
            sender = "kaspi.kz",
            bodyText = "Срочно пришлите код из СМС для отмены операции",
            language = AppLanguage.RUSSIAN,
        )
        assertEquals(RiskLevel.HIGH, out.riskLevel)
        assertEquals(listOf("otp_code_request"), out.matchedRuleIds)
    }

    @Test
    fun `does not dampen non sms sources`() {
        val raw = RuleEvaluationResult(
            riskLevel = RiskLevel.SUSPICIOUS,
            matchedRuleIds = listOf("bank_kaspi_impersonation"),
            explanation = "x",
            confidence = 0.5f,
            isUncertain = true,
        )
        val out = OfficialSenderPolicy.maybeDampen(
            result = raw,
            sourceApp = SourceApp.WHATSAPP,
            sender = "kaspi.kz",
            bodyText = "Kod 123456",
            language = AppLanguage.RUSSIAN,
        )
        assertEquals(RiskLevel.SUSPICIOUS, out.riskLevel)
    }
}
