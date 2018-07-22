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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.balicodes.quicksms.model.Recipient

class SMSStatusBroadcastReceiver : BroadcastReceiver() {

    private val TAG = javaClass.simpleName

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val bundle = intent.extras

        if (bundle != null) {
            val rec = bundle.get(Config.RECIPIENT_EXTRA_KEY) as Recipient

            if (action == Config.SENT_STATUS_ACTION) {
                Log.d(TAG, "====> SENT STATUS RECEIVED FOR " + rec.number)
            }

            if (action == Config.DELIVERY_STATUS_ACTION) {
                Log.d(TAG, "====> DELIVERY STATUS RECEIVED FOR " + rec.number)
            }
        }
    }
}
