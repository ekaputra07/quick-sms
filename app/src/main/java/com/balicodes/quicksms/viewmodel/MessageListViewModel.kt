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
import android.preference.PreferenceManager
import com.balicodes.quicksms.R
import com.balicodes.quicksms.entity.MessageEntity
import com.balicodes.quicksms.repository.MessageRepository

class MessageListViewModel(val app: Application) : AndroidViewModel(app) {

    private var messageRepository: MessageRepository = MessageRepository(app)
    private var messages: LiveData<List<MessageEntity>>? = null

    init {
        val ctx = app.applicationContext
        val sp = PreferenceManager.getDefaultSharedPreferences(ctx)
        val sortBy = sp.getString(ctx.getString(R.string.pref_sort_by_key), ctx.getString(R.string.pref_sort_by_default_value))

        messages = messageRepository.getMessages(sortBy)
    }

    fun getMessages(): LiveData<List<MessageEntity>>? {
        return messages
    }

    fun insertMessage(message: MessageEntity, listener: (Long) -> Unit){
        messageRepository.insertMessage(message, listener)
    }

    fun updateMessage(message: MessageEntity){
        messageRepository.updateMessage(message)
    }

    fun deleteMessage(message: MessageEntity){
        messageRepository.deleteMessage(message)
    }

    fun copyMessage(fromMessage: MessageEntity, listener: (MessageEntity) -> Unit){
        var copied = fromMessage
        copied.id = null
        copied.title = fromMessage.title + " (copy)"

        insertMessage(copied, {
            copied.id = it
            listener.invoke(copied)
        })
    }
}