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

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.balicodes.quicksms.viewmodel.SendingHistoryViewModel

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val viewModel = ViewModelProviders.of(this).get(SendingHistoryViewModel::class.java)

        if (savedInstanceState == null) {

            supportFragmentManager.beginTransaction()
                    .add(R.id.container, SendingHistoryListFragment())
                    .commit()

            val sendId = intent.getStringExtra(Config.SEND_ID_EXTRA_KEY)

            // If sendId exists, show sending status fragment of that id.
            if (sendId != null) {
                viewModel.setSelectedSendHistory(sendId)

                supportFragmentManager.beginTransaction()
                        .replace(R.id.container, SendingStatusListFragment())
                        .addToBackStack(null)
                        .commit()
            }
        }
    }
}