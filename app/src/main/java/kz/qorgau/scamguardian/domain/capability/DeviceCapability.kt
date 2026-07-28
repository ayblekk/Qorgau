package kz.qorgau.scamguardian.domain.capability

/**
 * Device analysis profile (ARCHITECTURE.md §3.5).
 * Progressive enhancement: weakest phones stay on pure rules.
 */
enum class DeviceCapabilityMode {
    /** < ~3.5 GB RAM — rules only by default. */
    RULES_ONLY,

    /** Mid-range — rules + optional light classifier when uncertain. */
    RULES_PLUS_LIGHT,

    /** Stronger devices — full optional model path allowed. */
    FULL,
}

data class DeviceCapability(
    val mode: DeviceCapabilityMode,
    /** Total RAM in MB (approx). */
    val totalRamMb: Long,
    val recommendRulesOnly: Boolean,
    val recommendModelEnabled: Boolean,
)
