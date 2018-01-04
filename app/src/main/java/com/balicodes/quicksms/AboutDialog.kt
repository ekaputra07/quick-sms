package com.balicodes.quicksms

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView

/**
 * Created by eka on 6/18/16.
 */
class AboutDialog constructor(context: Context) {
    private val context = context
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
        about.setText(aboutTxt);

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
