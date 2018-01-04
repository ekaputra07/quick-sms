package com.balicodes.quicksms;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by eka on 6/27/15.
 */
public class SMSListFragment extends Fragment implements AdapterView.OnItemClickListener {
    private final String TAG = SMSListFragment.class.toString();

    private DBHelper dbHelper;
    private List<SMSItem> listSMS = new ArrayList<SMSItem>();
    private SMSListAdapter listAdapter;
    private ListView listView;
    private FrameLayout tapInfo;
    private boolean afterSending = false;
    private MediaPlayer beep;
    private SentReceiver sentReceiver;
    private DeliveryReceiver deliveryReceiver;
    private boolean isReceiverRegistered = false;
    private SMSItem currentSMSitem;
    private int currentSMSitemIndex;
    private int currentSendingCount = 0;
    private SharedPreferences sp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        dbHelper = new DBHelper(getActivity());
        listAdapter = new SMSListAdapter(getActivity(), listSMS);
        beep = MediaPlayer.create(getActivity(), R.raw.beep);
        beep.setVolume((float) 0.5, (float) 0.5);
    }

    private void loadList() {
        listSMS.clear();
        List<SMSItem> all = dbHelper.all();

        // show hide tap info frame.
        if (all.size() == 0) {
            tapInfo.setVisibility(View.GONE);
        } else {
            tapInfo.setVisibility(View.VISIBLE);
        }

        for (SMSItem item : all) {
            listSMS.add(item);
        }
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.sms_list_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_new:
                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.container, new SMSFormFragment());
                ft.addToBackStack(null);
                ft.commit();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sms_list_fragment, container, false);
        tapInfo = (FrameLayout) view.findViewById(R.id.tap_info);
        listView = (ListView) view.findViewById(R.id.listView);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(this);
        registerForContextMenu(listView);

        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (afterSending) {
            afterSending = false;
            return;
        }

        SMSItem smsitem = listSMS.get(position);
        SMSFormFragment form = new SMSFormFragment();
        form.setArguments(smsitem.toBundle());

        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, form)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack("sms_form")
                .commit();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((MainActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        loadList();
    }

    /*----------------------------------------------------------------------------------------------
    Try to send sms on long tap.
    ----------------------------------------------------------------------------------------------*/
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

        if (currentSMSitem != null) {
            resetCurrentSMSItem();
        }

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        currentSMSitem = listSMS.get(info.position);
        currentSMSitemIndex = info.position;

        tryToSend();
    }

    /*----------------------------------------------------------------------------------------------
    Register sending and delivery receivers.
    ----------------------------------------------------------------------------------------------*/
    private void registerReceivers() {
        sentReceiver = new SentReceiver();
        deliveryReceiver = new DeliveryReceiver();

        getActivity().registerReceiver(sentReceiver, new IntentFilter(Config.SENT_STATUS_ACTION));
        getActivity().registerReceiver(deliveryReceiver, new IntentFilter(Config.DELIVERY_STATUS_ACTION));

        isReceiverRegistered = true;
        Log.d(TAG, "Sent and Delivery receivers registered.");
    }

    /*----------------------------------------------------------------------------------------------
    Un-register sending and delivery receivers.
    ----------------------------------------------------------------------------------------------*/
    private void unregisterReceivers() {
        try {
            if (isReceiverRegistered) {
                getActivity().unregisterReceiver(sentReceiver);
                getActivity().unregisterReceiver(deliveryReceiver);
                Log.d(TAG, "Sent and Delivery receivers un-registered.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /* ---------------------------------------------------------------------------------------------
    Try to send the message and show confirmation dialog if user set so.
    ----------------------------------------------------------------------------------------------*/
    private void tryToSend() {
        afterSending = true;
        boolean confirm = sp.getBoolean(getString(R.string.pref_sending_confirmation_key), false);

        // If sending confirmation required.
        if (confirm) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.confirm_sending);

            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    sendSMS();
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();

        } else {
            sendSMS();
        }
    }

    /*----------------------------------------------------------------------------------------------
    The actual function to send the message.
     ---------------------------------------------------------------------------------------------*/
    private void sendSMS() {
        if (currentSMSitem == null) return;

        // Android API 23+ requires this
        if(Build.VERSION.SDK_INT == 26){
            int permissionSendSMS = ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.SEND_SMS);
            int permissionReadPhoneState = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_PHONE_STATE);

            if(permissionSendSMS != PackageManager.PERMISSION_GRANTED || permissionReadPhoneState != PackageManager.PERMISSION_GRANTED){
                String[] perms = new String[]{Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE};
                ActivityCompat.requestPermissions(getActivity(), perms, Config.SEND_SMS_PERMISSION_REQUEST);
                return;
            }
        }else{
            int permission = ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.SEND_SMS);
            if(permission != PackageManager.PERMISSION_GRANTED){
                String[] perms = new String[]{Manifest.permission.SEND_SMS};
                ActivityCompat.requestPermissions(getActivity(), perms, Config.SEND_SMS_PERMISSION_REQUEST);
                return;
            }
        }

        Log.d(TAG, "Sending sms: " + currentSMSitem.getTitle());
        listAdapter.setSending(currentSMSitemIndex);
        boolean playBeep = sp.getBoolean(getString(R.string.pref_enable_beep_key), true);
        if (playBeep) beep.start();

        // start our sending service
        Intent sendingIntent = new Intent(getActivity().getApplicationContext(), SendingService.class);
        sendingIntent.putExtra(Config.SMS_BUNDLE_EXTRA_KEY, currentSMSitem.toBundle());
        getActivity().startService(sendingIntent);
    }

    private void resendSingleRecipient() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.sending_failed);
        builder.setMessage(R.string.sending_failed_info);
        builder.setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                currentSendingCount = 0;
                sendSMS();
            }
        });
        builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                resetCurrentSMSItem();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /*----------------------------------------------------------------------------------------------
    Private class to handle Sending status.
    ----------------------------------------------------------------------------------------------*/
    private class SentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle recBundle = intent.getBundleExtra(Config.RECIPIENT_EXTRA_KEY);

            if (getResultCode() == Activity.RESULT_OK) {
                if (recBundle != null) {
                    Recipient rec = Recipient.fromBundle(recBundle);
                    listAdapter.notifyStatusChanged(currentSMSitemIndex, SMSItem.STATUS_SENT, rec);
                    Log.d("SentReceiver", "====> OK: " + rec.getNumber());
                }
            } else {
                if (recBundle != null) {
                    Recipient rec = Recipient.fromBundle(recBundle);
                    Log.d("SentReceiver", String.format("%s - %s - %s", rec.getId(), rec.getName(), rec.getNumber()));

                    listAdapter.notifyStatusChanged(currentSMSitemIndex, SMSItem.STATUS_FAILED, rec);
                    Log.d("SentReceiver", "====> FAILED: " + rec.getNumber());
                }
            }

            // If finish sending, check whether all sent successfully.
            // if not, show resend screen with all failed recipients.
            currentSendingCount++;
            if (currentSendingCount == currentSMSitem.totalRecipients()) {

                if (currentSMSitem.failedInfoList.size() > 0) {
                    afterSending = true;

                    // If the recipient only one, no need to show Resend screen
                    if (currentSMSitem.totalRecipients() == 1) {
                        resendSingleRecipient();
                    } else {

                        boolean confirm_before_send = sp.getBoolean(getString(R.string.pref_sending_confirmation_key), false);
                        if (confirm_before_send) {
                            afterSending = false;
                        }

                        Intent resendIntent = new Intent(getActivity().getApplicationContext(), ResendActivity.class);
                        resendIntent.putExtra(Config.SMS_MESSAGE_EXTRA_KEY, currentSMSitem.getMessage());
                        resendIntent.putParcelableArrayListExtra(Config.RECIPIENT_PARCELS_EXTRA_KEY, currentSMSitem.failedInfoList);
                        startActivity(resendIntent);
                    }
                }
            }
        }
    }

    /*----------------------------------------------------------------------------------------------
    Private class to handle Delivery status.
    ----------------------------------------------------------------------------------------------*/
    private class DeliveryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle recBundle = intent.getBundleExtra(Config.RECIPIENT_EXTRA_KEY);

            if (getResultCode() == Activity.RESULT_OK) {
                if (recBundle != null) {
                    Recipient rec = Recipient.fromBundle(recBundle);
                    listAdapter.notifyStatusChanged(currentSMSitemIndex, SMSItem.STATUS_DELIVERED, rec);
                    Log.d("DeliveryReceiver", "====> OK: " + rec.getNumber());
                }
            }
        }
    }

    /*----------------------------------------------------------------------------------------------
    Reset current selected SMSItem state.
    ----------------------------------------------------------------------------------------------*/
    private void resetCurrentSMSItem() {
        currentSMSitem = null;
        currentSMSitemIndex = -1;
        currentSendingCount = 0;
        afterSending = false;
        Log.d(TAG, "Reset currentSMSItem");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (dbHelper != null) {
            dbHelper.close();
        }
        unregisterReceivers();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceivers();
        sp = PreferenceManager.getDefaultSharedPreferences(getActivity());

    }
}
