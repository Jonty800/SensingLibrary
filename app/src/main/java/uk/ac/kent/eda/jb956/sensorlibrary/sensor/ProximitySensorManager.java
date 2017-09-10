package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import uk.ac.kent.eda.jb956.sensorlibrary.callback.SensingEvent;
import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;
import uk.ac.kent.eda.jb956.sensorlibrary.data.ProximitySensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorConfig;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.database.MySQLiteHelper;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class ProximitySensorManager extends BaseSensor implements SensingInterface, SensorEventListener {

    private final String TAG = "ProximitySensorManager";
    private static ProximitySensorManager instance;
    private final Context context;
    private final android.hardware.SensorManager androidSensorManager;

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
    public SensorData getLastEntry() {
        return lastEntry;
    }

    private SensingEvent sensorEvent = null;

    @Override
    public SensingEvent getSensorEventListener() {
        if (sensorEvent == null)
            sensorEvent = new SensingEvent();
        return sensorEvent;
    }

    @Override
    public ProximitySensorManager startSensing() {
        if (isSensing())
            return this;
        try {
            if (Settings.PROXIMITY_ENABLED) {
                Log.i(TAG, "Registering listener...");
                if (sensor != null) {
                    androidSensorManager.registerListener(this, getSensor(), SensorManager.SENSOR_DELAY_NORMAL);
                    sensing = true;
                    getSensorEventListener().onSensingStarted();
                } else {
                    Log.i(TAG, "Cannot calculate Proximity, as Proximity sensor is not available!");
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, !isSensing() ? TAG + " not started: Disabled" : TAG + " started");
        return this;
    }

    @Override
    public ProximitySensorManager stopSensing() {
        if (!isSensing())
            return this;
        try {
            if (Settings.PROXIMITY_ENABLED) {
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
    public List<ProximitySensorData> history = new ArrayList<>();

    @Override
    public void setSensingWindowDuration(int duration) {

    }

    @Override
    public void setSleepingDuration(int duration) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
            long curTime = System.currentTimeMillis();

            Sensor mySensor = event.sensor;
            if (mySensor.getType() == Sensor.TYPE_PROXIMITY) {
                // only allow one update every SAMPLING_RATE (ms).
                if ((curTime - lastUpdate) > getSamplingRate()) {
                    lastUpdate = curTime;
                    float proximity = event.values[0]; //cm
                    ProximitySensorData sensorData = new ProximitySensorData();
                    sensorData.proximity = proximity;
                    sensorData.timestamp = System.currentTimeMillis();
                    lastEntry = sensorData;
                    MySQLiteHelper.getInstance(context).addToProximity(sensorData);
                    history.add(sensorData);
                    List<ProximitySensorData> temp = new ArrayList<>();
                    for (ProximitySensorData data : history) {
                        if (data.timestamp > (System.currentTimeMillis() - 4000))
                            temp.add(data);
                    }
                    history = new ArrayList<>(temp);
                    if (sensorEvent != null)
                        sensorEvent.onDataSensed(sensorData);
                }
                // System.out.println(""+(System.currentTimeMillis() - lastTimeCheckedHistory));

            }
        }
    }

    @Override
    public ProximitySensorManager withConfig(SensorConfig config){
        super.withConfig(config);
        return this;
    }

    @Override
    public List<SensorData> getDataFromRange(long start, long end) {
        List<SensorData> temp = new ArrayList<>();
        Cursor cur = MySQLiteHelper.getInstance(context).getReadableDatabase().rawQuery("SELECT * FROM proximity where timestamp >=" + start + " and timestamp <=" + end, null);
        while (cur.moveToNext()) {
            //Which column you want to export
            ProximitySensorData sensorData = new ProximitySensorData();
            sensorData.timestamp = Long.parseLong(cur.getString(1));
            sensorData.proximity = Float.parseFloat(cur.getString(2));
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
    public void removeDataFromDatabaseWithLimit(int limit) {
        String dbName = "proximity";
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        Log.i(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        if (limit == -1)
            database.execSQL("DELETE FROM " + dbName);
        else
            database.execSQL("DELETE FROM " + dbName + " WHERE id IN(SELECT id FROM " + dbName + " ORDER BY id ASC LIMIT " + limit + ")");

        Log.i(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }

    @Override
    public void removeAllDataFromDatabase() {
        removeDataFromDatabaseWithLimit(-1);
    }

    @Override
    public void setSaveToDatabase(boolean save) {
        Settings.SAVE_PROXIMITY_TO_DATABASE = save;
    }

    @Override
    public void removeDataFromDatabaseWithRange(long start, long end) {
        String dbName = "proximity";
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        Log.i(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        database.execSQL("DELETE FROM " + dbName + " where timestamp >=" + start + " and timestamp <=" + end);
        Log.i(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }

    @Override
    public void setEnabled(boolean enabled) {
        Settings.PROXIMITY_ENABLED = enabled;
    }
}

