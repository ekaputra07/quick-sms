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

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.RelativeLayout
import android.widget.TextView

internal class SMSListAdapter(private val context: Context, private val items: List<SMSItem>) : BaseAdapter() {

    private val sp = PreferenceManager.getDefaultSharedPreferences(context)

    fun setSending(position: Int) {
        items[position].setSending()
        notifyDataSetChanged()
    }

    fun notifyStatusChanged(position: Int, statusType: Int, rec: Recipient) {
        items[position].addStatusInfo(statusType, rec)
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView

        if (convertView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.sms_list_item, parent, false)
        }

        val title = items[position].title
        val titleTxt = convertView!!.findViewById<TextView>(R.id.smsTitleTxt)
        titleTxt.text = title

        val number = items[position].label
        val msgTxt = convertView.findViewById<TextView>(R.id.smsMessageTxt)
        msgTxt.text = context.getString(R.string.to).format(number)

        val statusTxt = convertView.findViewById<TextView>(R.id.smsStatusTxt)
        val statusContainer = convertView.findViewById<RelativeLayout>(R.id.statusContainer)
        val statusFailed = convertView.findViewById<TextView>(R.id.statusFailed)
        val statusSent = convertView.findViewById<TextView>(R.id.statusSent)
        val statusDelivered = convertView.findViewById<TextView>(R.id.statusDelivered)

        val item = items[position]
        val status = item.status

        when (status) {
            SMSItem.STATUS_INACTIVE -> {
                statusTxt.visibility = View.GONE
                statusContainer.visibility = View.GONE
            }

            SMSItem.STATUS_SENDING -> {
                statusTxt.setTextColor(context.resources.getColor(R.color.grey))
                statusTxt.text = context.resources.getString(R.string.status_sending)
                statusTxt.visibility = View.VISIBLE
                statusContainer.visibility = View.GONE
            }

            else -> {
                val enableDeliveryReport = sp.getBoolean(context.getString(R.string.pref_enable_delivery_report_key), false)

                statusTxt.visibility = View.GONE
                statusFailed.text = context.resources.getString(R.string.status_failed)
                        .replace("{COUNT}", item.failedInfoList.size.toString())
                statusSent.text = context.resources.getString(R.string.status_sent)
                        .replace("{COUNT}", item.sentInfoList.size.toString())

                if (enableDeliveryReport) {
                    statusDelivered.text = context.resources.getString(R.string.status_delivered)
                            .replace("{COUNT}", item.receivedInfoList.size.toString())
                    statusDelivered.visibility = View.VISIBLE
                } else {
                    statusDelivered.visibility = View.GONE
                }

                statusContainer.visibility = View.VISIBLE
            }
        }

        return convertView
    }
}
