package kz.qorgau.scamguardian.domain.model

/**
 * Messaging sources monitored in Stage 1 (ARCHITECTURE.md §3.1).
 * [storageValue] is what we persist in AnalysisLog.source_app.
 */
enum class SourceApp(
    val storageValue: String,
    val packageNames: Set<String>,
) {
    SMS(
        storageValue = "sms",
        packageNames = setOf(
            "com.android.mms",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
        ),
    ),
    WHATSAPP(
        storageValue = "whatsapp",
        packageNames = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
        ),
    ),
    TELEGRAM(
        storageValue = "telegram",
        packageNames = setOf(
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "org.thunderdog.challegram",
        ),
    ),
    MANUAL(
        storageValue = "manual",
        packageNames = emptySet(),
    );

    companion object {
        fun fromPackageName(packageName: String): SourceApp? =
            entries.firstOrNull { packageName in it.packageNames }

        fun fromStorage(value: String): SourceApp =
            entries.firstOrNull { it.storageValue == value }
                ?: error("Unknown source_app: $value")
    }
}
