package com.balicodes.quicksms;

import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new SMSListFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /*----------------------------------------------------------------------------------------------
    Share application.
     ---------------------------------------------------------------------------------------------*/
    private void shareApp() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        String shareTxt = getString(R.string.share_text).replace("{DOWNLOAD_LINK}", getString(R.string.download_link));
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareTxt);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    /*----------------------------------------------------------------------------------------------
    Rate application.
    --------------------------------------------------------------------------------------------- */
    private void rateApp() {
        Uri uri = Uri.parse("market://details?id=" + this.getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + this.getPackageName())));
        }
    }

    /*----------------------------------------------------------------------------------------------
    Return notification message builder instance.
    --------------------------------------------------------------------------------------------- */
    public NotificationCompat.Builder buildNotificationMessage(String title, String message) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(message);
//        Intent resultIntent = new Intent(this, MainActivity.class);
//        PendingIntent resultPendingIntent =
//                PendingIntent.getActivity(
//                        getActivity(),
//                        0,
//                        resultIntent,
//                        PendingIntent.FLAG_UPDATE_CURRENT
//                );
        return mBuilder;
    }

    /*----------------------------------------------------------------------------------------------
    Show notification.
    --------------------------------------------------------------------------------------------- */
    public void notify(int notifID, NotificationCompat.Builder mBuilder) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notifID, mBuilder.build());
    }

    /**
     * Open Export / Import screen
     */
    public void showImportExportScreen() {
        Intent intent = new Intent(this, ExportImportActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.action_export_import:
                showImportExportScreen();
                break;
            case R.id.action_share:
                shareApp();
                break;
            case R.id.action_rate:
                rateApp();
                break;
            case R.id.action_about:
                AboutDialog about = new AboutDialog(this);
                about.setRateListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        rateApp();
                    }
                });
                about.setShareListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        shareApp();
                    }
                });
                about.show();
                break;
            case android.R.id.home:
                hideKeyboard();
                super.onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void hideKeyboard() {
        // Check if no view has focus:
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == Config.SEND_SMS_PERMISSION_REQUEST && grantResults.length > 0){
            Log.d(MainActivity.class.getName(), String.valueOf(grantResults.length));

            if(Build.VERSION.SDK_INT == 26){
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, getString(R.string.permission_sms_granted), Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(this, getString(R.string.permission_sms_denied), Toast.LENGTH_LONG).show();
                }
            }else{
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, getString(R.string.permission_sms_granted), Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(this, getString(R.string.permission_sms_denied), Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
