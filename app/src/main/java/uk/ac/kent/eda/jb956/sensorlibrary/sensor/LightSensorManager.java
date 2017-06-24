package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;
import uk.ac.kent.eda.jb956.sensorlibrary.data.LightSensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.ProximitySensorData;
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
    public SensorData getSingleEntry() {
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
            if (Settings.LIGHT_ENABLED) {
                androidSensorManager.registerListener(this, getSensor(), SensorManager.SENSOR_DELAY_NORMAL, uk.ac.kent.eda.jb956.sensorlibrary.SensorManager.getInstance(context).getmSensorHandler());
                sensing = true;
            } else
                Log.i(TAG, "LIGHT_ENABLED=false, ignoring light collection");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, !isSensing() ? TAG + " not started: Disabled" : TAG + " started");
        //startRepeatingTask();
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
        //stopRepeatingTask();
    }

    private boolean sensing = false;

    @Override
    public boolean isSensing() {
        return sensing;
    }

    private long lastUpdate = 0;
    private SensorData lastEntry = null;
    //public List<LightSensorData> history = new ArrayList<>();
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
                    //history.add(ld);
                    MySQLiteHelper.getInstance(context).addToLight(ld);
                   // Log.i(TAG, "Lx: " + lx);
                    List<LightSensorData> temp = new ArrayList<>();
                    //for (LightSensorData data : history) {
                        //if (data.timestamp > (System.currentTimeMillis() - 4000))
                           //temp.add(data);
                   // }
                    //history = new ArrayList<>(temp);
                }
            }
        }
    }

    /*private Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                if ((System.currentTimeMillis()) - lastTimeCheckedHistory > 4000) {
                    //System.out.println("4 secs");
                    InPocketDetectionManager inPocketDetectionManager = InPocketDetectionManager.getInstance(context);
                    List<Double> temp = new ArrayList<>();
                    for (ProximitySensorData pd : ProximitySensorManager.getInstance(context).history) {
                        temp.add(pd.proximity);
                    }
                    inPocketDetectionManager.proximityValues = new ArrayList<>(temp);

                    temp = new ArrayList<>();
                    for (LightSensorData pd : LightSensorManager.getInstance(context).history) {
                        temp.add(pd.illuminance);
                    }
                    inPocketDetectionManager.lightValues = new ArrayList<>(temp);
                    System.out.println(inPocketDetectionManager.getDetectionResult());
                    lastTimeCheckedHistory = System.currentTimeMillis();
                }
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mStatusChecker, 4000);
            }
        }
    };*/

   // private void startRepeatingTask() {
       // mStatusChecker.run();
    //}

    //private void stopRepeatingTask() {
      //  mHandler.removeCallbacks(mStatusChecker);
   // }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}

