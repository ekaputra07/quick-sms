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

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import com.balicodes.quicksms.R
import com.balicodes.quicksms.data.entity.SendStatusEntity
import com.balicodes.quicksms.data.model.Status

class SendingStatusListAdapter(
        private val items: List<SendStatusEntity>,
        private val onResendClicked: (SendStatusEntity) -> Unit
) : BaseAdapter() {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var v = convertView

        if (v == null) {
            val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            v = inflater.inflate(R.layout.simple_list_item, parent, false)
        }

        val item = getItem(position)
        val title = item.number
        val titleTxt = v!!.findViewById<TextView>(R.id.title)
        titleTxt.text = title

        val status = item.status
        val msgTxt = v.findViewById<TextView>(R.id.subtitle)
        msgTxt.setTextColor(getStatusColor(item.status))
        msgTxt.text = status.label

        val resendButton = v.findViewById<Button>(R.id.btnResend)
        if (item.status == Status.SENDING) {
            resendButton.isEnabled = false
            resendButton.text = parent.context.getText(R.string.sending)
        } else {
            resendButton.isEnabled = true
            resendButton.text = parent.context.getText(R.string.resend)
        }
        resendButton.setOnClickListener { onResendClicked.invoke(item) }

        return v
    }

    private fun getStatusColor(status: Status): Int {
        when (status) {
            Status.SENT, Status.DELIVERED -> return Color.rgb(34, 139, 34)
            Status.NOT_SENT, Status.NOT_DELIVERED -> return Color.RED
            else -> return Color.BLACK
        }
    }

    override fun getItem(position: Int) = items.get(position)

    override fun getItemId(position: Int) = items.get(position).created.time

    override fun getCount() = items.size
}