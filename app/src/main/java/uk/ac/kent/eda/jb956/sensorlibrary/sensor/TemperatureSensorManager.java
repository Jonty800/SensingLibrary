package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import uk.ac.kent.eda.jb956.sensorlibrary.SensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.callback.SensingEvent;
import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorConfig;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.TemperatureSensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.database.MySQLiteHelper;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class TemperatureSensorManager extends BaseSensor implements SensingInterface, SensorEventListener {

    private final String TAG = "TemperatureSensor";
    private static TemperatureSensorManager instance;
    private final Context context;
    private final android.hardware.SensorManager androidSensorManager;

    public static synchronized TemperatureSensorManager getInstance(Context context) {
        if (instance == null)
            instance = new TemperatureSensorManager(context);
        return instance;
    }

    private TemperatureSensorManager(Context context) {
        this.context = context.getApplicationContext();
        androidSensorManager = (android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = androidSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        setSamplingRate(1000);
    }

    private SensingEvent sensorEvent = null;

    @Override
    public SensingEvent getSensorEventListener() {
        if (sensorEvent == null)
            sensorEvent = new SensingEvent();
        return sensorEvent;
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
    public TemperatureSensorManager startSensing() {
        if (isSensing())
            return this;
        try {
            if (Settings.TEMP_ENABLED) {
                Log.i(TAG, "Registering listener...");
                if (sensor != null) {
                    androidSensorManager.registerListener(this, getSensor(), getSamplingRateMicroseconds(), SensorManager.getInstance(context).getmSensorHandler());
                    sensing = true;
                    getSensorEventListener().onSensingStarted();
                } else {
                    Log.i(TAG, "Cannot calculate Ambient Temperature, as Ambient Temperature sensor is not available!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, !isSensing() ? TAG + " not started: Disabled" : TAG + " started");
        return this;
    }

    @Override
    public TemperatureSensorManager stopSensing() {
        if (!isSensing())
            return this;
        try {
            if (Settings.TEMP_ENABLED) {
                androidSensorManager.unregisterListener(this, getSensor());
                getSensorEventListener().onSensingStopped();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        sensing = false;
        Log.i(TAG, "Sensor stopped");
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
    public TemperatureSensorManager withConfig(SensorConfig config){
        super.withConfig(config);
        return this;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            long curTime = System.currentTimeMillis();

            Sensor mySensor = event.sensor;
            if (mySensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                // only allow one update every SAMPLING_RATE (ms).
                if ((curTime - lastUpdate) > getSamplingRate()) {
                    lastUpdate = curTime;
                    float degreesC = event.values[0];
                    TemperatureSensorData sensorData = new TemperatureSensorData();
                    sensorData.degreesC = degreesC;
                    sensorData.timestamp = System.currentTimeMillis();
                    lastEntry = sensorData;
                    if(canSaveToDatabase()) {
                        MySQLiteHelper.getInstance(context).addToTemperature(sensorData);
                    }
                    if (sensorEvent != null)
                        sensorEvent.onDataSensed(sensorData);
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
        Cursor cur = MySQLiteHelper.getInstance(context).getReadableDatabase().rawQuery("SELECT * FROM temp where timestamp >=" + start + " and timestamp <=" + end, null);
        while (cur.moveToNext()) {
            //Which column you want to export
            TemperatureSensorData sensorData = new TemperatureSensorData();
            sensorData.timestamp = Long.parseLong(cur.getString(1));
            sensorData.degreesC = Float.parseFloat(cur.getString(2));
            temp.add(sensorData);
        }
        cur.close();
        return temp;
    }

    @Override
    public void setEnabled(boolean enabled) {
        Settings.TEMP_ENABLED = enabled;
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
        String dbName = "temp";
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        Log.i(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        if (limit == -1)
            database.execSQL("DELETE FROM " + dbName);
        else
            database.execSQL("DELETE FROM " + dbName + " WHERE id IN(SELECT id FROM " + dbName + " ORDER BY id ASC LIMIT " + limit + ")");

        Log.i(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }

    @Override
    public void removeDataFromDatabaseWithRange(long start, long end) {
        String dbName = "temp";
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        Log.i(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        database.execSQL("DELETE FROM " + dbName + " where timestamp >=" + start + " and timestamp <=" + end);
        Log.i(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }
}
