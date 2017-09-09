package uk.ac.kent.eda.jb956.sensorlibrary.service;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;

import uk.ac.kent.eda.jb956.sensorlibrary.SensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.control.WorkerThread;
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

public class SensingService extends Service {

    SensorManager sensorManager;

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


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = SensorManager.getInstance(this);
        audioManager = AudioSensorManager.getInstance(this);
        accelerometerManager = AccelerometerManager.getInstance(this);
        gyroscopeManager = GyroscopeManager.getInstance(this);
        proximityManager = ProximitySensorManager.getInstance(this);
        lightSensorManager = LightSensorManager.getInstance(this);
        humiditySensorManager = HumiditySensorManager.getInstance(this);
        pressureSensorManager = PressureSensorManager.getInstance(this);
        temperatureSensorManager = TemperatureSensorManager.getInstance(this);
        magneticFieldManager = MagneticFieldManager.getInstance(this);
        wifiSensorManager = WifiSensorManager.getInstance(this);
        activitySensorManager = ActivitySensorManager.getInstance(this);

        //TODO test this
       /* Intent notificationIntent = new Intent(this, SensingService.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.arrow_up_float)
                .setContentTitle("Title")
                .setContentText("Desc")
                .setContentIntent(pendingIntent).build();

       startForeground(1337, notification); */
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startAllSensors();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
