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

package com.balicodes.quicksms.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import com.balicodes.quicksms.entity.SendStatusEntity

@Dao
interface SendStatusDao {

    @Query("SELECT * FROM " + SendStatusEntity.TABLE_NAME + " ORDER BY created ASC")
    fun loadAll(): LiveData<List<SendStatusEntity>>

    @Query("SELECT * FROM " + SendStatusEntity.TABLE_NAME + " WHERE send_id = :sendId ORDER BY created ASC")
    fun loadAllBySendId(sendId: String): LiveData<List<SendStatusEntity>>

    @Query("SELECT * FROM " + SendStatusEntity.TABLE_NAME + " WHERE id = :id")
    fun loadById(id: String): LiveData<SendStatusEntity>

    @Insert
    fun insert(entity: SendStatusEntity)

    @Insert
    fun insertMany(vararg entities: SendStatusEntity)

    @Delete
    fun delete(entity: SendStatusEntity)

    @Update
    fun update(entity: SendStatusEntity)
}