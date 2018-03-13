package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import uk.ac.kent.eda.jb956.sensorlibrary.DutyCyclingManager;
import uk.ac.kent.eda.jb956.sensorlibrary.SensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.WifiData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.WifiType;
import uk.ac.kent.eda.jb956.sensorlibrary.database.MySQLiteHelper;
import uk.ac.kent.eda.jb956.sensorlibrary.util.NTP;
import uk.ac.kent.eda.jb956.sensorlibrary.util.SensorUtils;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class WifiSensorManager extends BaseSensor implements SensingInterface, DutyCyclingManager.DutyCyclingEventListener {

    private final String TAG = "WifiSensorManager";
    private final Context context;
    // public static final int SAMPLING_RATE_MICRO = SAMPLING_RATE * 1000;

    public WifiSensorManager(Context context) {
        this.context = context.getApplicationContext();
        sensor = null;
        if (wifi == null)
            wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    private final Sensor sensor;

    @Override
    public Sensor getSensor() {
        Log.e(TAG, "getSensor() for this class is always null");
        return sensor;
    }

    @Override
    public List<SensorData> getDataFromRange(long start, long end) {
        List<SensorData> temp = new ArrayList<>();
        Cursor cur = MySQLiteHelper.getInstance(context).getReadableDatabase().rawQuery("SELECT * FROM wifi where timestamp >=" + start + " and timestamp <=" + end, null);
        while (cur.moveToNext()) {
            //Which column you want to export
            WifiData sensorData = new WifiData(SensorUtils.SENSOR_TYPE_WIFI);
            sensorData.timestamp = Long.parseLong(cur.getString(3));
            sensorData.bssid = cur.getString(1);
            sensorData.rssi = Double.parseDouble(cur.getString(2));
            temp.add(sensorData);
        }
        cur.close();
        return temp;
    }

    @Override
    public List<SensorData> getAllData() {
        return getDataFromRange(0L, NTP.getInstance().currentTimeMillis());
    }

    @Override
    public void removeAllDataFromDatabase() {
        removeDataFromDatabaseWithLimit(-1);
    }

    @Override
    public void removeDataFromDatabaseWithLimit(int limit) {
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        logInfo(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        if (limit == -1)
            database.execSQL("DELETE FROM wifi");
        else
            database.execSQL("DELETE FROM wifi WHERE id IN(SELECT id FROM wifi ORDER BY id ASC LIMIT " + limit + ")");

        logInfo(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }

    @Override
    public void removeDataFromDatabaseWithRange(long start, long end) {
        String dbName = "wifi";
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        logInfo(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        database.execSQL("DELETE FROM " + dbName + " where timestamp >=" + start + " and timestamp <=" + end);
        logInfo(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }

    @Override
    public WifiSensorManager startSensing() {
        if (isSensing())
            return this;
        try {
            context.registerReceiver(receiver, new IntentFilter(WifiManager
                    .SCAN_RESULTS_AVAILABLE_ACTION));
            if (config.dutyCycle) {
                dutyCyclingManager.subscribeToListener(this);
                dutyCyclingManager.run();
            }
            addNewSensingTask(0);
            sensing = true;
            getSensorEvent().onSensingStarted(SensorUtils.SENSOR_TYPE_WIFI);
        } catch (Exception e) {
            e.printStackTrace();
        }
        logInfo(TAG, !isSensing() ? TAG + " not started: Disabled" : TAG + " started");
        return this;
    }


    @Override
    public WifiSensorManager stopSensing() {
        if (!isSensing())
            return this;
        try {
            context.unregisterReceiver(receiver);
            stopSensingTask();
            dutyCyclingManager.unsubscribeListener();
            dutyCyclingManager.stop();
            getSensorEvent().onSensingStopped(SensorUtils.SENSOR_TYPE_WIFI);
            SensorManager.getInstance(context).stopSensor(SensorUtils.SENSOR_TYPE_WIFI);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sensing = false;
        logInfo(TAG, "Sensor stopped");
        return this;
    }

    private WifiManager wifi;
    private long timeLastInitiated = 0;
    private boolean wasBroadcastReceiverTriggered = false;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            //  logInfo(TAG, "onReceive();");
            wasBroadcastReceiverTriggered = true;
            List<ScanResult> results = wifi.getScanResults();
            //if 1 or more results found
            if (results.size() > 0) {
                logInfo(TAG, "Inserting new Wi-Fi fingerprint");
                List<WifiData> unparsedResults = new ArrayList<>();
                for (ScanResult r : results) {
                    WifiType wifiType = WifiType._5GHz;
                    if (r.frequency < 3000) {
                        wifiType = WifiType._2_4GHz;
                    }
                    WifiData wd = new WifiData(SensorUtils.SENSOR_TYPE_WIFI);
                    wd.rssi = r.level;
                    if (Build.VERSION.SDK_INT >= 17) {
                        wd.timestamp = NTP.getInstance().currentTimeMillis() - SystemClock.elapsedRealtime() + (r.timestamp / 1000);
                    } else {
                        wd.timestamp = NTP.getInstance().currentTimeMillis();//TODO test this
                    }
                    wd.bssid = r.BSSID;
                    wd.wifiType = wifiType;
                    wd.ssid = r.SSID;
                    wd.channel = ieee80211_frequency_to_channel(r.frequency);
                    // long testTs = wd.timestamp / 1000;
                    // long now = NTP.currentTimeMillis() / 1000;
                    //if (Math.abs(testTs - now) > 86400) {
                    //ACRA.getErrorReporter().handleSilentException(new Exception("Bssid: " + wd.bssid + " | Timestamp: " + wd.timestamp + " | SystemClock.elapsedRealtime(): " + SystemClock.elapsedRealtime() + " | r.timestamp: " + r.timestamp));
                    //}
                    unparsedResults.add(wd);

                }

                for (WifiData sensorData : unparsedResults) {
                    if (sensorData.timestamp <= NTP.getInstance().currentTimeMillis() && sensorData.timestamp >= timeLastInitiated) {
                        //NetworkCache.getInstance().getFingerprintData().add(wd);
                        if (canSaveToDatabase()) {
                            MySQLiteHelper.getInstance(context).addToWifi(sensorData);
                        }
                        getSensorEvent().onDataSensed(sensorData);
                        setLastEntry(sensorData);
                    }
                }

            } else {
                logInfo(TAG, "Scanned Wi-Fi list was empty - In Android 7+ this could be because location services are turned off");
            }
        }
    };

    public boolean checkPermissions() {
        return Build.VERSION.SDK_INT < 23 || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public double calculateDistance(double signalLevelInDb, double freqInMHz) {
        double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(signalLevelInDb)) / 20.0;
        return Math.pow(10.0, exp);
    }

    private void stopSensingTask() {
        SensorManager.getInstance(context).getWorkerThread().removeTask(task);
    }

    private void addNewSensingTask(int delay) {
        checkWifiSettings();
        SensorManager.getInstance(context).getWorkerThread().postDelayed(task, delay);
    }

    private Runnable task = new Runnable() {
        @Override
        public void run() {
            requestWifiScan();
        }
    };

    private int ieee80211_frequency_to_channel(int freq) {
        if (freq == 2484)
            return 14;

        if (freq < 2484)
            return (freq - 2407) / 5;

        return freq / 5 - 1000;
    }

    private boolean isDialogShowing = false;

    private void checkWifiSettings() {
        try {
            if (!canAccessWifiSignals()) {
                //wifi is enabled
                try {
                    if (!isDialogShowing) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setMessage(
                                Settings.appName + " needs Wi-Fi enabled. Do you wish to turn it on?")
                                .setCancelable(false)
                                .setPositiveButton("Yes",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int id) {
                                                wifi.setWifiEnabled(true);
                                                isDialogShowing = false;
                                            }
                                        })
                                .setNegativeButton("No",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int id) {
                                                isDialogShowing = false;
                                                checkWifiSettings();
                                                wifi.setWifiEnabled(false);
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
        boolean canProceed = wifi.isWifiEnabled();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            canProceed = canProceed || ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) &&
                    wifi.isScanAlwaysAvailable());
        }
        return canProceed;
    }

    private void requestWifiScan() {
        if (!canAccessWifiSignals()) {
            checkWifiSettings();
            logInfo(TAG, "Wi-Fi not enabled or not always available - ignoring requestWifiScan");
            return;
        }
        if (!checkPermissions()) {
            checkPermissions();
            logInfo(TAG, "Missing permissions for access to Wi-Fi. Aborting.");
            return;
        }
        timeLastInitiated = NTP.getInstance().currentTimeMillis();
        logInfo(TAG, "Starting a Wi-Fi scan");
        wifi.startScan();
        addNewSensingTask(getSamplingRate());
    }

    private boolean sensing = false;

    @Override
    public boolean isSensing() {
        return sensing;
    }

    @Override
    public void onWake(int duration) {
        logInfo(TAG, "Resuming sensor for " + duration);
        addNewSensingTask(0);
    }

    @Override
    public void onSleep(int duration) {
        logInfo(TAG, "Pausing sensor for " + duration);
        stopSensingTask();
    }
}
