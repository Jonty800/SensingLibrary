package uk.ac.kent.eda.jb956.sensorlibrary;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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
import uk.ac.kent.eda.jb956.sensorlibrary.service.SensingService;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class SensorManager {

    private static SensorManager instance;

    private final Context context;
    private final String TAG = "SensorManager";

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
        workerThread =  WorkerThread.create();
    }

    public static synchronized SensorManager getInstance(Context c) {
        if (instance == null)
            instance = new SensorManager(c.getApplicationContext());
        return instance;
    }

    public void startSensingService(){
        Intent i = new Intent(context, SensingService.class);
        context.startService(i);
    }

    public void stopSensingService(){
        Intent i = new Intent(context, SensingService.class);
        context.stopService(i);
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

    private HandlerThread mSensorThread;
    private Handler mSensorHandler;

    public Handler getmSensorHandler() {
        return mSensorHandler;
    }
}