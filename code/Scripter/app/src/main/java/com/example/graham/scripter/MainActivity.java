package com.example.graham.scripter;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

/*The home page of Scripter*/
public class MainActivity extends AppCompatActivity {
    //Used to request location permission
    public final static int REQUEST_LOCATION = 0;

    //Called when the Activity is started
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Request location access from the user if it has not already been granted
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //Request "Do Not Disturb" access from the user if it has not already been granted
        if (Build.VERSION.SDK_INT > 23 && !notificationManager.isNotificationPolicyAccessGranted()) {
            Intent requestNotificationAccess = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(requestNotificationAccess);
        }

        /*Start broadcast receivers*/
        Intent i = new Intent(this, ReceiverService.class);
        startService(i);

        /*Start ScriptFileTracker to keep track of scripts*/
        i = new Intent(this, ScriptFileTracker.class);
        startService(i);
    }

    //starts the MonitorState service
    public void startMonitorState(View view) {
        Intent i = new Intent(this, MonitorState.class);
        startService(i);
    }

    //stops the MonitorState service
    public void stopMonitorState(View view) {
        Intent i = new Intent(this, MonitorState.class);
        stopService(i);
    }

    //starts the ScriptsActivity
    public void goToScriptsActivity(View view) {
        Intent i = new Intent(this, ScriptsActivity.class);
        startActivity(i);
    }

    //starts the HelpPage
    public void goToHelpPage(View view) {
        Intent i = new Intent(this, HelpPage.class);
        startActivity(i);
    }

    //downloads a JavaScript file from the given URL
    public void downloadScript(View view) {
        final Intent i = new Intent(this, DownloadScript.class);

        //create a layout
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        //add text fields to the layout
        final EditText enterFilename = new EditText(this);
        enterFilename.setHint(R.string.enter_filename);
        linearLayout.addView(enterFilename);

        final EditText enterURL = new EditText(this);
        enterURL.setHint(R.string.enter_URL);
        linearLayout.addView(enterURL);

        //create an alert dialog and add the layout
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Download a Script");
        alert.setMessage("Enter a filename and the URL of your script.");
        alert.setView(linearLayout);

        //What to do when the Download button is pressed
        alert.setPositiveButton("Download", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String filename = enterFilename.getText().toString();
                String urlString = enterURL.getText().toString();
                i.putExtra("filename", filename);
                i.putExtra("url", urlString);
                //start the DownloadScript Service
                startService(i);
            }
        });

        //What to do when the Cancel button is pressed
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        //Show the alert
        alert.show();
    }

}



