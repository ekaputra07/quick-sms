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

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.ListView

import java.util.ArrayList

class SMSListFragment : Fragment(), AdapterView.OnItemClickListener {
    private val TAG = SMSListFragment::class.java.toString()

    private var dbHelper: DBHelper? = null
    private val listSMS = ArrayList<SMSItem>()
    private var listAdapter: SMSListAdapter? = null
    private var tapInfo: FrameLayout? = null
    private var afterSending = false
    private var beep: MediaPlayer? = null
    private var sentReceiver: SentReceiver? = null
    private var deliveryReceiver: DeliveryReceiver? = null
    private var isReceiverRegistered = false
    private var currentSMSitem: SMSItem? = null
    private var currentSMSitemIndex: Int = 0
    private var currentSendingCount = 0
    private var sp: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        dbHelper = DBHelper(activity)
        listAdapter = SMSListAdapter(activity, listSMS)
        beep = MediaPlayer.create(activity, R.raw.beep)
        beep!!.setVolume(0.5.toFloat(), 0.5.toFloat())
    }

    private fun loadList() {
        listSMS.clear()
        val all = dbHelper!!.all()

        // show hide tap info frame.
        if (all.size == 0) {
            tapInfo!!.visibility = View.GONE
        } else {
            tapInfo!!.visibility = View.VISIBLE
        }

        listSMS.addAll(all)
        listAdapter!!.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.sms_list_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item!!.itemId

        when (id) {
            R.id.action_new -> {
                val ft = activity.supportFragmentManager.beginTransaction()
                ft.replace(R.id.container, SMSFormFragment())
                ft.addToBackStack(null)
                ft.commit()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.sms_list_fragment, container, false)
        tapInfo = view.findViewById(R.id.tap_info)
        val listView = view.findViewById<ListView>(R.id.listView)
        listView.adapter = listAdapter
        listView.onItemClickListener = this
        registerForContextMenu(listView)

        return view
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        if (afterSending) {
            afterSending = false
            return
        }

        val smsitem = listSMS[position]
        val form = SMSFormFragment()
        form.arguments = smsitem.toBundle()

        activity.supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, form)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack("sms_form")
                .commit()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as MainActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        loadList()
    }

    /*----------------------------------------------------------------------------------------------
    Try to send sms on long tap.
    ----------------------------------------------------------------------------------------------*/
    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo) {

        if (currentSMSitem != null) {
            resetCurrentSMSItem()
        }

        val info = menuInfo as AdapterView.AdapterContextMenuInfo
        currentSMSitem = listSMS[info.position]
        currentSMSitemIndex = info.position

        tryToSend()
    }

    /*----------------------------------------------------------------------------------------------
    Register sending and delivery receivers.
    ----------------------------------------------------------------------------------------------*/
    private fun registerReceivers() {
        sentReceiver = SentReceiver()
        deliveryReceiver = DeliveryReceiver()

        activity.registerReceiver(sentReceiver, IntentFilter(Config.SENT_STATUS_ACTION))
        activity.registerReceiver(deliveryReceiver, IntentFilter(Config.DELIVERY_STATUS_ACTION))

        isReceiverRegistered = true
        Log.d(TAG, "Sent and Delivery receivers registered.")
    }

    /*----------------------------------------------------------------------------------------------
    Un-register sending and delivery receivers.
    ----------------------------------------------------------------------------------------------*/
    private fun unregisterReceivers() {
        try {
            if (isReceiverRegistered) {
                activity.unregisterReceiver(sentReceiver)
                activity.unregisterReceiver(deliveryReceiver)
                Log.d(TAG, "Sent and Delivery receivers un-registered.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /* ---------------------------------------------------------------------------------------------
    Try to send the message and show confirmation dialog if user set so.
    ----------------------------------------------------------------------------------------------*/
    private fun tryToSend() {
        afterSending = true
        val confirm = sp!!.getBoolean(getString(R.string.pref_sending_confirmation_key), false)

        // If sending confirmation required.
        if (confirm) {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(R.string.confirm_sending)

            builder.setPositiveButton(R.string.yes) { dialog, id -> sendSMS() }
            builder.setNegativeButton(R.string.no) { dialog, id -> }
            val dialog = builder.create()
            dialog.show()

        } else {
            sendSMS()
        }
    }

    /*----------------------------------------------------------------------------------------------
    The actual function to send the message.
     ---------------------------------------------------------------------------------------------*/
    private fun sendSMS() {
        if (currentSMSitem == null) return

        // Android API 23+ requires this
        if (Build.VERSION.SDK_INT == 26) {
            val permissionSendSMS = ContextCompat.checkSelfPermission(activity, android.Manifest.permission.SEND_SMS)
            val permissionReadPhoneState = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE)

            if (permissionSendSMS != PackageManager.PERMISSION_GRANTED || permissionReadPhoneState != PackageManager.PERMISSION_GRANTED) {
                val perms = arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE)
                ActivityCompat.requestPermissions(activity, perms, Config.SEND_SMS_PERMISSION_REQUEST)
                return
            }
        } else {
            val permission = ContextCompat.checkSelfPermission(activity, android.Manifest.permission.SEND_SMS)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                val perms = arrayOf(Manifest.permission.SEND_SMS)
                ActivityCompat.requestPermissions(activity, perms, Config.SEND_SMS_PERMISSION_REQUEST)
                return
            }
        }

        Log.d(TAG, "Sending sms: " + currentSMSitem!!.title)
        listAdapter!!.setSending(currentSMSitemIndex)
        val playBeep = sp!!.getBoolean(getString(R.string.pref_enable_beep_key), true)
        if (playBeep) beep!!.start()

        // start our sending service
        val sendingIntent = Intent(activity.applicationContext, SendingService::class.java)
        sendingIntent.putExtra(Config.SMS_BUNDLE_EXTRA_KEY, currentSMSitem!!.toBundle())
        activity.startService(sendingIntent)
    }

    private fun resendSingleRecipient() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.sending_failed)
        builder.setMessage(R.string.sending_failed_info)
        builder.setPositiveButton(R.string.retry) { dialog, id ->
            currentSendingCount = 0
            sendSMS()
        }
        builder.setNegativeButton(R.string.ok) { dialog, id -> resetCurrentSMSItem() }
        val dialog = builder.create()
        dialog.show()
    }

    /*----------------------------------------------------------------------------------------------
    Private class to handle Sending status.
    ----------------------------------------------------------------------------------------------*/
    private inner class SentReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val recBundle = intent.getBundleExtra(Config.RECIPIENT_EXTRA_KEY)

            if (resultCode == Activity.RESULT_OK) {
                if (recBundle != null) {
                    val rec = Recipient.fromBundle(recBundle)
                    listAdapter!!.notifyStatusChanged(currentSMSitemIndex, SMSItem.STATUS_SENT, rec)
                    Log.d("SentReceiver", "====> OK: " + rec.number)
                }
            } else {
                if (recBundle != null) {
                    val rec = Recipient.fromBundle(recBundle)
                    Log.d("SentReceiver", String.format("%s - %s - %s", rec.id, rec.name, rec.number))

                    listAdapter!!.notifyStatusChanged(currentSMSitemIndex, SMSItem.STATUS_FAILED, rec)
                    Log.d("SentReceiver", "====> FAILED: " + rec.number)
                }
            }

            // If finish sending, check whether all sent successfully.
            // if not, show resend screen with all failed recipients.
            currentSendingCount++
            if (currentSendingCount == currentSMSitem!!.totalRecipients()) {

                if (currentSMSitem!!.failedInfoList.size > 0) {
                    afterSending = true

                    // If the recipient only one, no need to show Resend screen
                    if (currentSMSitem!!.totalRecipients() == 1) {
                        resendSingleRecipient()
                    } else {

                        val confirm_before_send = sp!!.getBoolean(getString(R.string.pref_sending_confirmation_key), false)
                        if (confirm_before_send) {
                            afterSending = false
                        }

                        val resendIntent = Intent(activity.applicationContext, ResendActivity::class.java)
                        resendIntent.putExtra(Config.SMS_MESSAGE_EXTRA_KEY, currentSMSitem!!.message)
                        resendIntent.putParcelableArrayListExtra(Config.RECIPIENT_PARCELS_EXTRA_KEY, currentSMSitem!!.failedInfoList)
                        startActivity(resendIntent)
                    }
                }
            }
        }
    }

    /*----------------------------------------------------------------------------------------------
    Private class to handle Delivery status.
    ----------------------------------------------------------------------------------------------*/
    private inner class DeliveryReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            val recBundle = intent.getBundleExtra(Config.RECIPIENT_EXTRA_KEY)

            if (resultCode == Activity.RESULT_OK) {
                if (recBundle != null) {
                    val rec = Recipient.fromBundle(recBundle)
                    listAdapter!!.notifyStatusChanged(currentSMSitemIndex, SMSItem.STATUS_DELIVERED, rec)
                    Log.d("DeliveryReceiver", "====> OK: " + rec.number)
                }
            }
        }
    }

    /*----------------------------------------------------------------------------------------------
    Reset current selected SMSItem state.
    ----------------------------------------------------------------------------------------------*/
    private fun resetCurrentSMSItem() {
        currentSMSitem = null
        currentSMSitemIndex = -1
        currentSendingCount = 0
        afterSending = false
        Log.d(TAG, "Reset currentSMSItem")
    }

    override fun onPause() {
        super.onPause()
        if (dbHelper != null) {
            dbHelper!!.close()
        }
        unregisterReceivers()
    }

    override fun onStop() {
        super.onStop()
        if (dbHelper != null) {
            dbHelper!!.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dbHelper != null) {
            dbHelper!!.close()
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceivers()
        sp = PreferenceManager.getDefaultSharedPreferences(activity)

    }
}
