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
import android.content.Intent
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast

internal class RecipientListAdapter(private val fragment: SMSFormFragment, private val items: MutableList<Array<String>>) : BaseAdapter() {

    fun addItem(item: Array<String>) {
        items.add(item)
        notifyDataSetChanged()
    }

    private fun removeItem(position: Int) {
        items.removeAt(position)
        notifyDataSetChanged()
    }

    private fun pickNumber(position: Int) {
        val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        pickContactIntent.type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
        fragment.setRecipientPickIndex(position)
        fragment.startActivityForResult(pickContactIntent, Config.PICK_CONTACT_REQUEST)
    }

    fun addContactToItem(position: Int, recipient: Array<String>) {
        items[position] = recipient
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
            val inflater = fragment.activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.receiver_list_item, parent, false)
        }
        // Remove item button
        val removeNumberBtn = convertView!!.findViewById<ImageButton>(R.id.removeNumberBtn)
        removeNumberBtn.setOnClickListener(View.OnClickListener {
            if (count == 1) {
                Toast.makeText(fragment.activity, R.string.number_error, Toast.LENGTH_LONG).show()
                return@OnClickListener
            }
            removeItem(position)
        })

        // pick a number
        val pickNumberBtn = convertView.findViewById<ImageButton>(R.id.pickNumberBtn)
        pickNumberBtn.setOnClickListener { pickNumber(position) }

        val recName = convertView.findViewById<EditText>(R.id.recName)
        val recNumber = convertView.findViewById<EditText>(R.id.recNumber)

        try {
            val receiver = items[position]
            recName.setText(receiver[0])
            recNumber.setText(receiver[1])
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }

        return convertView
    }
}
