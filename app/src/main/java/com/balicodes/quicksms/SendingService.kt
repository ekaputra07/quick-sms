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

import android.app.IntentService
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import java.lang.Exception

class SendingService : IntentService("SendingService") {

    private val TAG = javaClass.simpleName

    override fun onHandleIntent(intent: Intent) {
        try {
            Log.d(TAG, "====> Start sending")
            val bundle: Bundle? = intent.getBundleExtra(Config.SMS_BUNDLE_EXTRA_KEY)

            if (bundle != null) {
                val sp = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val enableDeliveryReport = sp.getBoolean(getString(R.string.pref_enable_delivery_report_key), false)

                val item = SMSItem.fromBundle(bundle)
                item?.send(applicationContext, enableDeliveryReport)
            }
            Log.d(TAG, "====> Finished sending")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
