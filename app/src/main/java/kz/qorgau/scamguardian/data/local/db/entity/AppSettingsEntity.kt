package kz.qorgau.scamguardian.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row settings table (SCHEMA.md §3.2). Always id = 1.
 */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = DEFAULT_ID,

    @ColumnInfo(name = "language", defaultValue = "ru")
    val language: String = "ru",

    @ColumnInfo(name = "sensitivity", defaultValue = "medium")
    val sensitivity: String = "medium",

    @ColumnInfo(name = "monitor_sms", defaultValue = "1")
    val monitorSms: Boolean = true,

    @ColumnInfo(name = "monitor_whatsapp", defaultValue = "1")
    val monitorWhatsapp: Boolean = true,

    @ColumnInfo(name = "monitor_telegram", defaultValue = "1")
    val monitorTelegram: Boolean = true,
) {
    companion object {
        const val DEFAULT_ID: Int = 1

        fun defaults(): AppSettingsEntity = AppSettingsEntity()
    }
}
