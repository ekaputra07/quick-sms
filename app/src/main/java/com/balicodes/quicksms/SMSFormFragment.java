package com.balicodes.quicksms;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by eka on 6/27/15.
 */
public class SMSFormFragment extends Fragment {

    private Button saveBtn;
    private EditText smstitle, message;
    private Switch addShortcut;
    private TextView addRecipient;
    private List<String[]> recipients = new ArrayList<String[]>();
    private NoScrollListView recipientListView;
    private RecipientListAdapter recipientListAdapter;
    private DBHelper dbHelper;
    private SMSItem smsItem;
    private int recipientPickIndex;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Contact data request
        if (requestCode == Config.PICK_CONTACT_REQUEST && resultCode == getActivity().RESULT_OK) {

            String phoneNo = null;
            String displayName = null;

            Uri uri = data.getData();
            Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
            cursor.moveToFirst();

            int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int displayNameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            phoneNo = cursor.getString(phoneIndex);
            displayName = cursor.getString(displayNameIndex);
            String[] recipient = new String[]{displayName, phoneNo};

            recipientListAdapter.addContactToItem(recipientPickIndex, recipient);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sms_form_fragment, container, false);
        smstitle = (EditText) view.findViewById(R.id.titleTxt);
        message = (EditText) view.findViewById(R.id.messageTxt);

        recipientListView = (NoScrollListView) view.findViewById(R.id.receiverListView);
        recipientListAdapter = new RecipientListAdapter(this, recipients);
        recipientListView.setAdapter(recipientListAdapter);
        recipientListView.setDivider(null);
        recipientListView.setExpanded(true);

        addRecipient = (Button) view.findViewById(R.id.addReceiverBtn);
        addRecipient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recipients.size() < Config.MAX_RECIPIENTS_PER_SMS) {
                    saveRecipientsState(); // before adding new recipient, make sure we save any changes.
                    recipientListAdapter.addItem(new String[]{"", ""});
                } else {
                    String msg = String.format(getActivity().getResources().getString(R.string.max_recipients_warning), String.valueOf(Config.MAX_RECIPIENTS_PER_SMS));
                    Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
                }
            }
        });

        addShortcut = (Switch) view.findViewById(R.id.addShortcut);

        saveBtn = (Button) view.findViewById(R.id.saveBtn);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSMS();
            }
        });

        // Show hide addRecipient
        recipientListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                if(recipients.size() < Config.MAX_RECIPIENTS_PER_SMS){
                    addRecipient.setVisibility(View.VISIBLE);
                }else{
                    addRecipient.setVisibility(View.GONE);
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ((MainActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle bundle = getArguments();
        // edit mode
        if (bundle != null) {
            smsItem = SMSItem.fromBundle(bundle);

            smstitle.setText(smsItem.getTitle());
            message.setText(smsItem.getMessage());
            recipients.addAll(SMSItem.parseReceiverCSV(smsItem.getNumber()));
            addShortcut.setChecked("YES".equals(smsItem.getShortcut()));

            // new mode
        } else {
            smsItem = new SMSItem();
            recipients.add(new String[]{"", ""});
        }
        recipientListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (smsItem.getId() > 0) {
            inflater.inflate(R.menu.sms_form_menu, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_delete:
                deleteSMS();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void hideKeyboard() {
        // Check if no view has focus:
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private void deleteSMS() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.confirm_delete_title);

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                toggleShortcut(false); // delete shortcut
                smsItem.delete(SMSFormFragment.this.getActivity());

                Toast.makeText(getActivity(), R.string.message_deleted, Toast.LENGTH_SHORT).show();
                getActivity().onBackPressed();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /* This is to store any changes made to the recipients input back to the `recipients` */
    private void saveRecipientsState() {
        for (int i = 0; i < recipients.size(); i++) {
            View v = recipientListView.getChildAt(i);
            String name = ((EditText) v.findViewById(R.id.recName)).getText().toString().trim();
            String number = ((EditText) v.findViewById(R.id.recNumber)).getText().toString().trim();
            recipients.set(i, new String[]{name, number});
        }
    }

    private void saveSMS() {
        // do some simple validations
        if (smstitle.getText().toString().isEmpty()) {
            smstitle.setError(getString(R.string.title_error));
            return;
        }

        // get recipient as csv
        List<String> rec = new ArrayList<String>();
        for (int i = 0; i < recipients.size(); i++) {
            View v = recipientListView.getChildAt(i);
            String name = ((EditText) v.findViewById(R.id.recName)).getText().toString().trim();
            String number = ((EditText) v.findViewById(R.id.recNumber)).getText().toString().trim();
            if (number.length() > 0) {
                rec.add(name + ":" + number);
            }
        }
        String csv = TextUtils.join(",", rec);

        if (csv.isEmpty()) {
            Toast.makeText(getActivity(), R.string.number_error, Toast.LENGTH_LONG).show();
            return;
        }
        if (message.getText().toString().isEmpty()) {
            message.setError(getString(R.string.message_error));
            return;
        }

        String shortcut = addShortcut.isChecked() ? "YES" : "NO";

        if (smsItem.getId() > 0) {
            // Update
            smsItem.update(
                    getActivity(),
                    smstitle.getText().toString(),
                    csv,
                    message.getText().toString(),
                    shortcut);
        } else {
            // Create
            smsItem = SMSItem.create(
                    getActivity(),
                    smstitle.getText().toString(),
                    csv,
                    message.getText().toString(),
                    shortcut);
        }
        toggleShortcut(addShortcut.isChecked());

        hideKeyboard();
        Toast.makeText(getActivity(), R.string.message_saved, Toast.LENGTH_SHORT).show();
        getActivity().onBackPressed();
    }

    // used to set reference to recipient item index before contact Pick.
    public void setRecipientPickIndex(int index) {
        recipientPickIndex = index;
    }

    private void toggleShortcut(boolean create) {
        Intent shortCutInt = new Intent(getActivity().getApplicationContext(), ShortcutHandlerActivity.class);
        shortCutInt.setAction(Intent.ACTION_MAIN);
        shortCutInt.setData(ContentUris.withAppendedId(Uri.parse(Config.SMS_DATA_BASE_URI), smsItem.getId()));

        Intent addInt = new Intent();
        addInt.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortCutInt);
        addInt.putExtra(Intent.EXTRA_SHORTCUT_NAME, smsItem.getTitle());
        addInt.putExtra("duplicate", false);
        addInt.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getActivity().getApplicationContext(),
                        R.drawable.ic_launcher));
        if (create) {
            addInt.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        } else {
            addInt.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
        }

        getActivity().getApplicationContext().sendBroadcast(addInt);
    }
}

