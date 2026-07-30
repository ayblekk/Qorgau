package kz.qorgau.scamguardian.data.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RulePackLoaderTest {

    @Test
    fun `loads default pack from assets file`() {
        val json = File("src/main/assets/rules/default_rules_v1.json").readText()
        val pack = RulePackLoader().loadFromString(json)
        assertEquals("1.3.2", pack.version)
        assertTrue(pack.rules.size >= 45)
        assertTrue(pack.rules.any { it.id == "combo_bank_and_code" })
        assertTrue(pack.rules.any { it.id == "survey_bonus_bait" })
        assertTrue(pack.rules.any { it.id == "fake_credit_alert" })
        assertTrue(pack.rules.any { it.id == "mobile_operator_impersonation" })
        assertTrue(pack.rules.any { it.id == "tax_service_impersonation" })
        assertTrue(pack.rules.any { it.id == "courier_delivery_impersonation" })
        assertTrue(pack.rules.any { it.id == "combo_police_and_money" })
        assertTrue(pack.rules.all { it.patterns.isNotEmpty() })
        assertTrue(pack.rules.all { it.severityWeight > 0f })
        assertTrue(pack.rules.all { it.descriptionEn.isNotBlank() })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects empty rules`() {
        RulePackLoader().loadFromString(
            """{"version":"0","rules":[]}""",
        )
    }
}
