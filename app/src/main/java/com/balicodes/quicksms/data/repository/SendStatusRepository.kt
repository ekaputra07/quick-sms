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
import com.balicodes.quicksms.data.dao.SendStatusDao
import com.balicodes.quicksms.data.database.AppDatabase
import com.balicodes.quicksms.data.entity.SendStatusEntity
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*

class SendStatusRepository(val app: Application) {

    private val sendStatusDao: SendStatusDao = AppDatabase.getInstance(app)!!.sendStatusDao()

    fun loadAllBySendIdAsync(sendId: String) = sendStatusDao.loadAllBySendIdAsync(sendId)

    fun loadById(id: String) = sendStatusDao.loadById(id)

    fun loadByIdAsync(id: String) = sendStatusDao.loadByIdAsync(id)

    fun insertManyAsync(vararg entities: SendStatusEntity, listener: (Array<SendStatusEntity>) -> Unit) = doAsync {
        sendStatusDao.insertMany(*entities)
        uiThread { listener.invoke(arrayOf(*entities)) }
    }

    fun insertAsync(entity: SendStatusEntity, listener: (SendStatusEntity) -> Unit) = doAsync {
        sendStatusDao.insert(entity)
        uiThread { listener.invoke(entity) }
    }

    fun updateAsync(entity: SendStatusEntity, listener: (SendStatusEntity) -> Unit) = doAsync {
        entity.updated = Date(System.currentTimeMillis())
        sendStatusDao.update(entity)
        uiThread { listener.invoke(entity) }
    }

    fun update(entity: SendStatusEntity) {
        entity.updated = Date(System.currentTimeMillis())
        sendStatusDao.update(entity)
    }

    fun deleteAsync(entity: SendStatusEntity, listener: (SendStatusEntity) -> Unit) = doAsync {
        sendStatusDao.delete(entity)
        uiThread { listener.invoke(entity) }
    }
}