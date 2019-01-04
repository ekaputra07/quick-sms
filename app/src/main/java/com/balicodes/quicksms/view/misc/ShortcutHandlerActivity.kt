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

package com.balicodes.quicksms.view.misc

import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.content.ContentUris
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.balicodes.quicksms.Config
import com.balicodes.quicksms.R
import com.balicodes.quicksms.entity.MessageEntity
import com.balicodes.quicksms.repository.MessageRepository
import com.balicodes.quicksms.service.SendingService
import com.balicodes.quicksms.util.SmsPermissionChecker
import java.util.logging.Logger

class ShortcutHandlerActivity : FragmentActivity() {

    private lateinit var messageRepository: MessageRepository

    companion object {
        private val LOG: Logger = Logger.getLogger(ShortcutHandlerActivity::class.java.name)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for permission to send sms.
        if (!SmsPermissionChecker.hasPermissionToSendSms(this)) {
            LOG.warning("Permission not granted. Asking for permission...")

            SmsPermissionChecker.requestSendSmsPermission(this)
            finish()
        }

        messageRepository = MessageRepository(this.application)

        val uri = intent.data
        val smsID = ContentUris.parseId(uri)

        if (smsID != 0L) {
            messageRepository.getMessage(smsID).observe(this, Observer {
                if (it != null) {
                    LOG.info("Message found on DB.")
                    showConfirmSendingDialog(it)
                } else {
                    LOG.warning("Message not found on DB.")
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
            val intent = Intent(applicationContext, SendingService::class.java)
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
