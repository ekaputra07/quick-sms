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

import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.ContentUris
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.balicodes.quicksms.service.SendingService
import com.balicodes.quicksms.viewmodel.MessageViewModel

class ShortcutHandlerActivity : AppCompatActivity() {

    private lateinit var viewModel: MessageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(MessageViewModel::class.java)

        val uri = intent.data
        val smsID = ContentUris.parseId(uri)

        Log.d(this.localClassName.toString(), smsID.toString())

        if (smsID != 0L) {
            viewModel.getMessage(smsID).observe(this, Observer {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.confirm_sending)

                builder.setPositiveButton(R.string.yes) { _, _ ->
                    it?.let {
                        // Sent via sending service
                        val intent = Intent(this, SendingService::class.java)
                        intent.putExtra(Config.SMS_BUNDLE_EXTRA_KEY, it.toSmsItem().toBundle())
                        this.startService(intent)
                    }
                    finish()
                }

                builder.setNegativeButton(R.string.no) { _, _ -> finish() }
                builder.setOnCancelListener { finish() }
                val dialog = builder.create()
                dialog.show()
            })
        }else{
            finish()
        }
    }
}
