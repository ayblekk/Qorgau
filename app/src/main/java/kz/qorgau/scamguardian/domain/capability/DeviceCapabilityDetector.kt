package kz.qorgau.scamguardian.domain.capability

/**
 * Pure classification of RAM → capability mode (unit-testable).
 * Thresholds follow PRD mid-range guidance (3–4 GB = keep light).
 */
object DeviceCapabilityDetector {

    /** Below this → rules-only default. */
    const val RULES_ONLY_MAX_RAM_MB: Long = 3_500L

    /** Below this (and above rules-only) → light model OK. */
    const val LIGHT_MAX_RAM_MB: Long = 6_000L

    fun fromTotalRamMb(totalRamMb: Long): DeviceCapability {
        val safeRam = totalRamMb.coerceAtLeast(0L)
        val mode = when {
            safeRam < RULES_ONLY_MAX_RAM_MB -> DeviceCapabilityMode.RULES_ONLY
            safeRam < LIGHT_MAX_RAM_MB -> DeviceCapabilityMode.RULES_PLUS_LIGHT
            else -> DeviceCapabilityMode.FULL
        }
        return DeviceCapability(
            mode = mode,
            totalRamMb = safeRam,
            recommendRulesOnly = mode == DeviceCapabilityMode.RULES_ONLY,
            recommendModelEnabled = mode != DeviceCapabilityMode.RULES_ONLY,
        )
    }
}
