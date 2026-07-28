package kz.qorgau.scamguardian.data.local.db.mapper

import kz.qorgau.scamguardian.domain.model.AnalysisRecord
import kz.qorgau.scamguardian.domain.model.RiskLevel
import kz.qorgau.scamguardian.domain.model.SourceApp
import kz.qorgau.scamguardian.domain.model.UserFeedback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisLogMapperTest {

    @Test
    fun `round-trip domain entity preserves fields`() {
        val original = AnalysisRecord(
            id = 7L,
            sourceApp = SourceApp.WHATSAPP,
            sender = "Kaspi Bank",
            messageText = "Срочно переведите код",
            riskLevel = RiskLevel.HIGH,
            riskScore = 0.91f,
            explanation = "Имитация банка + запрос кода",
            matchedRules = listOf("bank_impersonation", "otp_request"),
            createdAtEpochMs = 1_700_000_000_000L,
            userFeedback = UserFeedback.FALSE_POSITIVE,
            isRead = true,
        )

        val entity = AnalysisLogMapper.toEntity(original)
        val restored = AnalysisLogMapper.toDomain(entity)

        assertEquals(original, restored)
    }

    @Test
    fun `empty matched rules encode as null`() {
        assertNull(AnalysisLogMapper.encodeRuleIds(emptyList()))
        assertTrue(AnalysisLogMapper.decodeRuleIds(null).isEmpty())
        assertTrue(AnalysisLogMapper.decodeRuleIds("[]").isEmpty())
    }

    @Test
    fun `rule ids encode as json array`() {
        val json = AnalysisLogMapper.encodeRuleIds(listOf("a", "b"))
        assertEquals("""["a","b"]""", json)
        assertEquals(listOf("a", "b"), AnalysisLogMapper.decodeRuleIds(json))
    }
}
