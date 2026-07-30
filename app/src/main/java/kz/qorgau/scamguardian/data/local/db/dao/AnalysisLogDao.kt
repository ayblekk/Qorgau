package kz.qorgau.scamguardian.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kz.qorgau.scamguardian.data.local.db.entity.AnalysisLogEntity

@Dao
interface AnalysisLogDao {

    @Query("SELECT * FROM analysis_log ORDER BY created_at DESC")
    fun observeAllNewestFirst(): Flow<List<AnalysisLogEntity>>

    @Query("SELECT COUNT(*) FROM analysis_log WHERE is_read = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("SELECT * FROM analysis_log WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AnalysisLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AnalysisLogEntity): Long

    @Query("UPDATE analysis_log SET is_read = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE analysis_log SET user_feedback = :feedback WHERE id = :id")
    suspend fun setFeedback(id: Long, feedback: String)

    @Query("SELECT * FROM analysis_log WHERE risk_level = :riskLevel ORDER BY created_at DESC")
    suspend fun findByRiskLevel(riskLevel: String): List<AnalysisLogEntity>

    @Query("DELETE FROM analysis_log WHERE created_at < :epochMs")
    suspend fun deleteOlderThan(epochMs: Long): Int

    @Query("DELETE FROM analysis_log")
    suspend fun clearAll()

    /**
     * Exact content match near [receivedAtEpochMs] — used to drop NLS reprocess /
     * OEM re-posts of the same shade notification (same postTime → ABS ≈ 0).
     */
    @Query(
        """
        SELECT * FROM analysis_log
        WHERE source_app = :sourceApp
          AND message_text = :messageText
          AND IFNULL(sender, '') = IFNULL(:sender, '')
          AND ABS(created_at - :receivedAtEpochMs) <= :proximityMs
        ORDER BY created_at DESC
        LIMIT 1
        """,
    )
    suspend fun findRecentDuplicate(
        sourceApp: String,
        sender: String?,
        messageText: String,
        receivedAtEpochMs: Long,
        proximityMs: Long,
    ): AnalysisLogEntity?
}
