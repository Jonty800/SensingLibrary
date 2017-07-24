package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;
import uk.ac.kent.eda.jb956.sensorlibrary.data.ProximitySensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.database.MySQLiteHelper;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class ProximitySensorManager implements SensingInterface, SensorEventListener {

    private final String TAG = "ProximitySensorManager";
    private static ProximitySensorManager instance;
    private final Context context;
    private final android.hardware.SensorManager androidSensorManager;
    public static int SAMPLING_RATE_PROXIMITY = 100; //ms
    public static final int SAMPLING_RATE_PROXIMITY_MICRO = SAMPLING_RATE_PROXIMITY * 1000;

    public static synchronized ProximitySensorManager getInstance(Context context) {
        if (instance == null)
            instance = new ProximitySensorManager(context);
        return instance;
    }

    private ProximitySensorManager(Context context) {
        this.context = context.getApplicationContext();
        androidSensorManager = (android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = androidSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    }

    private final Sensor sensor;

    @Override
    public Sensor getSensor() {
        return sensor;
    }

    @Override
    public SensorData getSingleEntry() {
        return lastEntry;
    }

    @Override
    public void setSamplingRate(int rate) {
        SAMPLING_RATE_PROXIMITY = rate;
    }

    @Override
    public int getSamplingRate() {
        return SAMPLING_RATE_PROXIMITY;
    }

    @Override
    public void startSensing() {
        if (isSensing())
            return;
        try {
            if (Settings.PROXIMITY_ENABLED) {
                androidSensorManager.registerListener(this, getSensor(), SensorManager.SENSOR_DELAY_NORMAL);
                sensing = true;
            } else
                Log.i(TAG, "PROXIMITY_ENABLED=false, ignoring Proximity collection");
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
            if (Settings.PROXIMITY_ENABLED)
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
    public List<ProximitySensorData> history = new ArrayList<>();

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
            long curTime = System.currentTimeMillis();

            Sensor mySensor = event.sensor;
            if (mySensor.getType() == Sensor.TYPE_PROXIMITY) {
                // only allow one update every SAMPLING_RATE (ms).
                if ((curTime - lastUpdate) > SAMPLING_RATE_PROXIMITY) {
                    lastUpdate = curTime;
                    float proximity = event.values[0]; //cm
                    ProximitySensorData pd = new ProximitySensorData();
                    pd.proximity = proximity;
                    pd.timestamp = System.currentTimeMillis();
                    lastEntry = pd;
                    MySQLiteHelper.getInstance(context).addToProximity(pd);
                    history.add(pd);
                    Log.i(TAG, "proximity: " + proximity);
                    List<ProximitySensorData> temp = new ArrayList<>();
                    for (ProximitySensorData data : history) {
                        if (data.timestamp > (System.currentTimeMillis() - 4000))
                            temp.add(data);
                    }
                    history = new ArrayList<>(temp);
                }
                // System.out.println(""+(System.currentTimeMillis() - lastTimeCheckedHistory));

            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}

