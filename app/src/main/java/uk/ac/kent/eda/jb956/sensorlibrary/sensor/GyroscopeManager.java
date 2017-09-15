package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import java.util.ArrayList;
import java.util.List;

import uk.ac.kent.eda.jb956.sensorlibrary.SensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorConfig;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.XYZSensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.database.MySQLiteHelper;
import uk.ac.kent.eda.jb956.sensorlibrary.util.SensorUtils;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class GyroscopeManager extends BaseSensor implements SensingInterface, SensorEventListener {

    private final String TAG = "GyroscopeManager";
    private static GyroscopeManager instance;
    private final Context context;
    private final android.hardware.SensorManager androidSensorManager;


    public static synchronized GyroscopeManager getInstance(Context context) {
        if (instance == null)
            instance = new GyroscopeManager(context);
        return instance;
    }

    public GyroscopeManager(Context context) {
        this.context = context.getApplicationContext();
        androidSensorManager = (android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = androidSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
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
    public GyroscopeManager withConfig(SensorConfig config) {
        super.withConfig(config);
        return this;
    }

    @Override
    public GyroscopeManager startSensing() {
        if (isSensing())
            return this;
        try {
            getSensorEvent().onSensingStarted(SensorUtils.SENSOR_TYPE_GYROSCOPE);
            logInfo(TAG, "Registering listener...");
            if (sensor != null) {
                androidSensorManager.registerListener(this, getSensor(), getSamplingRateMicroseconds(), SensorManager.getInstance(context).getmSensorHandler());
                sensing = true;
            } else {
                logInfo(TAG, "Cannot calculate Gyroscope data, as gyroscope sensor is not available!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logInfo(TAG, !isSensing() ? TAG + " not started: Disabled" : TAG + " started");
        return this;
    }

    @Override
    public GyroscopeManager stopSensing() {
        if (!isSensing())
            return this;
        try {
            androidSensorManager.unregisterListener(this, getSensor());
            getSensorEvent().onSensingStopped(SensorUtils.SENSOR_TYPE_GYROSCOPE);
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
    private SensorData lastEntry = null;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            long curTime = System.currentTimeMillis();

            Sensor mySensor = event.sensor;
            if (mySensor.getType() == Sensor.TYPE_GYROSCOPE) {
                // only allow one update every SAMPLING_RATE (ms).
                if ((curTime - lastUpdate) > getSamplingRate()) {
                    lastUpdate = curTime;
                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];
                    XYZSensorData sensorData = new XYZSensorData(SensorUtils.SENSOR_TYPE_GYROSCOPE);
                    sensorData.X = x;
                    sensorData.Y = y;
                    sensorData.Z = z;
                    sensorData.timestamp = System.currentTimeMillis();
                    lastEntry = sensorData;
                    if (canSaveToDatabase()) {
                        MySQLiteHelper.getInstance(context).addToGyro(sensorData);
                    }
                    if (getSensorEvent() != null)
                        getSensorEvent().onDataSensed(sensorData);
                    // Log.i(TAG, "X: " + x + " Y: " + y + " Z: " + z);
                }
            }
        }
    }

    @Override
    public void setSensingWindowDuration(int duration) {

    }

    @Override
    public void setSleepingDuration(int duration) {

    }

    @Override
    public List<SensorData> getDataFromRange(long start, long end) {
        List<SensorData> temp = new ArrayList<>();
        Cursor cur = MySQLiteHelper.getInstance(context).getReadableDatabase().rawQuery("SELECT * FROM gyro where timestamp >=" + start + " and timestamp <=" + end, null);
        while (cur.moveToNext()) {
            //Which column you want to export
            XYZSensorData sensorData = new XYZSensorData(SensorUtils.SENSOR_TYPE_GYROSCOPE);
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
        return getDataFromRange(0L, System.currentTimeMillis());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void removeAllDataFromDatabase() {
        removeDataFromDatabaseWithLimit(-1);
    }

    @Override
    public void removeDataFromDatabaseWithLimit(int limit) {
        String dbName = "gyro";
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        logInfo(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        if (limit == -1)
            database.execSQL("DELETE FROM " + dbName);
        else
            database.execSQL("DELETE FROM " + dbName + " WHERE id IN(SELECT id FROM " + dbName + " ORDER BY id ASC LIMIT " + limit + ")");

        logInfo(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }

    @Override
    public void removeDataFromDatabaseWithRange(long start, long end) {
        String dbName = "gyro";
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        logInfo(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        database.execSQL("DELETE FROM " + dbName + " where timestamp >=" + start + " and timestamp <=" + end);
        logInfo(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }
}
