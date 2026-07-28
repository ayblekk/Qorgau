package kz.qorgau.scamguardian.domain.capability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCapabilityDetectorTest {

    @Test
    fun `low ram is rules only`() {
        val cap = DeviceCapabilityDetector.fromTotalRamMb(2_048)
        assertEquals(DeviceCapabilityMode.RULES_ONLY, cap.mode)
        assertTrue(cap.recommendRulesOnly)
        assertFalse(cap.recommendModelEnabled)
    }

    @Test
    fun `mid ram is light`() {
        val cap = DeviceCapabilityDetector.fromTotalRamMb(4_096)
        assertEquals(DeviceCapabilityMode.RULES_PLUS_LIGHT, cap.mode)
        assertFalse(cap.recommendRulesOnly)
        assertTrue(cap.recommendModelEnabled)
    }

    @Test
    fun `high ram is full`() {
        val cap = DeviceCapabilityDetector.fromTotalRamMb(8_192)
        assertEquals(DeviceCapabilityMode.FULL, cap.mode)
        assertTrue(cap.recommendModelEnabled)
    }

    @Test
    fun `boundary under 3500 is rules only`() {
        assertEquals(
            DeviceCapabilityMode.RULES_ONLY,
            DeviceCapabilityDetector.fromTotalRamMb(3_499).mode,
        )
        assertEquals(
            DeviceCapabilityMode.RULES_PLUS_LIGHT,
            DeviceCapabilityDetector.fromTotalRamMb(3_500).mode,
        )
    }
}
