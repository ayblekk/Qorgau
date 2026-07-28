package kz.qorgau.scamguardian.notification

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import kz.qorgau.scamguardian.domain.model.IncomingMessage
import kz.qorgau.scamguardian.domain.model.SourceApp

/**
 * Pure-ish extraction of message text from status-bar notifications.
 * No I/O, no analysis — NotificationListener stays thin (RULES.md §3).
 *
 * [extractFromParts] is fully unit-testable without Android notification objects.
 */
object NotificationTextExtractor {

    /**
     * Result of extraction attempt.
     */
    sealed class ExtractResult {
        data class Success(val message: IncomingMessage) : ExtractResult()
        data class Ignored(val reason: IgnoreReason) : ExtractResult()
    }

    enum class IgnoreReason {
        UNSUPPORTED_PACKAGE,
        OWN_APP,
        EMPTY_TEXT,
        TOO_SHORT,
        TOO_LONG,
        GROUP_CHAT,
        ONGOING_OR_FOREGROUND_SERVICE,
        SUMMARY_NOTIFICATION,
    }

    fun extract(
        sbn: StatusBarNotification,
        ownPackageName: String,
    ): ExtractResult {
        val packageName = sbn.packageName ?: return ExtractResult.Ignored(IgnoreReason.UNSUPPORTED_PACKAGE)
        if (packageName == ownPackageName) {
            return ExtractResult.Ignored(IgnoreReason.OWN_APP)
        }
        val source = SourceApp.fromPackageName(packageName)
            ?: return ExtractResult.Ignored(IgnoreReason.UNSUPPORTED_PACKAGE)

        val notification = sbn.notification
            ?: return ExtractResult.Ignored(IgnoreReason.EMPTY_TEXT)

        // Ongoing / FGS style posts are rarely chat message bodies.
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0 &&
            notification.flags and Notification.FLAG_NO_CLEAR != 0
        ) {
            return ExtractResult.Ignored(IgnoreReason.ONGOING_OR_FOREGROUND_SERVICE)
        }

        val extras = notification.extras ?: Bundle.EMPTY
        if (isSummary(extras, notification)) {
            return ExtractResult.Ignored(IgnoreReason.SUMMARY_NOTIFICATION)
        }

        val title = readCharSequence(extras, Notification.EXTRA_TITLE)
            ?: readCharSequence(extras, Notification.EXTRA_TITLE_BIG)
        val text = readCharSequence(extras, Notification.EXTRA_TEXT)
        val bigText = readCharSequence(extras, Notification.EXTRA_BIG_TEXT)
        val infoText = readCharSequence(extras, Notification.EXTRA_INFO_TEXT)
        val messages = readMessagingStyleLines(extras)

        val flaggedGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false) ||
            extras.getBoolean("android.isGroupConversation", false)
        val conversationTitle = readCharSequence(extras, Notification.EXTRA_CONVERSATION_TITLE)
        val isGroup = isGroupConversation(
            sourceApp = source,
            title = title,
            text = text,
            flaggedAsGroup = flaggedGroup,
            conversationTitle = conversationTitle,
        )

        return extractFromParts(
            sourceApp = source,
            packageName = packageName,
            title = title,
            text = text,
            bigText = bigText,
            infoText = infoText,
            messagingLines = messages,
            isGroupConversation = isGroup,
            receivedAtEpochMs = sbn.postTime.takeIf { it > 0L } ?: System.currentTimeMillis(),
            notificationKey = sbn.key,
        )
    }

    /**
     * Core extraction logic — used by unit tests and [extract].
     */
    fun extractFromParts(
        sourceApp: SourceApp,
        packageName: String,
        title: String?,
        text: String?,
        bigText: String?,
        infoText: String? = null,
        messagingLines: List<String> = emptyList(),
        isGroupConversation: Boolean = false,
        receivedAtEpochMs: Long,
        notificationKey: String? = null,
    ): ExtractResult {
        if (isGroupConversation) {
            return ExtractResult.Ignored(IgnoreReason.GROUP_CHAT)
        }

        val body = pickBestBody(
            bigText = bigText,
            text = text,
            messagingLines = messagingLines,
            infoText = infoText,
        )
        val cleaned = cleanupBody(body, title, sourceApp)
        if (cleaned.isNullOrBlank()) {
            return ExtractResult.Ignored(IgnoreReason.EMPTY_TEXT)
        }
        if (cleaned.length < NotificationCaptureConfig.MIN_MESSAGE_LENGTH) {
            return ExtractResult.Ignored(IgnoreReason.TOO_SHORT)
        }
        if (cleaned.length > NotificationCaptureConfig.MAX_MESSAGE_LENGTH) {
            return ExtractResult.Ignored(IgnoreReason.TOO_LONG)
        }

        val sender = title?.trim()?.takeIf { it.isNotEmpty() }

        return ExtractResult.Success(
            IncomingMessage(
                sourceApp = sourceApp,
                packageName = packageName,
                sender = sender,
                text = cleaned,
                receivedAtEpochMs = receivedAtEpochMs,
                isGroupConversation = false,
                notificationKey = notificationKey,
            ),
        )
    }

    fun pickBestBody(
        bigText: String?,
        text: String?,
        messagingLines: List<String>,
        infoText: String?,
    ): String? {
        val fromMessaging = messagingLines
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n")
            .takeIf { it.isNotEmpty() }

        // Prefer the richest available representation.
        return listOfNotNull(bigText, fromMessaging, text, infoText)
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
    }

    /**
     * Removes WhatsApp/Telegram "Name: message" prefix when title already is the chat name.
     */
    fun cleanupBody(body: String?, title: String?, sourceApp: SourceApp): String? {
        if (body.isNullOrBlank()) return null
        var result = body.trim()

        // Messaging apps often format as "Sender: text" for 1:1 as well; keep full text if short.
        if (sourceApp == SourceApp.WHATSAPP || sourceApp == SourceApp.TELEGRAM) {
            val colonIndex = result.indexOf(':')
            if (colonIndex in 1..40) {
                val prefix = result.substring(0, colonIndex).trim()
                val rest = result.substring(colonIndex + 1).trim()
                if (rest.isNotEmpty() && (title == null || prefix.equals(title, ignoreCase = true) ||
                        prefix.length <= 32)
                ) {
                    // Keep content after "Name:" when it looks like a person name prefix.
                    if (!prefix.contains(' ') || prefix.split(' ').size <= 3) {
                        result = rest
                    }
                }
            }
        }

        return result.trim().ifEmpty { null }
    }

    /**
     * Group-chat heuristics (ARCHITECTURE.md §3.1 — ignore groups to save resources).
     * Pure function for unit tests.
     */
    fun isGroupConversation(
        sourceApp: SourceApp,
        title: String?,
        text: String?,
        flaggedAsGroup: Boolean = false,
        conversationTitle: String? = null,
    ): Boolean {
        if (flaggedAsGroup) return true

        if (!conversationTitle.isNullOrBlank() && sourceApp != SourceApp.SMS) {
            // Conversation title different from person title often means group.
            if (!title.isNullOrBlank() && conversationTitle != title) {
                return true
            }
        }

        // WhatsApp/Telegram group previews often look like "Alice: hi\nBob: ok"
        if (sourceApp != SourceApp.SMS && text != null) {
            val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size >= 2) {
                val namePrefixed = lines.count { line ->
                    val idx = line.indexOf(':')
                    idx in 1..32 && line.substring(0, idx).none { it.isDigit() }
                }
                if (namePrefixed >= 2) return true
            }
        }

        // Title with participant count: "Family (12)"
        if (title != null && PARTICIPANT_COUNT_IN_TITLE.containsMatchIn(title.trim())) {
            return true
        }

        return false
    }

    private val PARTICIPANT_COUNT_IN_TITLE = Regex("""\(\d{1,3}\)$""")

    private fun isSummary(extras: Bundle, notification: Notification): Boolean {
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return true
        // Inbox-style multi-line summary without a concrete body.
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        val text = readCharSequence(extras, Notification.EXTRA_TEXT)
        val bigText = readCharSequence(extras, Notification.EXTRA_BIG_TEXT)
        if (textLines != null && textLines.size > 1 && text.isNullOrBlank() && bigText.isNullOrBlank()) {
            return true
        }
        return false
    }

    private fun readMessagingStyleLines(extras: Bundle): List<String> {
        // Notification.EXTRA_MESSAGES is an array of bundles (MessagingStyle).
        @Suppress("DEPRECATION")
        val raw = extras.getParcelableArray(Notification.EXTRA_MESSAGES) ?: return emptyList()
        return raw.mapNotNull { item ->
            val bundle = item as? Bundle ?: return@mapNotNull null
            bundle.getCharSequence("text")?.toString()
        }
    }

    private fun readCharSequence(extras: Bundle, key: String): String? {
        val value = extras.getCharSequence(key) ?: return null
        val text = value.toString().trim()
        return text.ifEmpty { null }
    }
}
