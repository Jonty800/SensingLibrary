package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import uk.ac.kent.eda.jb956.sensorlibrary.SensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.TemeratureSensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.database.MySQLiteHelper;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class TemperatureSensorManager implements SensingInterface, SensorEventListener {

    private final String TAG = "TemperatureSensor";
    private static TemperatureSensorManager instance;
    private final Context context;
    private final android.hardware.SensorManager androidSensorManager;
    public static int SAMPLING_RATE = 1000; //ms
    public static final int SAMPLING_RATE_MICRO = SAMPLING_RATE * 1000;

    public static synchronized TemperatureSensorManager getInstance(Context context) {
        if (instance == null)
            instance = new TemperatureSensorManager(context);
        return instance;
    }

    private TemperatureSensorManager(Context context) {
        this.context = context.getApplicationContext();
        androidSensorManager = (android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = androidSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
    }

    private final Sensor sensor;

    @Override
    public Sensor getSensor() {
        return sensor;
    }

    @Override
    public SensorData getLastEntry() {
        return lastEntry;
    }

    @Override
    public void setSamplingRate(int rate) {
        SAMPLING_RATE = rate;
    }

    @Override
    public int getSamplingRate() {
        return SAMPLING_RATE;
    }

    @Override
    public void startSensing() {
        if (isSensing())
            return;
        try {
            if (Settings.TEMP_ENABLED) {
                Log.i(TAG, "Registering listener...");
                if (sensor != null) {
                    androidSensorManager.registerListener(this, getSensor(), SAMPLING_RATE_MICRO, SensorManager.getInstance(context).getmSensorHandler());
                    sensing = true;
                } else {
                    Log.i(TAG, "Cannot calculate Ambient Temperature, as Ambient Temperature sensor is not available!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, !isSensing() ? TAG + " not started: Disabled" : TAG + " started");
    }

    @Override
    public void stopSensing() {
        if (!isSensing())
            return;
        try {
            if (Settings.TEMP_ENABLED)
                androidSensorManager.unregisterListener(this, getSensor());
        } catch (Exception e) {
            e.printStackTrace();
        }
        sensing = false;
        Log.i(TAG, "Sensor stopped");
    }

    private boolean sensing = false;

    @Override
    public boolean isSensing() {
        return sensing;
    }

    private long lastUpdate = 0;
    private SensorData lastEntry = null;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            long curTime = System.currentTimeMillis();

            Sensor mySensor = event.sensor;
            if (mySensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                // only allow one update every SAMPLING_RATE (ms).
                if ((curTime - lastUpdate) > SAMPLING_RATE) {
                    lastUpdate = curTime;
                    float degreesC = event.values[0];
                    TemeratureSensorData hd = new TemeratureSensorData();
                    hd.degreesC = degreesC;
                    hd.timestamp = System.currentTimeMillis();
                    lastEntry = hd;
                    MySQLiteHelper.getInstance(context).addToTemperature(hd);
                    Log.i(TAG, "Humidity: " + hd.degreesC);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}