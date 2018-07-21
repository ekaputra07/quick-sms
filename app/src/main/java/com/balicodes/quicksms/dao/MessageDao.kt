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
import com.balicodes.quicksms.entity.MessageEntity

@Dao
interface MessageDao {

    @Query("SELECT * FROM " + MessageEntity.TABLE_NAME + " ORDER BY title")
    fun loadMessagesOrderByTitle(): LiveData<List<MessageEntity>>

    @Query("SELECT * FROM " + MessageEntity.TABLE_NAME + " ORDER BY id")
    fun loadMessagesOrderById(): LiveData<List<MessageEntity>>

    @Query("SELECT * FROM " + MessageEntity.TABLE_NAME + " ORDER BY id DESC")
    fun loadMessagesOrderByIdDesc(): LiveData<List<MessageEntity>>

    @Query("SELECT * FROM " + MessageEntity.TABLE_NAME + " WHERE id = :id")
    fun loadMessage(id: Long): LiveData<MessageEntity>

    @Insert
    fun insertMessage(message: MessageEntity): Long

    @Insert
    fun insertMessages(vararg messages: MessageEntity)

    @Delete
    fun deleteMessage(message: MessageEntity)

    @Update
    fun updateMessage(message: MessageEntity)

    @Query("SELECT COUNT(*) FROM " + MessageEntity.TABLE_NAME)
    fun countMessages(): Int
}