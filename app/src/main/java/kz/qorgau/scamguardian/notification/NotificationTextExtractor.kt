package kz.qorgau.scamguardian.notification

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import kz.qorgau.scamguardian.domain.model.IncomingMessage
import kz.qorgau.scamguardian.domain.model.SourceApp

/**
 * Extracts message text from status-bar notifications.
 * Optimized for SMS / WhatsApp / Telegram capture coverage (not silent filtering).
 */
object NotificationTextExtractor {

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
        ONGOING_OR_FOREGROUND_SERVICE,
    }

    fun extract(
        sbn: StatusBarNotification,
        ownPackageName: String,
    ): ExtractResult {
        val packageName = sbn.packageName
            ?: return ExtractResult.Ignored(IgnoreReason.UNSUPPORTED_PACKAGE)
        if (packageName == ownPackageName) {
            return ExtractResult.Ignored(IgnoreReason.OWN_APP)
        }
        val source = SourceApp.fromPackageName(packageName)
            ?: return ExtractResult.Ignored(IgnoreReason.UNSUPPORTED_PACKAGE)

        val notification = sbn.notification
            ?: return ExtractResult.Ignored(IgnoreReason.EMPTY_TEXT)

        // Skip only true ongoing services (not messaging apps with sticky-ish flags).
        val isOngoingService =
            notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0 ||
                (
                    notification.flags and Notification.FLAG_ONGOING_EVENT != 0 &&
                        notification.category == Notification.CATEGORY_SERVICE
                    )
        if (isOngoingService) {
            return ExtractResult.Ignored(IgnoreReason.ONGOING_OR_FOREGROUND_SERVICE)
        }

        val extras = notification.extras ?: Bundle.EMPTY

        val title = readCharSequence(extras, Notification.EXTRA_TITLE)
            ?: readCharSequence(extras, Notification.EXTRA_TITLE_BIG)
            ?: readCharSequence(extras, Notification.EXTRA_CONVERSATION_TITLE)
        val text = readCharSequence(extras, Notification.EXTRA_TEXT)
            ?: readCharSequence(extras, Notification.EXTRA_SUB_TEXT)
        val bigText = readCharSequence(extras, Notification.EXTRA_BIG_TEXT)
        val infoText = readCharSequence(extras, Notification.EXTRA_INFO_TEXT)
        val summaryText = readCharSequence(extras, Notification.EXTRA_SUMMARY_TEXT)
        val textLines = readTextLines(extras)
        val messages = readMessagingStyleLines(extras)

        val flaggedGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false) ||
            extras.getBoolean("android.isGroupConversation", false)

        return extractFromParts(
            sourceApp = source,
            packageName = packageName,
            title = title,
            text = text,
            bigText = bigText,
            infoText = infoText,
            summaryText = summaryText,
            textLines = textLines,
            messagingLines = messages,
            isGroupConversation = flaggedGroup,
            receivedAtEpochMs = sbn.postTime.takeIf { it > 0L } ?: System.currentTimeMillis(),
            notificationKey = sbn.key,
        )
    }

    fun extractFromParts(
        sourceApp: SourceApp,
        packageName: String,
        title: String?,
        text: String?,
        bigText: String?,
        infoText: String? = null,
        summaryText: String? = null,
        textLines: List<String> = emptyList(),
        messagingLines: List<String> = emptyList(),
        isGroupConversation: Boolean = false,
        receivedAtEpochMs: Long,
        notificationKey: String? = null,
    ): ExtractResult {
        // Groups are still analyzed (user asked to intercept all messaging notifications).
        val body = pickBestBody(
            bigText = bigText,
            text = text,
            messagingLines = messagingLines,
            textLines = textLines,
            infoText = infoText,
            summaryText = summaryText,
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
                isGroupConversation = isGroupConversation,
                notificationKey = notificationKey,
            ),
        )
    }

    fun pickBestBody(
        bigText: String?,
        text: String?,
        messagingLines: List<String>,
        textLines: List<String> = emptyList(),
        infoText: String? = null,
        summaryText: String? = null,
    ): String? {
        val fromMessaging = messagingLines
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n")
            .takeIf { it.isNotEmpty() }

        // Inbox / summary: take the last concrete line (usually newest message).
        val fromLines = textLines
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .lastOrNull()

        return listOfNotNull(bigText, fromMessaging, text, fromLines, infoText, summaryText)
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
    }

    fun cleanupBody(body: String?, title: String?, sourceApp: SourceApp): String? {
        if (body.isNullOrBlank()) return null
        var result = body.trim()

        if (sourceApp == SourceApp.WHATSAPP || sourceApp == SourceApp.TELEGRAM) {
            val colonIndex = result.indexOf(':')
            if (colonIndex in 1..40) {
                val prefix = result.substring(0, colonIndex).trim()
                val rest = result.substring(colonIndex + 1).trim()
                if (rest.isNotEmpty() &&
                    (
                        title == null ||
                            prefix.equals(title, ignoreCase = true) ||
                            prefix.length <= 32
                        )
                ) {
                    if (!prefix.contains(' ') || prefix.split(' ').size <= 3) {
                        result = rest
                    }
                }
            }
        }

        return result.trim().ifEmpty { null }
    }

    private fun readTextLines(extras: Bundle): List<String> {
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES) ?: return emptyList()
        return lines.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
    }

    private fun readMessagingStyleLines(extras: Bundle): List<String> {
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
