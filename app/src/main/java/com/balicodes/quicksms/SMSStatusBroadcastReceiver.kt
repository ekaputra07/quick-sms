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

package com.balicodes.quicksms

import android.app.Activity
import android.app.Application
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.Observer
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.balicodes.quicksms.entity.SendStatusEntity
import com.balicodes.quicksms.model.Status
import com.balicodes.quicksms.repository.SendStatusRepository
import java.util.logging.Logger

class SMSStatusBroadcastReceiver : BroadcastReceiver(), LifecycleOwner {

    lateinit var sendStatusRepository: SendStatusRepository
    lateinit var lifecycleRegistry: LifecycleRegistry

    companion object {
        val LOG: Logger = Logger.getLogger(SMSStatusBroadcastReceiver::class.java.name)
    }

    override fun onReceive(context: Context, intent: Intent) {
        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.markState(Lifecycle.State.STARTED)

        sendStatusRepository = SendStatusRepository(context.applicationContext as Application)

        val action = intent.getStringExtra("action")
        //val sendId = intent.getStringExtra(Config.SEND_ID_EXTRA_KEY)
        val sendStatusId = intent.getStringExtra(Config.SEND_STATUS_ID_EXTRA_KEY)

        if (resultCode == Activity.RESULT_OK) {
            if (action == Config.SENT_STATUS_ACTION) {
                LOG.info("SENT: $sendStatusId")
                updateSendStatusById(sendStatusRepository, sendStatusId, Status.SENT)
            }

            if (action == Config.DELIVERY_STATUS_ACTION) {
                LOG.info("DELIVERED: $sendStatusId")
                updateSendStatusById(sendStatusRepository, sendStatusId, Status.DELIVERED)
            }
        } else {
            if (action == Config.SENT_STATUS_ACTION) {
                LOG.info("NOT SENT: $sendStatusId")
                updateSendStatusById(sendStatusRepository, sendStatusId, Status.PENDING)
            }

            if (action == Config.DELIVERY_STATUS_ACTION) {
                LOG.info("NOT DELIVERED: $sendStatusId")
                updateSendStatusById(sendStatusRepository, sendStatusId, Status.PENDING)
            }
        }
    }

    private fun updateSendStatusById(repository: SendStatusRepository, id: String, status: Status) {
        sendStatusRepository.loadById(id).observe(this, Observer { entity ->
            entity?.let {
                if (it.status != status) {
                    it.status = status
                    repository.update(it, this::afterSendStatusUpdated);
                }
            }
        })
    }

    private fun afterSendStatusUpdated(entity: SendStatusEntity) {
        LOG.info("SendStatus ${entity.id} updated to ${entity.status.name}")
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }
}
