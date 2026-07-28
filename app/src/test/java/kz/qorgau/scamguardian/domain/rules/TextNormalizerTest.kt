package kz.qorgau.scamguardian.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun `collapses whitespace and lowercases`() {
        assertEquals(
            "срочно переведите деньги",
            TextNormalizer.normalize("  Срочно   ПЕРЕВЕДИТЕ\nденьги  "),
        )
    }

    @Test
    fun `maps yo to e`() {
        assertEquals("удаленный доступ", TextNormalizer.normalize("Удалённый доступ"))
    }

    @Test
    fun `normalizes mixed kaspi spellings`() {
        val a = TextNormalizer.normalize("Kаspi Gold")
        assertTrue(a.contains("kaspi"))
    }

    @Test
    fun `blank becomes empty`() {
        assertEquals("", TextNormalizer.normalize("   "))
        assertFalse(TextNormalizer.normalize("ok").isEmpty())
    }
}
