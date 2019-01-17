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

package com.balicodes.quicksms.ui.history

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import com.balicodes.quicksms.data.entity.SendSmsEntity
import com.balicodes.quicksms.data.entity.SendStatusEntity
import com.balicodes.quicksms.data.repository.SendSmsRepository
import com.balicodes.quicksms.data.repository.SendStatusRepository

class HistoryViewModel(val app: Application) : AndroidViewModel(app) {

    private val sendSmsRepository = SendSmsRepository(app)
    private val sendStatusRepository = SendStatusRepository(app)
    private var selectedSendHistory: MutableLiveData<String> = MutableLiveData()

    private var sendStatusList = Transformations.switchMap(selectedSendHistory, sendStatusRepository::loadAllBySendIdAsync)
    private var sendHistory = Transformations.switchMap(selectedSendHistory, sendSmsRepository::loadByIdAsync)

    fun setSelectedSendHistory(id: String) {
        selectedSendHistory.value = id
    }

    fun resetSelectedSendHistory() {
        selectedSendHistory.value = ""
    }

    fun getHistoryList() = sendSmsRepository.loadAll(0)
    fun getSelectedHistoryEntity(): LiveData<SendSmsEntity> = sendHistory
    fun getSendingStatusList(): LiveData<List<SendStatusEntity>> = sendStatusList

    fun updateSendingStatus(sendStatusEntity: SendStatusEntity, listener: (SendStatusEntity) -> Unit) {
        sendStatusRepository.updateAsync(sendStatusEntity, listener::invoke)
    }
}