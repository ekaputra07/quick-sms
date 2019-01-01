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
import com.balicodes.quicksms.entity.SendSmsEntity
import com.balicodes.quicksms.entity.SendStatusEntity
import com.balicodes.quicksms.repository.SendSmsRepository
import com.balicodes.quicksms.repository.SendStatusRepository

class HistoryViewModel(val app: Application) : AndroidViewModel(app) {

    private val sendSmsRepository = SendSmsRepository(app)
    private val sendStatusRepository = SendStatusRepository(app)
    private var selectedSendHistory: MutableLiveData<String> = MutableLiveData()

    private var sendStatusList = Transformations.switchMap(selectedSendHistory, sendStatusRepository::loadAllBySendId)
    private var sendHistory = Transformations.switchMap(selectedSendHistory, sendSmsRepository::loadById)

    fun setSelectedSendHistory(id: String) {
        selectedSendHistory.value = id
    }

    fun resetSelectedSendHistory() {
        selectedSendHistory.value = ""
    }

    fun getHistoryList() = sendSmsRepository.loadAll(0)
    fun getSelectedHistoryEntity(): LiveData<SendSmsEntity> = sendHistory
    fun getSendingStatusList(): LiveData<List<SendStatusEntity>> = sendStatusList
}