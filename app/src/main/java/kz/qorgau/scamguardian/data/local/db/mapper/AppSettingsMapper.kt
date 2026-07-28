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
            rulesOnlyMode = entity.rulesOnlyMode,
            monitorSms = entity.monitorSms,
            monitorWhatsapp = entity.monitorWhatsapp,
            monitorTelegram = entity.monitorTelegram,
            modelEnabled = entity.modelEnabled,
            lastModelCheckEpochMs = entity.lastModelCheck,
        )

    fun toEntity(settings: AppSettings): AppSettingsEntity =
        AppSettingsEntity(
            id = AppSettingsEntity.DEFAULT_ID,
            language = settings.language.storageValue,
            sensitivity = settings.sensitivity.storageValue,
            rulesOnlyMode = settings.rulesOnlyMode,
            monitorSms = settings.monitorSms,
            monitorWhatsapp = settings.monitorWhatsapp,
            monitorTelegram = settings.monitorTelegram,
            modelEnabled = settings.modelEnabled,
            lastModelCheck = settings.lastModelCheckEpochMs,
        )
}
