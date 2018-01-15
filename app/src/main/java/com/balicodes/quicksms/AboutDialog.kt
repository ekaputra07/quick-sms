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
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView

class AboutDialog constructor(val context: Context) {
    private val builder: AlertDialog.Builder = AlertDialog.Builder(context)
    var rateListener: View.OnClickListener? = null
    var shareListener: View.OnClickListener? = null

    fun show() {
        // Inflate the about message contents
        val ctx = context as Activity
        val messageView = ctx.layoutInflater.inflate(R.layout.about, null, false)

        val about: TextView = messageView.findViewById(R.id.about1)

        // Update about text based on BuildConfig.
        val aboutTxt = ctx.resources.getString(R.string.about_1)
                .replace("{APP_NAME}", ctx.resources.getString(R.string.app_name))
                .replace("{VERSION_NAME}", ctx.resources.getString(R.string.version_name))
        about.text = aboutTxt

        val btnRate: Button = messageView.findViewById(R.id.btnRate)
        if (rateListener != null)
            btnRate.setOnClickListener(rateListener)

        val btnShare: Button = messageView.findViewById(R.id.btnShare)
        if (shareListener != null)
            btnShare.setOnClickListener(shareListener)

        builder.setIcon(R.drawable.ic_launcher)
        builder.setTitle(R.string.app_name)
        builder.setView(messageView)
        builder.create()
        builder.show()
    }
}
