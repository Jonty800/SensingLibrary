package uk.ac.kent.eda.jb956.sensorlibrary;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;
import uk.ac.kent.eda.jb956.sensorlibrary.data.WifiData;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.AccelerometerManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.AudioManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.GyroscopeManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.HumiditySensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.LightSensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.MagneticFieldManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.PressureSensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.ProximitySensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.TemperatureSensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.service.ActivityRecognizedService;
import uk.ac.kent.eda.jb956.sensorlibrary.service.SensingService;
import uk.ac.kent.eda.jb956.sensorlibrary.service.WifiService;
import uk.ac.kent.eda.jb956.sensorlibrary.service.receiver.AlarmReceiver;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class SensorManager implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static SensorManager instance;
    public final WifiManager wifiManager;

    private final Context context;
    private final String TAG = "SensorManager";
    private GoogleApiClient mApiClient;

    public final AudioManager audioManager;
    private final AccelerometerManager accelerometerManager;
    private final GyroscopeManager gyroscopeManager;
    private final ProximitySensorManager proximityManager;
    private final LightSensorManager lightSensorManager;
    private final HumiditySensorManager humiditySensorManager;
    private final PressureSensorManager pressureSensorManager;
    private final TemperatureSensorManager temperatureSensorManager;
    private final MagneticFieldManager magneticFieldManager;
    private final List<WifiData> rawHistoricData = new ArrayList<>();

    private HandlerThread mSensorThread;
    private Handler mSensorHandler;

    public synchronized List<WifiData> getRawHistoricData() {
        return rawHistoricData;
    }
    //private OkHttpClient client;

    /**
     * Upon creation of this singleton class, it will attempt to start the
     * Audio recording instance, accelerometer, gyroscope and activity instance
     *
     * @param c
     */
    private SensorManager(Context c) {
        this.context = c.getApplicationContext();
        mSensorThread = new HandlerThread("Sensor thread", Thread.MAX_PRIORITY);
        mSensorThread.start();
        mSensorHandler = new Handler(mSensorThread.getLooper()); //Blocks until looper is prepared, which is fairly quick
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        audioManager = AudioManager.getInstance();
        accelerometerManager = AccelerometerManager.getInstance(context);
        gyroscopeManager = GyroscopeManager.getInstance(context);
        proximityManager = ProximitySensorManager.getInstance(context);
        lightSensorManager = LightSensorManager.getInstance(context);
        humiditySensorManager = HumiditySensorManager.getInstance(context);
        pressureSensorManager = PressureSensorManager.getInstance(context);
        temperatureSensorManager = TemperatureSensorManager.getInstance(context);
        magneticFieldManager = MagneticFieldManager.getInstance(context);
        //client = new OkHttpClient();

        if (Settings.ACTIVITY_ENABLED) {
            mApiClient = new GoogleApiClient.Builder(context)
                    .addApi(ActivityRecognition.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            mApiClient.connect();
        }
    }

    public static synchronized SensorManager getInstance(Context c) {
        if (instance == null)
            instance = new SensorManager(c.getApplicationContext());
        return instance;
    }

    /**
     * Event which occurs when the GoogleAPI has connected to the GooglePlayServices
     * This is where we request activity updates
     *
     * @param bundle
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Intent intent = new Intent(context, ActivityRecognizedService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, Settings.ACTIVITY_SAMPLING_RATE, pendingIntent);
    }

    /**
     * Event which occurs when the GoogleAPI connection has been suspended
     *
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mApiClient.connect();
        // removeActivityUpdates();
    }

    /**
     * Event which occurs when the GoogleAPI has failed to connect to the GooglePlayServices
     *
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
        mApiClient.connect();
    }


    public String getUserID() {
        return android.provider.Settings.Secure.ANDROID_ID;
    }

    /**
     * Attempts to start the Gyro, Wifi and Acc sensors. Will not start if Settings class forbids it
     */
    public void startAllSensors() {
        gyroscopeManager.startSensing();
        accelerometerManager.startSensing();
        proximityManager.startSensing();
        lightSensorManager.startSensing();
        humiditySensorManager.startSensing();
        pressureSensorManager.startSensing();
        temperatureSensorManager.startSensing();
        magneticFieldManager.startSensing();
        if (Settings.WIFI_ENABLED) {
            Log.i(TAG, "Starting Wi-Fi Fingerprinting Service");
            Intent fingerprintingAlarm = new Intent(context.getApplicationContext(), AlarmReceiver.class);
            fingerprintingAlarm.setAction("fingerprintingAlarm");
            startAlarm(fingerprintingAlarm, 0, WifiService.alarmReceiverID);
        }
    }

    public void startSensingService() {
        Intent intent = new Intent(context, SensingService.class);
        context.startService(intent);
    }

    /**
     * Starts an alarm
     *
     * @param alarmIntent The intent for the alarm
     * @param delayMs     The delay before the alarm is initiated
     * @param alarmId     The ID for the alarm
     */
    public void startAlarm(Intent alarmIntent, int delayMs, int alarmId) {
        try {
            PendingIntent recurringAlarm = PendingIntent.getBroadcast(context.getApplicationContext(), alarmId,
                    alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Calendar updateTime = Calendar.getInstance();
            if (Build.VERSION.SDK_INT >= 23) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis() + delayMs, recurringAlarm);
                // } else if (Build.VERSION.SDK_INT >= 19) {
                //alarms.setExact(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis() + delayMs, recurringAlarm);
            } else {
                alarms.set(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis() + delayMs, recurringAlarm);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //yes
    }

    public void storeIntoSharedPref(String key, Object entry, Type type) {

        storeIntoSharedPref(key, entry, type, Settings.appName + "Config");
    }

    public Object getFromSharedPref(String key, Type type) {
        return getFromSharedPref(key, type, Settings.appName + "Config");
    }

    public void deleteFromSharedPref(String key) {
        deleteFromSharedPref(key, Settings.appName + "Config");
    }

    public synchronized void deleteFromSharedPref(String key, String user_id) {
        SharedPreferences prefs = context.getSharedPreferences(
                user_id, Context.MODE_PRIVATE);
        Log.i("deleteFromSharedPref", key + "::" + user_id);
        prefs.edit().remove(key).apply();
    }

    public void storeIntoSharedPref(String key, Object entry, Type type, String user_id) {
        storeIntoSharedPref(key, entry, type, user_id, true);
    }

    public synchronized void storeIntoSharedPref(String key, Object entry, Type type, String user_id, boolean log) {
        SharedPreferences prefs = context.getSharedPreferences(
                user_id, Context.MODE_PRIVATE);
        String json = new Gson().toJson(entry, type);
        if (log)
            Log.i("storeIntoSharedPref", "" + json);
        prefs.edit().putString(key, json).apply();
    }

    public synchronized Object getFromSharedPref(String key, Type type, String user_id) {
        SharedPreferences prefs = context.getSharedPreferences(
                user_id, Context.MODE_PRIVATE);
        String json = prefs.getString(key, null);
        Log.i("getFromSharedPref: " + key, "" + json);
        return new Gson().fromJson(json, type);
    }

    public Object getStringEntryFromPrefs(String key) {
        return getStringEntryFromPrefs(key, Settings.appName + "Config");
    }

    public synchronized Object getStringEntryFromPrefs(String key, String user_id) {
        SharedPreferences prefs = context.getSharedPreferences(
                user_id, Context.MODE_PRIVATE);
        String value = prefs.getString(key, null);
        Log.i("getFromSharedPref: " + key, "" + value);
        return value;
    }

    public Handler getmSensorHandler() {
        return mSensorHandler;
    }
}
