/*
 * Copyright (C) 2016-2018 Eka Putra
 *
 * Quick SMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.balicodes.quicksms.database

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.migration.Migration
import android.content.Context
import android.support.annotation.VisibleForTesting
import com.balicodes.quicksms.dao.MessageDao
import com.balicodes.quicksms.entity.MessageEntity

@Database(entities = [MessageEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao

    companion object {
        const val DATABASE_NAME: String = "quicksms.db"
        private var INSTANCE: AppDatabase? = null

        /*
         * Migration from version 1 (plain SQLite) to version 2 (Room).
         */
        @VisibleForTesting
        val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(database: SupportSQLiteDatabase) {
                // Room uses an own database hash to uniquely identify the database
                // Since version 1 does not use Room, it doesn't have the database hash associated.
                // By implementing a Migration class, we're telling Room that it should use the data
                // from version 1 to version 2.
                // If no migration is provided, then the tables will be dropped and recreated.
                // Since we didn't alter the table, there's nothing else to do here.
            }
        }

        /*
         * Returns our AppDatabase instance.
         */
        fun getInstance(context: Context): AppDatabase? {
            if (INSTANCE == null) {
                synchronized(AppDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                            .addMigrations(MIGRATION_1_2)
                            .build()
                }
            }

            return INSTANCE
        }
    }
}