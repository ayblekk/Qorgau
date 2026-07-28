package kz.qorgau.scamguardian.domain.classifier

/**
 * Default Stage 1 classifier: model not shipped yet.
 * Guarantees pure-rules fallback always works (RULES.md §5).
 */
class UnavailableScamClassifier : ScamClassifier {
    override val isAvailable: Boolean = false

    override suspend fun classify(text: String): ClassifierResult? = null
}
