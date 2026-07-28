package kz.qorgau.scamguardian.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room table for analysis history (SCHEMA.md §3.1).
 * Indexes support History screen ordering and filtering.
 */
@Entity(
    tableName = "analysis_log",
    indices = [
        Index(value = ["created_at"]),
        Index(value = ["risk_level"]),
        Index(value = ["source_app"]),
    ],
)
data class AnalysisLogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "source_app")
    val sourceApp: String,

    @ColumnInfo(name = "sender")
    val sender: String?,

    @ColumnInfo(name = "message_text")
    val messageText: String,

    @ColumnInfo(name = "risk_level")
    val riskLevel: String,

    @ColumnInfo(name = "risk_score")
    val riskScore: Float?,

    @ColumnInfo(name = "explanation")
    val explanation: String,

    /** JSON array of rule IDs, e.g. ["kaspi_impersonation","urgency_finance"]. */
    @ColumnInfo(name = "matched_rules")
    val matchedRules: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "user_feedback")
    val userFeedback: String?,

    @ColumnInfo(name = "is_read", defaultValue = "0")
    val isRead: Boolean = false,
)
