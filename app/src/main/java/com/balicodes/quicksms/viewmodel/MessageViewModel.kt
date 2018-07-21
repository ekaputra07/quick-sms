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

package com.balicodes.quicksms.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import com.balicodes.quicksms.SMSItem
import com.balicodes.quicksms.entity.MessageEntity
import com.balicodes.quicksms.repository.MessageRepository

class MessageViewModel(val app: Application) : AndroidViewModel(app) {
    private val messageRepository = MessageRepository(app)

    //-- Used by Messages list
    private var orderBy: MutableLiveData<String> = MutableLiveData()
    private val messages = Transformations.switchMap(orderBy, messageRepository::getMessages)

    //-- Used by Message form
    private var selectedMessage: MutableLiveData<MessageEntity?> = MutableLiveData()

    //-- provide access to repository

    fun insertMessages(vararg messages: MessageEntity) = messageRepository.insertMessages(*messages)
    fun insertMessage(message: MessageEntity, listener: (MessageEntity) -> Unit) = messageRepository.insertMessage(message, listener)
    fun updateMessage(message: MessageEntity, listener: (MessageEntity) -> Unit) = messageRepository.updateMessage(message, listener)
    fun deleteMessage(message: MessageEntity, listener: (MessageEntity) -> Unit) = messageRepository.deleteMessage(message, listener)

    fun copyMessage(fromMessage: MessageEntity, listener: (MessageEntity) -> Unit) {
        val copied = fromMessage
        copied.id = null
        copied.title = fromMessage.title + " (copy)"
        copied.addShortcut = SMSItem.SHORTCUT_NO

        insertMessage(copied, listener::invoke)
    }

    //-- Messages list: getters and setters

    fun getMessages(): LiveData<List<MessageEntity>> = messages

    fun setOrderBy(order: String) {
        orderBy.value = order
    }

    //-- Message form: getters and setters

    fun selectMessage(message: MessageEntity?) {
        selectedMessage.value = message
    }

    fun getSelectedMessage(): LiveData<MessageEntity?> = selectedMessage
}