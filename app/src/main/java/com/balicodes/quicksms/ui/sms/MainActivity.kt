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

package com.balicodes.quicksms.ui.sms

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.balicodes.quicksms.Config
import com.balicodes.quicksms.R
import com.balicodes.quicksms.ui.history.HistoryActivity
import com.balicodes.quicksms.ui.misc.AboutDialog
import com.balicodes.quicksms.ui.misc.ExportImportActivity
import com.balicodes.quicksms.ui.settings.SettingsActivity
import com.balicodes.quicksms.util.Notification
import java.util.logging.Logger

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Add support for vector icons.
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        setContentView(R.layout.activity_main)

        // create notification channel
        Notification.createNotificationChannel(this)

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
        val shareTxt = getString(R.string.share_text).format(getString(R.string.download_link))
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

    /**
     * Open Export / Import screen
     */
    private fun showImportExportScreen() {
        val intent = Intent(this, ExportImportActivity::class.java)
        startActivity(intent)
    }

    /**
     * Open history screen
     */
    private fun showHistoryScreen(sendId: String?) {
        val intent = Intent(this, HistoryActivity::class.java)
        if (sendId != null) {
            intent.putExtra(Config.SEND_ID_EXTRA_KEY, sendId)
        }
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        when (id) {
            R.id.action_history -> showHistoryScreen(null)
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
        if (requestCode == Config.SEND_SMS_PERMISSION_REQUEST && grantResults.isNotEmpty()) {

            LOG.info(grantResults.size.toString())

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

    companion object {
        private val LOG: Logger = Logger.getLogger(MainActivity::class.java.name)
    }
}
