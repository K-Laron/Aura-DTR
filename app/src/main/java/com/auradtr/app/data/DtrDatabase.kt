package com.auradtr.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Profile::class, TimeLog::class], version = 3, exportSchema = true)
@TypeConverters(Converters::class)
abstract class DtrDatabase : RoomDatabase() {
    abstract fun dtrDao(): DtrDao

    companion object {
        @Volatile
        private var INSTANCE: DtrDatabase? = null

        /**
         * Standard production migration declaration pathway.
         * Safe schema transitions must execute database modifications cleanly without drop-table loss.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // If we add columns or tables, we place alter table SQL command statements here.
                // For example:
                // db.execSQL("ALTER TABLE profile ADD COLUMN signatureDataUrl TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE time_logs ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): DtrDatabase {
            return INSTANCE ?: synchronized(this) {
                // CRITICAL: Destructive migration has been disabled to prevent automatic
                // student OJT timecard database erasure on release deployments.
                // Always increment versions and add explicit migrations via .addMigrations(...) below.
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DtrDatabase::class.java,
                    "dtr_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
