package kz.qorgau.scamguardian.domain.repository

import kotlinx.coroutines.flow.Flow
import kz.qorgau.scamguardian.domain.model.AnalysisRecord
import kz.qorgau.scamguardian.domain.model.RiskLevel
import kz.qorgau.scamguardian.domain.model.UserFeedback

/**
 * Local history access. Implementations must keep all content on-device.
 */
interface AnalysisRepository {
    fun observeHistory(): Flow<List<AnalysisRecord>>

    fun observeUnreadCount(): Flow<Int>

    suspend fun getById(id: Long): AnalysisRecord?

    suspend fun insert(record: AnalysisRecord): Long

    suspend fun markRead(id: Long)

    suspend fun setFeedback(id: Long, feedback: UserFeedback)

    suspend fun deleteOlderThan(epochMs: Long): Int

    suspend fun clearAll()

    suspend fun findByRiskLevel(level: RiskLevel): List<AnalysisRecord>
}
