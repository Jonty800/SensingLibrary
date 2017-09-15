package uk.ac.kent.eda.jb956.sensorlibrary;

import android.content.Context;
import android.util.Log;

import uk.ac.kent.eda.jb956.sensorlibrary.callback.SensingEvent;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorConfig;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.BaseSensor;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.WifiSensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.util.SensorUtils;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class DutyCyclingManager {

    private boolean sleeping = false;
    private int sensorId;
    private Context context;
    private SensorConfig config;
    private BaseSensor baseSensor;

    public DutyCyclingManager(BaseSensor baseSensor, Context context, int sensorId, SensorConfig config) {
        this.baseSensor = baseSensor;
        this.context = context;
        this.config = config;
        this.sensorId = sensorId;
    }

    private void sleep() {
        sleeping = true;
        try {
            baseSensor.sleep();
            SensingEvent.getInstance().onSensingPaused(sensorId);
            stopSensingTask();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(String.valueOf(sensorId), "Sensor paused");
    }

    private void wake() {
        sleeping = false;
        try {
            Log.i(String.valueOf(sensorId), "Resuming sensor");
            startSleepingTask();
            baseSensor.wake();
            SensingEvent.getInstance().onSensingResumed(sensorId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopSensingTask() {
        SensorManager.getInstance(context).getWorkerThread().removeDelayedTask(task);
    }

    private Runnable task = null;
    public void run(Runnable r){
        this.task = r;
        begin();
    }

    private void begin(){
        startSleepingTask();
    }

    private boolean sleepingTaskStarted = false;

    private void startSleepingTask() {
        if (sleepingTaskStarted)
            return;
        SensorManager.getInstance(context).getWorkerThread().postDelayedTask(getSleepTask(), getAwakeWindowSize());
        sleepingTaskStarted = true;
    }

    private int getAwakeWindowSize(){
        return config.AWAKE_WINDOW_SIZE;
    }
    private int getSleepWindowSize(){
        return config.SLEEP_WINDOW_SIZE;
    }

    private Runnable getSleepTask() {
        return new Runnable() {
            @Override
            public void run() {
                if (!sleeping) {
                    Log.i(String.valueOf(sensorId), "Sleeping for " + getSleepWindowSize());
                    sleep();
                    SensorManager.getInstance(context).getWorkerThread().postDelayedTask(getSleepTask(), getSleepWindowSize());
                } else {
                    Log.i(String.valueOf(sensorId),"Sensing for " + getAwakeWindowSize());
                    wake();
                    SensorManager.getInstance(context).getWorkerThread().postDelayedTask(getSleepTask(), getAwakeWindowSize());
                }
            }
        };
    }
}
