package kz.qorgau.scamguardian.domain.classifier

/**
 * Optional on-device classifier (ARCHITECTURE.md §3.3, RULES.md §5).
 * Always optional — pure rules must work without this.
 */
interface ScamClassifier {
    val isAvailable: Boolean

    /**
     * Runs off the main thread. Returns null on failure / timeout (fail safe → rules).
     */
    suspend fun classify(text: String): ClassifierResult?
}

data class ClassifierResult(
    val riskScore: Float,
    val explanation: String,
)
