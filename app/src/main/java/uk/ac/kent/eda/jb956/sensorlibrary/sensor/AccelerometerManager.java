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
import uk.ac.kent.eda.jb956.sensorlibrary.callback.SensingCallbackData;
import uk.ac.kent.eda.jb956.sensorlibrary.callback.SensingEvent;
import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.XYZSensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.database.MySQLiteHelper;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class AccelerometerManager implements SensingInterface, SensorEventListener {

    private final String TAG = "AccelerometerManager";
    private static AccelerometerManager instance;
    private final Context context;
    private final android.hardware.SensorManager androidSensorManager;
    public static int SAMPLING_RATE = 100; //ms
    public static final int SAMPLING_RATE_MICRO = SAMPLING_RATE * 1000;

    public static synchronized AccelerometerManager getInstance(Context context) {
        if (instance == null)
            instance = new AccelerometerManager(context);
        return instance;
    }

    private AccelerometerManager(Context context) {
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

    private SensingEvent sensorEvent = null;

    @Override
    public SensingEvent getSensorEventListener() {
        if (sensorEvent == null)
            sensorEvent = new SensingEvent();
        return sensorEvent;
    }

    @Override
    public List<SensorData> getDataFromRange(long start, long end) {
        List<SensorData> temp = new ArrayList<>();
        Cursor cur = MySQLiteHelper.getInstance(context).getReadableDatabase().rawQuery("SELECT * FROM acc where timestamp >=" + start + " and timestamp <=" + end, null);
        while (cur.moveToNext()) {
            //Which column you want to export
            XYZSensorData sensorData = new XYZSensorData();
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
    public void removeAllDataFromDatabase() {
        removeDataFromDatabaseWithLimit(-1);
    }

    @Override
    public void setSaveToCSV(boolean save) {
        Settings.SAVE_ACCELEROMETER_TO_DATABASE = save;
    }

    @Override
    public void removeDataFromDatabaseWithLimit(int limit) {
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        Log.i(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        if (limit == -1)
            database.execSQL("DELETE FROM acc");
        else
            database.execSQL("DELETE FROM acc WHERE id IN(SELECT id FROM acc ORDER BY id ASC LIMIT " + limit + ")");

        Log.i(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }

    @Override
    public void removeDataFromDatabaseWithRange(long start, long end) {
        String dbName = "acc";
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        Log.i(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        database.execSQL("DELETE FROM " + dbName + " where timestamp >=" + start + " and timestamp <=" + end);
        Log.i(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }

    @Override
    public void startSensing() {
        if (isSensing())
            return;
        try {
            if (Settings.ACC_ENABLED) {
                Log.i(TAG, "Registering listener...");
                if (sensor != null) {
                    androidSensorManager.registerListener(this, getSensor(), SAMPLING_RATE_MICRO, SensorManager.getInstance(context).getmSensorHandler());
                    sensing = true;
                } else {
                    Log.i(TAG, "Cannot calculate Accelerometer data, as Accelerometer sensor is not available!");
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
            if (Settings.ACC_ENABLED)
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
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long curTime = System.currentTimeMillis();

            Sensor mySensor = event.sensor;
            if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                // only allow one update every SAMPLING_RATE (ms).
                if ((curTime - lastUpdate) > SAMPLING_RATE) {
                    lastUpdate = curTime;
                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];
                    XYZSensorData ad = new XYZSensorData();
                    ad.X = x;
                    ad.Y = y;
                    ad.Z = z;
                    ad.timestamp = System.currentTimeMillis();
                    lastEntry = ad;
                    MySQLiteHelper.getInstance(context).addToAcc(ad);
                    if (sensorEvent != null)
                        sensorEvent.onDataSensed(new SensingCallbackData(ad, ad.timestamp));
                    //Log.i(TAG, "X: " + x + " Y: " + y + " Z: " + z);
                }
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled){
        Settings.ACC_ENABLED = enabled;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
