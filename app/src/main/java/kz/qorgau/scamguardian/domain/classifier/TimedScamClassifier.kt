package kz.qorgau.scamguardian.domain.classifier

import android.util.Log
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Fail-safe wrapper: if inference is too slow, return null → keep rules result.
 * Target: analysis usable under ~1.5s on mid-range (PRD).
 */
class TimedScamClassifier(
    private val delegate: ScamClassifier,
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
) : ScamClassifier {

    override val isAvailable: Boolean
        get() = delegate.isAvailable

    override suspend fun classify(text: String): ClassifierResult? {
        if (!delegate.isAvailable) return null
        return try {
            withTimeoutOrNull(timeoutMs) {
                delegate.classify(text)
            }.also { result ->
                if (result == null && delegate.isAvailable) {
                    // Timeout or null — no message content in logs.
                    Log.w(TAG, "Classifier returned null (timeout or failure)")
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Classifier failed: ${t.javaClass.simpleName}")
            null
        }
    }

    companion object {
        private const val TAG = "TimedScamClassifier"
        const val DEFAULT_TIMEOUT_MS: Long = 1_500L
    }
}
