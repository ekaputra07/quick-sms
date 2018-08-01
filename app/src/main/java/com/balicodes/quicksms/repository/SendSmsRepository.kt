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
import com.balicodes.quicksms.dao.SendSmsDao
import com.balicodes.quicksms.database.AppDatabase
import com.balicodes.quicksms.entity.SendSmsEntity
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class SendSmsRepository(val app: Application) {

    private val sendSmsDao: SendSmsDao = AppDatabase.getInstance(app)!!.sendSmsDao()

    fun loadAll(offset: Int): LiveData<List<SendSmsEntity>> = sendSmsDao.loadAll(offset)

    fun loadById(id: String): LiveData<SendSmsEntity> = sendSmsDao.loadById(id)

    fun insert(entity: SendSmsEntity, listener: (SendSmsEntity) -> Unit) = doAsync {
        sendSmsDao.insert(entity)
        uiThread { listener.invoke(entity) }
    }

    fun delete(entity: SendSmsEntity, listener: (SendSmsEntity) -> Unit) = doAsync {
        sendSmsDao.delete(entity)
        uiThread { listener.invoke(entity) }
    }
}