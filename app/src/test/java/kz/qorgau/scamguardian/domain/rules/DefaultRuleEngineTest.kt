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
    fun `screenshot survey bonus bait is suspicious not scam on medium`() {
        val result = engine.evaluate(
            text = "Пройдите короткий опрос и получите бонус",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertEquals(
            "soft survey+bonus bait must be SUSPICIOUS, not Scam/HIGH; ids=${result.matchedRuleIds}",
            RiskLevel.SUSPICIOUS,
            result.riskLevel,
        )
        assertTrue(result.matchedRuleIds.contains("survey_bonus_bait"))
        assertFalse(
            "must not double-count removed combo_survey_bonus",
            result.matchedRuleIds.contains("combo_survey_bonus"),
        )
        assertTrue(result.isUncertain)
    }

    @Test
    fun `survey bonus bait stays suspicious even on high sensitivity`() {
        val result = engine.evaluate(
            text = "Пройдите короткий опрос и получите бонус",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.HIGH,
        )
        assertEquals(
            "even max sensitivity: survey+bonus alone is not Scam; ids=${result.matchedRuleIds}",
            RiskLevel.SUSPICIOUS,
            result.riskLevel,
        )
    }

    @Test
    fun `survey without bonus is safe on medium`() {
        val result = engine.evaluate(
            text = "Пройдите короткий опрос в нашем приложении",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertEquals(
            "survey alone without reward bait should not alert; ids=${result.matchedRuleIds}",
            RiskLevel.SAFE,
            result.riskLevel,
        )
    }

    @Test
    fun `bonus alone without survey is safe on medium`() {
        val result = engine.evaluate(
            text = "Получите бонус в личном кабинете банка",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertEquals(
            "bonus alone without survey bait should not alert; ids=${result.matchedRuleIds}",
            RiskLevel.SAFE,
            result.riskLevel,
        )
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
    fun `brand alone is safe on medium`() {
        val samples = listOf(
            "Kaspi: ваш заказ готов к выдаче",
            "Wolt: заказ доставлен",
            "Beeline: ваш баланс 1500 тг",
            "OLX: новое сообщение по объявлению",
            "Halyk Bank: напоминание о платеже",
        )
        for (text in samples) {
            val result = engine.evaluate(text, AppLanguage.RUSSIAN, Sensitivity.MEDIUM)
            assertEquals(
                "brand-only must be SAFE on medium: $text ids=${result.matchedRuleIds}",
                RiskLevel.SAFE,
                result.riskLevel,
            )
        }
    }

    @Test
    fun `brand alone is at most suspicious on high sensitivity`() {
        val result = engine.evaluate(
            text = "Kaspi: ваш заказ готов к выдаче",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.HIGH,
        )
        assertTrue(
            "brand-only must not be Scam on high sens; got ${result.riskLevel}",
            result.riskLevel != RiskLevel.HIGH,
        )
        assertEquals(RiskLevel.SUSPICIOUS, result.riskLevel)
    }

    @Test
    fun `bare lottery giveaway is safe on medium`() {
        val result = engine.evaluate(
            text = "Розыгрыш приза среди клиентов магазина",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertEquals(
            "bare giveaway without “you won” must be SAFE; ids=${result.matchedRuleIds}",
            RiskLevel.SAFE,
            result.riskLevel,
        )
    }

    @Test
    fun `you won prize bait is suspicious not scam on medium`() {
        val result = engine.evaluate(
            text = "Поздравляем! Вы выиграли приз в размере 100000 тенге",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertEquals(
            "classic you-won bait: SUSPICIOUS max without pay pressure; ids=${result.matchedRuleIds}",
            RiskLevel.SUSPICIOUS,
            result.riskLevel,
        )
    }

    @Test
    fun `in your name alone is safe on medium`() {
        val result = engine.evaluate(
            text = "На ваше имя пришло письмо",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertEquals(
            "bare “in your name” must not alert; ids=${result.matchedRuleIds}",
            RiskLevel.SAFE,
            result.riskLevel,
        )
    }

    @Test
    fun `get a loan marketing is safe on medium`() {
        val result = engine.evaluate(
            text = "Оформить кредит можно онлайн",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertEquals(
            "loan marketing without scare must be SAFE; ids=${result.matchedRuleIds}",
            RiskLevel.SAFE,
            result.riskLevel,
        )
    }

    @Test
    fun `new number alone is safe on medium`() {
        val result = engine.evaluate(
            text = "Новый номер, напиши когда удобно",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertEquals(
            "bare “new number” must be SAFE; ids=${result.matchedRuleIds}",
            RiskLevel.SAFE,
            result.riskLevel,
        )
    }

    @Test
    fun `if this was not you alone is safe on medium`() {
        val result = engine.evaluate(
            text = "Если это не вы — игнорируйте",
            language = AppLanguage.RUSSIAN,
            sensitivity = Sensitivity.MEDIUM,
        )
        assertEquals(
            "bare security notice must be SAFE; ids=${result.matchedRuleIds}",
            RiskLevel.SAFE,
            result.riskLevel,
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
