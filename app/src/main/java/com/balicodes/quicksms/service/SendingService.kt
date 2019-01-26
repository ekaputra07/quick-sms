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
import android.media.MediaPlayer
import android.preference.PreferenceManager
import android.telephony.SmsManager
import android.widget.Toast
import com.balicodes.quicksms.Config
import com.balicodes.quicksms.R
import com.balicodes.quicksms.data.entity.SendSmsEntity
import com.balicodes.quicksms.data.entity.SendStatusEntity
import com.balicodes.quicksms.data.model.SMSItem
import com.balicodes.quicksms.data.model.Status
import com.balicodes.quicksms.data.repository.SendSmsRepository
import com.balicodes.quicksms.data.repository.SendStatusRepository
import java.util.*
import java.util.logging.Logger

class SendingService : IntentService("SendingService") {

    lateinit var sendSmsRepository: SendSmsRepository
    lateinit var sendStatusRepository: SendStatusRepository
    lateinit var beep: MediaPlayer

    companion object {
        val LOG: Logger = Logger.getLogger(SendingService::class.java.name)
    }

    override fun onCreate() {
        super.onCreate()
        sendSmsRepository = SendSmsRepository(application)
        sendStatusRepository = SendStatusRepository(application)

        beep = MediaPlayer.create(this, R.raw.beep)
        beep.setVolume(0.5.toFloat(), 0.5.toFloat())
        beep.setOnCompletionListener { beep.release() }
    }

    override fun onHandleIntent(intent: Intent) {
        try {
            LOG.info("====> Start sending")

            val bundle = intent.getBundleExtra(Config.SMS_BUNDLE_EXTRA_KEY)

            bundle?.let {
                val sp = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val enableDeliveryReport = sp.getBoolean(getString(R.string.pref_enable_delivery_report_key), false)

                val playBeep = sp.getBoolean(getString(R.string.pref_enable_beep_key), true)
                if (playBeep) beep.start()

                SMSItem.fromBundle(it)?.let { sendSms(it, enableDeliveryReport) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendSms(item: SMSItem, enableDeliveryReport: Boolean) {

        val smsManager = SmsManager.getDefault()

        LOG.info("====> Delivery report: $enableDeliveryReport")

        val recipients = SMSItem.parseReceiverCSV(item.number)
        val sendId = SendSmsEntity.generateId()
        val sendDate = Date(System.currentTimeMillis())
        val sendSmsEntity = SendSmsEntity(sendId, item.title, item.message, recipients.size, sendDate)

        sendSmsRepository.insert(sendSmsEntity, {

            for ((index, recipient) in recipients.withIndex()) {
                val number = recipient[1]

                try {
                    // Create send status and send the message.
                    val sendStatusId = SendStatusEntity.generateId()
                    val now = Date(System.currentTimeMillis())
                    val sendStatusEntity = SendStatusEntity(sendStatusId, it.id, number, Status.SENDING, now, now)
                    sendStatusRepository.insertAsync(sendStatusEntity, { entity ->

                        // Create sent pending Intent
                        // explicit intent directly targeting your class (to comply with Android O)
                        val sentIntent = Intent(applicationContext, StatusBroadcastReceiver::class.java)

                        sentIntent.putExtra("action", Config.SENT_STATUS_ACTION)
                        sentIntent.putExtra("notification_id", sendDate.time.toInt())
                        sentIntent.putExtra("recipient_count", recipients.size)
                        sentIntent.putExtra(Config.SEND_ID_EXTRA_KEY, sendId)
                        sentIntent.putExtra(Config.SEND_STATUS_ID_EXTRA_KEY, sendStatusId)

                        val sentPI = PendingIntent.getBroadcast(applicationContext, index, sentIntent, PendingIntent.FLAG_UPDATE_CURRENT)

                        // Create delivery pending Intent, only if enabled
                        // explicit intent directly targeting your class (to comply with Android O)
                        var deliveryPI: PendingIntent? = null
                        if (enableDeliveryReport) {
                            val deliveryIntent = Intent(applicationContext, StatusBroadcastReceiver::class.java)

                            deliveryIntent.putExtra("action", Config.DELIVERY_STATUS_ACTION)
                            sentIntent.putExtra("notification_id", sendDate.time.toInt())
                            sentIntent.putExtra("recipient_count", recipients.size)
                            deliveryIntent.putExtra(Config.SEND_ID_EXTRA_KEY, sendId)
                            deliveryIntent.putExtra(Config.SEND_STATUS_ID_EXTRA_KEY, sendStatusId)

                            deliveryPI = PendingIntent.getBroadcast(applicationContext, index, deliveryIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                        }

                        LOG.info("====> Sending to " + number)
                        smsManager.sendTextMessage(number, null, item.message, sentPI, deliveryPI)
                    })

                } catch (e: SecurityException) {
                    LOG.warning("====> [Security] Error sending to $number")
                    LOG.warning(e.localizedMessage)
                } catch (e: Exception) {
                    LOG.warning("====> Error sending to $number")
                    e.printStackTrace()
                }
            }

            Toast.makeText(this, "Your message is being sent", Toast.LENGTH_LONG).show()
        })
    }
}
