package kz.qorgau.scamguardian.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kz.qorgau.scamguardian.data.local.db.dao.AnalysisLogDao
import kz.qorgau.scamguardian.data.local.db.mapper.AnalysisLogMapper
import kz.qorgau.scamguardian.domain.model.AnalysisRecord
import kz.qorgau.scamguardian.domain.model.RiskLevel
import kz.qorgau.scamguardian.domain.model.UserFeedback
import kz.qorgau.scamguardian.domain.repository.AnalysisRepository

class AnalysisRepositoryImpl(
    private val analysisLogDao: AnalysisLogDao,
) : AnalysisRepository {

    override fun observeHistory(): Flow<List<AnalysisRecord>> =
        analysisLogDao.observeAllNewestFirst().map { list ->
            list.map(AnalysisLogMapper::toDomain)
        }

    override fun observeUnreadCount(): Flow<Int> =
        analysisLogDao.observeUnreadCount()

    override suspend fun getById(id: Long): AnalysisRecord? =
        analysisLogDao.getById(id)?.let(AnalysisLogMapper::toDomain)

    override suspend fun insert(record: AnalysisRecord): Long =
        analysisLogDao.insert(AnalysisLogMapper.toEntity(record))

    override suspend fun markRead(id: Long) {
        analysisLogDao.markRead(id)
    }

    override suspend fun setFeedback(id: Long, feedback: UserFeedback) {
        analysisLogDao.setFeedback(id, feedback.storageValue)
    }

    override suspend fun deleteOlderThan(epochMs: Long): Int =
        analysisLogDao.deleteOlderThan(epochMs)

    override suspend fun clearAll() {
        analysisLogDao.clearAll()
    }

    override suspend fun findByRiskLevel(level: RiskLevel): List<AnalysisRecord> =
        analysisLogDao.findByRiskLevel(level.storageValue).map(AnalysisLogMapper::toDomain)
}
