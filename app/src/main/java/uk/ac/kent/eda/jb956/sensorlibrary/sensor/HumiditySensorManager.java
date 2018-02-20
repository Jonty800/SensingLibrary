package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import java.util.ArrayList;
import java.util.List;

import uk.ac.kent.eda.jb956.sensorlibrary.DutyCyclingManager;
import uk.ac.kent.eda.jb956.sensorlibrary.SensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.data.PressureSensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.database.MySQLiteHelper;
import uk.ac.kent.eda.jb956.sensorlibrary.util.NTP;
import uk.ac.kent.eda.jb956.sensorlibrary.util.SensorUtils;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class HumiditySensorManager extends BaseSensor implements SensingInterface, SensorEventListener, DutyCyclingManager.DutyCyclingEventListener {

    private final String TAG = "HumiditySensorManager";
    private final Context context;
    private final android.hardware.SensorManager androidSensorManager;

    public HumiditySensorManager(Context context) {
        this.context = context.getApplicationContext();
        androidSensorManager = (android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = androidSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
    }

    private final Sensor sensor;

    @Override
    public Sensor getSensor() {
        return sensor;
    }

    @Override
    public HumiditySensorManager startSensing() {
        if (isSensing())
            return this;
        try {
            logInfo(TAG, "Registering listener...");
            if (sensor != null) {
                dutyCyclingManager.subscribeToListener(this);
                dutyCyclingManager.run();
                androidSensorManager.registerListener(this, getSensor(), getSamplingRateMicroseconds(), SensorManager.getInstance(context).getmSensorHandler());
                sensing = true;
                getSensorEvent().onSensingStarted(SensorUtils.SENSOR_TYPE_HUMIDITY);
            } else {
                logInfo(TAG, "Cannot calculate Humidity, as humidity sensor is not available!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logInfo(TAG, !isSensing() ? TAG + " not started: Disabled" : TAG + " started");
        return this;
    }

    @Override
    public HumiditySensorManager stopSensing() {
        if (!isSensing())
            return this;
        try {
            dutyCyclingManager.stop();
            androidSensorManager.unregisterListener(this, getSensor());
            getSensorEvent().onSensingStopped(SensorUtils.SENSOR_TYPE_HUMIDITY);
            SensorManager.getInstance(context).stopSensor(SensorUtils.SENSOR_TYPE_HUMIDITY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sensing = false;
        logInfo(TAG, "Sensor stopped");
        return this;
    }

    private boolean sensing = false;

    @Override
    public boolean isSensing() {
        return sensing;
    }

    private long lastUpdate = 0;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
            long curTime = NTP.currentTimeMillis();

            Sensor mySensor = event.sensor;
            if (mySensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
                // only allow one update every SAMPLING_RATE (ms).
                if ((curTime - lastUpdate) > getSamplingRate()) {
                    lastUpdate = curTime;
                    float millibars_of_pressure = event.values[0];
                    PressureSensorData sensorData = new PressureSensorData(SensorUtils.SENSOR_TYPE_HUMIDITY);
                    sensorData.pressure = millibars_of_pressure;
                    sensorData.timestamp = NTP.currentTimeMillis();
                    setLastEntry(sensorData);
                    if (canSaveToDatabase()) {
                        MySQLiteHelper.getInstance(context).addToHumidity(sensorData);
                    }
                    if (getSensorEvent() != null)
                        getSensorEvent().onDataSensed(sensorData);
                }
            }
        }
    }

    @Override
    public void onWake(int duration) {
        logInfo(TAG, "Resuming sensor for " + duration);
        androidSensorManager.registerListener(this, getSensor(), getSamplingRateMicroseconds(), SensorManager.getInstance(context).getmSensorHandler());
    }

    @Override
    public void onSleep(int duration) {
        logInfo(TAG, "Pausing sensor for " + duration);
        androidSensorManager.unregisterListener(this, getSensor());
    }

    @Override
    public List<SensorData> getDataFromRange(long start, long end) {
        List<SensorData> temp = new ArrayList<>();
        Cursor cur = MySQLiteHelper.getInstance(context).getReadableDatabase().rawQuery("SELECT * FROM humidity where timestamp >=" + start + " and timestamp <=" + end, null);
        while (cur.moveToNext()) {
            //Which column you want to export
            PressureSensorData sensorData = new PressureSensorData(SensorUtils.SENSOR_TYPE_HUMIDITY);
            sensorData.timestamp = Long.parseLong(cur.getString(1));
            sensorData.pressure = Float.parseFloat(cur.getString(2));
            temp.add(sensorData);
        }
        cur.close();
        return temp;
    }

    @Override
    public List<SensorData> getAllData() {
        return getDataFromRange(0L, NTP.currentTimeMillis());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void removeAllDataFromDatabase() {
        removeDataFromDatabaseWithLimit(-1);
    }

    @Override
    public void removeDataFromDatabaseWithRange(long start, long end) {
        String dbName = "humidity";
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        logInfo(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        database.execSQL("DELETE FROM " + dbName + " where timestamp >=" + start + " and timestamp <=" + end);
        logInfo(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }

    @Override
    public void removeDataFromDatabaseWithLimit(int limit) {
        String dbName = "humidity";
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        logInfo(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        if (limit == -1)
            database.execSQL("DELETE FROM " + dbName);
        else
            database.execSQL("DELETE FROM " + dbName + " WHERE id IN(SELECT id FROM " + dbName + " ORDER BY id ASC LIMIT " + limit + ")");

        logInfo(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }
}
