package kz.qorgau.scamguardian.domain.repository

import kotlinx.coroutines.flow.Flow
import kz.qorgau.scamguardian.domain.model.AnalysisRecord
import kz.qorgau.scamguardian.domain.model.RiskLevel
import kz.qorgau.scamguardian.domain.model.SourceApp
import kz.qorgau.scamguardian.domain.model.UserFeedback

/**
 * Local history access. Implementations must keep all content on-device.
 */
interface AnalysisRepository {
    fun observeHistory(): Flow<List<AnalysisRecord>>

    fun observeUnreadCount(): Flow<Int>

    suspend fun getById(id: Long): AnalysisRecord?

    suspend fun insert(record: AnalysisRecord): Long

    /**
     * Returns an existing history row for the same source/sender/body near [receivedAtEpochMs],
     * or null if this looks like a new message.
     */
    suspend fun findRecentDuplicate(
        sourceApp: SourceApp,
        sender: String?,
        messageText: String,
        receivedAtEpochMs: Long,
        proximityMs: Long,
    ): AnalysisRecord?

    suspend fun markRead(id: Long)

    suspend fun setFeedback(id: Long, feedback: UserFeedback)

    suspend fun deleteOlderThan(epochMs: Long): Int

    suspend fun clearAll()

    suspend fun findByRiskLevel(level: RiskLevel): List<AnalysisRecord>
}
