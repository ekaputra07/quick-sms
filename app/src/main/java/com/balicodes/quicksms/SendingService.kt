package com.balicodes.quicksms;

import android.app.IntentService
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import java.lang.Exception

/**
 * Created by eka on 6/25/16.
 */
class SendingService : IntentService("SendingService") {

    private val TAG = javaClass.simpleName

    override fun onHandleIntent(intent: Intent) {
        try {
            Log.d(TAG, "====> Start sending")
            val bundle: Bundle? = intent.getBundleExtra(Config.SMS_BUNDLE_EXTRA_KEY)

            if (bundle != null) {
                val sp = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val enableDeliveryReport = sp.getBoolean(getString(R.string.pref_enable_delivery_report_key), false)

                val item = SMSItem.fromBundle(bundle)
                item.send(applicationContext, enableDeliveryReport)
            }
            Log.d(TAG, "====> Finished sending")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
