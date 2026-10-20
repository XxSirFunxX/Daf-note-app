package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProjectEntity::class,
        FolderEntity::class,
        CategoryEntity::class,
        NoteDefinitionEntity::class,
        CustomSymbolEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` TEXT NOT NULL, 
                        `titleFa` TEXT NOT NULL, 
                        `sortOrder` INTEGER NOT NULL DEFAULT 0, 
                        `isDefault` INTEGER NOT NULL DEFAULT 0, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `note_definitions` (
                        `id` TEXT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `symbolTypeName` TEXT NOT NULL, 
                        `categoryId` TEXT NOT NULL, 
                        `durationValue` TEXT NOT NULL DEFAULT '1/4', 
                        `isRingSound` INTEGER NOT NULL DEFAULT 0, 
                        `isDefault` INTEGER NOT NULL DEFAULT 0, 
                        `isHidden` INTEGER NOT NULL DEFAULT 0, 
                        `imagePath` TEXT, 
                        `createdAt` INTEGER NOT NULL DEFAULT 0, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `custom_symbols` (
                        `id` TEXT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `categoryId` TEXT NOT NULL DEFAULT 'CUSTOM', 
                        `isRingSound` INTEGER NOT NULL DEFAULT 0, 
                        `vectorData` TEXT, 
                        `imagePath` TEXT, 
                        `createdAt` INTEGER NOT NULL DEFAULT 0, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chaharkhat_score.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
