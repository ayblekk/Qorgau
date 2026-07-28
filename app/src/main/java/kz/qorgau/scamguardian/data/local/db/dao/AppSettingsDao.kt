package kz.qorgau.scamguardian.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kz.qorgau.scamguardian.data.local.db.entity.AppSettingsEntity

@Dao
interface AppSettingsDao {

    @Query("SELECT * FROM app_settings WHERE id = :id LIMIT 1")
    fun observe(id: Int = AppSettingsEntity.DEFAULT_ID): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = :id LIMIT 1")
    suspend fun get(id: Int = AppSettingsEntity.DEFAULT_ID): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppSettingsEntity)
}
