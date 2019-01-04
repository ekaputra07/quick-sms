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

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import com.balicodes.quicksms.Config
import com.balicodes.quicksms.R
import com.balicodes.quicksms.view.misc.ShortcutHandlerActivity
import com.balicodes.quicksms.entity.MessageEntity
import java.util.logging.Logger

class Shortcuts(val context: Context) {

    companion object {
        val LOG: Logger = Logger.getLogger(Shortcuts::class.java.name)
    }

    private fun getShortcutId(messageId: Long): String {
        return "QSMS_%s".format(messageId)
    }

    fun canCreateShortcuts(messageId: Long?): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            val shortcuts = shortcutManager.dynamicShortcuts

            // only allow create shortcuts max 4
            // 1. If current shortcuts count less than 4, allow create new one.
            // 2. If slots already full (4) but shortcut with this ID already exist allow (update).
            // 3. else, disallow.
            if (shortcuts.size < 4) {
                return true
            } else {
                val shortcutId = getShortcutId(messageId!!)
                if (isShortcutExists(shortcuts, shortcutId)) {
                    return true
                }
            }
            return false
        }
        return true
    }

    @SuppressLint("NewApi")
    private fun isShortcutExists(shortcuts: List<ShortcutInfo>, id: String): Boolean {
        var exists = false
        loop@ for (s in shortcuts) {
            if (s.id == id) {
                exists = true
                break@loop
            }
        }
        return exists
    }

    fun toggleShortcut(message: MessageEntity, create: Boolean) {

        // Create an intent to send SMS when shortcut tapped.
        val shortcutIntent = Intent(context, ShortcutHandlerActivity::class.java)
        shortcutIntent.action = Intent.ACTION_MAIN
        shortcutIntent.data = ContentUris.withAppendedId(Uri.parse(Config.SMS_DATA_BASE_URI), message.id!!)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            toggleNewShortcut(message, shortcutIntent, create);
        } else {
            toggleLegacyShortcut(message, shortcutIntent, create);
        }
    }

    @SuppressLint("NewApi")
    private fun toggleNewShortcut(message: MessageEntity, intent: Intent, create: Boolean) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        val shortcutId = getShortcutId(message.id!!)

        LOG.info("Attempting to create/delete shortcut with ID:" + shortcutId)

        val shortcutInfo = ShortcutInfo.Builder(context, shortcutId)
                .setShortLabel(message.title)
                .setIcon(Icon.createWithResource(context, R.drawable.ic_launcher_shortcut))
                .setIntent(intent)
                .build()

        val shortcuts = shortcutManager.dynamicShortcuts
        LOG.info("Shortcuts length: " + shortcuts.size)

        val exists = isShortcutExists(shortcuts, shortcutId)

        if (create && !exists) {
            shortcutManager.addDynamicShortcuts(listOf(shortcutInfo))
        } else if (create && exists) {
            shortcutManager.updateShortcuts(listOf(shortcutInfo))
        } else {
            shortcutManager.removeDynamicShortcuts(listOf(shortcutId))
        }
    }

    private fun toggleLegacyShortcut(message: MessageEntity, intent: Intent, create: Boolean) {
        val addInt = Intent()
        addInt.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent)
        addInt.putExtra(Intent.EXTRA_SHORTCUT_NAME, message.title)
        addInt.putExtra("duplicate", false)
        addInt.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(context, R.drawable.ic_launcher_shortcut))
        if (create) {
            addInt.action = "com.android.launcher.action.INSTALL_SHORTCUT"
        } else {
            addInt.action = "com.android.launcher.action.UNINSTALL_SHORTCUT"
        }

        context.applicationContext.sendBroadcast(addInt)
    }
}