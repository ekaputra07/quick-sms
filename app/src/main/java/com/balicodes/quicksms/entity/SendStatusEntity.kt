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

import android.arch.persistence.room.*
import com.balicodes.quicksms.model.Status
import java.util.*

@Entity(tableName = SendStatusEntity.TABLE_NAME,
        foreignKeys = [
            ForeignKey(entity = SendSmsEntity::class, parentColumns = ["id"],
                    childColumns = ["send_id"], onDelete = ForeignKey.CASCADE)
        ],
        indices = [Index("send_id", name = "status_send_id")])
class SendStatusEntity(@PrimaryKey var id: String,
                       @ColumnInfo(name = "send_id") var sendId: String,
                       var number: String,
                       var status: Status,
                       var created: Date,
                       var updated: Date) {

    companion object {
        const val TABLE_NAME = "SendStatus"

        fun generateId(): String {
            return "status_%s".format(UUID.randomUUID().toString())
        }
    }

    override fun toString(): String {
        return "SendStatusEntity: id=%s, sendId=%s, number=%s, status=%s, created=%s, updated=%s"
                .format(id, sendId, number, status, created, updated)
    }
}