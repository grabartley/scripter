package com.example.graham.scripter;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;

import java.util.Calendar;

/*Service used to listen for state changes and respond to them*/
public class ReceiverService extends Service implements LocationListener {
    //Create BroadcastReceivers to listen for state changes
    private BroadcastReceiver dateTimeBroadcastReceiver;
    private BroadcastReceiver headphoneBroadcastReceiver;
    private BroadcastReceiver WifiBroadcastReceiver;
    //Used to register the receivers
    private Context mainContext;
    //Create a LocationManager to listen for location changes
    private LocationManager locationManager;
    //Create a boolean to represent whether location permissions have been granted or not
    private boolean locationGranted;

    public ReceiverService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*Called when the service is started*/
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mainContext = getBaseContext();

        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        //If the build version is 22 or lower, location access is granted
        if (Build.VERSION.SDK_INT <= 22) {
            locationGranted = true;
        } else {
            //Otherwise, check if it has been granted by the user
            locationGranted = ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED));
        }

        //if we have location permissions, request location updates
        if (locationGranted) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }

        //multithreading
        Runnable r = new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    /*Instantiate receivers*/
                    dateTimeBroadcastReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            //When date or time change, update these states in MonitorState
                            Calendar cal = Calendar.getInstance();
                            MonitorState.updateStateByIndex(0, cal.get(Calendar.DAY_OF_MONTH));
                            MonitorState.updateStateByIndex(1, cal.get(Calendar.MONTH) + 1);
                            MonitorState.updateStateByIndex(2, cal.get(Calendar.YEAR));
                            MonitorState.updateStateByIndex(3, cal.get(Calendar.HOUR_OF_DAY));
                            MonitorState.updateStateByIndex(4, cal.get(Calendar.MINUTE));
                            MonitorState.updateStateByIndex(5, MonitorState.getDayOfWeek(cal.get(Calendar.DAY_OF_WEEK)));
                        }
                    };

                    headphoneBroadcastReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            //When headset state changes, update it in MonitorState
                            int hpState = intent.getIntExtra("state", -1);
                            switch (hpState) {
                                case 0:
                                    MonitorState.updateStateByIndex(6, false);
                                    break;
                                case 1:
                                    MonitorState.updateStateByIndex(6, true);
                                    break;
                            }
                        }
                    };

                    WifiBroadcastReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            //When Network state changes, update it in MonitorState
                            NetworkInfo wifiState = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                            MonitorState.updateStateByIndex(9, wifiState.isConnected());
                            WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                            String SSID = wifiInfo.getSSID();
                            MonitorState.updateStateByIndex(10, SSID.substring(1, SSID.length() - 1));
                        }
                    };

                    /*Create and instantiate IntentFilters with relevant actions*/
                    IntentFilter dateTimeFilter = new IntentFilter();
                    IntentFilter headphoneFilter = new IntentFilter();
                    IntentFilter WifiFilter = new IntentFilter();
                    dateTimeFilter.addAction(Intent.ACTION_TIME_TICK);
                    dateTimeFilter.addAction(Intent.ACTION_TIME_CHANGED);
                    dateTimeFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
                    headphoneFilter.addAction(Intent.ACTION_HEADSET_PLUG);
                    WifiFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

                    /*Register receivers with their IntentFilters*/
                    mainContext.registerReceiver(dateTimeBroadcastReceiver, dateTimeFilter);
                    mainContext.registerReceiver(headphoneBroadcastReceiver, headphoneFilter);
                    mainContext.registerReceiver(WifiBroadcastReceiver, WifiFilter);
                }
            }
        };

        //start a new thread for the service
        Thread receiverServiceThread = new Thread(r);
        receiverServiceThread.start();
        return Service.START_STICKY;
    }

    //Called when the Service is stopped
    @Override
    public void onDestroy() {
        super.onDestroy();

        /*Unregister receivers when the Service is finished*/
        this.unregisterReceiver(dateTimeBroadcastReceiver);
        this.unregisterReceiver(headphoneBroadcastReceiver);
        this.unregisterReceiver(WifiBroadcastReceiver);
    }

    //When location changes, update it in MonitorState
    @Override
    public void onLocationChanged(Location location) {
        MonitorState.updateStateByIndex(7, location.getLatitude());
        MonitorState.updateStateByIndex(8, location.getLongitude());
    }

    /*Required overrides*/
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
