package kz.qorgau.scamguardian.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kz.qorgau.scamguardian.data.local.db.dao.AnalysisLogDao
import kz.qorgau.scamguardian.data.local.db.dao.AppSettingsDao
import kz.qorgau.scamguardian.data.local.db.entity.AnalysisLogEntity
import kz.qorgau.scamguardian.data.local.db.entity.AppSettingsEntity

/**
 * Local Room database (SCHEMA.md). Version 3 — extra messenger monitor toggles.
 * Message content never leaves the device.
 */
@Database(
    entities = [
        AnalysisLogEntity::class,
        AppSettingsEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class ScamGuardianDatabase : RoomDatabase() {

    abstract fun analysisLogDao(): AnalysisLogDao

    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        const val DATABASE_NAME: String = "scamguardian.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS app_settings_new (
                        id INTEGER NOT NULL,
                        language TEXT NOT NULL DEFAULT 'ru',
                        sensitivity TEXT NOT NULL DEFAULT 'medium',
                        monitor_sms INTEGER NOT NULL DEFAULT 1,
                        monitor_whatsapp INTEGER NOT NULL DEFAULT 1,
                        monitor_telegram INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO app_settings_new (id, language, sensitivity, monitor_sms, monitor_whatsapp, monitor_telegram)
                    SELECT id, language, sensitivity, monitor_sms, monitor_whatsapp, monitor_telegram
                    FROM app_settings
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE app_settings")
                db.execSQL("ALTER TABLE app_settings_new RENAME TO app_settings")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE app_settings ADD COLUMN monitor_instagram INTEGER NOT NULL DEFAULT 1",
                )
                db.execSQL(
                    "ALTER TABLE app_settings ADD COLUMN monitor_messenger INTEGER NOT NULL DEFAULT 1",
                )
                db.execSQL(
                    "ALTER TABLE app_settings ADD COLUMN monitor_viber INTEGER NOT NULL DEFAULT 1",
                )
                db.execSQL(
                    "ALTER TABLE app_settings ADD COLUMN monitor_vk INTEGER NOT NULL DEFAULT 1",
                )
                db.execSQL(
                    "ALTER TABLE app_settings ADD COLUMN monitor_ok INTEGER NOT NULL DEFAULT 1",
                )
            }
        }

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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                // Stage 1: prefer open over crash if a device has a broken/partial schema.
                .fallbackToDestructiveMigration()
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
                    id, language, sensitivity,
                    monitor_sms, monitor_whatsapp, monitor_telegram,
                    monitor_instagram, monitor_messenger, monitor_viber,
                    monitor_vk, monitor_ok
                ) VALUES (1, 'ru', 'medium', 1, 1, 1, 1, 1, 1, 1, 1)
                """.trimIndent(),
            )
        }
    }
}
