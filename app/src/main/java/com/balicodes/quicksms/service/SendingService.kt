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
import com.balicodes.quicksms.Config
import com.balicodes.quicksms.R
import com.balicodes.quicksms.model.Recipient
import com.balicodes.quicksms.model.SMSItem
import com.balicodes.quicksms.repository.MessageRepository
import com.balicodes.quicksms.util.Notification
import java.lang.Exception
import java.util.logging.Logger

class SendingService : IntentService("SendingService") {

    lateinit var messageRepository: MessageRepository
    lateinit var beep: MediaPlayer

    companion object {
        val LOG: Logger = Logger.getLogger(SendingService::class.java.name)
    }

    override fun onCreate() {
        super.onCreate()
        messageRepository = MessageRepository(application)

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

        LOG.info("SMSItem ====> Delivery report: $enableDeliveryReport")

        val recipients = SMSItem.parseReceiverCSV(item.number)

        for ((index, recipient) in recipients.withIndex()) {
            // create Recipient object and later will be added to PendingIntent extra.
            val rec = Recipient(item.id, recipient[0], recipient[1])

            try {
                SMSItem.LOG.info("SMSItem ====> Sending to " + recipient[1])

                // Create sent pending Intent
                val sentIntent = Intent(Config.SENT_STATUS_ACTION)
                sentIntent.putExtra(Config.RECIPIENT_EXTRA_KEY, rec.toBundle())
                val sentPI = PendingIntent.getBroadcast(applicationContext, index, sentIntent, PendingIntent.FLAG_UPDATE_CURRENT)

                // Create delivery pending Intent, only if enabled
                var deliveryPI: PendingIntent? = null
                if (enableDeliveryReport) {
                    val deliveryIntent = Intent(Config.DELIVERY_STATUS_ACTION)
                    deliveryIntent.putExtra(Config.RECIPIENT_EXTRA_KEY, rec.toBundle())
                    deliveryPI = PendingIntent.getBroadcast(applicationContext, index, deliveryIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                }

                smsManager.sendTextMessage(recipient[1], null, item.message, sentPI, deliveryPI)
            } catch (e: SecurityException) {
                SMSItem.LOG.warning("SMSItem ====> [Security] Error sending to " + recipient[1])
                SMSItem.LOG.warning(e.localizedMessage)
            } catch (e: Exception) {
                SMSItem.LOG.warning("SMSItem ====> Error sending to " + recipient[1])
                e.printStackTrace()
            }

            LOG.info("====> Finished sending")

            // Show notification
            Notification.show(this,
                    1,
                    "Sending \"" + item.title + "\"...",
                    "Success 0, Failed 1 of 1 recipients",
                    Notification.getContentIntentMain(this))
        }
    }
}
