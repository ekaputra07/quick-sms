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

package com.balicodes.quicksms.data.repository

import android.app.Application
import android.arch.lifecycle.LiveData
import com.balicodes.quicksms.data.dao.MessageDao
import com.balicodes.quicksms.data.database.AppDatabase
import com.balicodes.quicksms.data.entity.MessageEntity
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class MessageRepository(val app: Application) {

    private val messageDao: MessageDao = AppDatabase.getInstance(app)!!.messageDao()

    //-- getMessages and getMessage returns LiveData so its safe to call directly from main thread.

    fun getMessages(orderBy: String): LiveData<List<MessageEntity>> {
        return when (orderBy) {
            "title" -> messageDao.loadMessagesOrderByTitle()
            "id_desc" -> messageDao.loadMessagesOrderByIdDesc()
            else -> messageDao.loadMessagesOrderById()
        }
    }

    fun getMessage(id: Long): LiveData<MessageEntity> = messageDao.loadMessage(id)

    //-- For Insert, Update and Delete can't be called directly from main thread (performance reason)
    //-- Need to call it from separate thread. We use doAsync from 'org.jetbrains.anko:anko-commons'.

    fun insertMessage(message: MessageEntity, listener: (MessageEntity) -> Unit) {
        message.id = null
        doAsync {
            val id = messageDao.insertMessage(message)

            uiThread {
                message.id = id
                listener.invoke(message)
            }
        }
    }

    fun insertMessages(vararg messages: MessageEntity) {
        doAsync {
            messageDao.insertMessages(*messages)
        }
    }

    fun updateMessage(message: MessageEntity, listener: (MessageEntity) -> Unit) = doAsync {
        messageDao.updateMessage(message)
        uiThread { listener.invoke(message) }
    }

    fun deleteMessage(message: MessageEntity, listener: (MessageEntity) -> Unit) = doAsync {
        messageDao.deleteMessage(message)
        uiThread { listener.invoke(message) }
    }
}