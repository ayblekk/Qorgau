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
        assertEquals("1.0.0", pack.version)
        assertTrue(pack.rules.size >= 15)
        assertTrue(pack.rules.any { it.id == "combo_bank_and_code" })
        assertTrue(pack.rules.all { it.patterns.isNotEmpty() })
        assertTrue(pack.rules.all { it.severityWeight > 0f })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects empty rules`() {
        RulePackLoader().loadFromString(
            """{"version":"0","rules":[]}""",
        )
    }
}
