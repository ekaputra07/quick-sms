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

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.container, SMSListFragment())
                    .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    /*----------------------------------------------------------------------------------------------
    Share application.
     ---------------------------------------------------------------------------------------------*/
    private fun shareApp() {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        val shareTxt = getString(R.string.share_text).replace("{DOWNLOAD_LINK}", getString(R.string.download_link))
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareTxt)
        sendIntent.type = "text/plain"
        startActivity(sendIntent)
    }

    /*----------------------------------------------------------------------------------------------
    Rate application.
    --------------------------------------------------------------------------------------------- */
    private fun rateApp() {
        val uri = Uri.parse("market://details?id=" + this.packageName)
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + this.packageName)))
        }

    }

    /*----------------------------------------------------------------------------------------------
    Return notification message builder instance.
    --------------------------------------------------------------------------------------------- */
    fun buildNotificationMessage(title: String, message: String): NotificationCompat.Builder {
        //        Intent resultIntent = new Intent(this, MainActivity.class);
        //        PendingIntent resultPendingIntent =
        //                PendingIntent.getActivity(
        //                        getActivity(),
        //                        0,
        //                        resultIntent,
        //                        PendingIntent.FLAG_UPDATE_CURRENT
        //                );
        return NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
    }

    /*----------------------------------------------------------------------------------------------
    Show notification.
    --------------------------------------------------------------------------------------------- */
    fun notify(notifID: Int, mBuilder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notifID, mBuilder.build())
    }

    /**
     * Open Export / Import screen
     */
    private fun showImportExportScreen() {
        val intent = Intent(this, ExportImportActivity::class.java)
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        when (id) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.action_export_import -> showImportExportScreen()
            R.id.action_share -> shareApp()
            R.id.action_rate -> rateApp()
            R.id.action_about -> {
                val about = AboutDialog(this)
                about.rateListener = View.OnClickListener { rateApp() }
                about.shareListener = View.OnClickListener { shareApp() }
                about.show()
            }
            android.R.id.home -> {
                hideKeyboard()
                super.onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun hideKeyboard() {
        // Check if no view has focus:
        val view = currentFocus
        if (view != null) {
            val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == Config.SEND_SMS_PERMISSION_REQUEST && grantResults.size > 0) {
            Log.d(MainActivity::class.java.name, grantResults.size.toString())

            if (Build.VERSION.SDK_INT == 26) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(R.string.permission_sms_granted), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, getString(R.string.permission_sms_denied), Toast.LENGTH_LONG).show()
                }
            } else {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(R.string.permission_sms_granted), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, getString(R.string.permission_sms_denied), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
