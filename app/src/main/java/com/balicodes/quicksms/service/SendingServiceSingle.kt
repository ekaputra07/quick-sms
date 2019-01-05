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

package com.balicodes.quicksms.service

import android.app.IntentService
import android.app.PendingIntent
import android.content.Intent
import android.preference.PreferenceManager
import android.telephony.SmsManager
import com.balicodes.quicksms.Config
import com.balicodes.quicksms.R
import com.balicodes.quicksms.data.repository.SendSmsRepository
import com.balicodes.quicksms.data.repository.SendStatusRepository
import java.util.logging.Logger

class SendingServiceSingle : IntentService("SendingServiceSingle") {

    companion object {
        private val LOG = Logger.getLogger(SendingServiceSingle::class.java.name)
    }

    lateinit var sendSmsRepository: SendSmsRepository
    lateinit var sendStatusRepository: SendStatusRepository

    override fun onCreate() {
        super.onCreate()

        sendSmsRepository = SendSmsRepository(application)
        sendStatusRepository = SendStatusRepository(application)
    }

    override fun onHandleIntent(intent: Intent) {
        val sendStatusId = intent.getStringExtra(Config.SEND_STATUS_ID_EXTRA_KEY)

        if (sendStatusId != null) {
            val sendStatus = sendStatusRepository.loadById(sendStatusId)
            if (sendStatus != null) {
                val sendSms = sendSmsRepository.loadById(sendStatus.sendId)
                if (sendSms != null) {

                    val number = sendStatus.number
                    val message = sendSms.message

                    try {
                        val smsManager = SmsManager.getDefault()
                        val sp = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                        val enableDeliveryReport = sp.getBoolean(getString(R.string.pref_enable_delivery_report_key), false)

                        // Create sent pending Intent
                        // explicit intent directly targeting your class (to comply with Android O)
                        val sentIntent = Intent(applicationContext, StatusBroadcastReceiver::class.java)

                        sentIntent.putExtra("action", Config.SENT_STATUS_ACTION)
                        sentIntent.putExtra(Config.SEND_ID_EXTRA_KEY, sendSms.id)
                        sentIntent.putExtra(Config.SEND_STATUS_ID_EXTRA_KEY, sendStatusId)

                        val sentPI = PendingIntent.getBroadcast(applicationContext, 0, sentIntent, PendingIntent.FLAG_UPDATE_CURRENT)

                        var deliveryPI: PendingIntent? = null
                        if (enableDeliveryReport) {
                            val deliveryIntent = Intent(applicationContext, StatusBroadcastReceiver::class.java)

                            deliveryIntent.putExtra("action", Config.DELIVERY_STATUS_ACTION)
                            deliveryIntent.putExtra(Config.SEND_ID_EXTRA_KEY, sendSms.id)
                            deliveryIntent.putExtra(Config.SEND_STATUS_ID_EXTRA_KEY, sendStatusId)

                            deliveryPI = PendingIntent.getBroadcast(applicationContext, 0, deliveryIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                        }

                        LOG.info("====> Sending to $number")
                        smsManager.sendTextMessage(number, null, message, sentPI, deliveryPI)
                        LOG.info("====> Finished sending")

                    } catch (e: SecurityException) {
                        LOG.warning("====> [Security] Error sending to $number")
                        LOG.warning(e.localizedMessage)
                    } catch (e: Exception) {
                        LOG.warning("====> Error sending to $number")
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}