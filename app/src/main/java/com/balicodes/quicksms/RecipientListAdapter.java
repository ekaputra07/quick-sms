package com.balicodes.quicksms;

import android.content.Context;
import android.content.Intent;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by eka on 6/18/16.
 */
public class RecipientListAdapter extends BaseAdapter {
    private List<String[]> items;
    private SMSFormFragment fragment;

    public RecipientListAdapter(SMSFormFragment fragment, List<String[]> items) {
        this.fragment = fragment;
        this.items = items;
    }

    public void addItem(String[] item) {
        items.add(item);
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        items.remove(position);
        notifyDataSetChanged();
    }

    public void pickNumber(int position) {
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        pickContactIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        fragment.setRecipientPickIndex(position);
        fragment.startActivityForResult(pickContactIntent, Config.PICK_CONTACT_REQUEST);
    }

    public void addContactToItem(int position, String[] recipient) {
        items.set(position, recipient);
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
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) fragment.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.receiver_list_item, parent, false);
        }
        // Remove item button
        ImageButton removeNumberBtn = (ImageButton) convertView.findViewById(R.id.removeNumberBtn);
        removeNumberBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getCount() == 1) {
                    Toast.makeText(fragment.getActivity(), R.string.number_error, Toast.LENGTH_LONG).show();
                    return;
                }
                removeItem(position);
            }
        });

        // pick a number
        ImageButton pickNumberBtn = (ImageButton) convertView.findViewById(R.id.pickNumberBtn);
        pickNumberBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickNumber(position);
            }
        });

        final EditText recName = (EditText) convertView.findViewById(R.id.recName);
        final EditText recNumber = (EditText) convertView.findViewById(R.id.recNumber);

        try {
            String[] receiver = items.get(position);
            recName.setText(receiver[0]);
            recNumber.setText(receiver[1]);
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        return convertView;
    }
}
