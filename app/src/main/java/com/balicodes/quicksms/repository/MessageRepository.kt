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

package com.balicodes.quicksms.repository

import android.app.Application
import android.arch.lifecycle.LiveData
import com.balicodes.quicksms.dao.MessageDao
import com.balicodes.quicksms.database.AppDatabase
import com.balicodes.quicksms.entity.MessageEntity
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class MessageRepository(val app: Application) {

    private val messageDao: MessageDao = AppDatabase.getInstance(app)!!.messageDao()

    //-- getMessages and getMessage returns LiveData so its safe to call directly from main thread.

    fun getMessages(orderBy: String): LiveData<List<MessageEntity>> {
        return when(orderBy){
            "title" -> messageDao.loadMessagesOrderByTitle()
            "id_desc" -> messageDao.loadMessagesOrderByIdDesc()
            else -> messageDao.loadMessagesOrderById()
        }
    }

    fun getMessage(id: Long): LiveData<MessageEntity> = messageDao.loadMessage(id)

    //-- For Insert, Update and Delete can't be called directly from main thread (performance reason)
    //-- Need to call it from separate thread. We use doAsync from 'org.jetbrains.anko:anko-commons'.

    fun insertMessage(message: MessageEntity, listener: (Long) -> Unit) {
        message.id = null
        doAsync {
            val id = messageDao.insertMessage(message)
            uiThread {
                listener.invoke(id)
            }
        }
    }

    fun updateMessage(message: MessageEntity) = doAsync { messageDao.updateMessage(message) }

    fun deleteMessage(message: MessageEntity) = doAsync { messageDao.deleteMessage(message) }
}