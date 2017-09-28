package uk.ac.kent.eda.jb956.sensorlibrary.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import uk.ac.kent.eda.jb956.sensorlibrary.SensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorConfig;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class SensingService extends Service {

    private static final String TAG = "SensingService";
    SensorManager sensorManager;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = SensorManager.getInstance(this);

        //TODO test this
        Intent notificationIntent = new Intent(this, SensingService.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.arrow_up_float)
                .setContentTitle(Settings.appName)
                .setContentText("Collecting sensor information...")
                .setContentIntent(pendingIntent).build();

        startForeground(1337, notification);
    }

    /**
     * Attempts to start the Gyro, Wifi and Acc sensors. Will not start if Settings class forbids it
     */
   /* public void startAllSensors() {
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
    }*/
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String exec = intent.getStringExtra("exec");
        if (exec != null) {
            if (exec.equalsIgnoreCase("stopservice")) {
                stopForeground(true);
                stopSelf();
            } else {
                int sensorId = intent.getIntExtra("type", -1);
                if (sensorId > -1) {
                    SensorConfig config = (SensorConfig) intent.getSerializableExtra("config");
                    if (exec.equalsIgnoreCase("start")) {
                        sensorManager.getSensorById(sensorId).withConfig(config).startSensing();
                    } else if (exec.equalsIgnoreCase("stop")) {
                        sensorManager.getSensorById(sensorId).stopSensing();
                    }
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
