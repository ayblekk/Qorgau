package kz.qorgau.scamguardian.data.local.db.mapper

import kz.qorgau.scamguardian.data.local.db.entity.AppSettingsEntity
import kz.qorgau.scamguardian.domain.model.AppLanguage
import kz.qorgau.scamguardian.domain.model.AppSettings
import kz.qorgau.scamguardian.domain.model.Sensitivity

internal object AppSettingsMapper {

    fun toDomain(entity: AppSettingsEntity): AppSettings =
        AppSettings(
            language = AppLanguage.fromStorage(entity.language),
            sensitivity = Sensitivity.fromStorage(entity.sensitivity),
            monitorSms = entity.monitorSms,
            monitorWhatsapp = entity.monitorWhatsapp,
            monitorTelegram = entity.monitorTelegram,
            monitorInstagram = entity.monitorInstagram,
            monitorMessenger = entity.monitorMessenger,
            monitorViber = entity.monitorViber,
            monitorVk = entity.monitorVk,
            monitorOk = entity.monitorOk,
        )

    fun toEntity(settings: AppSettings): AppSettingsEntity =
        AppSettingsEntity(
            id = AppSettingsEntity.DEFAULT_ID,
            language = settings.language.storageValue,
            sensitivity = settings.sensitivity.storageValue,
            monitorSms = settings.monitorSms,
            monitorWhatsapp = settings.monitorWhatsapp,
            monitorTelegram = settings.monitorTelegram,
            monitorInstagram = settings.monitorInstagram,
            monitorMessenger = settings.monitorMessenger,
            monitorViber = settings.monitorViber,
            monitorVk = settings.monitorVk,
            monitorOk = settings.monitorOk,
        )
}
