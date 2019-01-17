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

package com.balicodes.quicksms.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.balicodes.quicksms.Config
import java.util.logging.Logger

class SmsPermissionChecker {
    companion object {

        private val LOG: Logger = Logger.getLogger(SmsPermissionChecker::class.java.name)

        fun hasPermissionToSendSms(context: Context): Boolean {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                LOG.info("API < 23: No need to check sendSMS permission.")
                return true
            }

            LOG.info("API >= 23: Checking sendSMS permission...")
            return if (Build.VERSION.SDK_INT == 26) {
                // Android API 23+ requires this
                // There's a bug on API 26 which requires READ_PHONE_STATE to be able to send SMS.
                val permissionSendSMS = ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS)
                val permissionReadPhoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                (permissionSendSMS == PackageManager.PERMISSION_GRANTED) && (permissionReadPhoneState == PackageManager.PERMISSION_GRANTED)

            } else {
                val permissionSendSMS = ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS)
                LOG.info("Permission is: " + (permissionSendSMS == PackageManager.PERMISSION_GRANTED).toString())
                permissionSendSMS == PackageManager.PERMISSION_GRANTED
            }
        }

        fun requestSendSmsPermission(activity: Activity?) {
            LOG.info("Requesting sendSMS permission...")

            activity?.let {
                if (Build.VERSION.SDK_INT == 26) {
                    val perms = arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE)
                    ActivityCompat.requestPermissions(it, perms, Config.SEND_SMS_PERMISSION_REQUEST)
                } else {
                    val perms = arrayOf(Manifest.permission.SEND_SMS)
                    ActivityCompat.requestPermissions(it, perms, Config.SEND_SMS_PERMISSION_REQUEST)
                }
            }

        }
    }
}