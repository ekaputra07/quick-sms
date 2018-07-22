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
import android.app.PendingIntent
import android.arch.lifecycle.ViewModelProviders
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.database.DataSetObserver
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.balicodes.quicksms.entity.MessageEntity
import com.balicodes.quicksms.model.SMSItem
import com.balicodes.quicksms.viewmodel.MessageViewModel

class SMSFormFragment : Fragment() {

    lateinit var viewModel: MessageViewModel
    lateinit var recipientListView: NoScrollListView
    lateinit var smstitle: EditText
    lateinit var message: EditText
    lateinit var addShortcut: Switch
    lateinit var addRecipient: TextView

    private val recipients = ArrayList<Array<String>>()

    private var recipientListAdapter: RecipientListAdapter? = null
    private var smsItem: SMSItem? = null
    private var recipientPickIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        (activity as MainActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProviders.of(requireActivity()).get(MessageViewModel::class.java)
        viewModel.getSelectedMessage().observe(this, android.arch.lifecycle.Observer { setFormData(it) })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        // Contact data request
        if (requestCode == Config.PICK_CONTACT_REQUEST && resultCode == RESULT_OK && data != null) {

            val phoneNo: String
            val displayName: String

            val uri = data.data
            val cursor = requireActivity().contentResolver.query(uri, null, null, null, null)
            cursor.moveToFirst()

            val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val displayNameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            phoneNo = cursor.getString(phoneIndex)
            displayName = cursor.getString(displayNameIndex)
            val recipient = arrayOf(displayName, phoneNo)

            cursor.close()

            recipientListAdapter!!.addContactToItem(recipientPickIndex, recipient)
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.sms_form_fragment, container, false)
        smstitle = view.findViewById(R.id.titleTxt)
        message = view.findViewById(R.id.messageTxt)

        recipientListView = view.findViewById(R.id.receiverListView)
        recipientListAdapter = RecipientListAdapter(this, recipients)
        recipientListView.adapter = recipientListAdapter
        recipientListView.divider = null
        recipientListView.expanded = true

        addRecipient = view.findViewById<Button>(R.id.addReceiverBtn)
        addRecipient.setOnClickListener {
            if (recipients.size < Config.MAX_RECIPIENTS_PER_SMS) {
                saveRecipientsState() // before adding new recipient, make sure we save any changes.
                recipientListAdapter!!.addItem(arrayOf("", ""))
            } else {
                val msg = String.format(requireActivity().resources.getString(R.string.max_recipients_warning), Config.MAX_RECIPIENTS_PER_SMS.toString())
                Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
            }
        }

        addShortcut = view.findViewById(R.id.addShortcut)

        val saveBtn = view.findViewById<Button>(R.id.saveBtn)
        saveBtn.setOnClickListener { saveMessage() }

        // Show hide addRecipient
        recipientListAdapter!!.registerDataSetObserver(object : DataSetObserver() {
            override fun onChanged() {
                if (recipients.size < Config.MAX_RECIPIENTS_PER_SMS) {
                    addRecipient.visibility = View.VISIBLE
                } else {
                    addRecipient.visibility = View.GONE
                }
            }
        })

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        viewModel.getSelectedMessage().observe(this, android.arch.lifecycle.Observer {
            it?.let { inflater?.inflate(R.menu.sms_form_menu, menu) }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item!!.itemId

        when (id) {
            R.id.action_delete -> deleteMessage()
            R.id.action_copy -> viewModel.copyMessage(smsItem!!.toEntity(), this::afterDuplicate)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setFormData(messageEntity: MessageEntity?) {
        messageEntity?.let {
            smsItem = it.toSmsItem()

            smstitle.setText(it.title)
            message.setText(it.message)
            recipients.addAll(SMSItem.parseReceiverCSV(it.number))
            addShortcut.isChecked = (it.addShortcut == SMSItem.SHORTCUT_YES)

            recipientListAdapter!!.notifyDataSetChanged()
        }
    }

    private fun hideKeyboard() {
        // Check if no view has focus:
        val view = requireActivity().currentFocus
        if (view != null) {
            val inputManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    private fun deleteMessage() {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.confirm_delete_title)

        builder.setPositiveButton(R.string.yes) { _, _ ->
            viewModel.deleteMessage(smsItem!!.toEntity(), this::afterDelete)
        }
        builder.setNegativeButton(R.string.cancel) { _, _ -> }
        val dialog = builder.create()
        dialog.show()
    }

    /* This is to store any changes made to the recipients input back to the `recipients` */
    private fun saveRecipientsState() {
        for (i in recipients.indices) {
            val v = recipientListView.getChildAt(i)
            val name = (v.findViewById<View>(R.id.recName) as EditText).text.toString().trim { it <= ' ' }
            val number = (v.findViewById<View>(R.id.recNumber) as EditText).text.toString().trim { it <= ' ' }
            recipients[i] = arrayOf(name, number)
        }
    }

    // Save / Update the message.
    private fun saveMessage() {
        // do some simple validations
        if (smstitle.text.toString().isEmpty()) {
            smstitle.error = getString(R.string.title_error)
            return
        }

        if (message.text.toString().isEmpty()) {
            message.error = getString(R.string.message_error)
            return
        }
        // get recipient as csv
        val rec = ArrayList<String>()
        for (i in recipients.indices) {
            val v = recipientListView.getChildAt(i)
            val name = (v.findViewById<View>(R.id.recName) as EditText).text.toString().trim { it <= ' ' }
            val number = (v.findViewById<View>(R.id.recNumber) as EditText).text.toString().trim { it <= ' ' }
            if (number.isNotBlank()) {
                rec.add(name + ":" + number)
            }
        }
        val csv = TextUtils.join(",", rec)

        if (csv.isEmpty()) {
            Toast.makeText(requireActivity(), R.string.number_error, Toast.LENGTH_LONG).show()
            return
        }

        val shortcut = if (addShortcut.isChecked) SMSItem.SHORTCUT_YES else SMSItem.SHORTCUT_NO

        if (smsItem != null) {
            // Update
            val item = SMSItem(smsItem!!.id, smstitle.text.toString(), csv, message.text.toString(), shortcut)
            viewModel.updateMessage(item.toEntity(), this::afterSave)

        } else {
            // Create
            val item = SMSItem(0L, smstitle.text.toString(), csv, message.text.toString(), shortcut)
            viewModel.insertMessage(item.toEntity(), this::afterSave)
        }
    }

    // After message has been saved.
    private fun afterSave(messageEntity: MessageEntity) {
        toggleShortcut(messageEntity, messageEntity.addShortcut == SMSItem.SHORTCUT_YES)
        hideKeyboard()

        Toast.makeText(requireActivity(), R.string.message_saved, Toast.LENGTH_SHORT).show()
        requireActivity().onBackPressed()
    }

    // After message has been duplicated.
    private fun afterDuplicate(messageEntity: MessageEntity) {
        // Clear the recipients and notify adapter
        recipients.clear()
        recipientListAdapter?.notifyDataSetChanged()

        // Set selected message.
        viewModel.selectMessage(messageEntity)

        // POP current backstack
        requireActivity().supportFragmentManager.popBackStack()
        val duplicateForm = SMSFormFragment()

        // Replace current fragment with the new one.
        val ft = requireActivity().supportFragmentManager.beginTransaction()
        ft.replace(R.id.container, duplicateForm)
        ft.addToBackStack("sms_form")
        ft.commit()

        Toast.makeText(requireActivity(), R.string.message_copied, Toast.LENGTH_SHORT).show()
    }

    // After message deleted.
    private fun afterDelete(messageEntity: MessageEntity) {
        toggleShortcut(messageEntity, false) // delete shortcut

        Toast.makeText(requireActivity(), R.string.message_deleted, Toast.LENGTH_SHORT).show()
        requireActivity().onBackPressed()
    }

    // used to set reference to recipient item index before contact Pick.
    fun setRecipientPickIndex(index: Int) {
        recipientPickIndex = index
    }

    // Toogle shortcut icon
    // TODO: Move this code block into its own class.
    private fun toggleShortcut(messageEntity: MessageEntity, create: Boolean) {

        val shortCutInt = Intent(requireContext(), ShortcutHandlerActivity::class.java)
        shortCutInt.action = Intent.ACTION_MAIN
        shortCutInt.data = ContentUris.withAppendedId(Uri.parse(Config.SMS_DATA_BASE_URI), messageEntity.id!!)

        // On Android 25+ we can create dynamic shortcuts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = requireContext().getSystemService(ShortcutManager::class.java)
            val shortcutId = "QSMS_".format(messageEntity.id)

            val shortcutInfo = ShortcutInfo.Builder(requireActivity(), shortcutId)
                    .setShortLabel(messageEntity.title)
                    .setIcon(Icon.createWithResource(requireActivity(), R.drawable.ic_launcher_shortcut))
                    .setIntent(shortCutInt)
                    .build()

            // On Android 26+ we can create pinned shortcut on home screen.
            // else, we only create dynamic shortcut.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && shortcutManager.isRequestPinShortcutSupported) {
                val shortcuts = shortcutManager.pinnedShortcuts

                var exists = false
                loop@ for (s in shortcuts) {
                    if (s.id == shortcutId) {
                        exists = true
                        break@loop
                    }
                }
                if (create && !exists) {
                    val pinnedShortcutCallbackIntent = shortcutManager.createShortcutResultIntent(shortcutInfo)
                    val successCallback = PendingIntent.getBroadcast(requireContext(), 0, pinnedShortcutCallbackIntent, 0)
                    shortcutManager.requestPinShortcut(shortcutInfo, successCallback.intentSender)
                } else if (create && exists) {
                    shortcutManager.updateShortcuts(listOf(shortcutInfo))
                } else {
                    val builder = AlertDialog.Builder(requireActivity())
                    builder.setTitle("Deleting shortcut")
                    builder.setMessage("Unfortunately Android doesn't allow us to delete the shortcut for you, so you have to delete it manually :)")
                    builder.setPositiveButton(R.string.yes) { _, _ -> }
                    val dialog = builder.create()
                    dialog.show()
                }

            } else {
                val shortcuts = shortcutManager.dynamicShortcuts

                var exists = false
                loop@ for (s in shortcuts) {
                    if (s.id == shortcutId) {
                        exists = true
                        break@loop
                    }
                }
                if (create && !exists) {
                    shortcutManager.addDynamicShortcuts(listOf(shortcutInfo))
                } else if (create && exists) {
                    shortcutManager.updateShortcuts(listOf(shortcutInfo))
                } else {
                    shortcutManager.removeDynamicShortcuts(listOf(shortcutId))
                }
            }

        } else {
            val addInt = Intent()
            addInt.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortCutInt)
            addInt.putExtra(Intent.EXTRA_SHORTCUT_NAME, messageEntity.title)
            addInt.putExtra("duplicate", false)
            addInt.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(requireActivity().applicationContext,
                            R.drawable.ic_launcher_shortcut))
            if (create) {
                addInt.action = "com.android.launcher.action.INSTALL_SHORTCUT"
            } else {
                addInt.action = "com.android.launcher.action.UNINSTALL_SHORTCUT"
            }

            requireContext().applicationContext.sendBroadcast(addInt)
        }
    }
}

