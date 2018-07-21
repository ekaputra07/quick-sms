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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.telephony.SmsManager
import android.text.TextUtils
import android.util.Log
import com.balicodes.quicksms.entity.MessageEntity

import java.util.ArrayList

class SMSItem(val id: Long, val title: String, val number: String, val message: String, val shortcut: String) {

    var status: Int = STATUS_INACTIVE

    // status stats of current sending, stored as Parcelable object of recipient.
    // Why Parcelable? because we can easily sent it as Intent extras.
    val sentInfoList = ArrayList<Parcelable>()
    val failedInfoList = ArrayList<Parcelable>()
    val receivedInfoList = ArrayList<Parcelable>()

    val label: String
        get() {
            val recipientString = ArrayList<String>()
            val rec = SMSItem.parseReceiverCSV(number)
            for (r in rec) {
                if (r[0].isEmpty()) {
                    recipientString.add(r[1])
                } else {
                    recipientString.add(r[0])
                }
            }
            return TextUtils.join(", ", recipientString)
        }

    fun setSending() {
        // each time sending, clear current status.
        sentInfoList.clear()
        failedInfoList.clear()
        receivedInfoList.clear()

        status = STATUS_SENDING
    }

    fun addStatusInfo(statusType: Int, rec: Recipient) {
        // the first time we get status from BroadCast receiver, set sending status to LISTENING
        status = STATUS_LISTENING

        when (statusType) {
            STATUS_SENT -> this.sentInfoList.add(rec)
            STATUS_FAILED -> this.failedInfoList.add(rec)
            STATUS_DELIVERED -> this.receivedInfoList.add(rec)
        }
    }

    fun hasId(): Boolean {
        return id != 0L
    }

    fun totalRecipients(): Int {
        return SMSItem.parseReceiverCSV(number).size
    }

    fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putLong("id", id)
        bundle.putString("title", title)
        bundle.putString("number", number)
        bundle.putString("message", message)
        bundle.putString("shortcut", shortcut)
        return bundle
    }

    fun toEntity(): MessageEntity {
        return MessageEntity(id, title, number, message, shortcut)
    }

    fun send(context: Context, enableDeliveryReport: Boolean?) {
        val smsManager = SmsManager.getDefault()

        Log.d("SMSItem", "====> Delivery report: " + enableDeliveryReport!!)

        val recipients = SMSItem.parseReceiverCSV(this.number)

        var requestCode = 0

        for (recipient in recipients) {

            // create Recipient object and later will be added to PendingIntent extra.
            val rec = Recipient(id, recipient[0], recipient[1])

            try {
                Log.d("SMSItem", "====> Sending to " + recipient[1])

                // Create sent pending Intent
                val sentIntent = Intent(Config.SENT_STATUS_ACTION)
                sentIntent.putExtra(Config.RECIPIENT_EXTRA_KEY, rec.toBundle())
                val sentPI = PendingIntent.getBroadcast(context, requestCode, sentIntent, PendingIntent.FLAG_UPDATE_CURRENT)

                // Create delivery pending Intent, only if enabled
                var deliveryPI: PendingIntent? = null
                if (enableDeliveryReport) {
                    val deliveryIntent = Intent(Config.DELIVERY_STATUS_ACTION)
                    deliveryIntent.putExtra(Config.RECIPIENT_EXTRA_KEY, rec.toBundle())
                    deliveryPI = PendingIntent.getBroadcast(context, requestCode, deliveryIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                }

                smsManager.sendTextMessage(recipient[1], null, this.message, sentPI, deliveryPI)
            } catch (e: SecurityException) {
                Log.e("SMSItem", "====> [Security] Error sending to " + recipient[1])
                Log.e("SMSItem", e.localizedMessage)
            } catch (e: Exception) {
                Log.e("SMSItem", "====> Error sending to " + recipient[1])
                e.printStackTrace()
            }

            requestCode++
        }
    }

    companion object {
        const val STATUS_INACTIVE = 0
        const val STATUS_SENDING = 1
        const val STATUS_LISTENING = 2

        const val STATUS_SENT = 3
        const val STATUS_DELIVERED = 4
        const val STATUS_FAILED = 5

        const val SHORTCUT_YES = "YES"
        const val SHORTCUT_NO = "NO"

        fun fromBundle(bundle: Bundle): SMSItem? {
            try {
                return SMSItem(bundle.getLong("id"), bundle.getString("title"),
                        bundle.getString("number"), bundle.getString("message"), bundle.getString("shortcut"))
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return null
        }

        /* ---------------------------------------------------------------------------------------------
        Convert "Eka:0817476584,Vera:123456789"
        into [["Eka", "08174765840"], ["Vera","123456789"]]
        ----------------------------------------------------------------------------------------------*/
        fun parseReceiverCSV(numberCSV: String?): List<Array<String>> {
            // create a list of map.
            val list = ArrayList<Array<String>>()

            // always return at least on empty list
            if (numberCSV == null || numberCSV.isBlank()) {
                list.add(arrayOf("", ""))
                return list
            }

            for (contact in numberCSV.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                // handle legacy number format
                if (!contact.contains(":")) {
                    list.add(arrayOf("", contact))
                } else {
                    if (contact.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size != 2) {
                        list.add(arrayOf("", ""))
                    } else {
                        list.add(contact.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                    }
                }
            }
            return list
        }
    }
}
