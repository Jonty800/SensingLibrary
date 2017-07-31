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

import uk.ac.kent.eda.jb956.sensorlibrary.callback.SensingCallbackData;
import uk.ac.kent.eda.jb956.sensorlibrary.callback.SensingEvent;
import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;
import uk.ac.kent.eda.jb956.sensorlibrary.data.LightSensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.PressureSensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.database.MySQLiteHelper;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class LightSensorManager implements SensingInterface, SensorEventListener {

    private final String TAG = "LightSensorManager";
    private static LightSensorManager instance;
    private final Context context;
    private final android.hardware.SensorManager androidSensorManager;
    public static int SAMPLING_RATE = 100; //ms
    public static final int SAMPLING_RATE_MICRO = SAMPLING_RATE * 1000;
    private Handler mHandler;

    public static synchronized LightSensorManager getInstance(Context context) {
        if (instance == null)
            instance = new LightSensorManager(context);
        return instance;
    }

    private LightSensorManager(Context context) {
        this.context = context.getApplicationContext();
        androidSensorManager = (android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = androidSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mHandler = new Handler();
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
        if(sensorEvent ==null)
            sensorEvent = new SensingEvent();
        return sensorEvent;
    }

    @Override
    public void startSensing() {
        if (isSensing())
            return;
        try {
            if (Settings.LIGHT_ENABLED) {
                Log.i(TAG, "Registering listener...");
                if (sensor != null) {
                    androidSensorManager.registerListener(this, getSensor(), SensorManager.SENSOR_DELAY_NORMAL);
                    sensing = true;
                } else {
                    Log.i(TAG, "Cannot calculate Lux, as Light sensor is not available!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, !isSensing() ? TAG + " not started: Disabled" : TAG + " started");
        if (Settings.POCKET_ENABLED) {
            if (!Settings.PROXIMITY_ENABLED) {
                Log.i(TAG, "PROXIMITY_ENABLED=false, ignoring pocket detection");
                return;
            }
            if (!Settings.LIGHT_ENABLED) {
                Log.i(TAG, "LIGHT_ENABLED=false, ignoring pocket detection");
                return;
            }
            startRepeatingTask();
        }
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
        stopRepeatingTask();
    }

    private boolean sensing = false;

    @Override
    public boolean isSensing() {
        return sensing;
    }

    private long lastUpdate = 0;
    private SensorData lastEntry = null;
    public List<LightSensorData> history = new ArrayList<>();
    private long lastTimeCheckedHistory = System.currentTimeMillis();

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (sensor.getType() == Sensor.TYPE_LIGHT) {
            long curTime = System.currentTimeMillis();

            Sensor mySensor = event.sensor;
            if (mySensor.getType() == Sensor.TYPE_LIGHT) {
                // only allow one update every SAMPLING_RATE (ms).
                if ((curTime - lastUpdate) > SAMPLING_RATE) {
                    lastUpdate = curTime;
                    double lx = event.values[0];
                    LightSensorData ld = new LightSensorData();
                    ld.illuminance = lx;
                    ld.timestamp = System.currentTimeMillis();
                    lastEntry = ld;
                    history.add(ld);
                    MySQLiteHelper.getInstance(context).addToLight(ld);
                   // Log.i(TAG, "Lx: " + lx);
                    List<LightSensorData> temp = new ArrayList<>();
                    for (LightSensorData data : history) {
                        if (data.timestamp > (System.currentTimeMillis() - 4000))
                            temp.add(data);
                    }
                    history = new ArrayList<>(temp);
                    if(sensorEvent!=null)
                        sensorEvent.doEvent(new SensingCallbackData(ld, ld.timestamp));
                }
            }
        }
    }

    private Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                if ((System.currentTimeMillis()) - lastTimeCheckedHistory > 4000
                        && ProximitySensorManager.getInstance(context).history.size() > 0
                        && LightSensorManager.getInstance(context).history.size() > 0) {
                    InPocketDetectionHelper inPocketDetectionHelper = InPocketDetectionHelper.getInstance();
                    List<Double> temp = new ArrayList<>();
                    temp.add(ProximitySensorManager.getInstance(context).history.get(ProximitySensorManager.getInstance(context).history.size() - 1).proximity);
                    // for (ProximitySensorData pd : ProximitySensorManager.getInstance(context).history) {
                    // temp.add(pd.proximity);
                    //}
                    inPocketDetectionHelper.proximityValues = new ArrayList<>(temp);

                    temp = new ArrayList<>();
                    temp.add(LightSensorManager.getInstance(context).history.get(LightSensorManager.getInstance(context).history.size() - 1).illuminance);
                    //for (LightSensorData pd : LightSensorManager.getInstance(context).history) {
                    // temp.add(pd.illuminance);
                    // }
                    inPocketDetectionHelper.lightValues = new ArrayList<>(temp);
                    if (Settings.SAVE_POCKET_TO_DATABASE)
                        MySQLiteHelper.getInstance(context).addToPocket(inPocketDetectionHelper.getDetectionResult(), System.currentTimeMillis());
                    Log.i(TAG, "PocketDetectionResult: " + inPocketDetectionHelper.getDetectionResult().toString());
                    lastTimeCheckedHistory = System.currentTimeMillis();
                    ProximitySensorManager.getInstance(context).history.clear();
                    LightSensorManager.getInstance(context).history.clear();
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
            LightSensorData sensorData = new LightSensorData();
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
    public void removeAllDataFromDatabase(){
        removeDataFromDatabaseWithLimit(-1);
    }

    @Override
    public void removeDataFromDatabaseWithRange(long start, long end) {
        String dbName = "light";
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        Log.i(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        database.execSQL("DELETE FROM "+dbName+" where timestamp >=" + start + " and timestamp <=" + end);
        Log.i(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }

    @Override
    public void removeDataFromDatabaseWithLimit(int limit) {
        String dbName = "light";
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        Log.i(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        if (limit == -1)
            database.execSQL("DELETE FROM "+ dbName);
        else
            database.execSQL("DELETE FROM "+dbName+" WHERE id IN(SELECT id FROM "+dbName+" ORDER BY id ASC LIMIT " + limit + ")");

        Log.i(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }
}

