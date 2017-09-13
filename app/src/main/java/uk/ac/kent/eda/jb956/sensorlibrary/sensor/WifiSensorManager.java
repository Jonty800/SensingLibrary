package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.Manifest;
import android.app.PendingIntent;
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
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import uk.ac.kent.eda.jb956.sensorlibrary.SensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.callback.SensingEvent;
import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorConfig;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.WifiData;
import uk.ac.kent.eda.jb956.sensorlibrary.database.MySQLiteHelper;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class WifiSensorManager extends BaseSensor implements SensingInterface  {

    private final String TAG = "WifiSensorManager";
    private static WifiSensorManager instance;
    private final Context context;
   // public static final int SAMPLING_RATE_MICRO = SAMPLING_RATE * 1000;

    public static synchronized WifiSensorManager getInstance(Context context) {
        if (instance == null)
            instance = new WifiSensorManager(context);
        return instance;
    }

    private WifiSensorManager(Context context) {
        this.context = context.getApplicationContext();
        sensor = null;
        if (wifi == null)
            wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        setSamplingRate(10000);
    }

    private final Sensor sensor;

    @Override
    public Sensor getSensor() {
        Log.e(TAG, "getSensor() for this class is always null");
        return sensor;
    }

    @Override
    public SensorData getLastEntry() {
        return lastEntry;
    }

    private SensingEvent sensorEvent = null;

    @Override
    public SensingEvent getSensorEventListener() {
        if (sensorEvent == null)
            sensorEvent = new SensingEvent();
        return sensorEvent;
    }

    @Override
    public WifiSensorManager withConfig(SensorConfig config){
        super.withConfig(config);
        return this;
    }

    @Override
    public List<SensorData> getDataFromRange(long start, long end) {
        List<SensorData> temp = new ArrayList<>();
        Cursor cur = MySQLiteHelper.getInstance(context).getReadableDatabase().rawQuery("SELECT * FROM wifi where timestamp >=" + start + " and timestamp <=" + end, null);
        while (cur.moveToNext()) {
            //Which column you want to export
            WifiData sensorData = new WifiData();
            sensorData.timestamp = Long.parseLong(cur.getString(3));
            sensorData.bssid = cur.getString(1);
            sensorData.rssi = Double.parseDouble(cur.getString(2));
            temp.add(sensorData);
        }
        cur.close();
        return temp;
    }

    @Override
    public void setEnabled(boolean enabled) {
        Settings.WIFI_ENABLED = enabled;
    }

    @Override
    public void setSensingWindowDuration(int duration) {
        config.AWAKE_WINDOW_SIZE = duration;
    }

    @Override
    public void setSleepingDuration(int duration) {
        config.SLEEP_WINDOW_SIZE = duration;
    }

    @Override
    public List<SensorData> getAllData() {
        return getDataFromRange(0L, System.currentTimeMillis());
    }

    @Override
    public void removeAllDataFromDatabase() {
        removeDataFromDatabaseWithLimit(-1);
    }

    @Override
    public void removeDataFromDatabaseWithLimit(int limit) {
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        Log.i(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        if (limit == -1)
            database.execSQL("DELETE FROM wifi");
        else
            database.execSQL("DELETE FROM wifi WHERE id IN(SELECT id FROM wifi ORDER BY id ASC LIMIT " + limit + ")");

        Log.i(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }

    @Override
    public void removeDataFromDatabaseWithRange(long start, long end) {
        String dbName = "wifi";
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        Log.i(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        database.execSQL("DELETE FROM " + dbName + " where timestamp >=" + start + " and timestamp <=" + end);
        Log.i(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }

    @Override
    public WifiSensorManager startSensing() {
        if (isSensing())
            return this;
        try {
            if (Settings.WIFI_ENABLED) {
                Log.i(TAG, "Starting Wi-Fi Fingerprinting Service");
                startSleepingTask();
                addNewSensingTask();
                sensing = true;
                getSensorEventListener().onSensingStarted();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, !isSensing() ? TAG + " not started: Disabled" : TAG + " started");
        return this;
    }

    @Override
    public WifiSensorManager stopSensing() {
        if (!isSensing())
            return this;
        try {
            if (Settings.WIFI_ENABLED) {
                stopSensingTask();
                getSensorEventListener().onSensingStopped();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        sensing = false;
        sleepingTaskStarted = false;
        Log.i(TAG, "Sensor stopped");
        return this;
    }

    boolean sleeping = false;
    private void sleep(){
        sleeping = true;
        try {
            if (Settings.WIFI_ENABLED) {
                stopSensingTask();
                getSensorEventListener().onSensingPaused();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, "Sensor paused");
    }

    private void wake(){
        sleeping = false;
        try {
            if (Settings.WIFI_ENABLED) {
                Log.i(TAG, "Resuming Wi-Fi Fingerprinting Service");
                startSleepingTask();
                addNewSensingTask();
                getSensorEventListener().onSensingResumed();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean sleepingTaskStarted = false;
    private void startSleepingTask(){
        if(sleepingTaskStarted)
            return;
        SensorManager.getInstance(context).getWorkerThread().postDelayedTask(getSleepTask(), getAwakeWindowSize());
        sleepingTaskStarted = true;
    }

    private Runnable getSleepTask() {
        return new Runnable() {
            @Override
            public void run() {
                if (!sleeping) {
                    Log.i(TAG, "Sleeping for " + getSleepWindowSize());
                    sleep();
                    SensorManager.getInstance(context).getWorkerThread().postDelayedTask(getSleepTask(), getSleepWindowSize());
                } else {
                    Log.i(TAG, "Sensing for " + getAwakeWindowSize());
                    wake();
                    SensorManager.getInstance(context).getWorkerThread().postDelayedTask(getSleepTask(), getAwakeWindowSize());
                }
            }
        };
    }

    private WifiManager wifi;
    private long timeLastInitiated = 0;
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
                    }
                }

                List<WifiData> currentEntries = new ArrayList<>();
                for (WifiData sensorData : unparsedResults) {
                    if (sensorData.timestamp <= System.currentTimeMillis() && sensorData.timestamp >= timeLastInitiated) {
                        //NetworkCache.getInstance().getFingerprintData().add(wd);
                        if(canSaveToDatabase()) {
                            MySQLiteHelper.getInstance(context).addToWifi(sensorData);
                        }
                        currentEntries.add(sensorData);
                        WifiSensorManager.getInstance(c).getSensorEventListener().onDataSensed(sensorData);
                    }
                }

            } else {
                Log.i(TAG, "Scanned Wi-Fi list was empty - In Android 7+ this could be because location services are turned off");
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

    private void stopSensingTask(){
        SensorManager.getInstance(context).getWorkerThread().removeDelayedTask(task);
    }

    private void addNewSensingTask() {
        checkWifiSettings();
        int next_delay = WifiSensorManager.getInstance(context).getSamplingRate();
        SensorManager.getInstance(context).getWorkerThread().postDelayedTask(task, next_delay);
    }

    private Runnable task = new Runnable() {
        @Override
        public void run() {
            requestWifiScan();
        }
    };

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
            Log.i(TAG, "Wi-Fi not enabled or not always available - ignoring requestWifiScan");
            return;
        }
        if (!checkPermissions()) {
            Log.i(TAG, "Missing permissions for access to Wi-Fi. Aborting.");
            return;
        }
        try {
            context.registerReceiver(receiver, new IntentFilter(WifiManager
                    .SCAN_RESULTS_AVAILABLE_ACTION));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        timeLastInitiated = System.currentTimeMillis();
        wifi.startScan();
        addNewSensingTask();
    }

    private boolean sensing = false;

    @Override
    public boolean isSensing() {
        return sensing;
    }

    private SensorData lastEntry = null;
}
