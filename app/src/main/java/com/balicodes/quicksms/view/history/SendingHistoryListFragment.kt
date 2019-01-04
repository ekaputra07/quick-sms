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

package com.balicodes.quicksms.view.history

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import com.balicodes.quicksms.R
import com.balicodes.quicksms.entity.SendSmsEntity
import java.util.*

class SendingHistoryListFragment : Fragment(), AdapterView.OnItemClickListener {

    private val items = ArrayList<SendSmsEntity>()
    private lateinit var listAdapter: SendingHistoryListAdapter
    private lateinit var viewModel: HistoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        listAdapter = SendingHistoryListAdapter(items)
        viewModel = ViewModelProviders.of(requireActivity()).get(HistoryViewModel::class.java)
        viewModel.getHistoryList().observe(this, android.arch.lifecycle.Observer { list ->
            list?.let {
                items.clear()
                items.addAll(it)
                listAdapter.notifyDataSetChanged()
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.simple_list_fragment, container, false)
        requireActivity().title = getString(R.string.title_activity_history)

        val listView = view.findViewById<ListView>(R.id.listView)
        listView.adapter = listAdapter
        listView.onItemClickListener = this
        return view
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val item = items.get(position)
        viewModel.setSelectedSendHistory(item.id)

        requireActivity().supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, SendingStatusListFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit();
    }
}