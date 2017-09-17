package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import uk.ac.kent.eda.jb956.sensorlibrary.DutyCyclingManager;
import uk.ac.kent.eda.jb956.sensorlibrary.data.LightSensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorConfig;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.database.MySQLiteHelper;
import uk.ac.kent.eda.jb956.sensorlibrary.util.SensorUtils;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class LightSensorManager extends BaseSensor implements SensingInterface, SensorEventListener, DutyCyclingManager.DutyCyclingEventListener {

    private final String TAG = "LightSensorManager";
    private final Context context;
    private final android.hardware.SensorManager androidSensorManager;
    private Handler mHandler;

    public LightSensorManager(Context context) {
        this.context = context.getApplicationContext();
        androidSensorManager = (android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = androidSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mHandler = new Handler();
        dutyCyclingManager.subscribeToListener(this);
    }

    private final Sensor sensor;

    @Override
    public Sensor getSensor() {
        return sensor;
    }

    @Override
    public LightSensorManager startSensing() {
        if (isSensing())
            return this;
        try {
            logInfo(TAG, "Registering listener...");
            if (sensor != null) {
                dutyCyclingManager.run();
                androidSensorManager.registerListener(this, getSensor(), SensorManager.SENSOR_DELAY_NORMAL);
                sensing = true;
                getSensorEvent().onSensingStarted(SensorUtils.SENSOR_TYPE_LIGHT);
            } else {
                logInfo(TAG, "Cannot calculate Lux, as Light sensor is not available!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logInfo(TAG, !isSensing() ? TAG + " not started: Disabled" : TAG + " started");
        /*if (Settings.POCKET_ENABLED) {
            if (!Settings.PROXIMITY_ENABLED) {
                Log.i(TAG, "PROXIMITY_ENABLED=false, ignoring pocket detection");
                return this;
            }
            if (!Settings.LIGHT_ENABLED) {
                Log.i(TAG, "LIGHT_ENABLED=false, ignoring pocket detection");
                return this;
            }
            startRepeatingTask();
        }*/
        return this;
    }

    @Override
    public LightSensorManager stopSensing() {
        if (!isSensing())
            return this;
        try {
            dutyCyclingManager.stop();
            androidSensorManager.unregisterListener(this, getSensor());
            getSensorEvent().onSensingStopped(SensorUtils.SENSOR_TYPE_LIGHT);
            uk.ac.kent.eda.jb956.sensorlibrary.SensorManager.getInstance(context).stopSensor(SensorUtils.SENSOR_TYPE_LIGHT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sensing = false;
        logInfo(TAG, "Sensor stopped");
        /*
        stopRepeatingTask();
        */
        return this;
    }

    private boolean sensing = false;

    @Override
    public boolean isSensing() {
        return sensing;
    }

    private long lastUpdate = 0;
    public List<LightSensorData> history = new ArrayList<>();
    private long lastTimeCheckedHistory = System.currentTimeMillis();

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (sensor.getType() == Sensor.TYPE_LIGHT) {
            long curTime = System.currentTimeMillis();

            Sensor mySensor = event.sensor;
            if (mySensor.getType() == Sensor.TYPE_LIGHT) {
                // only allow one update every SAMPLING_RATE (ms).
                if ((curTime - lastUpdate) > getSamplingRate()) {
                    lastUpdate = curTime;
                    double lx = event.values[0];
                    LightSensorData sensorData = new LightSensorData(SensorUtils.SENSOR_TYPE_LIGHT);
                    sensorData.illuminance = lx;
                    sensorData.timestamp = System.currentTimeMillis();
                    setLastEntry(sensorData);
                    history.add(sensorData);
                    if (canSaveToDatabase()) {
                        MySQLiteHelper.getInstance(context).addToLight(sensorData);
                    }
                    // Log.i(TAG, "Lx: " + lx);
                    List<LightSensorData> temp = new ArrayList<>();
                    for (LightSensorData data : history) {
                        if (data.timestamp > (System.currentTimeMillis() - 4000))
                            temp.add(data);
                    }
                    history = new ArrayList<>(temp);
                    if (getSensorEvent() != null)
                        getSensorEvent().onDataSensed(sensorData);
                }
            }
        }
    }

    @Override
    public void onWake(int duration) {
        logInfo(TAG, "Resuming sensor for " + duration);
        androidSensorManager.registerListener(this, getSensor(), getSamplingRateMicroseconds(), uk.ac.kent.eda.jb956.sensorlibrary.SensorManager.getInstance(context).getmSensorHandler());
    }

    @Override
    public void onSleep(int duration) {
        logInfo(TAG, "Pausing sensor for " + duration);
        androidSensorManager.unregisterListener(this, getSensor());
    }

    private Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                if ((System.currentTimeMillis()) - lastTimeCheckedHistory > 4000
                        && ((ProximitySensorManager)uk.ac.kent.eda.jb956.sensorlibrary.SensorManager.getInstance(context).getSensorById(SensorUtils.SENSOR_TYPE_PROXIMITY)).history.size() > 0
                        && history.size() > 0) {
                    InPocketDetectionHelper inPocketDetectionHelper = InPocketDetectionHelper.getInstance();
                    List<Double> temp = new ArrayList<>();
                    temp.add(((ProximitySensorManager)uk.ac.kent.eda.jb956.sensorlibrary.SensorManager.getInstance(context).getSensorById(SensorUtils.SENSOR_TYPE_PROXIMITY)).history.get(((ProximitySensorManager)uk.ac.kent.eda.jb956.sensorlibrary.SensorManager.getInstance(context).getSensorById(SensorUtils.SENSOR_TYPE_PROXIMITY)).history.size() - 1).proximity);
                    // for (ProximitySensorData pd : ProximitySensorManager.getInstance(context).history) {
                    // temp.add(pd.proximity);
                    //}
                    inPocketDetectionHelper.proximityValues = new ArrayList<>(temp);

                    temp = new ArrayList<>();
                    temp.add(history.get(history.size() - 1).illuminance);
                    //for (LightSensorData pd : LightSensorManager.getInstance(context).history) {
                    // temp.add(pd.illuminance);
                    // }
                    inPocketDetectionHelper.lightValues = new ArrayList<>(temp);
                    if (canSaveToDatabase()) {
                        MySQLiteHelper.getInstance(context).addToPocket(inPocketDetectionHelper.getDetectionResult(), System.currentTimeMillis());
                    }
                    logInfo(TAG, "PocketDetectionResult: " + inPocketDetectionHelper.getDetectionResult().toString());
                    lastTimeCheckedHistory = System.currentTimeMillis();
                    ((ProximitySensorManager)uk.ac.kent.eda.jb956.sensorlibrary.SensorManager.getInstance(context).getSensorById(SensorUtils.SENSOR_TYPE_PROXIMITY)).history.clear();
                    history.clear();
                }
            } finally {
                mHandler.postDelayed(mStatusChecker, 4000);
            }
        }
    };

    private void startRepeatingTask() {
        mStatusChecker.run();
    }

    private void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    @Override
    public List<SensorData> getDataFromRange(long start, long end) {
        List<SensorData> temp = new ArrayList<>();
        Cursor cur = MySQLiteHelper.getInstance(context).getReadableDatabase().rawQuery("SELECT * FROM light where timestamp >=" + start + " and timestamp <=" + end, null);
        while (cur.moveToNext()) {
            //Which column you want to export
            LightSensorData sensorData = new LightSensorData(SensorUtils.SENSOR_TYPE_LIGHT);
            sensorData.timestamp = Long.parseLong(cur.getString(1));
            sensorData.illuminance = Float.parseFloat(cur.getString(2));
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
    public void removeDataFromDatabaseWithRange(long start, long end) {
        String dbName = "light";
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        logInfo(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        database.execSQL("DELETE FROM " + dbName + " where timestamp >=" + start + " and timestamp <=" + end);
        logInfo(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }

    @Override
    public void removeDataFromDatabaseWithLimit(int limit) {
        String dbName = "light";
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        logInfo(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        if (limit == -1)
            database.execSQL("DELETE FROM " + dbName);
        else
            database.execSQL("DELETE FROM " + dbName + " WHERE id IN(SELECT id FROM " + dbName + " ORDER BY id ASC LIMIT " + limit + ")");

        logInfo(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }
}

