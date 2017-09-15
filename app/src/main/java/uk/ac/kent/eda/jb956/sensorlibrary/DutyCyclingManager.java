package uk.ac.kent.eda.jb956.sensorlibrary;

import android.content.Context;
import android.util.Log;

import uk.ac.kent.eda.jb956.sensorlibrary.callback.SensingEvent;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorConfig;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class DutyCyclingManager {

    public boolean isSleeping() {
        return sleeping;
    }

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
    }

    private void wake() {
        sleeping = false;
        try {
            SensingEvent.getInstance().onSensingResumed(sensorId);
            onWake();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run(){
        startDutyCycling();
    }

    public void updateSensorConfig(SensorConfig config){
        this.config = config;
    }

    public void stop(){
        SensorManager.getInstance(context).getWorkerThread().removeDelayedTask(dutyCyclingTask);
        sleepingTaskStarted = false;
        sleeping = false;
    }

    private boolean sleepingTaskStarted = false;

    private void startDutyCycling() {
        if (sleepingTaskStarted)
            return;
        SensorManager.getInstance(context).getWorkerThread().postDelayedTask(dutyCyclingTask, getAwakeWindowSize());
        sleepingTaskStarted = true;
    }

    private int getAwakeWindowSize(){
        return config.AWAKE_WINDOW_SIZE;
    }
    private int getSleepWindowSize(){
        return config.SLEEP_WINDOW_SIZE;
    }

    private Runnable dutyCyclingTask = new Runnable() {
        @Override
        public void run() {
            if (!sleeping) {
                Log.i(String.valueOf(sensorId), "Sleeping for " + getSleepWindowSize());
                sleep();
                SensorManager.getInstance(context).getWorkerThread().postDelayedTask(this, getSleepWindowSize());
            } else {
                Log.i(String.valueOf(sensorId),"Sensing for " + getAwakeWindowSize());
                wake();
                SensorManager.getInstance(context).getWorkerThread().postDelayedTask(this, getAwakeWindowSize());
            }
        }
    };

    private DutyCyclingEventListener dutyCyclingEventListener;
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
