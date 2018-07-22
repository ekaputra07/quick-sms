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
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.*
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
import android.view.*
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.ListView
import com.balicodes.quicksms.entity.MessageEntity
import com.balicodes.quicksms.viewmodel.MessageViewModel
import java.util.*

class SMSListFragment : Fragment(), AdapterView.OnItemClickListener {

    private val TAG = SMSListFragment::class.java.toString()

    private lateinit var viewModel: MessageViewModel
    private lateinit var tapInfo: FrameLayout
    private lateinit var beep: MediaPlayer
    private lateinit var sentReceiver: SentReceiver
    private lateinit var deliveryReceiver: DeliveryReceiver
    private lateinit var sp: SharedPreferences

    private val listSMS = ArrayList<SMSItem>()
    private var listAdapter: SMSListAdapter? = null
    private var afterSending = false
    private var isReceiverRegistered = false
    private var currentSMSitem: SMSItem? = null
    private var currentSMSitemIndex: Int = 0
    private var currentSendingCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val sp = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val sortBy = sp.getString(requireContext().getString(R.string.pref_sort_by_key), requireContext().getString(R.string.pref_sort_by_default_value))

        viewModel = ViewModelProviders.of(requireActivity()).get(MessageViewModel::class.java)
        viewModel.setOrderBy(sortBy)
        viewModel.getMessages().observe(this, Observer<List<MessageEntity>> {
            listSMS.clear()

            // show hide tap info frame.
            if (it!!.isEmpty()) {
                tapInfo.visibility = View.GONE
            } else {
                tapInfo.visibility = View.VISIBLE
            }

            val items = ArrayList<SMSItem>()
            it.forEach { items.add(it.toSmsItem()) }

            listSMS.addAll(items)
            listAdapter!!.notifyDataSetChanged()
        })

        listAdapter = SMSListAdapter(activity!!, listSMS)
        beep = MediaPlayer.create(activity, R.raw.beep)
        beep.setVolume(0.5.toFloat(), 0.5.toFloat())
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.sms_list_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item!!.itemId

        when (id) {
            R.id.action_new -> {
                viewModel.selectMessage(null)

                val ft = requireActivity().supportFragmentManager.beginTransaction()
                ft.replace(R.id.container, SMSFormFragment())
                ft.addToBackStack(null)
                ft.commit()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.sms_list_fragment, container, false)

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

        val item = listSMS[position]
        viewModel.selectMessage(item.toEntity())

        val form = SMSFormFragment()
        requireActivity().supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, form)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack("sms_form")
                .commit()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as MainActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)
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

        requireActivity().registerReceiver(sentReceiver, IntentFilter(Config.SENT_STATUS_ACTION))
        requireActivity().registerReceiver(deliveryReceiver, IntentFilter(Config.DELIVERY_STATUS_ACTION))

        isReceiverRegistered = true
        Log.d(TAG, "Sent and Delivery receivers registered.")
    }

    /*----------------------------------------------------------------------------------------------
    Un-register sending and delivery receivers.
    ----------------------------------------------------------------------------------------------*/
    private fun unregisterReceivers() {
        try {
            if (isReceiverRegistered) {
                requireActivity().unregisterReceiver(sentReceiver)
                requireActivity().unregisterReceiver(deliveryReceiver)
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
        val confirm = sp.getBoolean(getString(R.string.pref_sending_confirmation_key), false)

        // If sending confirmation required.
        if (confirm) {
            val builder = AlertDialog.Builder(requireContext())
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
        // There's a bug on API 26 which requires READ_PHONE_STATE to be able to send SMS.
        if (Build.VERSION.SDK_INT == 26) {
            val permissionSendSMS = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.SEND_SMS)
            val permissionReadPhoneState = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE)

            if (permissionSendSMS != PackageManager.PERMISSION_GRANTED || permissionReadPhoneState != PackageManager.PERMISSION_GRANTED) {
                val perms = arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE)
                ActivityCompat.requestPermissions(requireActivity(), perms, Config.SEND_SMS_PERMISSION_REQUEST)
                return
            }
        } else {
            val permission = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.SEND_SMS)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                val perms = arrayOf(Manifest.permission.SEND_SMS)
                ActivityCompat.requestPermissions(requireActivity(), perms, Config.SEND_SMS_PERMISSION_REQUEST)
                return
            }
        }

        Log.d(TAG, "Sending sms: " + currentSMSitem!!.title)
        listAdapter!!.setSending(currentSMSitemIndex)
        val playBeep = sp.getBoolean(getString(R.string.pref_enable_beep_key), true)
        if (playBeep) beep.start()

        // start our sending service
        val sendingIntent = Intent(requireContext(), SendingService::class.java)
        sendingIntent.putExtra(Config.SMS_BUNDLE_EXTRA_KEY, currentSMSitem!!.toBundle())
        requireActivity().startService(sendingIntent)
    }

    private fun resendSingleRecipient() {
        val builder = AlertDialog.Builder(requireContext())
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
            val confirmSending = sp.getBoolean(getString(R.string.pref_sending_confirmation_key), false)

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
            if (currentSendingCount == currentSMSitem?.totalRecipients()) {

                if (currentSMSitem!!.failedInfoList.size > 0) {
                    afterSending = true

                    // If the recipient only one, no need to show Resend screen
                    if (currentSMSitem!!.totalRecipients() == 1) {
                        resendSingleRecipient()
                    } else {

                        if (confirmSending) {
                            afterSending = false
                        }

                        val resendIntent = Intent(requireContext(), ResendActivity::class.java)
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
        unregisterReceivers()
    }

    override fun onResume() {
        super.onResume()
        registerReceivers()
        sp = PreferenceManager.getDefaultSharedPreferences(requireContext())

    }
}
