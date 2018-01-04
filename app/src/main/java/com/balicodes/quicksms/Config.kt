package com.balicodes.quicksms;

/**
 * Created by eka on 6/21/16.
 */
class Config {
    companion object {
        const val PICK_CONTACT_REQUEST = 1
        const val PICK_FILE_REQUEST = 1
        const val SEND_SMS_PERMISSION_REQUEST = 1
        const val WRITE_STORAGE_PERMISSION_REQUEST = 2
        const val MAX_RECIPIENTS_PER_SMS = 5
        const val SMS_DATA_BASE_URI = "content://com.balicodes.quicksms.SMS"
        const val SENT_STATUS_ACTION = "com.balicodes.quicksms.SENT_STATUS"
        const val DELIVERY_STATUS_ACTION = "com.balicodes.quicksms.DELIVERY_STATUS"
        const val SMS_BUNDLE_EXTRA_KEY = "com.balicodes.quicksms.SMS_BUNDLE"
        const val SMS_ID_EXTRA_KEY = "com.balicodes.quicksms.SMS_ID"
        const val RECIPIENT_EXTRA_KEY = "com.balicodes.quicksms.SMS_RECIPIENT"
        const val RECIPIENT_PARCELS_EXTRA_KEY = "com.balicodes.quicksms.RECIPIENT_PARCEL"
        const val SMS_MESSAGE_EXTRA_KEY = "com.balicodes.quicksms.SMS_MESSAGE"
    }
}
