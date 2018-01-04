package com.balicodes.quicksms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by eka on 6/25/16.
 */
class SMSStatusBroadcastReceiver : BroadcastReceiver() {

    private val TAG = javaClass.simpleName

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val bundle = intent.extras

        if (bundle != null) {
            val rec = bundle.get(Config.RECIPIENT_EXTRA_KEY) as Recipient

            if (action == Config.SENT_STATUS_ACTION) {
                Log.d(TAG, "====> SENT STATUS RECEIVED FOR " + rec.number)
            }

            if (action == Config.DELIVERY_STATUS_ACTION) {
                Log.d(TAG, "====> DELIVERY STATUS RECEIVED FOR " + rec.number);
            }
        }
    }
}
