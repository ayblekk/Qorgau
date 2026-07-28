package kz.qorgau.scamguardian.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kz.qorgau.scamguardian.data.local.db.dao.AppSettingsDao
import kz.qorgau.scamguardian.data.local.db.entity.AppSettingsEntity
import kz.qorgau.scamguardian.data.local.db.mapper.AppSettingsMapper
import kz.qorgau.scamguardian.domain.model.AppSettings
import kz.qorgau.scamguardian.domain.repository.SettingsRepository

class SettingsRepositoryImpl(
    private val appSettingsDao: AppSettingsDao,
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> =
        appSettingsDao.observe().map { entity ->
            AppSettingsMapper.toDomain(entity ?: AppSettingsEntity.defaults())
        }

    override suspend fun getSettings(): AppSettings {
        val entity = appSettingsDao.get() ?: AppSettingsEntity.defaults().also {
            appSettingsDao.upsert(it)
        }
        return AppSettingsMapper.toDomain(entity)
    }

    override suspend fun updateSettings(settings: AppSettings) {
        appSettingsDao.upsert(AppSettingsMapper.toEntity(settings))
    }
}
