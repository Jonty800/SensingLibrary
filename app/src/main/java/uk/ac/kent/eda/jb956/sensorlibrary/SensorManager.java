package uk.ac.kent.eda.jb956.sensorlibrary;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.util.ArraySet;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import uk.ac.kent.eda.jb956.sensorlibrary.callback.SensingEvent;
import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;
import uk.ac.kent.eda.jb956.sensorlibrary.control.WorkerThread;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorConfig;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.AccelerometerManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.ActivitySensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.AudioSensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.BaseSensor;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.GyroscopeManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.HumiditySensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.LightSensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.MagneticFieldManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.PressureSensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.ProximitySensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.TemperatureSensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.WifiSensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.service.SensingService;
import uk.ac.kent.eda.jb956.sensorlibrary.util.SensorUtils;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class SensorManager {

    private static SensorManager instance;

    private Map<Integer, SensorConfig> activeSensors = new HashMap<>();
    private final Context context;
    private final String TAG = "SensorManager";

    private AudioSensorManager audioManager;
    private AccelerometerManager accelerometerManager;
    private GyroscopeManager gyroscopeManager;
    private ProximitySensorManager proximityManager;
    private LightSensorManager lightSensorManager;
    private HumiditySensorManager humiditySensorManager;
    private PressureSensorManager pressureSensorManager;
    private TemperatureSensorManager temperatureSensorManager;
    private MagneticFieldManager magneticFieldManager;
    private WifiSensorManager wifiSensorManager;
    private ActivitySensorManager activitySensorManager;

    /**
     * Upon creation of this singleton class, it will attempt to start the
     * Audio recording instance, accelerometer, gyroscope and activity instance
     *
     * @param c
     */
    private SensorManager(Context c) {
        this.context = c.getApplicationContext();
        HandlerThread mSensorThread = new HandlerThread("Sensor thread", Thread.MAX_PRIORITY);
        mSensorThread.start();
        mSensorHandler = new Handler(mSensorThread.getLooper()); //Blocks until looper is prepared, which is fairly quick
        workerThread = WorkerThread.create();
        if (activeSensors.isEmpty()) {
            if (prefsContainsKey("activeSensors")) {
                String json = getStringEntryFromPrefs("activeSensors");
                Type type = new TypeToken<Map<Integer, SensorConfig>>() {
                }.getType();
                activeSensors = new Gson().fromJson(json, type);
            }
        }
        audioManager = new AudioSensorManager(context);
        accelerometerManager = new AccelerometerManager(context);
        gyroscopeManager = new GyroscopeManager(context);
        wifiSensorManager = new WifiSensorManager(context);
        proximityManager = new ProximitySensorManager(context);
        lightSensorManager = new LightSensorManager(context);
        humiditySensorManager = new HumiditySensorManager(context);
        pressureSensorManager = new PressureSensorManager(context);
        temperatureSensorManager = new TemperatureSensorManager(context);
        magneticFieldManager = new MagneticFieldManager(context);
        activitySensorManager = new ActivitySensorManager(context);
    }

    public static synchronized SensorManager getInstance(Context c) {
        if (instance == null)
            instance = new SensorManager(c.getApplicationContext());
        return instance;
    }

    public BaseSensor getSensorById(int sensorId) {
        switch (sensorId) {
            case SensorUtils.SENSOR_TYPE_GYROSCOPE:
                return gyroscopeManager;
            case SensorUtils.SENSOR_TYPE_ACTIVITY:
                return activitySensorManager;
            case SensorUtils.SENSOR_TYPE_ACCELEROMETER:
                return accelerometerManager;
            case SensorUtils.SENSOR_TYPE_PROXIMITY:
                return proximityManager;
            case SensorUtils.SENSOR_TYPE_LIGHT:
                return lightSensorManager;
            case SensorUtils.SENSOR_TYPE_HUMIDITY:
                return humiditySensorManager;
            case SensorUtils.SENSOR_TYPE_PRESSURE:
                return pressureSensorManager;
            case SensorUtils.SENSOR_TYPE_AMBIENT_TEMPERATURE:
                return temperatureSensorManager;
            case SensorUtils.SENSOR_TYPE_MAGNETIC_FIELD:
                return magneticFieldManager;
            case SensorUtils.SENSOR_TYPE_WIFI:
                return wifiSensorManager;
            case SensorUtils.SENSOR_TYPE_MICROPHONE:
                return audioManager;
            default:
                Log.i(TAG, "Invalid sensor ID");
        }
        return null;
    }

    public Map<Integer, SensorConfig> getActiveSensors() {
        return activeSensors;
    }

    public synchronized void startSensors(Map<Integer, SensorConfig> sensorMap) {
        Set<Integer> keys = new ArraySet<>(getActiveSensors().keySet());
        for (int key : keys) {
            startSensor(key, sensorMap.get(key));
        }
    }

    public synchronized void startSensor(int sensorId) {
        SensorConfig config = new SensorConfig();
        Intent i = new Intent(context, SensingService.class);
        i.putExtra("type", sensorId);
        i.putExtra("exec", "start");
        i.putExtra("config", config);
        putSensorIntoMap(sensorId, config);
        context.startService(i);
    }

    public synchronized void updateConfigInMap(int sensorId, SensorConfig config) {
        if (getActiveSensors().containsKey(sensorId)) {
            putSensorIntoMap(sensorId, config);
        }
    }

    public synchronized void startSensor(int sensorId, SensorConfig config) {
        Intent i = new Intent(context, SensingService.class);
        i.putExtra("type", sensorId);
        i.putExtra("exec", "start");
        i.putExtra("config", config);
        putSensorIntoMap(sensorId, config);
        context.startService(i);
    }

    public synchronized void stopSensor(int sensorId) {
        Intent i = new Intent(context, SensingService.class);
        i.putExtra("type", sensorId);
        i.putExtra("exec", "stop");
        removeSensorFromMap(sensorId);
        context.startService(i);
    }

    public synchronized void stopAllSensors() {
        Set<Integer> keys = new ArraySet<>(getActiveSensors().keySet());
        for (int key : keys) {
            stopSensor(key);
        }
        stopSensingService(); //stop service
    }

    public void stopSensingService() {
        Intent i = new Intent(context, SensingService.class);
        i.putExtra("exec", "stopservice");
        context.startService(i); //send stop command
    }

    private synchronized void putSensorIntoMap(int sensorId, SensorConfig config) {
        activeSensors.put(sensorId, config);
        Type type = new TypeToken<Map<Integer, SensorConfig>>() {
        }.getType();
        storeIntoSharedPref("activeSensors", activeSensors, type);
    }

    private synchronized void removeSensorFromMap(int sensorId) {
        activeSensors.remove(sensorId);
        Type type = new TypeToken<Map<Integer, SensorConfig>>() {
        }.getType();
        storeIntoSharedPref("activeSensors", activeSensors, type);
    }

    public void subscribeToSensorListener(SensingEvent.SensingEventListener listener) {
        SensingEvent.getInstance().subscribeToSensor(listener);
    }

    public void unsubscribeFromSensorListener() {
        SensingEvent.getInstance().unsubscribeFromSensor();
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
    }

    public void stopAlarm(Intent alarmIntent, int alarmId) {
        try {
            PendingIntent recurringAlarm = PendingIntent.getBroadcast(context.getApplicationContext(), alarmId,
                    alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarms.cancel(recurringAlarm);
            recurringAlarm.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public WorkerThread getWorkerThread() {
        return workerThread;
    }

    private WorkerThread workerThread;

    private Handler mSensorHandler;

    public Handler getmSensorHandler() {
        return mSensorHandler;
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

    private synchronized void deleteFromSharedPref(String key, String dbName) {
        SharedPreferences prefs = context.getSharedPreferences(
                dbName, Context.MODE_PRIVATE);
        Log.i("deleteFromSharedPref", key + "::" + dbName);
        prefs.edit().remove(key).apply();
    }

    private void storeIntoSharedPref(String key, Object entry, Type type, String dbName) {
        storeIntoSharedPref(key, entry, type, dbName, true);
    }

    private synchronized void storeIntoSharedPref(String key, Object entry, Type type, String dbName, boolean log) {
        SharedPreferences prefs = context.getSharedPreferences(
                dbName, Context.MODE_PRIVATE);
        String json = new Gson().toJson(entry, type);
        if (log)
            Log.i("storeIntoSharedPref", "" + json);
        prefs.edit().putString(key, json).apply();
    }

    private synchronized Object getFromSharedPref(String key, Type type, String dbName) {
        SharedPreferences prefs = context.getSharedPreferences(
                dbName, Context.MODE_PRIVATE);
        String json = prefs.getString(key, null);
        Log.i("getFromSharedPref: " + key, "" + json);
        return new Gson().fromJson(json, type);
    }

    private boolean prefsContainsKey(String key) {
        SharedPreferences prefs = context.getSharedPreferences(
                Settings.appName + "Config", Context.MODE_PRIVATE);
        return prefs.contains(key);
    }

    private String getStringEntryFromPrefs(String key) {
        return getStringEntryFromPrefs(key, Settings.appName + "Config");
    }

    private synchronized String getStringEntryFromPrefs(String key, String dbName) {
        SharedPreferences prefs = context.getSharedPreferences(
                dbName, Context.MODE_PRIVATE);
        String value = prefs.getString(key, null);
        Log.i("getFromSharedPref: " + key, "" + value);
        return value;
    }
}