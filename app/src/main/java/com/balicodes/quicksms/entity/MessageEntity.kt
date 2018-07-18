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

package com.balicodes.quicksms.entity

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.balicodes.quicksms.SMSItem

@Entity(tableName = MessageEntity.TABLE_NAME)
class MessageEntity(@PrimaryKey(autoGenerate = true) var id: Long?,
                    var title: String?,
                    var number: String?,
                    var message: String?,
                    var confirm: String?) {

    fun toSmsItem(): SMSItem{
        return SMSItem(id!!, title!!, number!!, message!!, confirm!!)
    }

    companion object {
        const val TABLE_NAME = "sms"
    }
}