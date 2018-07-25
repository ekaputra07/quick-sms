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

package com.balicodes.quicksms.model

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsManager
import android.text.TextUtils
import com.balicodes.quicksms.Config
import com.balicodes.quicksms.entity.MessageEntity
import java.util.*
import java.util.logging.Logger

class SMSItem(val id: Long, val title: String, val number: String, val message: String, val shortcut: String) {

    val label: String
        get() {
            val recipientString = ArrayList<String>()
            val rec = parseReceiverCSV(number)
            for (r in rec) {
                if (r[0].isEmpty()) {
                    recipientString.add(r[1])
                } else {
                    recipientString.add(r[0])
                }
            }
            return TextUtils.join(", ", recipientString)
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

        LOG.info("SMSItem ====> Delivery report: " + enableDeliveryReport!!)

        val recipients = parseReceiverCSV(this.number)

        var requestCode = 0

        for (recipient in recipients) {

            // create Recipient object and later will be added to PendingIntent extra.
            val rec = Recipient(id, recipient[0], recipient[1])

            try {
                LOG.info("SMSItem ====> Sending to " + recipient[1])

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
                LOG.warning("SMSItem ====> [Security] Error sending to " + recipient[1])
                LOG.warning(e.localizedMessage)
            } catch (e: Exception) {
                LOG.warning("SMSItem ====> Error sending to " + recipient[1])
                e.printStackTrace()
            }

            requestCode++
        }
    }

    companion object {
        const val SHORTCUT_YES = "YES"
        const val SHORTCUT_NO = "NO"

        val LOG: Logger = Logger.getLogger(SMSItem::class.java.name)

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
