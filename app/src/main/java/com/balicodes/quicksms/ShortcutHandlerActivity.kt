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

import android.app.Activity
import android.app.AlertDialog
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.ContentUris
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.balicodes.quicksms.entity.MessageEntity
import com.balicodes.quicksms.repository.MessageRepository
import com.balicodes.quicksms.service.SendingService
import com.balicodes.quicksms.viewmodel.MessageViewModel

class ShortcutHandlerActivity : FragmentActivity() {

    private lateinit var messageRepository: MessageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        messageRepository = MessageRepository(this.application)

        val uri = intent.data
        val smsID = ContentUris.parseId(uri)

        Log.d(this.localClassName.toString(), smsID.toString())

        if (smsID != 0L) {
            messageRepository.getMessage(smsID).observe(this, Observer {
                if (it != null) {
                    showConfirmSendingDialog(it)
                } else {
                    showNotFoundDialog()
                }
            })
        } else {
            finish()
        }
    }

    private fun showConfirmSendingDialog(messageEntity: MessageEntity) {

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.confirm_sending)

        builder.setNegativeButton(R.string.no) { _, _ -> finish() }
        builder.setOnCancelListener { finish() }

        builder.setPositiveButton(R.string.yes) { _, _ ->
            // Sent via sending service
            val intent = Intent(this, SendingService::class.java)
            intent.putExtra(Config.SMS_BUNDLE_EXTRA_KEY, messageEntity.toSmsItem().toBundle())
            this.startService(intent)

            finish()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun showNotFoundDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Message is not available anymore.")

        builder.setOnCancelListener { finish() }

        builder.setPositiveButton(R.string.ok) { _, _ ->
            finish()
        }

        val dialog = builder.create()
        dialog.show()
    }
}
