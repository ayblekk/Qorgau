package kz.qorgau.scamguardian.domain.usecase

import kz.qorgau.scamguardian.domain.repository.AnalysisRepository

/**
 * Local retention: keep last 90 days by default (SCHEMA.md §5).
 */
class PruneHistoryUseCase(
    private val analysisRepository: AnalysisRepository,
) {
    suspend fun execute(retentionDays: Int = DEFAULT_RETENTION_DAYS): Int {
        val cutoff = System.currentTimeMillis() - retentionDays * DAY_MS
        return analysisRepository.deleteOlderThan(cutoff)
    }

    companion object {
        const val DEFAULT_RETENTION_DAYS: Int = 90
        private const val DAY_MS: Long = 86_400_000L
    }
}
