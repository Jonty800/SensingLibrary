package uk.ac.kent.eda.jb956.sensorlibrary;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import uk.ac.kent.eda.jb956.sensorlibrary.callback.PermissionsEvent;
import uk.ac.kent.eda.jb956.sensorlibrary.control.Permissions;
import uk.ac.kent.eda.jb956.sensorlibrary.control.WorkerThread;
import uk.ac.kent.eda.jb956.sensorlibrary.data.WifiData;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.AccelerometerManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.ActivitySensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.AudioSensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.GyroscopeManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.HumiditySensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.LightSensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.MagneticFieldManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.PressureSensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.ProximitySensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.TemperatureSensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.WifiSensorManager;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class SensorManager {

    private static SensorManager instance;
    public final WifiManager wifiManager;

    private final Context context;
    private final String TAG = "SensorManager";

    private final AudioSensorManager audioManager;
    private final AccelerometerManager accelerometerManager;
    private final GyroscopeManager gyroscopeManager;
    private final ProximitySensorManager proximityManager;
    private final LightSensorManager lightSensorManager;
    private final HumiditySensorManager humiditySensorManager;
    private final PressureSensorManager pressureSensorManager;
    private final TemperatureSensorManager temperatureSensorManager;
    private final MagneticFieldManager magneticFieldManager;
    private final WifiSensorManager wifiSensorManager;
    private final ActivitySensorManager activitySensorManager;
    private final List<WifiData> rawHistoricData = new ArrayList<>();

    public WorkerThread getWorkerThread() {
        return workerThread;
    }

    private WorkerThread workerThread;

    private HandlerThread mSensorThread;
    private Handler mSensorHandler;

    public synchronized List<WifiData> getRawHistoricData() {
        return rawHistoricData;
    }

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
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        audioManager = AudioSensorManager.getInstance();
        accelerometerManager = AccelerometerManager.getInstance(context);
        gyroscopeManager = GyroscopeManager.getInstance(context);
        proximityManager = ProximitySensorManager.getInstance(context);
        lightSensorManager = LightSensorManager.getInstance(context);
        humiditySensorManager = HumiditySensorManager.getInstance(context);
        pressureSensorManager = PressureSensorManager.getInstance(context);
        temperatureSensorManager = TemperatureSensorManager.getInstance(context);
        magneticFieldManager = MagneticFieldManager.getInstance(context);
        wifiSensorManager = WifiSensorManager.getInstance(context);
        activitySensorManager = ActivitySensorManager.getInstance(context);
        workerThread =  WorkerThread.create();
    }

    public static synchronized SensorManager getInstance(Context c) {
        if (instance == null)
            instance = new SensorManager(c.getApplicationContext());
        return instance;
    }

    public void startListeningAndRequestPermissions(Activity activity, PermissionsEvent.OnEventListener eventListener){
        PermissionsEvent.getInstance().startListening(eventListener);
        new Permissions(activity).checkPermissions();

    }

    public void stopListeningForPermissions(PermissionsEvent.OnEventListener eventListener){
        PermissionsEvent.getInstance().stopListening();
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
        wifiSensorManager.startSensing();
        activitySensorManager.startSensing();
        audioManager.startSensing();
    }

    public void stopAllSensors() {
        gyroscopeManager.stopSensing();
        accelerometerManager.stopSensing();
        proximityManager.stopSensing();
        lightSensorManager.stopSensing();
        humiditySensorManager.stopSensing();
        pressureSensorManager.stopSensing();
        temperatureSensorManager.stopSensing();
        magneticFieldManager.stopSensing();
        wifiSensorManager.stopSensing();
        activitySensorManager.stopSensing();
        audioManager.stopSensing();
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

    public Handler getmSensorHandler() {
        return mSensorHandler;
    }
}