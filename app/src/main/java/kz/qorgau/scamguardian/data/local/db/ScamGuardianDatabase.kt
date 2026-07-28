package kz.qorgau.scamguardian.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kz.qorgau.scamguardian.data.local.db.dao.AnalysisLogDao
import kz.qorgau.scamguardian.data.local.db.dao.AppSettingsDao
import kz.qorgau.scamguardian.data.local.db.entity.AnalysisLogEntity
import kz.qorgau.scamguardian.data.local.db.entity.AppSettingsEntity

/**
 * Local Room database (SCHEMA.md). Version 1 — first Stage 1 release.
 * Message content never leaves the device.
 */
@Database(
    entities = [
        AnalysisLogEntity::class,
        AppSettingsEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ScamGuardianDatabase : RoomDatabase() {

    abstract fun analysisLogDao(): AnalysisLogDao

    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        const val DATABASE_NAME: String = "scamguardian.db"

        @Volatile
        private var instance: ScamGuardianDatabase? = null

        fun getInstance(context: Context): ScamGuardianDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): ScamGuardianDatabase {
            return Room.databaseBuilder(
                context,
                ScamGuardianDatabase::class.java,
                DATABASE_NAME,
            )
                .addCallback(SeedCallback)
                .build()
        }
    }

    /**
     * Seeds the single-row AppSettings table on first create (SCHEMA.md §3.2).
     */
    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            db.execSQL(
                """
                INSERT OR IGNORE INTO app_settings (
                    id, language, sensitivity, rules_only_mode,
                    monitor_sms, monitor_whatsapp, monitor_telegram,
                    model_enabled, last_model_check
                ) VALUES (1, 'ru', 'medium', 0, 1, 1, 1, 1, 0)
                """.trimIndent(),
            )
        }
    }
}
