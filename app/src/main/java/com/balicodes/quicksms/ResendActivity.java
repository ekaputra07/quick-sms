package com.balicodes.quicksms;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

public class ResendActivity extends AppCompatActivity {
    private final String TAG = "ResendActivity";
    private final String RECIPIENT_INDEX_KEY = "RECIPIENT_INDEX";

    private ArrayList<Recipient> recipients = new ArrayList<Recipient>();
    private ListView listView;
    private ResendListAdapter resendListAdapter;
    private String message;
    private SentReceiver sentReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resend);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            recipients = bundle.getParcelableArrayList(Config.RECIPIENT_PARCELS_EXTRA_KEY);
            message = bundle.getString(Config.SMS_MESSAGE_EXTRA_KEY);
        }

        resendListAdapter = new ResendListAdapter();
        listView = (ListView) findViewById(R.id.resendList);
        listView.setAdapter(resendListAdapter);
    }

    private void resendSMS(int index) {

        Recipient rec = recipients.get(index);
        rec.setSending(true);
        rec.setSent(false);

        // notify list one of its recipient status are sending.
        recipients.set(index, rec);
        resendListAdapter.notifyDataSetChanged();

        Log.d(this.getClass().getName(), rec.getNumber() + " - " + message);

        SmsManager smsManager = SmsManager.getDefault();

        // Create sent pending Intent
        Intent sentIntent = new Intent(Config.SENT_STATUS_ACTION);
        sentIntent.putExtra(Config.RECIPIENT_EXTRA_KEY, rec);
        sentIntent.putExtra(RECIPIENT_INDEX_KEY, index);

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, sentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        smsManager.sendTextMessage(rec.getNumber(), null, message, sentPI, null); // only listen for sent status
    }

    private class ResendListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return recipients.size();
        }

        @Override
        public Object getItem(int position) {
            return recipients.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.resend_list_item, parent, false);
            }

            Recipient recipient = (Recipient) getItem(position);

            TextView resendNumber = (TextView) convertView.findViewById(R.id.resendNumber);
            resendNumber.setText(recipient.getNumber());

            TextView resendName = (TextView) convertView.findViewById(R.id.resendName);
            resendName.setText(recipient.getName());

            if (recipient.getName().equals(""))
                resendName.setText("N/A");

            final ProgressBar progressBar = (ProgressBar) convertView.findViewById(R.id.progressBar);
            final ImageView successImg = (ImageView) convertView.findViewById(R.id.resendSuccess);
            final Button retryBtn = (Button) convertView.findViewById(R.id.resendBtn);

            retryBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resendSMS(position);
                }
            });

            // togle buttons and progressbar display based on recepient status.
            if (recipient.isSending()) {
                progressBar.setVisibility(View.VISIBLE);
                retryBtn.setVisibility(View.GONE);
            } else {
                if (!recipient.isSent()) {
                    progressBar.setVisibility(View.GONE);
                    retryBtn.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(View.GONE);
                    retryBtn.setVisibility(View.GONE);
                    successImg.setVisibility(View.VISIBLE);
                }
            }

            return convertView;
        }
    }

    /*----------------------------------------------------------------------------------------------
    Private class to handle Sending status.
    ----------------------------------------------------------------------------------------------*/
    private class SentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();

            if (getResultCode() == Activity.RESULT_OK) {
                if (bundle != null) {
                    int index = bundle.getInt(RECIPIENT_INDEX_KEY);

                    Recipient rec = (Recipient) bundle.get(Config.RECIPIENT_EXTRA_KEY);
                    rec.setSending(false);
                    rec.setSent(true);
                    recipients.set(index, rec);
                    resendListAdapter.notifyDataSetChanged();

                    Log.d("SentReceiver", "====> OK: " + rec.getNumber());
                }
            } else {
                if (bundle != null) {
                    int index = bundle.getInt(RECIPIENT_INDEX_KEY);

                    Recipient rec = (Recipient) bundle.get(Config.RECIPIENT_EXTRA_KEY);
                    rec.setSending(false);
                    rec.setSent(false);
                    recipients.set(index, rec);
                    resendListAdapter.notifyDataSetChanged();

                    Log.d("SentReceiver", "====> FAILED: " + rec.getNumber());
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        sentReceiver = new SentReceiver();
        registerReceiver(sentReceiver, new IntentFilter(Config.SENT_STATUS_ACTION));
        Log.d(this.getClass().getName(), "====> Registering a sentReceiver");
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (sentReceiver != null) {
            unregisterReceiver(sentReceiver);
            Log.d(this.getClass().getName(), "====> Unregistering a sentReceiver");

        }
    }
}
