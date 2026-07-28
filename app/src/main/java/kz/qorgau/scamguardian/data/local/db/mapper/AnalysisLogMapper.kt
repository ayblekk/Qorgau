package kz.qorgau.scamguardian.data.local.db.mapper

import kz.qorgau.scamguardian.data.local.db.entity.AnalysisLogEntity
import kz.qorgau.scamguardian.domain.model.AnalysisRecord
import kz.qorgau.scamguardian.domain.model.RiskLevel
import kz.qorgau.scamguardian.domain.model.SourceApp
import kz.qorgau.scamguardian.domain.model.UserFeedback

/**
 * Maps Room entities ↔ domain models.
 * JSON for matched_rules is pure-Kotlin so unit tests run without Android runtime.
 */
internal object AnalysisLogMapper {

    fun toDomain(entity: AnalysisLogEntity): AnalysisRecord =
        AnalysisRecord(
            id = entity.id,
            sourceApp = SourceApp.fromStorage(entity.sourceApp),
            sender = entity.sender,
            messageText = entity.messageText,
            riskLevel = RiskLevel.fromStorage(entity.riskLevel),
            riskScore = entity.riskScore,
            explanation = entity.explanation,
            matchedRules = decodeRuleIds(entity.matchedRules),
            createdAtEpochMs = entity.createdAt,
            userFeedback = UserFeedback.fromStorage(entity.userFeedback),
            isRead = entity.isRead,
        )

    fun toEntity(record: AnalysisRecord): AnalysisLogEntity =
        AnalysisLogEntity(
            id = record.id,
            sourceApp = record.sourceApp.storageValue,
            sender = record.sender,
            messageText = record.messageText,
            riskLevel = record.riskLevel.storageValue,
            riskScore = record.riskScore,
            explanation = record.explanation,
            matchedRules = encodeRuleIds(record.matchedRules),
            createdAt = record.createdAtEpochMs,
            userFeedback = record.userFeedback?.storageValue,
            isRead = record.isRead,
        )

    fun encodeRuleIds(ids: List<String>): String? {
        if (ids.isEmpty()) return null
        return ids.joinToString(
            prefix = "[",
            postfix = "]",
            separator = ",",
        ) { id -> "\"${escapeJson(id)}\"" }
    }

    fun decodeRuleIds(json: String?): List<String> {
        if (json.isNullOrBlank() || json == "[]") return emptyList()
        val inner = json.trim().removePrefix("[").removeSuffix("]")
        if (inner.isBlank()) return emptyList()
        return STRING_TOKEN.findAll(inner).map { match ->
            unescapeJson(match.groupValues[1])
        }.toList()
    }

    private fun escapeJson(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

    private fun unescapeJson(value: String): String =
        value
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")

    private val STRING_TOKEN = Regex("\"((?:\\\\.|[^\"\\\\])*)\"")
}
