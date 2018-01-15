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
import android.content.ContentUris
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle

class ShortcutHandlerActivity : Activity() {
    private val currentSendingCount = 0
    private var smsItem: SMSItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        val uri = intent.data
        val smsID = ContentUris.parseId(uri)

        if (smsID != 0L) {
            val dbHelper = DBHelper(this)
            smsItem = dbHelper.get(smsID)
            dbHelper.close()

            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.confirm_sending)

            builder.setPositiveButton(R.string.yes) { dialog, id ->
                if (smsItem != null) {
                    // Sent via sending service
                    val intent = Intent(this@ShortcutHandlerActivity, SendingService::class.java)
                    intent.putExtra(Config.SMS_BUNDLE_EXTRA_KEY, smsItem!!.toBundle())

                    startService(intent)
                }
                finish()
            }
            builder.setNegativeButton(R.string.no) { dialog, id -> finish() }
            val dialog = builder.create()
            dialog.show()
        } else {
            finish()
        }
    }
}
