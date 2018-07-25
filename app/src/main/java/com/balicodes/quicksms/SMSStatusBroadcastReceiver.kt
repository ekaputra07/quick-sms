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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.balicodes.quicksms.model.Recipient
import java.util.logging.Logger

class SMSStatusBroadcastReceiver : BroadcastReceiver() {

    companion object {
        val LOG: Logger = Logger.getLogger(SMSStatusBroadcastReceiver::class.java.name)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val bundle = intent.getBundleExtra(Config.RECIPIENT_EXTRA_KEY)

        if (resultCode == Activity.RESULT_OK) {
            if (action == Config.SENT_STATUS_ACTION && bundle != null) {
                val rec = Recipient.fromBundle(bundle)
                LOG.info("SENT: " + rec.number)
            }

            if (action == Config.DELIVERY_STATUS_ACTION && bundle != null) {
                val rec = Recipient.fromBundle(bundle)
                LOG.info("DELIVERED: " + rec.number)
            }
        } else {
            if (action == Config.SENT_STATUS_ACTION && bundle != null) {
                val rec = Recipient.fromBundle(bundle)
                LOG.info("NOT SENT: " + rec.number)
            }

            if (action == Config.DELIVERY_STATUS_ACTION && bundle != null) {
                val rec = Recipient.fromBundle(bundle)
                LOG.info("NOT DELIVERED: " + rec.number)
            }
        }
    }
}
