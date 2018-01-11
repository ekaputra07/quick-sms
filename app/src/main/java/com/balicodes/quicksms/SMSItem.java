package com.balicodes.quicksms;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.content.SharedPreferences;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by eka on 6/27/15.
 */
public class SMSItem {
    public static final int STATUS_INACTIVE = 0;
    public static final int STATUS_SENDING = 1;
    public static final int STATUS_LISTENING = 2;

    public static final int STATUS_SENT = 3;
    public static final int STATUS_DELIVERED = 4;
    public static final int STATUS_FAILED = 5;

    private long id = 0;
    private String title, number, message, shortcut = "";
    private int status;

    // status stats of current sending, stored as Parcelable object of recipient.
    // Why Parcelable? because we can easily sent it as Intent extras.
    public ArrayList<Parcelable> sentInfoList = new ArrayList<Parcelable>();
    public ArrayList<Parcelable> failedInfoList = new ArrayList<Parcelable>();
    public ArrayList<Parcelable> receivedInfoList = new ArrayList<Parcelable>();

    public SMSItem() {
    }

    public SMSItem(long id, String title, String number,
                   String message, String shortcut) {
        this.id = id;
        this.title = title;
        this.number = number;
        this.message = message;
        this.shortcut = shortcut; // now used to store shortcut state
        this.status = STATUS_INACTIVE;
    }

    public void setSending() {
        // each time sending, clear current status.
        sentInfoList.clear();
        failedInfoList.clear();
        receivedInfoList.clear();

        status = STATUS_SENDING;
    }

    public void addStatusInfo(int statusType, Recipient rec) {
        // the first time we get status from BroadCast receiver, set sending status to LISTENING
        status = STATUS_LISTENING;

        switch (statusType) {
            case STATUS_SENT:
                this.sentInfoList.add(rec);
                break;
            case STATUS_FAILED:
                this.failedInfoList.add(rec);
                break;
            case STATUS_DELIVERED:
                this.receivedInfoList.add(rec);
                break;
        }
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return this.status;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNumber() {
        return number;
    }

    public String getLabel() {
        List<String> recipientString = new ArrayList<String>();
        List<String[]> rec = SMSItem.parseReceiverCSV(number);
        for (String[] r : rec) {
            if (r[0].length() == 0) {
                recipientString.add(r[1]);
            } else {
                recipientString.add(r[0]);
            }
        }
        return TextUtils.join(", ", recipientString);
    }

    public String getMessage() {
        return message;
    }

    public String getShortcut() {
        return shortcut;
    }

    public boolean hasId() {
        return id != 0;
    }

    public int totalRecipients() {
        return SMSItem.parseReceiverCSV(number).size();
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putLong("id", id);
        bundle.putString("title", title);
        bundle.putString("number", number);
        bundle.putString("message", message);
        bundle.putString("shortcut", shortcut);
        return bundle;
    }

    public static SMSItem fromBundle(Bundle bundle) {
        try {
            SMSItem item = new SMSItem(bundle.getLong("id"), bundle.getString("title"),
                    bundle.getString("number"), bundle.getString("message"), bundle.getString("shortcut"));
            return item;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /* ---------------------------------------------------------------------------------------------
    Convert "Eka:0817476584,Vera:123456789"
    into [["Eka", "08174765840"], ["Vera","123456789"]]
    ----------------------------------------------------------------------------------------------*/
    public static List<String[]> parseReceiverCSV(String numberCSV) {
        // create a list of map.
        List<String[]> list = new ArrayList<String[]>();

        // always return at least on empty list
        if (numberCSV == null || numberCSV.length() == 0) {
            list.add(new String[]{"", ""});
            return list;
        }

        for (String contact : numberCSV.split(",")) {
            // handle legacy number format
            if (contact.indexOf(":") == -1) {
                list.add(new String[]{"", contact});
            } else {
                if (contact.split(":").length != 2) {
                    list.add(new String[]{"", ""});
                } else {
                    list.add(contact.split(":"));
                }
            }
        }
        return list;
    }


    public static SMSItem create(Context context, String title, String number, String message, String shortcut) {
        DBHelper dbHelper = new DBHelper(context);
        Long insertId = dbHelper.insert(title, number, message, shortcut);
        dbHelper.close();

        SMSItem item = new SMSItem(insertId, title, number, message, shortcut);
        return item;
    }

    public SMSItem update(Context context, String title, String number, String message, String shortcut) {
        DBHelper dbHelper = new DBHelper(context);
        dbHelper.update(id, title, number, message, shortcut);
        dbHelper.close();
        return this;
    }

    public static SMSItem copyFrom(Context context, SMSItem item){
        return SMSItem.create(context, item.getTitle() + " (copy)", item.getNumber(), item.getMessage(), item.getShortcut());
    }

    public void delete(Context context) {
        DBHelper dbHelper = new DBHelper(context);
        dbHelper.delete(id);
        dbHelper.close();
    }

    public void send(Context context, Boolean enableDeliveryReport) {
        SmsManager smsManager = SmsManager.getDefault();

        Log.d("SMSItem", "====> Delivery report: " + enableDeliveryReport);

        List<String[]> recipients = SMSItem.parseReceiverCSV(this.getNumber());

        int requestCode = 0;

        for (String[] recipient : recipients) {

            // create Recipient object and later will be added to PendingIntent extra.
            Recipient rec = new Recipient(getId(), recipient[0], recipient[1]);

            try {
                Log.d("SMSItem", "====> Sending to " + recipient[1]);

                // Create sent pending Intent
                Intent sentIntent = new Intent(Config.SENT_STATUS_ACTION);
                sentIntent.putExtra(Config.RECIPIENT_EXTRA_KEY, rec.toBundle());
                PendingIntent sentPI = PendingIntent.getBroadcast(context, requestCode, sentIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                // Create delivery pending Intent, only if enabled
                PendingIntent deliveryPI = null;
                if (enableDeliveryReport) {
                    Intent deliveryIntent = new Intent(Config.DELIVERY_STATUS_ACTION);
                    deliveryIntent.putExtra(Config.RECIPIENT_EXTRA_KEY, rec.toBundle());
                    deliveryPI = PendingIntent.getBroadcast(context, requestCode, deliveryIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                }

                smsManager.sendTextMessage(recipient[1], null, this.getMessage(), sentPI, deliveryPI);
            }catch (SecurityException e){
                Log.e("SMSItem", "====> [Security] Error sending to " + recipient[1]);
                Log.e("SMSItem", e.getLocalizedMessage());
            } catch (Exception e) {
                Log.e("SMSItem", "====> Error sending to " + recipient[1]);
                e.printStackTrace();
            }
            requestCode++;
        }
    }
}
