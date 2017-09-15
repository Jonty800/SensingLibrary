package uk.ac.kent.eda.jb956.sensorlibrary;

import android.content.Context;
import android.util.Log;

import uk.ac.kent.eda.jb956.sensorlibrary.callback.SensingEvent;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorConfig;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;
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

    public DutyCyclingManager(Context context, int sensorId, SensorConfig config) {
        this.context = context;
        this.config = config;
        this.sensorId = sensorId;
    }

    private void sleep() {
        sleeping = true;
        try {
            SensingEvent.getInstance().onSensingPaused(sensorId);
            onSleep();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(String.valueOf(sensorId), "Sensor paused");
    }

    private void wake() {
        sleeping = false;
        try {
            Log.i(String.valueOf(sensorId), "Resuming sensor");
            SensingEvent.getInstance().onSensingResumed(sensorId);
            onWake();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run(){
        startDutyCycling();
    }

    private boolean sleepingTaskStarted = false;

    private void startDutyCycling() {
        if (sleepingTaskStarted)
            return;
        SensorManager.getInstance(context).getWorkerThread().postDelayedTask(processDutyCyclingTask(), getAwakeWindowSize());
        sleepingTaskStarted = true;
    }

    private int getAwakeWindowSize(){
        return config.AWAKE_WINDOW_SIZE;
    }
    private int getSleepWindowSize(){
        return config.SLEEP_WINDOW_SIZE;
    }

    private Runnable processDutyCyclingTask() {
        return new Runnable() {
            @Override
            public void run() {
                if (!sleeping) {
                    Log.i(String.valueOf(sensorId), "Sleeping for " + getSleepWindowSize());
                    sleep();
                    SensorManager.getInstance(context).getWorkerThread().postDelayedTask(processDutyCyclingTask(), getSleepWindowSize());
                } else {
                    Log.i(String.valueOf(sensorId),"Sensing for " + getAwakeWindowSize());
                    wake();
                    SensorManager.getInstance(context).getWorkerThread().postDelayedTask(processDutyCyclingTask(), getAwakeWindowSize());
                }
            }
        };
    }

    DutyCyclingEventListener dutyCyclingEventListener;
    private void onWake() {
        if (dutyCyclingEventListener != null)
            dutyCyclingEventListener.onWake();
    }

    public synchronized DutyCyclingManager subscribeToListener(DutyCyclingEventListener listener) {
        dutyCyclingEventListener = listener;
        return this;
    }

    private void onSleep() {
        if (dutyCyclingEventListener != null)
            dutyCyclingEventListener.onSleep();
    }

    public interface DutyCyclingEventListener {
        void onWake();

        void onSleep();
    }
}
