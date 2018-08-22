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

import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.ListView
import com.balicodes.quicksms.entity.MessageEntity
import com.balicodes.quicksms.model.SMSItem
import com.balicodes.quicksms.service.SendingService
import com.balicodes.quicksms.util.SmsPermissionChecker
import com.balicodes.quicksms.viewmodel.MessageViewModel
import java.util.*
import java.util.logging.Logger

class SMSListFragment : Fragment(), AdapterView.OnItemClickListener {

    private val TAG = SMSListFragment::class.java.toString()

    private lateinit var viewModel: MessageViewModel
    private lateinit var tapInfo: FrameLayout
    private lateinit var sp: SharedPreferences

    private val listSMS = ArrayList<SMSItem>()
    private var listAdapter: SMSListAdapter? = null
    private var afterSending = false
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

        listAdapter = SMSListAdapter(listSMS)
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
        (requireActivity() as MainActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)
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

        // Check for permission to send sms.
        if (!SmsPermissionChecker.hasPermissionToSendSms(requireContext())) {
            LOG.warning("Permission not granted. Asking for permission...")

            SmsPermissionChecker.requestSendSmsPermission(requireActivity())
            return
        }

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
    Reset current selected SMSItem state.
    ----------------------------------------------------------------------------------------------*/
    private fun resetCurrentSMSItem() {
        currentSMSitem = null
        currentSMSitemIndex = -1
        currentSendingCount = 0
        afterSending = false
        Log.d(TAG, "Reset currentSMSItem")
    }

    override fun onResume() {
        super.onResume()
        sp = PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(SMSListFragment::class.java.name)
    }
}
