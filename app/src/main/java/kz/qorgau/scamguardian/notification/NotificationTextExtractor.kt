package kz.qorgau.scamguardian.notification

import android.app.Notification
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import kz.qorgau.scamguardian.domain.model.IncomingMessage
import kz.qorgau.scamguardian.domain.model.SourceApp

/**
 * Extracts message text from status-bar notifications.
 *
 * WhatsApp / modern SMS apps primarily use [Notification.MessagingStyle].
 * We prefer the public MessagingStyle API, then fall back to classic extras,
 * inbox lines, ticker, and summary text so OEM SMS is not missed.
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
        USELESS_SUMMARY,
        /** Group chats are skipped (ARCHITECTURE.md §3.1 — battery / noise). */
        GROUP_CONVERSATION,
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

        val messaging = readMessagingStyle(notification)
        val isMessageCategory = isMessageLikeCategory(notification.category)
        val source = SourceApp.resolve(
            packageName = packageName,
            isMessageCategory = isMessageCategory,
            hasMessagingStyle = messaging.hasContent,
        ) ?: return ExtractResult.Ignored(IgnoreReason.UNSUPPORTED_PACKAGE)

        val extras = notification.extras ?: Bundle.EMPTY

        val title = firstNonBlank(
            messaging.conversationTitle,
            readCharSequence(extras, Notification.EXTRA_TITLE),
            readCharSequence(extras, Notification.EXTRA_TITLE_BIG),
            readCharSequence(extras, Notification.EXTRA_CONVERSATION_TITLE),
            messaging.lastSender,
        )
        val text = readCharSequence(extras, Notification.EXTRA_TEXT)
            ?: readCharSequence(extras, Notification.EXTRA_SUB_TEXT)
        val bigText = readCharSequence(extras, Notification.EXTRA_BIG_TEXT)
        val infoText = readCharSequence(extras, Notification.EXTRA_INFO_TEXT)
        val summaryText = readCharSequence(extras, Notification.EXTRA_SUMMARY_TEXT)
        val textLines = readTextLines(extras)
        val ticker = notification.tickerText?.toString()?.trim()?.takeIf { it.isNotEmpty() }

        val flaggedGroup = messaging.isGroupConversation ||
            extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false) ||
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
            messagingLines = messaging.lines,
            ticker = ticker,
            isGroupConversation = flaggedGroup,
            receivedAtEpochMs = sbn.postTime.takeIf { it > 0L } ?: System.currentTimeMillis(),
            notificationKey = sbn.key,
            isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
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
        ticker: String? = null,
        isGroupConversation: Boolean = false,
        receivedAtEpochMs: Long,
        notificationKey: String? = null,
        isGroupSummary: Boolean = false,
    ): ExtractResult {
        val body = pickBestBody(
            bigText = bigText,
            text = text,
            messagingLines = messagingLines,
            textLines = textLines,
            infoText = infoText,
            summaryText = summaryText,
            ticker = ticker,
            title = title,
        )
        val cleaned = cleanupBody(body, title, sourceApp)
        if (cleaned.isNullOrBlank()) {
            return ExtractResult.Ignored(IgnoreReason.EMPTY_TEXT)
        }
        if (isUselessSummary(cleaned, isGroupSummary)) {
            return ExtractResult.Ignored(IgnoreReason.USELESS_SUMMARY)
        }
        // ARCHITECTURE.md §3.1: skip group chats (noise + battery).
        if (isGroupConversation) {
            return ExtractResult.Ignored(IgnoreReason.GROUP_CONVERSATION)
        }
        if (cleaned.length < NotificationCaptureConfig.MIN_MESSAGE_LENGTH) {
            return ExtractResult.Ignored(IgnoreReason.TOO_SHORT)
        }
        if (cleaned.length > NotificationCaptureConfig.MAX_MESSAGE_LENGTH) {
            return ExtractResult.Ignored(IgnoreReason.TOO_LONG)
        }

        val sender = normalizeSenderTitle(title)

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

    /**
     * WhatsApp / Telegram often append " (2 messages)" to the conversation title when
     * several unread lines are stacked. Store the clean chat name in History.
     */
    fun normalizeSenderTitle(title: String?): String? {
        if (title.isNullOrBlank()) return null
        val trimmed = title.trim()
        val withoutCount = SENDER_MESSAGE_COUNT_SUFFIX.replace(trimmed, "").trim()
        return withoutCount.takeIf { it.isNotEmpty() }
    }

    private val SENDER_MESSAGE_COUNT_SUFFIX = Regex(
        """\s*\(\d+\s+[^)]{1,40}\)\s*$""",
    )

    fun pickBestBody(
        bigText: String?,
        text: String?,
        messagingLines: List<String>,
        textLines: List<String> = emptyList(),
        infoText: String? = null,
        summaryText: String? = null,
        ticker: String? = null,
        title: String? = null,
    ): String? {
        // Prefer MessagingStyle messages (WhatsApp / modern SMS / Telegram).
        val fromMessaging = messagingLines
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .let { lines ->
                when {
                    lines.isEmpty() -> null
                    // Newest message is usually last in MessagingStyle.
                    lines.size == 1 -> lines.first()
                    else -> lines.last()
                }
            }

        val fromLines = textLines
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .lastOrNull()

        // Prefer richest concrete body; avoid picking title-as-body when real text exists.
        val candidates = listOfNotNull(
            bigText,
            fromMessaging,
            text,
            fromLines,
            infoText,
            summaryText,
            ticker,
        ).map { it.trim() }.filter { it.isNotEmpty() }

        val best = candidates.maxByOrNull { scoreBodyCandidate(it, title) }
        if (best != null) return best

        // Last resort: some OEM SMS put the whole body into the title.
        return title?.trim()?.takeIf { it.length >= NotificationCaptureConfig.MIN_MESSAGE_LENGTH }
    }

    fun cleanupBody(body: String?, title: String?, sourceApp: SourceApp): String? {
        if (body.isNullOrBlank()) return null
        var result = stripInvisible(body).trim()

        // Strip "Name: message" prefixes common in WhatsApp / Telegram / SMS previews.
        if (sourceApp != SourceApp.MANUAL) {
            val colonIndex = result.indexOf(':')
            if (colonIndex in 1..48) {
                val prefix = result.substring(0, colonIndex).trim()
                val rest = result.substring(colonIndex + 1).trim()
                if (rest.isNotEmpty() && looksLikeSenderPrefix(prefix, title)) {
                    result = rest
                }
            }
        }

        // WhatsApp sometimes duplicates "Name\nmessage" in bigText.
        if (title != null && result.startsWith(title, ignoreCase = true)) {
            val withoutTitle = result.removePrefix(title).trim()
            if (withoutTitle.length >= NotificationCaptureConfig.MIN_MESSAGE_LENGTH) {
                result = withoutTitle.trimStart(':', '—', '-', ' ').trim()
            }
        }

        return result.trim().ifEmpty { null }
    }

    private fun scoreBodyCandidate(candidate: String, title: String?): Int {
        var score = candidate.length
        // Prefer multi-word human text over short labels like "WhatsApp" / contact name.
        if (candidate.any { it.isWhitespace() }) score += 40
        if (title != null && candidate.equals(title, ignoreCase = true)) score -= 100
        if (isGenericLabel(candidate)) score -= 80
        // Placeholder shade lines ("Message from +7…") must lose to real body text.
        if (isSenderPlaceholderBody(candidate)) score -= 120
        return score
    }

    private fun looksLikeSenderPrefix(prefix: String, title: String?): Boolean {
        if (prefix.contains('\n')) return false
        if (title != null && prefix.equals(title, ignoreCase = true)) return true
        if (prefix.length > 40) return false
        val words = prefix.split(' ').filter { it.isNotEmpty() }
        return words.size <= 4 && !prefix.any { it == '.' && prefix.indexOf(it) > 3 }
    }

    private fun isUselessSummary(text: String, isGroupSummary: Boolean): Boolean {
        val lower = text.lowercase()
        val summaryPatterns = listOf(
            "new messages",
            "новых сообщени",
            "новое сообщение",
            "жаңа хабарлама",
            "messages from",
            "message from",
            "сообщений от",
            "сообщение от",
            "хабарламадан",
            "checking for new messages",
            "backup in progress",
        )
        if (summaryPatterns.any { it in lower }) return true
        if (isSenderPlaceholderBody(text)) return true
        if (isGroupSummary && text.length < 24 && !text.any { it.isLetter() && it.code > 127 || it.isWhitespace() }) {
            return true
        }
        return isGenericLabel(text)
    }

    /**
     * OEM / messenger placeholders with no analyzable content, e.g.
     * "Message from +7 707 028 1515" or "Сообщение от Alice".
     */
    internal fun isSenderPlaceholderBody(text: String): Boolean {
        val trimmed = stripInvisible(text).trim()
        if (trimmed.isEmpty()) return false
        val lower = trimmed.lowercase()

        // Whole body is a meta line: "Message from <name/phone>".
        val placeholderPrefixes = listOf(
            "message from ",
            "messages from ",
            "сообщение от ",
            "сообщения от ",
            "сообщений от ",
        )
        for (prefix in placeholderPrefixes) {
            if (lower.startsWith(prefix)) {
                val rest = trimmed.substring(prefix.length).trim()
                // Only a contact/phone left — nothing to run rules on.
                if (rest.isNotEmpty() && rest.length <= 64 && !rest.contains('\n')) {
                    return true
                }
            }
        }

        // Body is only a phone number (sometimes duplicated as "text" with title=phone).
        if (PHONE_ONLY.matches(trimmed)) return true

        return false
    }

    private fun isGenericLabel(text: String): Boolean {
        val lower = text.lowercase().trim()
        return lower in setOf(
            "whatsapp",
            "telegram",
            "messages",
            "message",
            "sms",
            "mms",
            "chat",
            "you",
            "photo",
            "video",
            "voice message",
            "стикер",
            "изображение",
            "фото",
            "видео",
            "голосовое сообщение",
            "this message was deleted",
            "сообщение удалено",
        )
    }

    private val PHONE_ONLY = Regex(
        """^\+?[\d\s\-().]{7,22}$""",
    )

    private fun isMessageLikeCategory(category: String?): Boolean =
        category == Notification.CATEGORY_MESSAGE ||
            category == Notification.CATEGORY_SOCIAL ||
            category == Notification.CATEGORY_EMAIL ||
            // Some OEM SMS apps omit category or use "msg".
            category == "msg" ||
            category == "sms"

    private data class MessagingExtract(
        val lines: List<String> = emptyList(),
        val conversationTitle: String? = null,
        val lastSender: String? = null,
        val isGroupConversation: Boolean = false,
    ) {
        val hasContent: Boolean
            get() = lines.isNotEmpty() || !conversationTitle.isNullOrBlank()
    }

    private fun readMessagingStyle(notification: Notification): MessagingExtract {
        // Compat path — WhatsApp / Google Messages rely on MessagingStyle extras.
        runCatching {
            val style = NotificationCompat.MessagingStyle
                .extractMessagingStyleFromNotification(notification)
            if (style != null) {
                val lines = style.messages.mapNotNull { msg ->
                    msg.text?.toString()?.let { stripInvisible(it).trim() }?.takeIf { it.isNotEmpty() }
                }
                val title = style.conversationTitle?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                val lastSender = style.messages.lastOrNull()?.person?.name?.toString()
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: style.messages.lastOrNull()?.sender?.toString()
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                    ?: style.user?.name?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                return MessagingExtract(
                    lines = lines,
                    conversationTitle = title,
                    lastSender = lastSender,
                    isGroupConversation = style.isGroupConversation,
                )
            }
        }

        // Fallback: raw EXTRA_MESSAGES bundles (OEM quirks).
        val extras = notification.extras ?: return MessagingExtract()
        val lines = readMessagingStyleLines(extras)
        return MessagingExtract(lines = lines)
    }

    private fun readTextLines(extras: Bundle): List<String> {
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES) ?: return emptyList()
        return lines.mapNotNull { it?.toString()?.let { s -> stripInvisible(s).trim() }?.takeIf(String::isNotEmpty) }
    }

    private fun readMessagingStyleLines(extras: Bundle): List<String> {
        val raw: Array<out Parcelable>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelableArray(Notification.EXTRA_MESSAGES, Parcelable::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        }
        if (raw == null) return emptyList()
        return raw.mapNotNull { item ->
            when (item) {
                is Bundle -> {
                    firstNonBlank(
                        item.getCharSequence("text")?.toString(),
                        item.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                        item.getString("text"),
                    )
                }
                else -> null
            }?.let { stripInvisible(it).trim() }?.takeIf { it.isNotEmpty() }
        }
    }

    private fun readCharSequence(extras: Bundle, key: String): String? {
        val value = extras.getCharSequence(key) ?: return null
        val text = stripInvisible(value.toString()).trim()
        return text.ifEmpty { null }
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }?.trim()

    /** Strip LTR/RTL marks and zero-width chars WhatsApp injects into previews. */
    private fun stripInvisible(input: String): String =
        buildString(input.length) {
            for (ch in input) {
                if (ch == '\u200E' || ch == '\u200F' || ch == '\u200B' ||
                    ch == '\u200C' || ch == '\u200D' || ch == '\uFEFF'
                ) {
                    continue
                }
                append(ch)
            }
        }
}
