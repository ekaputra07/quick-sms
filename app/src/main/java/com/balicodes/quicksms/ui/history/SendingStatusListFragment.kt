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

package com.balicodes.quicksms.ui.history

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.balicodes.quicksms.Config
import com.balicodes.quicksms.R
import com.balicodes.quicksms.data.entity.SendStatusEntity
import com.balicodes.quicksms.data.model.Status
import com.balicodes.quicksms.service.SendingServiceSingle
import com.balicodes.quicksms.util.SmsPermissionChecker
import java.util.*
import java.util.logging.Logger

class SendingStatusListFragment : Fragment() {

    companion object {
        private val LOG = Logger.getLogger(SendingStatusListFragment::class.java.name)
    }

    private val items = ArrayList<SendStatusEntity>()
    private lateinit var listAdapter: SendingStatusListAdapter
    private lateinit var viewModel: HistoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        viewModel = ViewModelProviders.of(requireActivity()).get(HistoryViewModel::class.java)
        listAdapter = SendingStatusListAdapter(items, this::handleResend)
        viewModel.getSelectedHistoryEntity().observe(this, android.arch.lifecycle.Observer { entity ->
            entity?.let {
                requireActivity().title = it.name
            }
        })
        viewModel.getSendingStatusList().observe(this, android.arch.lifecycle.Observer { list ->
            list?.let {
                items.clear()
                items.addAll(it)
                listAdapter.notifyDataSetChanged()
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.simple_list_fragment, container, false)

        val listView = view.findViewById<ListView>(R.id.listView)
        listView.adapter = listAdapter
        return view
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        when (id) {
            android.R.id.home -> {
                viewModel.resetSelectedSendHistory()
                requireActivity().onBackPressed()
                return true // we need this, otherwise back button will close the activity.
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleResend(sendStatusEntity: SendStatusEntity) {

        // Check for permission to send sms.
        if (!SmsPermissionChecker.hasPermissionToSendSms(requireContext())) {
            LOG.warning("Permission not granted. Asking for permission...")

            SmsPermissionChecker.requestSendSmsPermission(requireActivity())
            return
        }

        // Update the status
        sendStatusEntity.status = Status.SENDING

        // Update Record and start sending service
        viewModel.updateSendingStatus(sendStatusEntity, {
            val sendingIntent = Intent(requireContext(), SendingServiceSingle::class.java)
            sendingIntent.putExtra(Config.SEND_STATUS_ID_EXTRA_KEY, it.id)
            requireActivity().startService(sendingIntent)
        })
    }
}