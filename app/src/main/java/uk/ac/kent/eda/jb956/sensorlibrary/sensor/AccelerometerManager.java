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
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorConfig;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.XYZSensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.database.MySQLiteHelper;
import uk.ac.kent.eda.jb956.sensorlibrary.util.NTP;
import uk.ac.kent.eda.jb956.sensorlibrary.util.SensorUtils;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class AccelerometerManager extends BaseSensor implements SensingInterface, SensorEventListener, DutyCyclingManager.DutyCyclingEventListener {

    private final String TAG = "AccelerometerManager";
    private final Context context;
    private final android.hardware.SensorManager androidSensorManager;

    public AccelerometerManager(Context context) {
        this.context = context.getApplicationContext();
        androidSensorManager = (android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = androidSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private final Sensor sensor;

    @Override
    public Sensor getSensor() {
        return sensor;
    }

    @Override
    public List<SensorData> getDataFromRange(long start, long end) {
        List<SensorData> temp = new ArrayList<>();
        Cursor cur = MySQLiteHelper.getInstance(context).getReadableDatabase().rawQuery("SELECT * FROM acc where timestamp >=" + start + " and timestamp <=" + end, null);
        while (cur.moveToNext()) {
            //Which column you want to export
            XYZSensorData sensorData = new XYZSensorData(SensorUtils.SENSOR_TYPE_ACCELEROMETER);
            sensorData.timestamp = Long.parseLong(cur.getString(1));
            sensorData.X = Double.parseDouble(cur.getString(2));
            sensorData.Y = Double.parseDouble(cur.getString(3));
            sensorData.Z = Double.parseDouble(cur.getString(4));
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
    public void removeAllDataFromDatabase() {
        removeDataFromDatabaseWithLimit(-1);
    }

    @Override
    public void removeDataFromDatabaseWithLimit(int limit) {
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        logInfo(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        if (limit == -1)
            database.execSQL("DELETE FROM acc");
        else
            database.execSQL("DELETE FROM acc WHERE id IN(SELECT id FROM acc ORDER BY id ASC LIMIT " + limit + ")");

        logInfo(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }

    @Override
    public void removeDataFromDatabaseWithRange(long start, long end) {
        String dbName = "acc";
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        logInfo(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        database.execSQL("DELETE FROM " + dbName + " where timestamp >=" + start + " and timestamp <=" + end);
        logInfo(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }

    @Override
    public AccelerometerManager withConfig(SensorConfig config) {
        super.withConfig(config);
        return this;
    }

    @Override
    public AccelerometerManager startSensing() {
        if (isSensing())
            return this;
        try {
            logInfo(TAG, "Registering listener...");
            if (sensor != null) {
                dutyCyclingManager.subscribeToListener(this);
                androidSensorManager.registerListener(this, getSensor(), getSamplingRateMicroseconds(), SensorManager.getInstance(context).getmSensorHandler());
                sensing = true;
                getSensorEvent().onSensingStarted(SensorUtils.SENSOR_TYPE_ACCELEROMETER);
                dutyCyclingManager.run();
            } else {
                logInfo(TAG, "Cannot calculate Accelerometer data, as Accelerometer sensor is not available!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logInfo(TAG, !isSensing() ? TAG + " not started: Disabled" : TAG + " started");
        return this;
    }

    @Override
    public AccelerometerManager stopSensing() {
        if (!isSensing())
            return this;
        try {
            androidSensorManager.unregisterListener(this, getSensor());
            dutyCyclingManager.stop();
            getSensorEvent().onSensingStopped(SensorUtils.SENSOR_TYPE_ACCELEROMETER);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sensing = false;
        logInfo(TAG, "Sensor stopped");
        SensorManager.getInstance(context).stopSensor(SensorUtils.SENSOR_TYPE_ACCELEROMETER);
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
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long curTime = NTP.currentTimeMillis();

            Sensor mySensor = event.sensor;
            if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                // only allow one update every SAMPLING_RATE (ms).
                if ((curTime - lastUpdate) > getSamplingRate()) {
                    lastUpdate = curTime;
                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];
                    XYZSensorData sensorData = new XYZSensorData(SensorUtils.SENSOR_TYPE_ACCELEROMETER);
                    sensorData.X = x;
                    sensorData.Y = y;
                    sensorData.Z = z;
                    sensorData.timestamp = NTP.currentTimeMillis();
                    setLastEntry(sensorData);
                    if (canSaveToDatabase()) {
                        MySQLiteHelper.getInstance(context).addToAcc(sensorData);
                    }
                    if (getSensorEvent() != null)
                        getSensorEvent().onDataSensed(sensorData);
                    //Log.i(TAG, "X: " + x + " Y: " + y + " Z: " + z);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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
}
