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

package com.balicodes.quicksms.data.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import com.balicodes.quicksms.data.entity.SendSmsEntity

@Dao
interface SendSmsDao {

    @Query("SELECT * FROM " + SendSmsEntity.TABLE_NAME + " ORDER BY created DESC LIMIT 20 OFFSET :offset")
    fun loadAll(offset: Int): LiveData<List<SendSmsEntity>>

    @Query("SELECT * FROM " + SendSmsEntity.TABLE_NAME + " WHERE id = :id")
    fun loadByIdAsync(id: String): LiveData<SendSmsEntity>

    @Query("SELECT * FROM " + SendSmsEntity.TABLE_NAME + " WHERE id = :id")
    fun loadById(id: String): SendSmsEntity?

    @Insert
    fun insert(entity: SendSmsEntity)

    @Delete
    fun delete(entity: SendSmsEntity)
}