package kz.qorgau.scamguardian.domain.model

/**
 * Normalized message captured from a notification or manual paste.
 * Content stays on-device for the whole pipeline.
 */
data class IncomingMessage(
    val sourceApp: SourceApp,
    val packageName: String,
    val sender: String?,
    val text: String,
    val receivedAtEpochMs: Long,
    val isGroupConversation: Boolean = false,
    val notificationKey: String? = null,
)
