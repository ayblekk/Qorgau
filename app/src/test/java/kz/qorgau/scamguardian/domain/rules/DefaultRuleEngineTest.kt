package kz.qorgau.scamguardian.domain.rules

import kz.qorgau.scamguardian.data.rules.RulePackLoader
import kz.qorgau.scamguardian.domain.model.AppLanguage
import kz.qorgau.scamguardian.domain.model.RiskLevel
import kz.qorgau.scamguardian.domain.model.Sensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class DefaultRuleEngineTest {

    private lateinit var engine: DefaultRuleEngine

    @Before
    fun setUp() {
        val json = readDefaultRulesJson()
        val pack = RulePackLoader().loadFromString(json)
        engine = DefaultRuleEngine(pack)
        assertTrue("rule pack should not be empty", engine.ruleCount > 10)
    }

    @Test
    fun `classic kaspi otp scam is high risk`() {
        val result = engine.evaluate(
            text = "Kaspi Bank: ваш счёт будет заблокирован. Срочно пришлите код из СМС.",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertEquals(RiskLevel.HIGH, result.riskLevel)
        assertTrue(result.matchedRuleIds.isNotEmpty())
        assertTrue(result.explanation.isNotBlank())
        assertFalse(result.isUncertain)
    }

    @Test
    fun `anydesk plus bank is high risk`() {
        val result = engine.evaluate(
            text = "Сотрудник банка: установите AnyDesk для отмены подозрительной операции по карте",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertEquals(RiskLevel.HIGH, result.riskLevel)
        assertTrue(
            result.matchedRuleIds.any {
                it.contains("remote") || it.contains("combo") || it.contains("bank")
            },
        )
    }

    @Test
    fun `kazakh urgency transfer is flagged`() {
        val result = engine.evaluate(
            text = "Шұғыл! Картаңыз бұғатталады. Дереу аударыңыз және растау кодын жіберіңіз.",
            language = AppLanguage.KAZAKH,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertTrue(
            "expected high or suspicious, got ${result.riskLevel} ids=${result.matchedRuleIds}",
            result.riskLevel == RiskLevel.HIGH || result.riskLevel == RiskLevel.SUSPICIOUS,
        )
        assertTrue(result.explanation.isNotBlank())
    }

    @Test
    fun `normal everyday message is safe`() {
        val result = engine.evaluate(
            text = "Привет! Давай встретимся завтра в 18:00 у метро. Купи хлеб пожалуйста.",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertTrue(result.matchedRuleIds.isEmpty())
    }

    @Test
    fun `empty text is safe`() {
        val result = engine.evaluate(
            text = "   ",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.HIGH,
        )
        assertEquals(RiskLevel.SAFE, result.riskLevel)
    }

    @Test
    fun `high sensitivity catches weaker signals`() {
        val weak = "Перейдите по ссылке чтобы подтвердить данные: https://bit.ly/abc123"
        val low = engine.evaluate(weak, AppLanguage.RUSSIAN, Sensitivity.LOW)
        val high = engine.evaluate(weak, AppLanguage.RUSSIAN, Sensitivity.HIGH)
        // High sensitivity should be at least as strict (higher or equal risk ordinal).
        assertTrue(
            "high=$high low=$low",
            high.riskLevel.ordinal <= low.riskLevel.ordinal ||
                high.matchedRuleIds.size >= low.matchedRuleIds.size,
        )
        assertTrue(high.matchedRuleIds.isNotEmpty() || high.riskLevel != RiskLevel.SAFE)
    }

    @Test
    fun `police impersonation with secrecy is suspicious or high`() {
        val result = engine.evaluate(
            text = "Сотрудник полиции. Возбуждено уголовное дело. Никому не говорите, переведите на карту для проверки.",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertTrue(
            result.riskLevel == RiskLevel.HIGH || result.riskLevel == RiskLevel.SUSPICIOUS,
        )
    }

    @Test
    fun `explanation uses kazakh when language is kk`() {
        val result = engine.evaluate(
            text = "Kaspi: кодты жіберіңіз, әйтпесе шот бұғатталады",
            language = AppLanguage.KAZAKH,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertTrue(result.riskLevel != RiskLevel.SAFE)
        // Header or body should contain Kazakh characters / phrases from templates.
        assertTrue(
            result.explanation.contains("алаяқтық") ||
                result.explanation.contains("Күдікті") ||
                result.explanation.contains("күдікті") ||
                result.explanation.contains("код"),
        )
    }

    @Test
    fun `explanation uses english when language is en`() {
        val result = engine.evaluate(
            text = "Kaspi Bank: your account will be blocked. Send the SMS code now.",
            language = AppLanguage.ENGLISH,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertTrue(result.riskLevel != RiskLevel.SAFE)
        assertTrue(
            result.explanation.contains("scam", ignoreCase = true) ||
                result.explanation.contains("suspicious", ignoreCase = true) ||
                result.explanation.contains("code", ignoreCase = true) ||
                result.explanation.contains("Kaspi", ignoreCase = true),
        )
    }

    @Test
    fun `screenshot survey bonus bait is not safe on medium`() {
        val result = engine.evaluate(
            text = "Пройдите короткий опрос и получите бонус",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertTrue(
            "expected not SAFE, got ${result.riskLevel} ids=${result.matchedRuleIds}",
            result.riskLevel != RiskLevel.SAFE,
        )
        assertTrue(result.matchedRuleIds.isNotEmpty())
        assertFalse(result.explanation.contains("No clear scam", ignoreCase = true))
        assertFalse(result.explanation.contains("Явных признаков", ignoreCase = true))
    }

    @Test
    fun `screenshot fake credit alert is high or suspicious on medium`() {
        val result = engine.evaluate(
            text = "На ваше имя пытаются оформить кредит",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertTrue(
            "expected HIGH or SUSPICIOUS, got ${result.riskLevel} ids=${result.matchedRuleIds}",
            result.riskLevel == RiskLevel.HIGH || result.riskLevel == RiskLevel.SUSPICIOUS,
        )
        assertTrue(
            result.matchedRuleIds.any {
                it.contains("credit") || it.contains("combo_credit")
            },
        )
    }

    @Test
    fun `high sensitivity flags weak click bait alone`() {
        val text = "Подробнее по ссылке и обновите данные"
        val low = engine.evaluate(text, AppLanguage.RUSSIAN, Sensitivity.LOW)
        val high = engine.evaluate(text, AppLanguage.RUSSIAN, Sensitivity.HIGH)
        assertEquals(RiskLevel.SAFE, low.riskLevel)
        assertTrue(
            "HIGH should flag weak bait, got ${high.riskLevel} ids=${high.matchedRuleIds}",
            high.riskLevel != RiskLevel.SAFE,
        )
    }

    @Test
    fun `sender brand spoof in evaluation text is matched`() {
        val result = engine.evaluate(
            text = "Kaspi\nСрочно пришлите код из СМС",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertEquals(RiskLevel.HIGH, result.riskLevel)
    }

    @Test
    fun `mobile operator code request is high risk`() {
        val result = engine.evaluate(
            text = "Beeline: ваш номер будет заблокирован. Пришлите код из СМС.",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertEquals(RiskLevel.HIGH, result.riskLevel)
        assertTrue(
            result.matchedRuleIds.any {
                it.contains("operator") || it.contains("otp") || it.contains("mobile")
            },
        )
    }

    @Test
    fun `tax service payment demand is flagged`() {
        val result = engine.evaluate(
            text = "Налоговая служба КГД: задолженность по налогам. Оплатите штраф по ссылке.",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertTrue(
            "expected HIGH or SUSPICIOUS, got ${result.riskLevel} ids=${result.matchedRuleIds}",
            result.riskLevel == RiskLevel.HIGH || result.riskLevel == RiskLevel.SUSPICIOUS,
        )
        assertTrue(result.matchedRuleIds.any { it.contains("tax") || it.contains("combo_tax") })
    }

    @Test
    fun `courier asks for payment is flagged`() {
        val result = engine.evaluate(
            text = "Я ваш курьер Wolt у подъезда. Переведите оплату за доставку на карту.",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertTrue(
            "expected HIGH or SUSPICIOUS, got ${result.riskLevel} ids=${result.matchedRuleIds}",
            result.riskLevel == RiskLevel.HIGH || result.riskLevel == RiskLevel.SUSPICIOUS,
        )
        assertTrue(
            result.matchedRuleIds.any {
                it.contains("courier") || it.contains("combo_courier")
            },
        )
    }

    @Test
    fun `police money pressure is high risk`() {
        val result = engine.evaluate(
            text = "Сотрудник МВД. Уголовное дело. Никому не говорите, переведите на карту для проверки.",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertEquals(RiskLevel.HIGH, result.riskLevel)
        assertTrue(
            result.matchedRuleIds.any {
                it.contains("police") || it.contains("combo_police") || it.contains("secret")
            },
        )
    }

    private fun readDefaultRulesJson(): String {
        val candidates = listOf(
            File("src/main/assets/rules/default_rules_v1.json"),
            File("app/src/main/assets/rules/default_rules_v1.json"),
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("default_rules_v1.json not found. cwd=${File(".").absolutePath}")
        return file.readText(Charsets.UTF_8)
    }
}
