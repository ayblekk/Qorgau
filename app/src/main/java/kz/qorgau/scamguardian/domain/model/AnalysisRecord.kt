package kz.qorgau.scamguardian.domain.model

/**
 * Domain model for a stored analysis result (maps to AnalysisLog).
 */
data class AnalysisRecord(
    val id: Long = 0L,
    val sourceApp: SourceApp,
    val sender: String?,
    val messageText: String,
    val riskLevel: RiskLevel,
    val riskScore: Float?,
    val explanation: String,
    val matchedRules: List<String>,
    val createdAtEpochMs: Long,
    val userFeedback: UserFeedback? = null,
    val isRead: Boolean = false,
)
