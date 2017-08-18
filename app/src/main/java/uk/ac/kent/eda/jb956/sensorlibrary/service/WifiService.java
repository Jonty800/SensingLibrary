package uk.ac.kent.eda.jb956.sensorlibrary.service;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import uk.ac.kent.eda.jb956.sensorlibrary.SensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;
import uk.ac.kent.eda.jb956.sensorlibrary.data.WifiData;
import uk.ac.kent.eda.jb956.sensorlibrary.database.MySQLiteHelper;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.WifiSensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.service.receiver.AlarmReceiver;

public class WifiService extends Service {

    /**
     * Command to the service to display a message
     */
    public static final String MSG_HEARTBEAT = "heartbeat.start";
    public static final String MSG_SENDING = "sendingService.start";
    public static boolean uploading = false;
    private String TAG = "WifiService";
    Callback callback = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            e.printStackTrace();
            WifiService.uploading = false;
            //if (lock.isHeld())
            //  lock.release();
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            Log.i(TAG, "sendRequestXCompressed response => " + response.body().string());
            SensorManager.getInstance(getApplication()).getRawHistoricData().clear();
            Type type = new TypeToken<List<WifiData>>() {
            }.getType();
            SensorManager.getInstance(getApplication()).storeIntoSharedPref("rawHistoricData", SensorManager.getInstance(getApplication()).getRawHistoricData(), type, SensorManager.getInstance(getApplication()).getUserID(), false);

            WifiService.uploading = false;
        }
    };
    private WifiManager wifi;
    private long timeLastInitiated = 0;
    public static final int alarmReceiverID = 0;
    private boolean wasBroadcastReceiverTriggered = false;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            wasBroadcastReceiverTriggered = true;
            List<ScanResult> results = wifi.getScanResults();
            //if 1 or more results found
            if (results.size() > 0) {
                Log.i(TAG, "Inserting new Wi-Fi fingerprint");
                //filter anything except 2.4GHZ access points
                List<WifiData> unparsedResults = new ArrayList<>();
                for (ScanResult r : results) {
                    if (r.frequency < 3000) {
                        WifiData wd = new WifiData();
                        wd.rssi = r.level;
                        if (Build.VERSION.SDK_INT >= 17) {
                            wd.timestamp = System.currentTimeMillis() - SystemClock.elapsedRealtime() + (r.timestamp / 1000);
                        } else {
                            wd.timestamp = System.currentTimeMillis();//TODO test this
                        }
                        wd.bssid = r.BSSID;
                        wd.distanceEstimate = calculateDistance(r.level, r.frequency);
                        // long testTs = wd.timestamp / 1000;
                        // long now = System.currentTimeMillis() / 1000;
                        //if (Math.abs(testTs - now) > 86400) {
                        //ACRA.getErrorReporter().handleSilentException(new Exception("Bssid: " + wd.bssid + " | Timestamp: " + wd.timestamp + " | SystemClock.elapsedRealtime(): " + SystemClock.elapsedRealtime() + " | r.timestamp: " + r.timestamp));
                        //}
                        unparsedResults.add(wd);
                        if (Settings.SAVE_WIFI_TO_DATABASE)
                            MySQLiteHelper.getInstance(getApplicationContext()).addToWifi(wd);
                    }
                }

                List<WifiData> currentEntries = new ArrayList<>();
                for (WifiData sensorData : unparsedResults) {
                    if (sensorData.timestamp <= System.currentTimeMillis() && sensorData.timestamp >= timeLastInitiated) {
                        //NetworkCache.getInstance().getFingerprintData().add(wd);
                        SensorManager.getInstance(getApplication()).getRawHistoricData().add(sensorData);
                        currentEntries.add(sensorData);
                        WifiSensorManager.getInstance(c).getSensorEventListener().onDataSensed(sensorData);
                    }
                }

                Type type = new TypeToken<List<WifiData>>() {
                }.getType();
                SensorManager.getInstance(getApplication()).storeIntoSharedPref("rawHistoricData", SensorManager.getInstance(getApplication()).getRawHistoricData(), type, SensorManager.getInstance(getApplication()).getUserID(), false);
            } else {
                Log.i(TAG, "Scanned Wi-Fi list was empty - In Android 7+ this could be because location services are turned off");
            }

            try {
                //unregister this receiver
                unregisterReceiver(receiver);
                if (!WifiService.uploading)
                    stopSelf();
            } catch (Exception e) { //catch any errors
                e.printStackTrace();
            }
        }
    };

    public boolean checkPermissions() {
        return Build.VERSION.SDK_INT < 23 || ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public double calculateDistance(double signalLevelInDb, double freqInMHz) {
        double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(signalLevelInDb)) / 20.0;
        return Math.pow(10.0, exp);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (wifi == null)
            wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //lock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL, "LockTag");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!wasBroadcastReceiverTriggered)
            Log.i(TAG, "BroadcastReceiver was not triggered. Was this running on an emulator or is Wi-Fi unavailable?");
        try {
            //unregister this receiver
            unregisterReceiver(receiver);
        } catch (Exception e) { //catch any errors
            //e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        setAlarm();
        if (intent != null) {
            switch (intent.getStringExtra("code")) {
                case MSG_HEARTBEAT:
                    sendHeartbeat();
                    break;
                case MSG_SENDING: //TODO IMPLEMENT THIS
                    if (!checkPermissions()) {
                        Log.i(TAG, "Ending SendingService: No permissions");
                    } else {
                        WifiService.uploading = true;
                        //TODO old code, cleanup
                        if (SensorManager.getInstance(getApplication()).getRawHistoricData().size() > 0) {
                            // SensorManager.getInstance(getApplication()).sendRequestXCompressed(SensorManager.getInstance(getApplication()).generateAverageFingerprint(), callback);
                        }
                    }
                    break;
                default:
                    //Log no action
                    Log.i(TAG, "No action was set in onStartCommand");
                    break;
            }
        } else {
            Log.i(TAG, "onStartCommand: intent=null");
        }

        boolean alarmUp = (PendingIntent.getBroadcast(getApplicationContext(), alarmReceiverID,
                new Intent(getApplicationContext(), AlarmReceiver.class)
                        .setAction("fingerprintingAlarm"), PendingIntent.FLAG_NO_CREATE) != null);
        if (!alarmUp) {
            Log.i(TAG, "Alarm was not set: Setting now");
            setAlarm();
        }

        return START_STICKY;
    }

    int dutyIndex = 0;

    void setAlarm() {
        Intent myAlarm = new Intent(getApplicationContext(), AlarmReceiver.class);
        myAlarm.setAction("fingerprintingAlarm");
        int next_delay = WifiSensorManager.getInstance(this).getSamplingRate();// - (num);
        checkWifiSettings();
        WifiSensorManager wifiSensorManager = WifiSensorManager.getInstance(this);
        int[] profile = wifiSensorManager.getDutyCyclingIntervalProfile();
        if (profile == null)
            SensorManager.getInstance(getApplication()).startAlarm(myAlarm, next_delay, alarmReceiverID);
        else {
            //TODO test this
            SensorManager.getInstance(getApplication()).startAlarm(myAlarm, profile[dutyIndex++], alarmReceiverID);
            if (dutyIndex > profile.length)
                dutyIndex = 0;
        }
    }

    private boolean isDialogShowing = false;

    private void checkWifiSettings() {
        try {
            if (!canAccessWifiSignals()) {
                //wifi is enabled
                try {
                    if (!isDialogShowing) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage(
                                Settings.appName + " needs Wi-Fi enabled. Do you wish to turn it on?")
                                .setCancelable(false)
                                .setPositiveButton("Yes",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int id) {
                                                SensorManager.getInstance(getApplication()).wifiManager.setWifiEnabled(true);
                                                isDialogShowing = false;
                                            }
                                        })
                                .setNegativeButton("No",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int id) {
                                                isDialogShowing = false;
                                                checkWifiSettings();
                                                SensorManager.getInstance(getApplication()).wifiManager.setWifiEnabled(false);
                                            }
                                        });
                        AlertDialog alert = builder.create();

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean canAccessWifiSignals() {
        boolean canProceed = SensorManager.getInstance(getApplication()).wifiManager.isWifiEnabled();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            canProceed = canProceed || ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) &&
                    SensorManager.getInstance(getApplication()).wifiManager.isScanAlwaysAvailable());
        }
        return canProceed;
    }

    private void sendHeartbeat() {
        if (!canAccessWifiSignals()) {
            Log.i(TAG, "Wi-Fi not enabled or not always available - ignoring sendHeartbeat");
            return;
        }
        if (!checkPermissions()) {
            Log.i(TAG, "Missing permissions for access to Wi-Fi. Aborting.");
            return;
        }
        try {
            registerReceiver(receiver, new IntentFilter(WifiManager
                    .SCAN_RESULTS_AVAILABLE_ACTION));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        timeLastInitiated = System.currentTimeMillis();
        wifi.startScan();
    }

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}


