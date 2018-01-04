package com.balicodes.quicksms;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by eka on 6/25/16.
 */
public class ShortcutHandlerActivity extends Activity {
    private int currentSendingCount = 0;
    private SMSItem smsItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Uri uri = intent.getData();
        long smsID = ContentUris.parseId(uri);

        if (smsID != 0) {
            DBHelper dbHelper = new DBHelper(this);
            smsItem = dbHelper.get(smsID);
            dbHelper.close();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.confirm_sending);

            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    if (smsItem != null) {
                        // Sent via sending service
                        Intent intent = new Intent(ShortcutHandlerActivity.this, SendingService.class);
                        intent.putExtra(Config.SMS_BUNDLE_EXTRA_KEY, smsItem.toBundle());

                        startService(intent);
                    }
                    finish();
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    finish();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            finish();
        }
    }
}
