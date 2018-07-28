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

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import java.util.*

/*
 Since this table will be used as sending history, we don't add reference to the MessageEntity here
 so even if the message has been deleted, we still have some details about it.
 */
@Entity(tableName = SendSmsEntity.TABLE_NAME)
class SendSmsEntity(@PrimaryKey var id: String,
                    var name: String,
                    var message: String,
                    @ColumnInfo(name = "num_recipients") var numRecipients: Int,
                    var created: Date) {

    companion object {
        const val TABLE_NAME = "SendSms"

        fun generateId(): String {
            return "send_%s".format(UUID.randomUUID().toString())
        }
    }

    override fun toString(): String {
        return "SendSms: id=%s, name=%s, numRecipients=%s, created=%s, message=%s"
                .format(id, name, numRecipients, created, message)
    }
}