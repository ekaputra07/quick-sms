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
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import com.balicodes.quicksms.model.Recipient

import java.util.ArrayList

class ResendActivity : AppCompatActivity() {
    private val TAG = "ResendActivity"
    private val RECIPIENT_INDEX_KEY = "RECIPIENT_INDEX"

    private var recipients: ArrayList<Recipient>? = ArrayList()
    private var resendListAdapter: ResendListAdapter? = null
    private var message: String? = null
    private var sentReceiver: SentReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resend)

        val intent = intent
        val bundle = intent.extras
        if (bundle != null) {
            recipients = bundle.getParcelableArrayList(Config.RECIPIENT_PARCELS_EXTRA_KEY)
            message = bundle.getString(Config.SMS_MESSAGE_EXTRA_KEY)
        }

        resendListAdapter = ResendListAdapter()
        val listView = findViewById<ListView>(R.id.resendList)
        listView.adapter = resendListAdapter
    }

    private fun resendSMS(index: Int) {

        val rec = recipients!![index]
        rec.sending = true
        rec.sent = false

        // notify list one of its recipient status are sending.
        recipients!![index] = rec
        resendListAdapter!!.notifyDataSetChanged()

        Log.d(this.javaClass.name, rec.number + " - " + message)

        val smsManager = SmsManager.getDefault()

        // Create sent pending Intent
        val sentIntent = Intent(Config.SENT_STATUS_ACTION)
        sentIntent.putExtra(Config.RECIPIENT_EXTRA_KEY, rec)
        sentIntent.putExtra(RECIPIENT_INDEX_KEY, index)

        val sentPI = PendingIntent.getBroadcast(this, 0, sentIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        smsManager.sendTextMessage(rec.number, null, message, sentPI, null) // only listen for sent status
    }

    private inner class ResendListAdapter : BaseAdapter() {

        override fun getCount(): Int {
            return recipients!!.size
        }

        override fun getItem(position: Int): Any {
            return recipients!![position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            if (convertView == null) {
                val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                convertView = inflater.inflate(R.layout.resend_list_item, parent, false)
            }

            val recipient = getItem(position) as Recipient

            val resendNumber = convertView!!.findViewById<TextView>(R.id.resendNumber)
            resendNumber.text = recipient.number

            val resendName = convertView.findViewById<TextView>(R.id.resendName)
            resendName.text = recipient.name

            if (recipient.name == "")
                resendName.text = "N/A"

            val progressBar = convertView.findViewById<ProgressBar>(R.id.progressBar)
            val successImg = convertView.findViewById<ImageView>(R.id.resendSuccess)
            val retryBtn = convertView.findViewById<Button>(R.id.resendBtn)

            retryBtn.setOnClickListener { resendSMS(position) }

            // togle buttons and progressbar display based on recepient status.
            if (recipient.isSending) {
                progressBar.visibility = View.VISIBLE
                retryBtn.visibility = View.GONE
            } else {
                if (!recipient.isSent) {
                    progressBar.visibility = View.GONE
                    retryBtn.visibility = View.VISIBLE
                } else {
                    progressBar.visibility = View.GONE
                    retryBtn.visibility = View.GONE
                    successImg.visibility = View.VISIBLE
                }
            }

            return convertView
        }
    }

    /*----------------------------------------------------------------------------------------------
    Private class to handle Sending status.
    ----------------------------------------------------------------------------------------------*/
    private inner class SentReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val bundle = intent.extras

            if (resultCode == Activity.RESULT_OK) {
                if (bundle != null) {
                    val index = bundle.getInt(RECIPIENT_INDEX_KEY)

                    val rec = bundle.get(Config.RECIPIENT_EXTRA_KEY) as Recipient
                    rec.sending = false
                    rec.sent = true
                    recipients!![index] = rec
                    resendListAdapter!!.notifyDataSetChanged()

                    Log.d("SentReceiver", "====> OK: " + rec.number)
                }
            } else {
                if (bundle != null) {
                    val index = bundle.getInt(RECIPIENT_INDEX_KEY)

                    val rec = bundle.get(Config.RECIPIENT_EXTRA_KEY) as Recipient
                    rec.sending = false
                    rec.sent = false
                    recipients!![index] = rec
                    resendListAdapter!!.notifyDataSetChanged()

                    Log.d("SentReceiver", "====> FAILED: " + rec.number)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        sentReceiver = SentReceiver()
        registerReceiver(sentReceiver, IntentFilter(Config.SENT_STATUS_ACTION))
        Log.d(this.javaClass.name, "====> Registering a sentReceiver")
    }

    override fun onPause() {
        super.onPause()

        if (sentReceiver != null) {
            unregisterReceiver(sentReceiver)
            Log.d(this.javaClass.name, "====> Unregistering a sentReceiver")

        }
    }
}
