package kz.qorgau.scamguardian.domain.model

/**
 * Messaging sources monitored in Stage 1 (ARCHITECTURE.md §3.1).
 * Package list is intentionally broad so OEM SMS / messenger variants are not missed.
 */
enum class SourceApp(
    val storageValue: String,
    val packageNames: Set<String>,
) {
    SMS(
        storageValue = "sms",
        packageNames = setOf(
            "com.android.mms",
            "com.android.messaging",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.samsung.android.messaging.service",
            "com.miui.mms",
            "com.android.mms.service",
            "com.huawei.message",
            "com.huawei.mms",
            "com.oneplus.mms",
            "com.coloros.mms",
            "com.oppo.mms",
            "com.vivo.mms",
            "com.realme.mms",
            "com.sonymobile.android.messaging",
            "com.motorola.messaging",
            "com.android.providers.telephony",
        ),
    ),
    WHATSAPP(
        storageValue = "whatsapp",
        packageNames = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "com.whatsapp.w4b.smb",
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
        ),
    ),
    MANUAL(
        storageValue = "manual",
        packageNames = emptySet(),
    );

    companion object {
        fun fromPackageName(packageName: String): SourceApp? {
            entries.firstOrNull { packageName in it.packageNames }?.let { return it }
            // Soft match for OEM forks / clones that keep a known prefix.
            val lower = packageName.lowercase()
            return when {
                lower.contains("whatsapp") -> WHATSAPP
                lower.contains("telegram") || lower.contains("challegram") -> TELEGRAM
                lower.endsWith(".mms") ||
                    lower.contains("messaging") && (
                        lower.contains("samsung") ||
                            lower.contains("google") ||
                            lower.contains("android") ||
                            lower.contains("miui") ||
                            lower.contains("huawei") ||
                            lower.contains("oppo") ||
                            lower.contains("vivo") ||
                            lower.contains("coloros")
                        ) -> SMS
                else -> null
            }
        }

        fun fromStorage(value: String): SourceApp =
            entries.firstOrNull { it.storageValue == value }
                ?: error("Unknown source_app: $value")
    }
}
