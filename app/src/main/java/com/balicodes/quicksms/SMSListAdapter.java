package com.balicodes.quicksms;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Created by eka on 6/27/15.
 */
public class SMSListAdapter extends BaseAdapter {
    private List<SMSItem> items;
    private Context context;
    private SharedPreferences sp;

    public SMSListAdapter(Context context, List<SMSItem> items) {
        this.context = context;
        this.items = items;

        sp = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setSending(int position) {
        items.get(position).setSending();
        notifyDataSetChanged();
    }

    public void notifyStatusChanged(int position, int statusType, Recipient rec) {
        items.get(position).addStatusInfo(statusType, rec);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.sms_list_item, parent, false);
        }

        String title = items.get(position).getTitle();
        TextView titleTxt = (TextView) convertView.findViewById(R.id.smsTitleTxt);
        titleTxt.setText(title);

        String number = items.get(position).getLabel();
        TextView msgTxt = (TextView) convertView.findViewById(R.id.smsMessageTxt);
        msgTxt.setText(context.getString(R.string.to) + " " + number);

        TextView statusTxt = (TextView) convertView.findViewById(R.id.smsStatusTxt);
        RelativeLayout statusContainer = (RelativeLayout) convertView.findViewById(R.id.statusContainer);
        TextView statusFailed = (TextView) convertView.findViewById(R.id.statusFailed);
        TextView statusSent = (TextView) convertView.findViewById(R.id.statusSent);
        TextView statusDelivered = (TextView) convertView.findViewById(R.id.statusDelivered);

        SMSItem item = items.get(position);
        int status = item.getStatus();

        switch (status) {
            case SMSItem.STATUS_INACTIVE:
                statusTxt.setVisibility(View.GONE);
                statusContainer.setVisibility(View.GONE);
                break;

            case SMSItem.STATUS_SENDING:
                statusTxt.setTextColor(context.getResources().getColor(R.color.grey));
                statusTxt.setText(context.getResources().getString(R.string.status_sending));
                statusTxt.setVisibility(View.VISIBLE);
                statusContainer.setVisibility(View.GONE);
                break;

            default:
                boolean enableDeliveryReport = sp.getBoolean(context.getString(R.string.pref_enable_delivery_report_key), false);

                statusTxt.setVisibility(View.GONE);
                statusFailed.setText(context.getResources().getString(R.string.status_failed)
                        .replace("{COUNT}", String.valueOf(item.failedInfoList.size())));
                statusSent.setText(context.getResources().getString(R.string.status_sent)
                        .replace("{COUNT}", String.valueOf(item.sentInfoList.size())));

                if (enableDeliveryReport) {
                    statusDelivered.setText(context.getResources().getString(R.string.status_delivered)
                            .replace("{COUNT}", String.valueOf(item.receivedInfoList.size())));
                    statusDelivered.setVisibility(View.VISIBLE);
                } else {
                    statusDelivered.setVisibility(View.GONE);
                }

                statusContainer.setVisibility(View.VISIBLE);
        }

        return convertView;
    }
}
