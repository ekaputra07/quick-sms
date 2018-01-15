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

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.DataSetObserver
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import java.util.*

class SMSFormFragment : Fragment() {

    private var smstitle: EditText? = null
    private var message: EditText? = null
    private var addShortcut: Switch? = null
    private var addRecipient: TextView? = null
    private val recipients = ArrayList<Array<String>>()
    private var recipientListView: NoScrollListView? = null
    private var recipientListAdapter: RecipientListAdapter? = null
    private val dbHelper: DBHelper? = null
    private var smsItem: SMSItem? = null
    private var recipientPickIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        // Contact data request
        if (requestCode == Config.PICK_CONTACT_REQUEST && resultCode == RESULT_OK && data != null) {

            val phoneNo: String
            val displayName: String

            val uri = data.data
            val cursor = activity.contentResolver.query(uri!!, null, null, null, null)
            cursor!!.moveToFirst()

            val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val displayNameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            phoneNo = cursor.getString(phoneIndex)
            displayName = cursor.getString(displayNameIndex)
            val recipient = arrayOf(displayName, phoneNo)

            cursor.close()

            recipientListAdapter!!.addContactToItem(recipientPickIndex, recipient)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.sms_form_fragment, container, false)
        smstitle = view.findViewById(R.id.titleTxt)
        message = view.findViewById(R.id.messageTxt)

        recipientListView = view.findViewById(R.id.receiverListView)
        recipientListAdapter = RecipientListAdapter(this, recipients)
        recipientListView!!.adapter = recipientListAdapter
        recipientListView!!.divider = null
        recipientListView!!.expanded = true

        addRecipient = view.findViewById<Button>(R.id.addReceiverBtn)
        addRecipient!!.setOnClickListener {
            if (recipients.size < Config.MAX_RECIPIENTS_PER_SMS) {
                saveRecipientsState() // before adding new recipient, make sure we save any changes.
                recipientListAdapter!!.addItem(arrayOf("", ""))
            } else {
                val msg = String.format(activity.resources.getString(R.string.max_recipients_warning), Config.MAX_RECIPIENTS_PER_SMS.toString())
                Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
            }
        }

        addShortcut = view.findViewById(R.id.addShortcut)

        val saveBtn = view.findViewById<Button>(R.id.saveBtn)
        saveBtn.setOnClickListener { saveSMS(false) }

        // Show hide addRecipient
        recipientListAdapter!!.registerDataSetObserver(object : DataSetObserver() {
            override fun onChanged() {
                if (recipients.size < Config.MAX_RECIPIENTS_PER_SMS) {
                    addRecipient!!.visibility = View.VISIBLE
                } else {
                    addRecipient!!.visibility = View.GONE
                }
            }
        })

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        (activity as MainActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val bundle = arguments
        // edit mode
        if (bundle != null) {
            smsItem = SMSItem.fromBundle(bundle)

            smstitle!!.setText(smsItem!!.title)
            message!!.setText(smsItem!!.message)
            recipients.addAll(SMSItem.parseReceiverCSV(smsItem!!.number))
            addShortcut!!.isChecked = "YES" == smsItem!!.shortcut

            // new mode
        } else {
            smsItem = SMSItem(0, "", "", "", "")
            recipients.add(arrayOf("", ""))
        }
        recipientListAdapter!!.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        if (smsItem!!.id > 0) {
            inflater!!.inflate(R.menu.sms_form_menu, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item!!.itemId

        when (id) {
            R.id.action_delete -> deleteSMS()
            R.id.action_copy -> saveSMS(true)
        }
        return super.onOptionsItemSelected(item)
    }


    private fun hideKeyboard() {
        // Check if no view has focus:
        val view = activity.currentFocus
        if (view != null) {
            val inputManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    private fun deleteSMS() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.confirm_delete_title)

        builder.setPositiveButton(R.string.yes) { dialog, id ->
            toggleShortcut(false) // delete shortcut
            smsItem!!.delete(this@SMSFormFragment.activity)

            Toast.makeText(activity, R.string.message_deleted, Toast.LENGTH_SHORT).show()
            activity.onBackPressed()
        }
        builder.setNegativeButton(R.string.cancel) { dialog, id -> }
        val dialog = builder.create()
        dialog.show()
    }

    /* This is to store any changes made to the recipients input back to the `recipients` */
    private fun saveRecipientsState() {
        for (i in recipients.indices) {
            val v = recipientListView!!.getChildAt(i)
            val name = (v.findViewById<View>(R.id.recName) as EditText).text.toString().trim { it <= ' ' }
            val number = (v.findViewById<View>(R.id.recNumber) as EditText).text.toString().trim { it <= ' ' }
            recipients[i] = arrayOf(name, number)
        }
    }

    private fun saveSMS(duplicate: Boolean) {
        // do some simple validations
        if (smstitle!!.text.toString().isEmpty()) {
            smstitle!!.error = getString(R.string.title_error)
            return
        }

        if (message!!.text.toString().isEmpty()) {
            message!!.error = getString(R.string.message_error)
            return
        }
        // get recipient as csv
        val rec = ArrayList<String>()
        for (i in recipients.indices) {
            val v = recipientListView!!.getChildAt(i)
            val name = (v.findViewById<View>(R.id.recName) as EditText).text.toString().trim { it <= ' ' }
            val number = (v.findViewById<View>(R.id.recNumber) as EditText).text.toString().trim { it <= ' ' }
            if (number.isNotBlank()) {
                rec.add(name + ":" + number)
            }
        }
        val csv = TextUtils.join(",", rec)

        if (csv.isEmpty()) {
            Toast.makeText(activity, R.string.number_error, Toast.LENGTH_LONG).show()
            return
        }

        val shortcut = if (addShortcut!!.isChecked) "YES" else "NO"

        if (smsItem!!.id > 0) {
            // Update
            smsItem!!.update(
                    activity,
                    smstitle!!.text.toString(),
                    csv,
                    message!!.text.toString(),
                    shortcut)
        } else {
            // Create
            smsItem = SMSItem.create(
                    activity,
                    smstitle!!.text.toString(),
                    csv,
                    message!!.text.toString(),
                    shortcut)
        }

        toggleShortcut(addShortcut!!.isChecked)
        hideKeyboard()

        if (duplicate) {
            // POP current backstack
            activity.supportFragmentManager.popBackStack()

            val duplicateForm = SMSFormFragment()
            val duplicateItem = SMSItem.copyFrom(activity, smsItem!!)
            duplicateForm.arguments = duplicateItem.toBundle()

            // Replace current fragment with the new one.
            val ft = activity.supportFragmentManager.beginTransaction()
            ft.replace(R.id.container, duplicateForm)
            ft.addToBackStack("sms_form")
            ft.commit()

            Toast.makeText(activity, R.string.message_copied, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(activity, R.string.message_saved, Toast.LENGTH_SHORT).show()
            activity.onBackPressed()
        }

    }

    // used to set reference to recipient item index before contact Pick.
    fun setRecipientPickIndex(index: Int) {
        recipientPickIndex = index
    }

    private fun toggleShortcut(create: Boolean) {
        val shortCutInt = Intent(activity.applicationContext, ShortcutHandlerActivity::class.java)
        shortCutInt.action = Intent.ACTION_MAIN
        shortCutInt.data = ContentUris.withAppendedId(Uri.parse(Config.SMS_DATA_BASE_URI), smsItem!!.id)

        val addInt = Intent()
        addInt.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortCutInt)
        addInt.putExtra(Intent.EXTRA_SHORTCUT_NAME, smsItem!!.title)
        addInt.putExtra("duplicate", false)
        addInt.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(activity.applicationContext,
                        R.drawable.ic_launcher))
        if (create) {
            addInt.action = "com.android.launcher.action.INSTALL_SHORTCUT"
        } else {
            addInt.action = "com.android.launcher.action.UNINSTALL_SHORTCUT"
        }

        activity.applicationContext.sendBroadcast(addInt)
    }
}

