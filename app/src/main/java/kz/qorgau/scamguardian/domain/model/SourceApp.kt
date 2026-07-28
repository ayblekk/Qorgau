package kz.qorgau.scamguardian.domain.model

/**
 * Messaging sources monitored in Stage 1 (ARCHITECTURE.md §3.1).
 * Package list is intentionally broad so OEM SMS / messenger variants are not missed.
 * [OTHER] covers any notification that looks like a chat/SMS message (category / MessagingStyle).
 */
enum class SourceApp(
    val storageValue: String,
    val packageNames: Set<String>,
) {
    SMS(
        storageValue = "sms",
        packageNames = setOf(
            // AOSP / Google
            "com.android.mms",
            "com.android.messaging",
            "com.android.mms.service",
            "com.android.providers.telephony",
            "com.google.android.apps.messaging",
            "com.google.android.ims",
            // Samsung
            "com.samsung.android.messaging",
            "com.samsung.android.messaging.service",
            "com.samsung.android.mms",
            // Xiaomi / MIUI / HyperOS
            "com.miui.mms",
            "com.xiaomi.mms",
            "com.android.mms.oem",
            // Huawei / Honor
            "com.huawei.message",
            "com.huawei.mms",
            "com.huawei.android.messaging",
            "com.hihonor.mms",
            "com.hihonor.message",
            // Oppo / Realme / ColorOS / OnePlus
            "com.coloros.mms",
            "com.oppo.mms",
            "com.oneplus.mms",
            "com.realme.mms",
            "com.oplus.mms",
            // Vivo / iQOO
            "com.vivo.mms",
            "com.android.mms.bbk",
            // Transsion (Tecno / Infinix / itel)
            "com.transsion.smartmessage",
            "com.transsion.mms",
            // Others
            "com.sonymobile.android.messaging",
            "com.sonyericsson.conversations",
            "com.motorola.messaging",
            "com.motorola.ccc.mainplm",
            "com.asus.message",
            "com.lge.message",
            "com.lge.mms",
            "com.zte.mms",
            "com.lenovo.ideafriend",
            "com.htc.sense.mms",
            "com.verizon.messaging.vzmsgs",
            "com.android.cellbroadcastreceiver",
        ),
    ),
    WHATSAPP(
        storageValue = "whatsapp",
        packageNames = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "com.whatsapp.w4b.smb",
            "com.whatsapp.smb",
        ),
    ),
    TELEGRAM(
        storageValue = "telegram",
        packageNames = setOf(
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "org.telegram.messenger.beta",
            "org.thunderdog.challegram", // Telegram X
            "org.telegram.plus",
            "nekox.messenger",
            "org.telegram.messenger.huawei",
            "org.telegram.messenger.web.huawei",
        ),
    ),
    INSTAGRAM(
        storageValue = "instagram",
        packageNames = setOf(
            "com.instagram.android",
        ),
    ),
    MESSENGER(
        storageValue = "messenger",
        packageNames = setOf(
            "com.facebook.orca",
            "com.facebook.mlite",
        ),
    ),
    VIBER(
        storageValue = "viber",
        packageNames = setOf(
            "com.viber.voip",
        ),
    ),
    VK(
        storageValue = "vk",
        packageNames = setOf(
            "com.vkontakte.android",
            "com.vk.im",
        ),
    ),
    OK(
        storageValue = "ok",
        packageNames = setOf(
            "ru.ok.android",
        ),
    ),
    /** Other chat / social messengers captured via category or MessagingStyle. */
    OTHER(
        storageValue = "other",
        packageNames = setOf(
            "org.thoughtcrime.securesms", // Signal
            "com.skype.raider",
            "com.discord",
            "jp.naver.line.android",
            "com.snapchat.android",
            "com.tencent.mm", // WeChat
            "im.vector.app",
            "com.bsb.hike",
            "com.imo.android.imoim",
            "com.truecaller",
            "com.icq.mobile.client",
            "com.google.android.apps.dynamite", // Google Chat
            "com.google.android.apps.hangouts",
            "com.microsoft.teams",
            "us.zoom.videomeetings",
        ),
    ),
    MANUAL(
        storageValue = "manual",
        packageNames = emptySet(),
    );

    companion object {
        fun fromPackageName(packageName: String): SourceApp? {
            entries.firstOrNull { packageName in it.packageNames }?.let { return it }
            val lower = packageName.lowercase()
            return when {
                lower.contains("whatsapp") -> WHATSAPP
                lower.contains("telegram") ||
                    lower.contains("challegram") ||
                    lower.contains("nekox") -> TELEGRAM
                lower.contains("instagram") -> INSTAGRAM
                lower.contains("facebook.orca") ||
                    lower.contains("facebook.mlite") ||
                    lower.contains("messenger") && lower.contains("facebook") -> MESSENGER
                lower.contains("viber") -> VIBER
                lower.contains("vkontakte") ||
                    lower.startsWith("com.vk.") ||
                    lower.contains(".vk.") -> VK
                lower.contains("ru.ok") ||
                    lower.contains("odnoklassniki") -> OK
                isLikelySmsPackage(lower) -> SMS
                isLikelyMessengerPackage(lower) -> OTHER
                else -> null
            }
        }

        /**
         * Resolve source from package first; if unknown, accept message-like notifications
         * so OEM SMS / rare messengers are not dropped.
         */
        fun resolve(
            packageName: String,
            isMessageCategory: Boolean,
            hasMessagingStyle: Boolean,
        ): SourceApp? {
            fromPackageName(packageName)?.let { return it }
            if (isMessageCategory || hasMessagingStyle) return OTHER
            return null
        }

        fun fromStorage(value: String): SourceApp =
            entries.firstOrNull { it.storageValue == value } ?: OTHER

        private fun isLikelySmsPackage(lower: String): Boolean {
            if (lower.contains("voicemail") || lower.contains("dialer")) return false
            return lower.contains(".mms") ||
                lower.endsWith(".mms") ||
                lower.contains("mms.") ||
                lower.contains("messaging") ||
                lower.contains("messages") ||
                lower.contains(".sms") ||
                lower.contains("sms.") ||
                lower.contains("telephony") ||
                lower.contains("smartmessage") ||
                lower.contains("conversations") && (
                    lower.contains("sony") ||
                        lower.contains("android") ||
                        lower.contains("oem")
                    )
        }

        private fun isLikelyMessengerPackage(lower: String): Boolean {
            return lower.contains("signal") ||
                lower.contains("imessage") ||
                lower.contains("wechat") ||
                lower.contains("line.android") ||
                lower.contains("imoim") ||
                lower.contains("threema") ||
                lower.contains("element.") ||
                lower.contains("matrix")
        }
    }
}
