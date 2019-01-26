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

class Config {
    companion object {
        const val PICK_CONTACT_REQUEST = 1
        const val PICK_FILE_REQUEST = 1
        const val SEND_SMS_PERMISSION_REQUEST = 1
        const val WRITE_STORAGE_PERMISSION_REQUEST = 2
        const val MAX_RECIPIENTS_PER_SMS = 10
        const val SMS_DATA_BASE_URI = "content://com.balicodes.quicksms.SMS"
        const val SENT_STATUS_ACTION = "com.balicodes.quicksms.SENT_STATUS"
        const val DELIVERY_STATUS_ACTION = "com.balicodes.quicksms.DELIVERY_STATUS"
        const val SMS_BUNDLE_EXTRA_KEY = "com.balicodes.quicksms.SMS_BUNDLE"
        const val RECIPIENT_EXTRA_KEY = "com.balicodes.quicksms.SMS_RECIPIENT"
        const val RECIPIENT_PARCELS_EXTRA_KEY = "com.balicodes.quicksms.RECIPIENT_PARCEL"
        const val SMS_MESSAGE_EXTRA_KEY = "com.balicodes.quicksms.SMS_MESSAGE"
        const val SEND_ID_EXTRA_KEY = "com.balicodes.quicksms.SEND_ID"
        const val SEND_STATUS_ID_EXTRA_KEY = "com.balicodes.quicksms.SEND_STATUS_ID"
        const val CREATE_SENDING_NOTIFICATION = "com.balicodes.quicksms.CREATE_SENDING_NOTIFICATION"
    }
}
